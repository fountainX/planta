package com.metal.fetcher.handle.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metal.fetcher.handle.SearchFetchHandle;
import com.metal.fetcher.model.SubTask;

public class CommonResultHandle implements SearchFetchHandle {

	private static Logger log = LoggerFactory.getLogger(CommonResultHandle.class);

	@Override
	public void handle(SubTask subTask, String url, String html) {
		// TODO Auto-generated method stub
		System.out.println(url);
//		System.out.println(html);
	}
	

}
