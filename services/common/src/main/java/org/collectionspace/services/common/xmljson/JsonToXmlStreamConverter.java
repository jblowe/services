package org.collectionspace.services.common.xmljson;

import static org.collectionspace.services.common.xmljson.ConversionUtils.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * <p>Converts a CSpace JSON payload to an XML payload.</p>
 *
 * <p>This class is not intended to serve as a general purpose JSON to XML
 * translator. It is instead a lightweight processor tuned for the kinds
 * of JSON generated by CSpace, and the particular transformations needed
 * to generate XML for CSpace.</p>
 *
 * <p>
 * The conversion is performed as follows:
 * <ul>
 * <li>JSON fields starting with "@xmlns:" are converted XML namespace declarations.</li>
 * <li>JSON fields starting with "@" are converted to XML attributes.</li>
 * <li>Other JSON fields are converted to identically-named XML elements.</li>
 * <li>The contents of JSON objects are converted to XML child elements.</li>
 * <li>The contents of JSON arrays are expanded into multiple XML elements, each
 *     named with the field name of the JSON array.</li>
 * </ul>
 * </p>
 *
 * <p>This implementation is schema-unaware. It operates by examining only the input
 * document, without utilizing any XML schema information.</p>
 *
 * <p>Example:</p>
 *
 * <p>
 * JSON
 * <pre>
 * {
 *   "document": {
 *     "@name": "collectionobjects",
 *     "ns2:collectionobjects_common": {
 *       "@xmlns:ns2": "http://collectionspace.org/services/collectionobject",
 *       "@xmlns:xsi": "http://www.w3.org/2001/XMLSchema-instance",
 *       "objectNumber": "2016.1.1",
 *       "objectNameList": {
 *         "objectNameGroup": [
 *           {
 *             "objectNameCurrency": null,
 *             "objectNameLanguage": null,
 *             "objectName": "Object name",
 *             "objectNameSystem": null,
 *             "objectNameType": null,
 *             "objectNameNote": null,
 *             "objectNameLevel": null
 *           },
 *           {
 *             "objectNameCurrency": null,
 *             "objectNameLanguage": null,
 *             "objectName": "Another name",
 *             "objectNameSystem": null,
 *             "objectNameType": null,
 *             "objectNameNote": null,
 *             "objectNameLevel": null
 *           }
 *         ]
 *       },
 *       "comments": {
 *         "comment": [
 *           "Some comment text",
 *           "Another comment"
 *         ]
 *       }
 *     }
 *   }
 * }
 * </pre>
 * </p>
 *
 * <p>
 * XML
 * <pre>
 * &lt;document name="collectionobjects"&gt;
 *   &lt;ns2:collectionobjects_common xmlns:ns2="http://collectionspace.org/services/collectionobject" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"&gt;
 *     &lt;objectNumber&gt;2016.1.1&lt;/objectNumber&gt;
 *     &lt;objectNameList&gt;
 *       &lt;objectNameGroup&gt;
 *         &lt;objectNameCurrency/&gt;
 *         &lt;objectNameLanguage/&gt;
 *         &lt;objectName&gt;Object name&lt;/objectName&gt;
 *         &lt;objectNameSystem/&gt;
 *         &lt;objectNameType/&gt;
 *         &lt;objectNameNote/&gt;
 *         &lt;objectNameLevel/&gt;
 *       &lt;/objectNameGroup&gt;
 *       &lt;objectNameGroup&gt;
 *         &lt;objectNameCurrency/&gt;
 *         &lt;objectNameLanguage/&gt;
 *         &lt;objectName&gt;Another name&lt;/objectName&gt;
 *         &lt;objectNameSystem/&gt;
 *         &lt;objectNameType/&gt;
 *         &lt;objectNameNote/&gt;
 *         &lt;objectNameLevel/&gt;
 *       &lt;/objectNameGroup&gt;
 *     &lt;/objectNameList&gt;
 *     &lt;comments&gt;
 *       &lt;comment&gt;Some comment text&lt;/comment&gt;
 *       &lt;comment&gt;Another comment&lt;/comment&gt;
 *     &lt;/comments&gt;
 *   &lt;/ns2:collectionobjects_common&gt;
 * &lt;/document&gt;
 * </pre>
 * </p>
 *
 * <p>This implementation uses a streaming JSON parser and a streaming
 * XML writer to do a direct stream-to-stream conversion, without
 * building a complete in-memory representation of the document.</p>
 */
public class JsonToXmlStreamConverter {

    /**
     * The JSON parser used to parse the input stream.
     */
    protected JsonParser jsonParser;

    /**
     * The XML writer used to write to the output stream.
     */
    protected XMLStreamWriter xmlWriter;

    /**
     * A stack used to track the state of JSON parsing.
     * JsonField instances are pushed onto the stack as fields
     * are entered, and popped off as fields are exited.
     * References to fields are not retained once they
     * are popped from the stack, so there is never a full
     * representation of the JSON document in memory.
     */
    protected Stack<JsonField> stack = new Stack<JsonField>();

    /**
     * Creates an JsonToXmlStreamConverter that reads JSON from an input stream,
     * and writes XML to an output stream.
     *
     * @param in the JSON input stream
     * @param out the XML output stream
     * @throws JsonParseException
     * @throws IOException
     * @throws XMLStreamException
     */
    public JsonToXmlStreamConverter(InputStream in, OutputStream out) throws JsonParseException, IOException, XMLStreamException {
        JsonFactory jsonFactory = new JsonFactory();
        XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();

        jsonParser = jsonFactory.createParser(in);
        xmlWriter = xmlFactory.createXMLStreamWriter(out);
    }

    /**
     * Performs the conversion.
     *
     * @throws JsonParseException
     * @throws IOException
     * @throws XMLStreamException
     */
    public void convert() throws JsonParseException, IOException, XMLStreamException {
        xmlWriter.writeStartDocument();

        // Read tokens from the input stream, and dispatch to handlers.
        // Handlers may write XML to the output stream.

        while (jsonParser.nextToken() != null) {
            JsonToken token = jsonParser.currentToken();

            switch(token) {
                case FIELD_NAME:
                    onFieldName(jsonParser.getText());
                    break;
                case VALUE_STRING:
                    onScalar(jsonParser.getText());
                    break;
                case VALUE_NULL:
                    onScalar("");
                    break;
                case START_OBJECT:
                    onStartObject();
                    break;
                case END_OBJECT:
                    onEndObject();
                    break;
                case START_ARRAY:
                    onStartArray();
                    break;
                case END_ARRAY:
                    onEndArray();
                    break;
                case VALUE_TRUE:
                    onScalar("true");
                    break;
                case VALUE_FALSE:
                    onScalar("false");
                    break;
                case VALUE_NUMBER_INT:
                    onScalar(jsonParser.getValueAsString());
                    break;
                case VALUE_NUMBER_FLOAT:
                    onScalar(jsonParser.getValueAsString());
                    break;
                default:
            }
        }

        xmlWriter.writeEndDocument();
    }

    /**
     * Event handler executed when a field name is encountered
     * in the input stream.
     *
     * @param name the field name
     */
    public void onFieldName(String name) {
        // Push the field onto the stack.

        stack.push(new JsonField(name));
    }

    /**
     * Event handler executed when a scalar field value is encountered
     * in the input stream. Boolean, integer, float, and null values are
     * converted to strings prior to being passed in.
     *
     * @param value the scalar value, as a string
     * @throws XMLStreamException
     * @throws IOException
     */
    public void onScalar(String value) throws XMLStreamException, IOException {
        JsonField field = stack.peek();
        String name = field.getName();

        if (field.isScalar() && isXmlAttribute(name)) {
            // We're in a scalar field whose name looks like an XML attribute
            // or namespace declaration.

            if (isXmlNamespace(name)) {
                // It looks like a namespace declaration.
                // Output an XML namespace declaration.

                String prefix = jsonFieldNameToXmlNamespacePrefix(name);
                String namespaceUri = value;

                xmlWriter.writeNamespace(prefix, namespaceUri);
            }
            else {
                // It looks like an attribute.
                // Output an XML attribute.

                String localName = jsonFieldNameToXmlAttributeName(name);

                xmlWriter.writeAttribute(localName, value);
            }
        }
        else {
            // It doesn't look like an XML attribute or namespace declaration.
            // Output an XML element with the same name as the field, whose
            // contents are the value.

            xmlWriter.writeStartElement(name);
            xmlWriter.writeCharacters(value);
            xmlWriter.writeEndElement();
        }

        if (!field.isArray()) {
            // If the field we're in is not an array, we're done with it.
            // Pop it off the stack.

            // If it is an array, there may be more values to come. The
            // field shouldn't be popped until the end of array is
            // found.

            stack.pop();
        }
    }

    /**
     * Event handler executed when an object start ({) is encountered
     * in the input stream.
     *
     * @throws XMLStreamException
     */
    public void onStartObject() throws XMLStreamException {
        if (stack.isEmpty()) {
            // This is the root object. Do nothing.

            return;
        }

        JsonField field = stack.peek();

        if (field.isArray()) {
            // If we're in an array, an object should be expanded
            // into a field with the same name as the array.

            field = new JsonField(field.getName());
            stack.push(field);
        }

        field.setType(JsonField.Type.OBJECT);

        // Write an XML start tag to the output stream.

        xmlWriter.writeStartElement(field.getName());
    }

    /**
     * Event handler executed when an object end (}) is encountered
     * in the input stream.
     *
     * @throws XMLStreamException
     */
    public void onEndObject() throws XMLStreamException {
        if (stack.isEmpty()) {
            // This is the root object. Do nothing.

            return;
        }

        // Pop the current field off the stack.

        stack.pop();

        // Write an XML end tag to the output stream.

        xmlWriter.writeEndElement();
    }

    /**
     * Event handler executed when an array start ([) is encountered
     * in the input stream.
     *
     * @throws IOException
     * @throws XMLStreamException
     */
    public void onStartArray() throws IOException, XMLStreamException {
        // Set the current field type to array.

        JsonField field = stack.peek();

        field.setType(JsonField.Type.ARRAY);
    }

    /**
     * Event handler executed when an array end (]) is encountered
     * in the input stream.
     *
     * @throws XMLStreamException
     */
    public void onEndArray() throws XMLStreamException {
       // Pop the current field off the stack.

       stack.pop();
    }
}
