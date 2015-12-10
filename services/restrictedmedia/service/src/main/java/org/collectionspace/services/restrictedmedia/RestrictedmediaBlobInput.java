package org.collectionspace.services.restrictedmedia;

import java.io.File;

public class RestrictedmediaBlobInput {
	private String restrictedmediaCsid;
	private File blobFile;
	private String blobUri;
	
	RestrictedmediaBlobInput(String restrictedmediaCsid, File blobFile, String blobUri) {
		this.restrictedmediaCsid = restrictedmediaCsid;
		this.blobFile = blobFile;
		this.blobUri = blobUri;
	}
	
	String getRestrictedmediaCsid() {
		return restrictedmediaCsid;
	}
	
	File getBlobFile() {
		return blobFile;
	}
	
	String getBlobUri() {
		return blobUri;
	}
}
