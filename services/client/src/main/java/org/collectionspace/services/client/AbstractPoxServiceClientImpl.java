package org.collectionspace.services.client;

import javax.ws.rs.core.Response;
import org.jboss.resteasy.client.ClientResponse;

import org.collectionspace.services.jaxb.AbstractCommonList;

/*
 * CLT = List type
 * P = Proxy type
 */
public abstract class AbstractPoxServiceClientImpl<CLT extends AbstractCommonList, P extends CollectionSpacePoxProxy<CLT>>
	extends AbstractServiceClientImpl<CLT, PoxPayloadOut, String, P> 
	implements CollectionSpacePoxClient<CLT, P> {
	
    @Override
	public Response create(PoxPayloadOut xmlPayload) {
        return getProxy().create(xmlPayload.getBytes());
    }
		
    @Override
	public Response read(String csid) {
        return getProxy().read(csid);
    }
    
    public Response readList() {
    	CollectionSpaceProxy<CLT> proxy = (CollectionSpaceProxy<CLT>)getProxy();
    	return proxy.readList();
    }    
    
    @Override
    public Response readIncludeDeleted(Boolean includeDeleted) {
    	CollectionSpacePoxProxy<CLT> proxy = getProxy();
    	return proxy.readIncludeDeleted(includeDeleted.toString());
    }
    
    @Override
	public Response readIncludeDeleted(String csid, Boolean includeDeleted) {
        return getProxy().readIncludeDeleted(csid, includeDeleted.toString());
    }

    @Override
    public Response update(String csid, PoxPayloadOut xmlPayload) {
        return getProxy().update(csid, xmlPayload.getBytes());
    }
    

    @Override
    public Response keywordSearchIncludeDeleted(String keywords, Boolean includeDeleted) {
        CollectionSpacePoxProxy<CLT> proxy = getProxy();
        return proxy.keywordSearchIncludeDeleted(keywords, includeDeleted.toString());
    }

    @Override
    public Response advancedSearchIncludeDeleted(String whereClause, Boolean includeDeleted) {
        CollectionSpacePoxProxy<CLT> proxy = getProxy();
        return proxy.advancedSearchIncludeDeleted(whereClause, includeDeleted.toString());
    }

}
