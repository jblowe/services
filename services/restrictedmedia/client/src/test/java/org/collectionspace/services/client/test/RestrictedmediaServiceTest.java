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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.collectionspace.services.client.test;

import java.io.File;
import java.net.URL;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.collectionspace.services.client.CollectionSpaceClient;
import org.collectionspace.services.client.RestrictedmediaClient;
import org.collectionspace.services.client.PayloadOutputPart;
import org.collectionspace.services.client.PoxPayloadOut;
import org.collectionspace.services.jaxb.AbstractCommonList;
import org.collectionspace.services.restrictedmedia.LanguageList;
import org.collectionspace.services.restrictedmedia.RestrictedmediaCommon;
import org.collectionspace.services.restrictedmedia.SubjectList;

import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.jboss.resteasy.plugins.providers.multipart.OutputPart;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RestrictedmediaServiceTest, carries out tests against a deployed and running Restrictedmedia Service. <p/>
 * $LastChangedRevision:  $
 * $LastChangedDate:  $
 */
public class RestrictedmediaServiceTest extends AbstractPoxServiceTestImpl<AbstractCommonList, RestrictedmediaCommon> {

    private final String CLASS_NAME = RestrictedmediaServiceTest.class.getName();
    private final Logger logger = LoggerFactory.getLogger(RestrictedmediaServiceTest.class);
    private final static String PUBLIC_URL_DECK = "http://farm8.staticflickr.com/7231/6962564226_4bdfc17599_k_d.jpg";

    private boolean restrictedmediaCleanup = true;
    
    /**
     * Sets up create tests.
     */
    @Override
	protected void setupCreate() {
        super.setupCreate();
        String noRestrictedmediaCleanup = System.getProperty(NO_MEDIA_CLEANUP);
    	if(Boolean.TRUE.toString().equalsIgnoreCase(noRestrictedmediaCleanup)) {
    		//
    		// Don't delete the blobs that we created during the test cycle
    		//
            this.restrictedmediaCleanup = false;
    	}
    }
    
    private boolean isRestrictedmediaCleanup() {
    	return restrictedmediaCleanup;
    }

    
    @Override
	public String getServicePathComponent() {
		return RestrictedmediaClient.SERVICE_PATH_COMPONENT;
	}

	@Override
	protected String getServiceName() {
		return RestrictedmediaClient.SERVICE_NAME;
	}
    
    @Override
    protected CollectionSpaceClient getClientInstance() {
        return new RestrictedmediaClient();
    }

    @Override
    protected AbstractCommonList getCommonList(ClientResponse<AbstractCommonList> response) {
        return response.getEntity(AbstractCommonList.class);
    }

    /**
     * Looks in the .../src/test/resources/blobs directory for files from which to create Blob
     * instances.
     *
     * @param testName the test name
     * @param fromUri - if 'true' then send the service a URI from which to create the blob.
     * @param fromUri - if 'false' then send the service a multipart/form-data POST from which to create the blob.
     * @throws Exception the exception
     */    
	public void createBlob(String testName, boolean fromUri) throws Exception {
		setupCreate();
		//
		// First create a restricted media record
		//
		RestrictedmediaClient client = new RestrictedmediaClient();
		PoxPayloadOut multipart = createRestrictedmediaInstance(createIdentifier());
		ClientResponse<Response> restrictedmediaRes = client.create(multipart);
		String restrictedmediaCsid = null;
		try {
			assertStatusCode(restrictedmediaRes, testName);
			restrictedmediaCsid = extractId(restrictedmediaRes);
		} finally {
			if (restrictedmediaRes != null) {
				restrictedmediaRes.releaseConnection();
			}
		}
		//
		// Next, create a blob record to associate with the restricted media record
		// FIXME: REM - 1/2012, This method is too large.  Break it up.  The code below
		// could be put into a utility class that could also be used by the blob service tests.
		//
		String currentDir = this.getResourceDir();
		String blobsDirPath = currentDir + File.separator + BLOBS_DIR;
		File blobsDir = new File(blobsDirPath);
		if (blobsDir != null && blobsDir.exists()) {
			File[] children = blobsDir.listFiles();
			if (children != null && children.length > 0) {
				File blobFile = null;
				//
				// Since Restrictedmedia records can have only a single associated
				// blob, we'll stop after we find a valid candidate.
				//
				for (File child : children) {
					if (isBlobbable(child) == true) {
						blobFile = child;
						break;
					}
				}
				//
				// If we found a good blob candidate file, then try to
				// create the blob record
				//
				if (blobFile != null) {
					client = new RestrictedmediaClient();
					ClientResponse<Response> res = null;
					String mimeType = this.getMimeType(blobFile);
					logger.debug("Processing file URI: " + blobFile.getAbsolutePath());
					logger.debug("MIME type is: " + mimeType);
					if (fromUri == true) {
						URL childUrl = blobFile.toURI().toURL();
						res = client.createBlobFromUri(restrictedmediaCsid,
								childUrl.toString());
					} else {
						MultipartFormDataOutput formData = new MultipartFormDataOutput();
						OutputPart outputPart = formData.addFormData("file",
								blobFile, MediaType.valueOf(mimeType));
						res = client
								.createBlobFromFormData(restrictedmediaCsid, formData);
					}
					try {
						assertStatusCode(res, testName);
						String blobCsid = extractId(res);
						if (isRestrictedmediaCleanup() == true) {
							allResourceIdsCreated.add(blobCsid);
							allResourceIdsCreated.add(restrictedmediaCsid);
						}
					} finally {
						if (res != null) {
							res.releaseConnection();
						}
					}
				} else {
					logger.debug("Directory: " + blobsDirPath
							+ " contains no readable files.");
				}
			} else {
				logger.debug("Directory: " + blobsDirPath
						+ " is empty or cannot be read.");
			}
		} else {
			logger.debug("Directory: " + blobsDirPath
					+ " is missing or cannot be read.");
		}
	}
    
    @Test(dataProvider = "testName", 
    		dependsOnMethods = {"CRUDTests"})
    public void createWithBlobUri(String testName) throws Exception {
        createBlob(testName, true /*with URI*/);
    }
    
    @Test(dataProvider = "testName", 
    		dependsOnMethods = {"createWithBlobUri"})
    public void createRestrictedmediaAndBlobWithUri(String testName) throws Exception {
		RestrictedmediaClient client = new RestrictedmediaClient();
		PoxPayloadOut multipart = createRestrictedmediaInstance(createIdentifier());
		ClientResponse<Response> restrictedmediaRes = client.createRestrictedmediaAndBlobWithUri(multipart, PUBLIC_URL_DECK, true); // purge the original
		String restrictedmediaCsid = null;
		try {
			assertStatusCode(restrictedmediaRes, testName);
			restrictedmediaCsid = extractId(restrictedmediaRes);
//			allResourceIdsCreated.add(restrictedmediaCsid); // Re-enable this and also add code to delete the associated blob
		} finally {
			if (restrictedmediaRes != null) {
				restrictedmediaRes.releaseConnection();
			}
		}
    }
    
    
    @Test(dataProvider = "testName", 
    		dependsOnMethods = {"createWithBlobUri"})
    public void createWithBlobPost(String testName) throws Exception {
        createBlob(testName, false /*with POST*/);
    }
    
//    @Test(dataProvider = "testName", dataProviderClass = AbstractServiceTestImpl.class, dependsOnMethods = {"update"})
//    public void updateWithBlob(String testName) throws Exception {
//        logger.debug(testBanner(testName, CLASS_NAME));
//        setupCreate();
//        RestrictedmediaClient client = new RestrictedmediaClient();
//        PoxPayloadOut multipart = createRestrictedmediaInstance(createIdentifier());
//        ClientResponse<Response> res = client.create(multipart);
//        assertStatusCode(res, testName);
//        String csid = extractId(res);
//        
//        
//        allResourceIdsCreated.add(extractId(res)); // Store the IDs from every resource created by tests so they can be deleted after tests have been run.
//    }

    // ---------------------------------------------------------------
    // Utility tests : tests of code used in tests above
    // ---------------------------------------------------------------

    @Override
    protected PoxPayloadOut createInstance(String identifier) {
    	return createRestrictedmediaInstance(identifier);
    }
    
    // ---------------------------------------------------------------
    // Utility methods used by tests above
    // ---------------------------------------------------------------
    private PoxPayloadOut createRestrictedmediaInstance(String title) {
        String identifier = "restrictedmedia.title-" + title;
        RestrictedmediaCommon restrictedmedia = new RestrictedmediaCommon();
        restrictedmedia.setTitle(identifier);
        restrictedmedia.setContributor("Joe-bob briggs");
        restrictedmedia.setCoverage("Lots of stuff");
        restrictedmedia.setPublisher("Ludicrum Enterprises");
        SubjectList subjects = new SubjectList();
        List<String> subjList = subjects.getSubject();
        subjList.add("Pints of blood");
        subjList.add("Much skin");
        restrictedmedia.setSubjectList(subjects);
        LanguageList languages = new LanguageList();
        List<String> langList = languages.getLanguage();
        langList.add("English");
        langList.add("German");
        restrictedmedia.setLanguageList(languages);
        PoxPayloadOut multipart = new PoxPayloadOut(RestrictedmediaClient.SERVICE_PAYLOAD_NAME);
        PayloadOutputPart commonPart = multipart.addPart(restrictedmedia, MediaType.APPLICATION_XML_TYPE);
        commonPart.setLabel(new RestrictedmediaClient().getCommonPartName());

        if (logger.isDebugEnabled()) {
            logger.debug("to be created, restricted media common");
            logger.debug(objectAsXmlString(restrictedmedia, RestrictedmediaCommon.class));
        }

        return multipart;
    }

	@Override
	protected PoxPayloadOut createInstance(String commonPartName,
			String identifier) {
		return createRestrictedmediaInstance(identifier);
	}

	@Override
	protected RestrictedmediaCommon updateInstance(final RestrictedmediaCommon original) {
		RestrictedmediaCommon result = new RestrictedmediaCommon();
		
		result.setTitle("updated-" + original.getTitle());

		return result;
	}

	@Override
	protected void compareUpdatedInstances(RestrictedmediaCommon original,
			RestrictedmediaCommon updated) throws Exception {
		Assert.assertEquals(updated.getTitle(), original.getTitle());
	}

	@Override
    @Test(dataProvider = "testName",
	dependsOnMethods = {
		"org.collectionspace.services.client.test.AbstractServiceTestImpl.baseCRUDTests"})	
	public void CRUDTests(String testName) {
		// TODO Auto-generated method stub
		
	}
}