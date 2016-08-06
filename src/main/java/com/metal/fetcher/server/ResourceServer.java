package com.metal.fetcher.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.seleniumhq.jetty7.server.Request;
import org.seleniumhq.jetty7.server.Server;
import org.seleniumhq.jetty7.server.handler.AbstractHandler;
import org.seleniumhq.jetty7.server.handler.ResourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metal.fetcher.common.Config;
import com.metal.fetcher.fetcher.impl.SogouWeixinFetcher;
import com.metal.fetcher.utils.Utils;

public class ResourceServer {
	private static Logger log = LoggerFactory.getLogger(ResourceServer.class);
	
	private static String HTML_FORMAT = "<html>"
										 + "<head>"
										 + "<title>搜狗微信解封</title>"
										 + "<meta charset=\"UTF-8\">"
									 + "</head>"
									 + "<body>"
										 + "<form id=\"form\" method=\"get\" action=\"post_code\">"
											 + "<img id=\"img\" name=\"door.png\" src=\"" + Config.getProperty("static.host") + Config.getProperty("weixin_code_img") + "\" />"
											 + "<input id=\"code\" name=\"code\" type=\"text\" />"
											 + "<input type=\"submit\" id=\"btn\" value=\"确定\"/>"
										 + "</form>"
										 + "<script type=\"text/javascript\">"
										 	+ "document.getElementById('code').focus();"
										 + "</script>"
									 + "</body>"
								+ "</html>";
	
//	private static final ObjectMapper mapper = new ObjectMapper();

	// public static final ThreadLocal<Main> local = new ThreadLocal<Main>();
	public enum Mapping {
		DAMA("check", "check", ResourceServer.class),
		POST_CODE("post_code", "postCode", ResourceServer.class);
		public static Mapping mapped(String uri) {
			log.info("uri: " + uri);
			uri = uri.split("/")[1];
			log.info("=uri: " + uri);
			for (Mapping c : Mapping.values()) {
				if (c.getUri().equals(uri)) {
					return c;
				}
			}
			return null;
		}

		public void action(HttpServletRequest request,
				HttpServletResponse response) throws InstantiationException,
				IllegalAccessException, NoSuchMethodException,
				SecurityException, IllegalArgumentException,
				InvocationTargetException {
			Object obj = this.t.newInstance();
			Method method = t.getMethod(this.getMapping(), new Class[] {
					HttpServletRequest.class, HttpServletResponse.class });
			if(method!=null){
				method.invoke(obj, new Object[] { request, response });
			}
		}
		
		public void action(HttpServletRequest request,
				HttpServletResponse response, String path) throws InstantiationException,
				IllegalAccessException, NoSuchMethodException,
				SecurityException, IllegalArgumentException,
				InvocationTargetException {
			Object obj = this.t.newInstance();
			Method method = t.getMethod(this.getMapping(), new Class[] {
					HttpServletRequest.class, HttpServletResponse.class, String.class });
			if(method!=null){
				method.invoke(obj, new Object[] { request, response, path });
			}
		}

		private String uri;
		private String mapping;
		private Class t;

		private <T> Mapping(String uri, String mapping, Class<T> t) {
			this.uri = uri;
			this.mapping = mapping;
			this.t = t;
		}

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}

		public String getMapping() {
			return mapping;
		}

		public void setMapping(String mapping) {
			this.mapping = mapping;
		}

		public Class getT() {
			return t;
		}

		public void setT(Class t) {
			this.t = t;
		}

	}

	public static void main(String[] args) throws Exception {
		startJetty();
	}

	public static void startJetty() throws Exception, InterruptedException {
		Server server = new Server(Config.getIntProperty("jetty.port"));
		server.setHandler(new AbstractHandler() {
			public void handle(String target, Request baseRequest,
					HttpServletRequest request, HttpServletResponse response)
					throws IOException, ServletException {
				baseRequest.setHandled(true);
				String uri = request.getRequestURI();
				if (StringUtils.isNotBlank(uri)) {
					Mapping m = Mapping.mapped(uri);
					if (m != null) {
						try {
							if(uri.split("/").length>2) {
								m.action(request, response, uri.split("/")[2]);
							} else {
								m.action(request, response);
							}
						} catch (InstantiationException
								| IllegalAccessException
								| NoSuchMethodException | SecurityException
								| IllegalArgumentException
								| InvocationTargetException e) {
							e.printStackTrace();
						}
					} else {
						log.warn("there is no uri mapped for " + uri);
						Utils.writeResult("there is no uri mapped for " + uri, response);
					}
				}
			}
		});
		server.start();
//		server.join();
		
		Server staticServer = new Server(Config.getIntProperty("jetty.static.port"));
		ResourceHandler handler = new ResourceHandler();
		handler.setResourceBase(Config.getProperty("static.base.path"));
		staticServer.setHandler(handler);
		staticServer.start();
	}
	
	public static void check(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		if(!SogouWeixinFetcher.isBan) {
			Utils.writeResult("It is not banned.", response);
			return;
		} else {
			if(SogouWeixinFetcher.unFreeze()) {
				Utils.writeResult("It is alright", response);
			} else {
				Utils.writeResult(HTML_FORMAT, response);
			}
		}
	}
	
	public static void postCode(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String code = request.getParameter("code");
		boolean isUnFreeze = SogouWeixinFetcher.dama(code);
		log.info("unfreeze: " + isUnFreeze);
		response.sendRedirect("dama");
	}
}
