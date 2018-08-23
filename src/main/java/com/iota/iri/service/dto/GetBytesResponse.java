package com.iota.iri.service.dto;

import java.util.List;

public class GetBytesResponse extends AbstractResponse {
	
    private String [] bytesStr;
    
	public static GetBytesResponse create(List<String> elements) {
		GetBytesResponse res = new GetBytesResponse();
		res.bytesStr = elements.toArray(new String[] {});
		return res;
	}

	public String [] getBytesStr() {
		return bytesStr;
	}
}
