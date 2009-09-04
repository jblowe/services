/**   
 * This document is a part of the source code and related artifacts
 * for CollectionSpace, an open source collections management system
 * for museums and related institutions:
 *
 * http://www.collectionspace.org
 * http://wiki.collectionspace.org
 *
 * Copyright © 2009 Regents of the University of California
 *
 * Licensed under the Educational Community License (ECL), Version 2.0.
 * You may not use this file except in compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at
 * https://source.collectionspace.org/collection-space/LICENSE.txt
 */

// @TODO: Add Javadoc comments

// @TODO: Need to set and enforce maximum String length.

package org.collectionspace.services.id;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * StringIDGeneratorPart
 *
 * Generates identifiers (IDs) that consist of a static String value.
 *
 * $LastChangedRevision$
 * $LastChangedDate$
 */
public class StringIDGeneratorPart implements IDGeneratorPart {
    
    private String initialValue = null;
    private String currentValue = null;
    
    public StringIDGeneratorPart(String initialValue)
        throws IllegalArgumentException {

        if (initialValue == null || initialValue.equals("")) {
            throw new IllegalArgumentException(
                "Initial ID value must not be null or empty");
        }
        
        this.initialValue = initialValue;
        this.currentValue = initialValue;

    }

    public String getInitialID() {
        return this.initialValue;
    }

    public String getCurrentID() {
        return this.currentValue;
    }

    public void setCurrentID(String value) throws IllegalArgumentException {
        if (value == null || value.equals("")) {
            throw new IllegalArgumentException(
            "ID value must not be null or empty");
        }
        this.currentValue = value;
    }
    
    public void resetID() {
        // Do nothing
    }

    public String nextID() {
        return this.currentValue;
  }

    public boolean isValidID(String value) {

        if (value == null || value.equals("")) {
            return false;
        }

        Pattern pattern = Pattern.compile(getRegex());
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            return true;
        } else {
            return false;
        }
        
    }

    public String getRegex() {

        String initial = this.initialValue;
        
        // Escape or otherwise modify various characters that have
        // significance in regular expressions.
        //
        // @TODO Test these thoroughly, add processing of more
        // special characters as needed.
        
        // Escape un-escaped period/full stop characters.
        Pattern pattern = Pattern.compile("([^\\\\]{0,1})\\.");
        Matcher matcher = pattern.matcher(initial);
        String escapedInitial = matcher.replaceAll("$1\\\\.");

        String regex = "(" + escapedInitial + ")";
        return regex;
    }
    
}
