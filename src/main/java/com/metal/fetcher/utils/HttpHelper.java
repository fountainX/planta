package com.metal.fetcher.utils;

import com.metal.fetcher.common.Config;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 重写isRedirected 目的机制重定向
 * @author wanglong
 *
 */
public class HttpHelper {
	
	/**
	 * 重定向标示位
	 */
	private  Boolean  isRediect=true;
	private static final HttpHelper instance = new HttpHelper();
	
	private static Logger log = LoggerFactory.getLogger(HttpHelper.class);
	
	private static final String[] DEFAULT_USER_AGENT = {
		//mac crome
		"Internet Explorer 9.0 Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)",
		//mac safari
		"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/600.4.8 (KHTML, like Gecko) Version/8.0.3 Safari/600.4.8",
		//windows chrome
		"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.71 Safari/537.36",
		//windows firefox
		"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:42.0) Gecko/20100101 Firefox/42.0"
	};
	
	private static Random r = new Random();
	
	public boolean isRediect() {
		return isRediect;
	}
	
	public HttpHelper setRediect(boolean isRediect) {
		 this.isRediect=isRediect;
		return this;
	}
	
	public static String getRandomUserAgent() {
		return DEFAULT_USER_AGENT[r.nextInt(DEFAULT_USER_AGENT.length)];
	}

	private HttpHelper(){
		initHttpClient();
	}
	//创建httpclient实例
	private CloseableHttpClient httpClient = null;
	/**
	 * 描述：创建httpClient连接池，并初始化httpclient
	 */
	private void initHttpClient() {
		//创建httpclient连接池
		PoolingHttpClientConnectionManager httpClientConnectionManager = new PoolingHttpClientConnectionManager();
		httpClientConnectionManager.setMaxTotal(Config.HTTP_MAX_TOTAL);	//设置连接池线程最大数量
		httpClientConnectionManager.setDefaultMaxPerRoute(Config.HTTP_MAX_ROUTE);	//设置单个路由最大的连接线程数量
		
		//创建http request的配置信息
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(Config.HTTP_CONN_TIMEOUT)
//				.setCookieSpec(CookieSpecs.IGNORE_COOKIES)
				.setSocketTimeout(Config.HTTP_SOCKET_TIMEOUT).build();
		//设置重定向策略
		LaxRedirectStrategy redirectStrategy = new LaxRedirectStrategy(){
			/**
			 * false 禁止重定向  true 允许
			 */
			@Override
			public boolean isRedirected(HttpRequest request,
					HttpResponse response, HttpContext context)
					throws ProtocolException {
				// TODO Auto-generated method stub
				return isRediect ? super.isRedirected(request, response, context) : isRediect;
			}
		};
		//初始化httpclient客户端
		httpClient = HttpClients.custom().setConnectionManager(httpClientConnectionManager)
						.setDefaultRequestConfig(requestConfig)
						//.setUserAgent(NewsConstant.USER_AGENT)
						.setRedirectStrategy(redirectStrategy)
						.build();
	}
	
	public synchronized static HttpHelper getInstance(){
		return instance;
	}
	
	
	public static void main(String[] args) throws Exception {
//		HttpHelper.getInstance().doPost("http://192.168.11.248:8080/crawlers/crawler/send_urls", null, "");
		System.out.println(HttpHelper.getInstance().httpGet("http://www.le.com/ptv/vplay/24387403.html", null, null, new HttpHost("127.0.0.1",8888), null));
	}
	
	public String doPost(String url,List<NameValuePair> pairs) {
		return doPost(url, pairs,"UTF-8");
	}
	
   public  String doPost(String url,List<NameValuePair> pairs,String charset){
        if(StringUtils.isBlank(url)){
            return null;
        }
        log.info(" post url="+url);
        try {
            HttpPost httpPost = new HttpPost(url);
            if(pairs != null && pairs.size() > 0){
                httpPost.setEntity(new UrlEncodedFormEntity(pairs,charset));
            }
            CloseableHttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                httpPost.abort();
                throw new RuntimeException("HttpClient,error status code :" + statusCode);
            }
            HttpEntity entity = response.getEntity();
            String result = null;
            if (entity != null){
                result = EntityUtils.toString(entity, charset);
            }
            EntityUtils.consume(entity);
            response.close();
            return result;
        } catch (Exception e) {
        	log.error("to request addr="+url +", "+e.getMessage());
        	e.printStackTrace();
        }
        return null;
    }
	
	public  String doPost(String url,Map<String,String> params){
		return this.doPost(url, params, 5000);
	}
	
	/**
     * HTTP Post 获取内容
     * @param url  请求的url地址 ?之前的地址
     * @param params 请求的参数
//     * @param charset    编码格式
     * @return    页面内容
     */
    public  String doPost(String url,Map<String,String> params, int timeout){
        if(StringUtils.isBlank(url)){
            return null;
        }
        log.info(" post url="+url);
        try {
            List<NameValuePair> pairs = null;
            if(params != null && !params.isEmpty()){
                pairs = new ArrayList<NameValuePair>(params.size());
                for(Map.Entry<String,String> entry : params.entrySet()){
                    String value = entry.getValue();
                    if(value != null){
                        pairs.add(new BasicNameValuePair(entry.getKey(),value));
                    }
                }
            }
            RequestConfig requestConfig = RequestConfig.custom()
					.setSocketTimeout(timeout)
					.setConnectTimeout(timeout).build();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(requestConfig);
            if(pairs != null && pairs.size() > 0){
                httpPost.setEntity(new UrlEncodedFormEntity(pairs,"utf8"));
            }
            CloseableHttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            log.info("response code is =>"+statusCode);
            if (statusCode != 200) {
                httpPost.abort();
//                throw new RuntimeException("HttpClient,error status code :" + statusCode);
                return null;
            }
            HttpEntity entity = response.getEntity();
            String result = null;
            if (entity != null){
                result = EntityUtils.toString(entity, "utf8");
            }
            EntityUtils.consume(entity);
            response.close();
            return result;
        } catch (Exception e) {
        	log.error("to request addr="+url +", "+e.getMessage());
        	e.printStackTrace();
        }
        return null;
    }
    
	public boolean isAborted(String url){
		return false;
	}
	
	public String get(String url){
		return get(url,"UTF-8");
	}
	
	/**
	 * 
	 */
	public String get(String url, String charset){
		
		if(StringUtils.isBlank(url)) {
			return null;	//如果url为空或者null
		}
		//创建httpclient请求方式
		HttpGet httpGet = new HttpGet(url);
		
		CloseableHttpResponse response = null;
		
		try {
			response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			log.info("response code is =>"+statusCode);
			if (statusCode != 200) {
				httpGet.abort();
				return null;
			}
			HttpEntity entity = response.getEntity();
            String result = null;
            if (entity != null){
                result = EntityUtils.toString(entity, charset);
            }
            EntityUtils.consume(entity);
            response.close();
            return result;
		} catch(Exception e) {
			log.error("get failed.", e);
			return "";
		} finally{
			httpGet.releaseConnection();
			if(null != response){
				try {
					//关闭response
					response.close();
				} catch (IOException e) {
					log.error("response close. " , e);
				}
			}
		}
	}

	public HttpResult httpGet(String url){
		
		if(StringUtils.isBlank(url)) {
			return null;	//如果url为空或者null
		}
		//创建httpclient请求方式
		HttpGet httpGet = new HttpGet(url);
		
		return httpRequest(httpGet, null, null, null, null);
	}
	
	public HttpResult httpGet(String url, Header[] headers, Boolean isRedirect, HttpHost proxy, HttpContext httpContext) {
		if(StringUtils.isBlank(url)) {
			return null;	//如果url为空或者null
		}
		//创建httpclient请求方式
		HttpGet httpGet = new HttpGet(url);
		
		return httpRequest(httpGet, headers, isRedirect, proxy, httpContext);
	}
	
	public HttpResult httpPost(String url, Map<String,String> params, Header[] headers, HttpContext httpContext) {
		if(StringUtils.isBlank(url)) {
			return null;	//如果url为空或者null
		}
		//创建httpclient请求方式
		HttpPost httpPost = new HttpPost(url);

		List<NameValuePair> pairs = null;
        if(params != null && !params.isEmpty()){
            pairs = new ArrayList<NameValuePair>(params.size());
            for(Map.Entry<String,String> entry : params.entrySet()){
                String value = entry.getValue();
                if(value != null){
                    pairs.add(new BasicNameValuePair(entry.getKey(),value));
                }
            }
        }
        if(pairs != null && pairs.size() > 0){
            try {
				httpPost.setEntity(new UrlEncodedFormEntity(pairs,"utf8"));
			} catch (UnsupportedEncodingException e) {
				log.error("http post builder failed. ", e);
				return null;
			}
        }
		
		return httpRequest(httpPost, headers, true, null, httpContext);
	}

	public HttpResult httpGet(String url, Header[] headers, Map<String, String> cookieConfig, Boolean isRedirect, HttpHost proxy, HttpContext httpContext) {
		if(StringUtils.isBlank(url)) {
			return null;	//如果url为空或者null
		}
		//创建httpclient请求方式
		HttpGet httpGet = new HttpGet(url);
		if(cookieConfig != null && cookieConfig.size() > 0) {
			BasicCookieStore cookieStore = getCookie(cookieConfig);
			if(httpContext == null) {
				httpContext = new BasicHttpContext();
			}
			httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

		}
		return httpRequest(httpGet, headers, isRedirect, proxy, httpContext);
	}
	
	private HttpResult httpRequest(HttpRequestBase request, Header[] headers, Boolean isRedirect, HttpHost proxy, HttpContext httpContext) {
		HttpResult httpResult = new HttpResult();
		
		if(request == null) {
			return null;	//如果url为空或者null
		}
		
		request.setHeader(HttpHeaders.ACCEPT, "*/*");
		request.setHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.8,en;q=0.6");
		request.setHeader(HttpHeaders.CONNECTION, "keep-alive");
		String ua = getRandomUserAgent();
//		log.debug("user agent: " + ua);
		request.setHeader(HttpHeaders.USER_AGENT, ua);
		if(headers != null) {
			for(Header header : headers) {
				request.addHeader(header);
			}
		}

		if(isRedirect == null) {
			isRedirect = true;
		}
		Builder requestBuilder = RequestConfig.custom().setRedirectsEnabled(isRedirect);
		if(proxy != null) {
			requestBuilder.setProxy(proxy);
		}
		
		RequestConfig requestConfig = requestBuilder.build();
		request.setConfig(requestConfig);

		if(httpContext == null) {
			httpContext = new BasicHttpContext();
		}
		
		CloseableHttpResponse response = null;
		
		try {
			response = httpClient.execute(request, httpContext);
			HttpEntity entity = response.getEntity();
            String result = null;
            if (entity != null){
                result = EntityUtils.toString(entity, "utf-8");
            }
            EntityUtils.consume(entity);
            response.close();
            httpResult.setContent(result);
            httpResult.setResponse(response);
            httpResult.setContext(httpContext);
		} catch(Exception e) {
			log.error("get failed.", e);
		} finally{
			request.abort();
			if(null != response){
				try {
					//关闭response
					response.close();
				} catch (IOException e) {
					log.error("response close. " , e);
				}
			}
		}
		return httpResult;
	}

	private BasicCookieStore getCookie(Map<String, String> cookies){
		BasicCookieStore cookieStore = new BasicCookieStore();
		BasicClientCookie cookie;
		String domain = (cookies.get("domain"));
		for (String cookieName: cookies.keySet()) {
			if(cookieName.equals("domain")) {
				continue;
			}
			cookie = new BasicClientCookie(cookieName, cookies.get(cookieName));
			cookie.setDomain(domain);
			cookie.setPath("/");
			cookieStore.addCookie(cookie);
		}
		return cookieStore;
	}
//	public WebUrlResult get(String url, Map<String,String> headers, Proxy proxy, boolean allowRedirect){
//		WebUrlResult webUrlResult = new WebUrlResult();
//		if(null==url || "".equals(url)) {
//			return null;	//如果url为空或者null
//		}
//		//创建httpclient请求方式
//		HttpGet httpGet = new HttpGet(url);
//		
//		RequestConfig requestConfig = null;
//		if(allowRedirect) {
//			requestConfig = RequestConfig.custom()
//					.setProxy(proxy.getHttpHost()).setRedirectsEnabled(true).build();
//		} else {
//			requestConfig = RequestConfig.custom()
//					.setProxy(proxy.getHttpHost()).setRedirectsEnabled(false).build();
//		}
//		httpGet.setConfig(requestConfig);
//		
//		CloseableHttpResponse response = null;
//		httpGet.setHeader("Accept", "*/*");
//		httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6");
//		httpGet.setHeader("Connection", "keep-alive");
//		if(StringUtils.isNotBlank(proxy.getUserAgent())) {
//			httpGet.setHeader("User-Agent", proxy.getUserAgent());
//		} else {
//			proxy.setUserAgent(getRandomUserAgent());
//			httpGet.setHeader("User-Agent", proxy.getUserAgent());
//		}
//		
//		if(headers != null && headers.size() > 0) {
//			for(String headName : headers.keySet()) {
//				if(headName.toLowerCase().equals("cookie") || headName.toLowerCase().equals("user-agent")) {
//					//cookie和userAgent用其它处理方式
//					continue;
//				}
//				httpGet.setHeader(headName, headers.get(headName));
//			}
//		}
//		
//		try {
//			HttpContext httpContext = new BasicHttpContext();
//			if(proxy.getCookieStore() != null) {
//				httpContext.setAttribute(HttpClientContext.COOKIE_STORE, proxy.getCookieStore());
//			} else {
//				BasicCookieStore cookieStore = new BasicCookieStore();
//				httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
//			}
//			
//			response = httpClient.execute(httpGet, httpContext);
//
//			Header[] reHeaders = response.getAllHeaders();
//			int statusCode = response.getStatusLine().getStatusCode();
//			if(log.isDebugEnabled()) {
//				log.debug("response code is =>"+statusCode);
//			}
//			HttpEntity entity = response.getEntity();
//            String result = null;
//            if (entity != null){
//                result = EntityUtils.toString(entity, "utf-8");
//            }
//            EntityUtils.consume(entity);
//
//            webUrlResult.setHeaders(header2String(reHeaders));
//            webUrlResult.setStatusCode(statusCode);
//            webUrlResult.setResult(result);
//            if(allowRedirect) {
//                HttpHost currentHost = (HttpHost) httpContext
//                        .getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
//
//                HttpUriRequest req = (HttpUriRequest) httpContext
//                        .getAttribute(HttpCoreContext.HTTP_REQUEST);
//                String targetUrl = (req.getURI().isAbsolute()) ? req.getURI()
//                        .toString() : (currentHost.toURI() + req.getURI());
//                webUrlResult.setTargetUrl(targetUrl);
//            } else {
//            	webUrlResult.setLocation(response.getFirstHeader("Location") == null ? null : response.getFirstHeader("Location").getValue());
//            }
//          if(proxy.getCookieStore() == null) {
//	          BasicCookieStore cookieStore = (BasicCookieStore)httpContext.getAttribute(HttpClientContext.COOKIE_STORE);
//	          proxy.setCookieStore(cookieStore);
//          }
//            response.close();
//            return webUrlResult;
//		} catch(Exception e) {
//			e.printStackTrace();
//			return null;
//		} finally{
//			httpGet.releaseConnection();
//			if(null != response){
//				try {
//					//关闭response
//					response.close();
//				} catch (IOException e) {
//					// TODO 这里写异常处理的代码
//					e.printStackTrace();
//				}
//			}
//		}
//	}

	private static String[] header2String(Header[] headers) {
		String[] headStr = new String[headers.length];
		for(int i=0; i<headers.length; i++) {
			headStr[i] = headers[i].toString();
		}
		return headStr;
	}
	
	public void download(String url, Map<String,String> headers, HttpHost httpHost, String file, HttpContext httpContext){
		
		if(null==url || "".equals(url)) {
			return;	//如果url为空或者null
		}
		//创建httpclient请求方式
		HttpGet httpGet = new HttpGet(url);
		
		RequestConfig requestConfig = RequestConfig.custom()
					.setProxy(httpHost).setRedirectsEnabled(true).build();
		httpGet.setConfig(requestConfig);
		
		CloseableHttpResponse response = null;
		httpGet.setHeader("Accept", "*/*");
		httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6");
		httpGet.setHeader("Connection", "keep-alive");
//		httpGet.setHeader("User-Agent", getRandomUserAgent());
		if(headers != null && headers.size() > 0) {
			for(String headName : headers.keySet()) {
				httpGet.setHeader(headName, headers.get(headName));
			}
		}
		
		if(httpContext == null) {
			httpContext = new BasicHttpContext();
		}
		
		try {
			response = httpClient.execute(httpGet, httpContext);
			int statusCode = response.getStatusLine().getStatusCode();
			if(log.isInfoEnabled()){
				log.info("response code is =>"+statusCode);
			}
			if (statusCode != 200) {
				httpGet.abort();
				return;
			}
			HttpEntity entity = response.getEntity();
			IOUtils.copy(entity.getContent(), new FileOutputStream(new File(file)));
			
			EntityUtils.consume(entity);
            response.close();
			//关闭httpEntity流
			//EntityUtils.consume(entity);
		} catch(Exception e) {
			e.printStackTrace();
			return;
		} finally{
			httpGet.releaseConnection();
			if(null != response){
				try {
					//关闭response
					response.close();
				} catch (IOException e) {
					// TODO 这里写异常处理的代码
					e.printStackTrace();
				}
			}
		}
	}
	
	public static class HttpResult {
		HttpResponse response;
		HttpContext context;
		String content;
		
		public HttpResponse getResponse() {
			return response;
		}
		public void setResponse(HttpResponse response) {
			this.response = response;
		}
		public HttpContext getContext() {
			return context;
		}
		public void setContext(HttpContext context) {
			this.context = context;
		}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
		public int getStatusCode() {
			try {
				return this.response.getStatusLine().getStatusCode();
			} catch (Exception e) {
				return -1;
			}
		}
		
		@Override
		public String toString() {
			return "HttpResult [response=" + response + ", context=" + context
					+ ", content=" + content + "]";
		}
	}
	
	public HttpResult httpGetWithRetry(String url, int retry) {
		HttpResult result = null;
		for(int i=0; i<retry; i++) {
			result = httpGet(url);
			if(result == null) {
				log.warn("get failed. null");
				continue;
			}
			if(result.getStatusCode() != HttpStatus.SC_OK) {
				log.warn("get failed. status code: " + result.getStatusCode());
				continue;
			}
			if(StringUtils.isBlank(result.getContent())) {
				log.warn("get failed. content is null.");
				continue;
			}
		}
		return result;
	}
}
