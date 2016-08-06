package com.metal.fetcher.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Utils {
	
	private static final Random RANDOM = new Random();
	
	private static SimpleDateFormat GMTFORMAT = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.UK);  
    
	static {
		GMTFORMAT.setTimeZone(new java.util.SimpleTimeZone(0, "GMT"));
    }
	
	public static String toGMTString(Date date) {
		return GMTFORMAT.format(date);
	}
	
	public static String getHost(String url) {
		if(StringUtils.isNotBlank(url) && url.startsWith("http://")) {
			// after http://
			String subUrl = url.substring(7);
			String[] pathArr = subUrl.split("/");
			return pathArr[0];
		}
		return null;
	}
	
	public static String buildAbsoluteUrl(String currUrl, String href) {
		if(href.startsWith("http://") || href.startsWith("https://")) {
			return href;
		} else if(href.startsWith("/")) {
			if(currUrl.startsWith("http://")) {
				String prefix = currUrl.substring(0, currUrl.indexOf("/", 7));
				return prefix + href;
			} else if(currUrl.startsWith("https://")) {
				String prefix = currUrl.substring(0, currUrl.indexOf("/", 8));
				return prefix + href;
			} else {
				return null;
			}
		} else {
			if(currUrl.startsWith("http://") || currUrl.startsWith("https://")) {
				String prefix = currUrl.substring(0, currUrl.lastIndexOf("/"));
				return prefix + "/" + href;
			} else {
				return null;
			}
		}
	}
	
	public static String getPath(String url) {
		if(url.startsWith("http://")) {
			return url.substring(7);
		} else if(url.startsWith("https://")) {
			return url.substring(8);
		} else {
			return null;
		}
	}
	
	public static String getLastPath(String url) {
		if(StringUtils.isBlank(url)) {
			return null;
		}
		String[] pathArr = url.split("/");
		return pathArr[pathArr.length - 1];
	}
	
	public static String htmlToText(String html) {
		Document doc = Jsoup.parse(html);
		return doc.text();
	}
	
	public static int parseInt(String str) {
		if(StringUtils.isBlank(str)) {
			return 0;
		}
		StringBuilder result = new StringBuilder(3);
		boolean getFlag = false;
		for(int i=0; i<str.length(); i++) {
			char c = str.charAt(i);
			if(c>=48 && c<=57) {
				getFlag = true;
				result.append(c);
			} else {
				if(getFlag) {
					break;
				}
			}
		}
		int r = 0;
		if(result.length() > 0) {
			try {
				r = Integer.parseInt(result.toString());
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return r;
	}

	/**
	 * sleep
	 */
	public static void randomSleep(int sec, int ran) {
		int seconds = 0;
		if(ran <= 0) {
			seconds = sec;
		} else {
			seconds = sec + RANDOM.nextInt(ran);
		}
		if(seconds <= 0) {
			return;
		}
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String getStrBtwn(String str, String prefix, String subfix) {
		int start = str.indexOf(prefix);
		if(start < 0) {
			return null;
		}
		int end = str.indexOf(subfix, start + prefix.length());
		if(end < 0 || end < start) {
			return null;
		}
		return str.substring(start + prefix.length(), end);
	}
	
	public static void writeResult(String result, HttpServletResponse response)
			throws IOException {
		PrintWriter out = null;
		if (StringUtils.isNotBlank(result)) {
			try {
				response.setCharacterEncoding("UTF-8");
				out = response.getWriter();
				out.print(result);
				out.flush();
				response.flushBuffer();
			} finally {
				if (out != null) {
					out.close();
				}
			}
		}
	}
	
	public static void main(String[] args) {
		System.out.println(parseInt("sdf"));
	}
}

