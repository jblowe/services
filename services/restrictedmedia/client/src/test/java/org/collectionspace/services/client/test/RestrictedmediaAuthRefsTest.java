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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.collectionspace.services.PersonJAXBSchema;
import org.collectionspace.services.client.CollectionSpaceClient;
import org.collectionspace.services.client.RestrictedmediaClient;
import org.collectionspace.services.client.PayloadOutputPart;
import org.collectionspace.services.client.PersonAuthorityClient;
import org.collectionspace.services.client.PersonAuthorityClientUtils;
import org.collectionspace.services.client.PoxPayloadIn;
import org.collectionspace.services.client.PoxPayloadOut;
import org.collectionspace.services.common.authorityref.AuthorityRefList;
import org.collectionspace.services.jaxb.AbstractCommonList;
import org.collectionspace.services.restrictedmedia.RestrictedmediaCommon;
import org.collectionspace.services.person.PersonTermGroup;

import org.jboss.resteasy.client.ClientResponse;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RestrictedmediaAuthRefsTest, carries out Authority References tests against a deployed and running Restrictedmedia Service.
 * $LastChangedRevision:  $
 * $LastChangedDate:  $
 */
public class RestrictedmediaAuthRefsTest extends BaseServiceTest<AbstractCommonList> {
    private final String CLASS_NAME = RestrictedmediaAuthRefsTest.class.getName();
    private final Logger logger = LoggerFactory.getLogger(RestrictedmediaAuthRefsTest.class);
    final String PERSON_AUTHORITY_NAME = "RestrictedmediaPersonAuth";
    private String knownResourceId = null;
    private List<String> restrictedmediaIdsCreated = new ArrayList<String>();
    private List<String> personIdsCreated = new ArrayList<String>();
    private String personAuthCSID = null;
    private String depositorRefName = null;
    private String title = null;

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
        throw new UnsupportedOperationException(); //method not supported (or needed) in this test class
    }

    @Override
    protected AbstractCommonList getCommonList(ClientResponse<AbstractCommonList> response) {
        throw new UnsupportedOperationException(); //method not supported (or needed) in this test class
    }

    private PoxPayloadOut createRestrictedmediaInstance(String depositorRefName, String title) {
        this.title = title;
        this.depositorRefName = depositorRefName;
        RestrictedmediaCommon restrictedmedia = new RestrictedmediaCommon();
        restrictedmedia.setTitle(title);

        PoxPayloadOut multipart = new PoxPayloadOut(RestrictedmediaClient.SERVICE_PAYLOAD_NAME);
        PayloadOutputPart commonPart = multipart.addPart(new RestrictedmediaClient().getCommonPartName(), restrictedmedia);
        logger.debug("to be created, restricted media common: " + objectAsXmlString(restrictedmedia, RestrictedmediaCommon.class));
        return multipart;
    }

    @Test(dataProvider = "testName")
    public void createWithAuthRefs(String testName) throws Exception {
        testSetup(STATUS_CREATED, ServiceRequestType.CREATE);
        String identifier = createIdentifier(); // Submit the request to the service and store the response.
        createPersonRefs();// Create all the person refs and entities
        // Create a new Loans In resource. One or more fields in this resource will be PersonAuthority
        //    references, and will refer to Person resources by their refNames.
        RestrictedmediaClient restrictedmediaClient = new RestrictedmediaClient();
        PoxPayloadOut multipart = createRestrictedmediaInstance(depositorRefName, "restrictedmedia.title-" + identifier);
        ClientResponse<Response> res = restrictedmediaClient.create(multipart);
        try {
	        assertStatusCode(res, testName);
	        if (knownResourceId == null) {// Store the ID returned from the first resource created for additional tests below.
	            knownResourceId = extractId(res);
	        }
	        restrictedmediaIdsCreated.add(extractId(res));// Store the IDs from every resource created; delete on cleanup
        } finally {
        	if (res != null) {
                res.releaseConnection();
            }
        }
    }

    protected void createPersonRefs() {
        PersonAuthorityClient personAuthClient = new PersonAuthorityClient();
        // Create a temporary PersonAuthority resource, and its corresponding refName by which it can be identified.
        PoxPayloadOut multipart = PersonAuthorityClientUtils.createPersonAuthorityInstance(
        		PERSON_AUTHORITY_NAME, PERSON_AUTHORITY_NAME, personAuthClient.getCommonPartName());
        ClientResponse<Response> res = personAuthClient.create(multipart);
        try {
	        assertStatusCode(res, "createPersonRefs (not a surefire test)");
	        personAuthCSID = extractId(res);
        } finally {
        	if (res != null) {
                res.releaseConnection();
            }
        }
        String authRefName = PersonAuthorityClientUtils.getAuthorityRefName(personAuthCSID, null);
        // Create temporary Person resources, and their corresponding refNames by which they can be identified.
        String csid = "";

        csid = createPerson("Owen the Cur", "Owner", "owenCurOwner", authRefName);
        personIdsCreated.add(csid);
        depositorRefName = PersonAuthorityClientUtils.getPersonRefName(personAuthCSID, csid, null);

        csid = createPerson("Davenport", "Depositor", "davenportDepositor", authRefName);
        personIdsCreated.add(csid);
        depositorRefName = PersonAuthorityClientUtils.getPersonRefName(personAuthCSID, csid, null);
    }

    protected String createPerson(String firstName, String surName, String shortId, String authRefName) {
    	String result = null;
    	
        PersonAuthorityClient personAuthClient = new PersonAuthorityClient();
        Map<String, String> personInfo = new HashMap<String, String>();
        personInfo.put(PersonJAXBSchema.FORE_NAME, firstName);
        personInfo.put(PersonJAXBSchema.SUR_NAME, surName);
        personInfo.put(PersonJAXBSchema.SHORT_IDENTIFIER, shortId);
        List<PersonTermGroup> personTerms = new ArrayList<PersonTermGroup>();
        PersonTermGroup term = new PersonTermGroup();
        String termName = firstName + " " + surName;
        term.setTermDisplayName(termName);
        term.setTermName(termName);
        personTerms.add(term);
        PoxPayloadOut multipart = PersonAuthorityClientUtils.createPersonInstance(
        		personAuthCSID, authRefName, personInfo, personTerms, personAuthClient.getItemCommonPartName());
        ClientResponse<Response> res = personAuthClient.createItem(personAuthCSID, multipart);
        try {
	        assertStatusCode(res, "createPerson (not a surefire test)");
	        result = extractId(res);
        } finally {
        	if (res != null) {
                res.releaseConnection();
            }
        }
        
        return result;
    }

    // @Test annotation commented out by Aron 2010-12-02 until Restrictedmedia payload is set to the
    // actual payload - it currently appears to be cloned from another record type - and
    // it's determined that this payload has at least one authority field.
    //
    // When that happens, this test class will also need to be revised accordingly to
    // reflect the actual names and number of authref fields in that payload.
    //
    // @Test(dataProvider = "testName", dataProviderClass = AbstractServiceTestImpl.class, dependsOnMethods = {"createWithAuthRefs"})
    public void readAndCheckAuthRefs(String testName) throws Exception {
        testSetup(STATUS_OK, ServiceRequestType.READ);

        RestrictedmediaClient restrictedmediaClient = new RestrictedmediaClient();
        ClientResponse<String> res = restrictedmediaClient.read(knownResourceId);
        PoxPayloadIn input = null;
        RestrictedmediaCommon restrictedmedia = null;
        try {
	        assertStatusCode(res, testName);
	        input = new PoxPayloadIn(res.getEntity());
	        restrictedmedia = (RestrictedmediaCommon) extractPart(input, restrictedmediaClient.getCommonPartName(), RestrictedmediaCommon.class);
	        Assert.assertNotNull(restrictedmedia);
	        logger.debug(objectAsXmlString(restrictedmedia, RestrictedmediaCommon.class));
        } finally {
        	if (res != null) {
                res.releaseConnection();
            }
        }

        // Check a couple of fields
        Assert.assertEquals(restrictedmedia.getTitle(), title);

        // Get the auth refs and check them
        ClientResponse<AuthorityRefList> res2 = restrictedmediaClient.getAuthorityRefs(knownResourceId);
        AuthorityRefList list = null;
        try {
	        assertStatusCode(res2, testName);
	        list = res2.getEntity();
        } finally {
        	if (res2 != null) {
        		res2.releaseConnection();
            }
        }
        
        List<AuthorityRefList.AuthorityRefItem> items = list.getAuthorityRefItem();
        int numAuthRefsFound = items.size();
        logger.debug("Authority references, found " + numAuthRefsFound);
        //Assert.assertEquals(numAuthRefsFound, NUM_AUTH_REFS_EXPECTED,
        //                    "Did not find all expected authority references! " +
        //                    "Expected " + NUM_AUTH_REFS_EXPECTED + ", found " + numAuthRefsFound);
        if (logger.isDebugEnabled()) {
            int i = 0;
            for (AuthorityRefList.AuthorityRefItem item : items) {
                logger.debug(testName + ": list-item[" + i + "] Field:" + item.getSourceField() + "= " + item.getAuthDisplayName() + item.getItemDisplayName());
                logger.debug(testName + ": list-item[" + i + "] refName=" + item.getRefName());
                logger.debug(testName + ": list-item[" + i + "] URI=" + item.getUri());
                i++;
            }
        }
    }

    /**
     * Deletes all resources created by tests, after all tests have been run.
     * <p/>
     * This cleanup method will always be run, even if one or more tests fail.
     * For this reason, it attempts to remove all resources created
     * at any point during testing, even if some of those resources
     * may be expected to be deleted by certain tests.
     */
    @AfterClass(alwaysRun = true)
    public void cleanUp() {
        String noTest = System.getProperty("noTestCleanup");
        if (Boolean.TRUE.toString().equalsIgnoreCase(noTest)) {
            logger.debug("Skipping Cleanup phase ...");
            return;
        }
        logger.debug("Cleaning up temporary resources created for testing ...");
        PersonAuthorityClient personAuthClient = new PersonAuthorityClient();
        // Delete Person resource(s) (before PersonAuthority resources).
        for (String resourceId : personIdsCreated) {
            // Note: Any non-success responses are ignored and not reported.
            personAuthClient.deleteItem(personAuthCSID, resourceId);
        }
        // Delete PersonAuthority resource(s).
        // Note: Any non-success response is ignored and not reported.
        if (personAuthCSID != null) {
            personAuthClient.delete(personAuthCSID);
            // Delete Loans In resource(s).
            RestrictedmediaClient restrictedmediaClient = new RestrictedmediaClient();
            for (String resourceId : restrictedmediaIdsCreated) {
                // Note: Any non-success responses are ignored and not reported.
                restrictedmediaClient.delete(resourceId);
            }
        }
    }

}
