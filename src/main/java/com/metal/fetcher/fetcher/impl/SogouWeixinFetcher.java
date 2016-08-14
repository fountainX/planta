package com.metal.fetcher.fetcher.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metal.fetcher.common.Config;
import com.metal.fetcher.common.Constants;
import com.metal.fetcher.fetcher.SearchFetcher;
import com.metal.fetcher.handle.SearchFetchHandle;
import com.metal.fetcher.handle.impl.CommonResultHandle;
import com.metal.fetcher.mapper.ArticleTaskMapper;
import com.metal.fetcher.model.SubTask;
import com.metal.fetcher.model.Task;
import com.metal.fetcher.utils.HttpHelper;
import com.metal.fetcher.utils.HttpHelper.HttpResult;
import com.metal.fetcher.utils.Utils;

/**
 * weixin.sogou's fetcher(search)
 * @author wxp
 *
 */
public class SogouWeixinFetcher extends SearchFetcher {

	public SogouWeixinFetcher(SubTask subTask, SearchFetchHandle handle) {
		super(subTask, handle);
		// TODO Auto-generated constructor stub
	}

	private static Logger log = LoggerFactory.getLogger(SogouWeixinFetcher.class);
	
//	private static final String DOMAIN = "http://weixin.sogou.com";// todo dinymic 
	
	private static final String URL_NOPAGE_FORMAT = "http://weixin.sogou.com/weixin?type=2&query=%s&ie=utf8";
	
//	private static final String URL_FORMAT = "http://weixin.sogou.com/weixin?type=2&query=%s&ie=utf8&page=%d";
	
	private static BasicCookieStore cookieStore;
	
	public static boolean isBan = true; // TODO tmp
	
	public static String freezeUrl = null;
	
	public static String antispiderUrl = null;
	
	public static String DEFAULT_FREEZE_URL = "http://weixin.sogou.com/weixin?type=2&query=%E9%B9%BF%E6%99%97&ie=utf8";
	
	private static final int[] sleepTime = {7, 10};
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private static final Random RANDOM = new Random();
	
	static {
		readCookieStore();
	}
	
	public static void createSubTask(Task task) {
		String url = String.format(URL_NOPAGE_FORMAT, task.getKey_word());
		SubTask subTask = new SubTask();
		subTask.setTask_id(task.getTask_id());
		subTask.setPlatform(Constants.PLATFORM_WEIXIN);
		subTask.setUrl(url);
		ArticleTaskMapper.insertSubTask(subTask);
	}
	
	protected void fetch() {
		
		String url = subTask.getUrl() + "&page=1";
//		Header header = new BasicHeader(HttpHeaders.USER_AGENT, HttpHelper.getRandomUserAgent());
		if(cookieStore == null) {
			cookieStore = new BasicCookieStore();
		}
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		
		HttpResult articleListResult = HttpHelper.getInstance().httpGet(url, null, false, null, httpContext);
		
		if(articleListResult.getResponse().getFirstHeader("Location") != null) {
			log.error("the ip is freezed. locatioin: " + articleListResult.getResponse().getFirstHeader("Location").getValue());
			freezeUrl = url;
			isBan = true;
			ArticleTaskMapper.subTaskFinish(subTask, false);
			return;
		} else {
			isBan = false;
		}
		
		String html = articleListResult.getContent();
		
		Document doc = Jsoup.parse(html);
		if(!isExistResult(doc)) {
			log.warn("search \"keyword\" no result");
			ArticleTaskMapper.subTaskFinish(subTask, false);
			return;
		}
		int pageCount = getPageCount(doc);
		if(pageCount <= 0) {
			log.warn("search \"" + subTask.getUrl() + "\" result has 0 page.");
			ArticleTaskMapper.subTaskFinish(subTask, false);
			return;
		}
		try {
			Document listDoc = Jsoup.parse(articleListResult.getContent());
			List<String> listLinks = getAritcleUrls(listDoc);
			fetcherArticles(listLinks);
		} catch (Exception e) {
			log.error("sub fetcher task failed. url: " + url, e);
		}
//		Thread th1 = new Thread(new SubFetcherTask(firstUrl, articleListResult));
//		th1.start();
//		th1.run(); // 单线程执行。为免被封
		for(int i=2; i<pageCount+1; i++) { // TODO pageCount
			Utils.randomSleep(sleepTime[0], sleepTime[1]);
			url = subTask.getUrl() + "&page=" + i;
			articleListResult = HttpHelper.getInstance().httpGet(url, null, false, null, httpContext);
			if(articleListResult.getResponse().getFirstHeader("Location") != null) {
				log.error("the ip is freezed. locatioin: " + articleListResult.getResponse().getFirstHeader("Location").getValue());
				freezeUrl = url;
				isBan = true;
				ArticleTaskMapper.subTaskFinish(subTask, false);
				return;
			} else {
				isBan = false;
			}
			try {
				Document listDoc = Jsoup.parse(articleListResult.getContent());
				List<String> listLinks = getAritcleUrls(listDoc);
				fetcherArticles(listLinks);
			} catch (Exception e) {
				log.error("sub fetcher task failed. url: " + url, e);
			}
//			Header theHeader = new BasicHeader(HttpHeaders.USER_AGENT, HttpHelper.getRandomUserAgent());
//			Thread th = new Thread(new SubFetcherTask(url));
//			th.start();
//			th.run();
		}
		ArticleTaskMapper.subTaskFinish(subTask, true);
		saveCookieStore();
	}

	private static void httpPvRequest(String url) {
		if(cookieStore == null) {
			return;
		}
		String uigs_cookie = "";
		for(Cookie cookie : cookieStore.getCookies()) {
			if("SUID".equals(cookie.getName()) && ".weixin.sogou.com".equals(cookie.getDomain())) {
				uigs_cookie = "SUID=" + cookie.getValue() + "&sct=3";
			}
		}
		
		String uigs_productid = "webapp";
		String uigs_uuid = String.valueOf(new Date().getTime()) + String.valueOf(Math.abs(RANDOM.nextInt() % 1000));
		String uigs_version = "v2.0";
		String uigs_refer = url;
//		String uigs_cookie = "SUID=950115DF6F1C920A0000000057AB3D7F&sct=3";
		String uuid = UUID.randomUUID().toString().toLowerCase();

		int start = url.indexOf("query=") + 6;
		int end = url.indexOf("&", start);
		
		String query = url.substring(start, end);
		String weixintype = "2";
		String exp_status = "null";
		String exp_id = "null_0-null_1-null_2-null_3-null_4-null_5-null_6-null_7-null_8-null_9-"; // TODO
		String exp_id_list = "null";
		String noresult = "0";
		String type = "weixin_search_pc";
		String xy = "1216,650";
		String uigs_t = String.valueOf(new Date().getTime()) + String.valueOf(Math.abs(RANDOM.nextInt() % 1000));
		
		String pvUrl = "http://pb.sogou.com/pv.gif?uigs_productid=" + uigs_productid + "&uigs_uuid=" + uigs_uuid + "&uigs_version=" + uigs_version 
				+ "&uigs_refer=" + uigs_refer + "&uigs_cookie=" + uigs_cookie + "&uuid=" + uuid + "&query=" + query 
				+ "&weixintype=" + weixintype + "&exp_status=" + exp_status + "&exp_id=" + exp_id + "&exp_id_list=" + exp_id_list + "&noresult=" + noresult 
				+ "&type=" + type + "&xy=" + xy + "&uigs_t=" + uigs_t;
		
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		
		log.info("pv url: " + pvUrl);
		
		HttpHelper.getInstance().httpGet(pvUrl, null, false, null, httpContext);
	}
	
	/**
	 * 搜索结果列表页是否存在文章
	 * @param doc
	 * @return
	 */
	private boolean isExistResult(Document doc) {
		Element ele = doc.getElementById("noresult_part1_container");
		if(ele == null) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 搜索结果列表页、页数
	 * @param doc
	 * @return
	 */
	private int getPageCount(Document doc) {
		try {
			Element pageContainer = doc.getElementById("pagebar_container");
			Elements as = pageContainer.getElementsByTag("a");
			return as.size();
		} catch (Exception e) {
			log.error("Get article page count failed.", e);
			return 0;
		}
	}
	
	/**
	 * 解析文章链接
	 * @param doc
	 * @return
	 */
	private List<String> getAritcleUrls(Document doc) {
		List<String> links = new ArrayList<String>();
		try {
			Elements articleLinks = doc.getElementsByClass("txt-box");
			for(Element ele : articleLinks) {
				try {
					Element a = ele.getElementsByTag("a").first();
					String link = a.attr("href");
					links.add(Utils.buildAbsoluteUrl(subTask.getUrl(), link));
				} catch (Exception e) {
					log.error("Get article links failed", e);
				}
			}
		} catch (Exception e) {
			log.error("Resolve article list page failed", e);
		}
		return links;
	}
	
	/**
	 * 循环访问文章详情页
	 * @param links
	 * @param context
	 */
	private void fetcherArticles(List<String> links) {
		for(String link : links) {
//			Utils.randomSleep(sleepTime[0], sleepTime[1]);
			HttpResult articleResult = HttpHelper.getInstance().httpGet(link, null, null, null, null);
			
			RedirectLocations locations = (RedirectLocations)articleResult.getContext().getAttribute(HttpClientContext.REDIRECT_LOCATIONS);
			if(locations == null || locations.size() <= 0) {
				log.info("article url: " + link);
				handle.handle(subTask, link, articleResult.getContent());
			} else {
				log.info("article url: " + locations.get(locations.size() - 1).toString());
				handle.handle(subTask, locations.get(locations.size() - 1).toString(), articleResult.getContent());// result handle
			}
		}
	}
	
	/**
	 * @deprecated
	 * @author wxp
	 *
	 */
	class SubFetcherTask implements Runnable {
		
		String url;
		HttpResult httpResult;
		
		public SubFetcherTask(String url) {
			this.url = url;
		}
		
		public SubFetcherTask(String url, HttpResult httpResult) {
			this.url = url;
			this.httpResult = httpResult;
		}
		
		public void run() {
			if(StringUtils.isBlank(url) && httpResult == null) {
				log.error("url and httpResult is both null.");
				return;
			}
			if(httpResult == null) {
				initHttpGet();
			}
			if(httpResult == null) {
				log.error("init http get failed. url: " + url);
				return;
			}
			try {
				Document listDoc = Jsoup.parse(httpResult.getContent());
				List<String> listLinks = getAritcleUrls(listDoc);
				fetcherArticles(listLinks);
			} catch (Exception e) {
				log.error("sub fetcher task failed. url: " + url, e);
			}
		}
		
		private void initHttpGet() {
			if(StringUtils.isNotBlank(url)) {
				httpResult = HttpHelper.getInstance().httpGet(url);
			}
		}
	}
	
	public static boolean unFreeze() {
		
//		File codePng = new File(Config.getProperty("static.base.path") + Config.getProperty("weixin_code_img"));
//		if(codePng.exists()) {
//			codePng.delete();
//		}
		
		if(cookieStore == null) {
			cookieStore = new BasicCookieStore();
		}
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		
		if(StringUtils.isBlank(freezeUrl)) {
			freezeUrl = DEFAULT_FREEZE_URL;
		}
		
		HttpResult articleListResult = HttpHelper.getInstance().httpGet(freezeUrl, null, false, null, httpContext);
		
		// TODO
//		log.info("content: " + articleListResult.getContent());
		
		antispiderUrl = null;
		if(articleListResult.getResponse().getFirstHeader("Location") != null) {
			antispiderUrl = articleListResult.getResponse().getFirstHeader("Location").getValue();
			log.info("location: " + antispiderUrl);
		} else {
			isBan = false;
			log.info("the ip is not freezed.");
			return true;
		}
		HttpResult antispiderResult = HttpHelper.getInstance().httpGet(antispiderUrl, null, false, null, httpContext);
		HttpResult pvResult = HttpHelper.getInstance().httpGet("http://pb.sogou.com/pv.gif?uigs_productid=webapp&type=antispider&subtype=index&domain=weixin&suv=&snuid=&t=" + new Date().getTime(), null, false, null, httpContext);
		HttpHelper.getInstance().download("http://weixin.sogou.com/antispider/util/seccode.php?tc=" + new Date().getTime(), null, null, Config.getProperty("static.base.path") + Config.getProperty("weixin_code_img"), httpContext);
		log.info("unfreeze, download code png.");
//		dama(url, location);
		return false;
	}
	
	public static boolean dama(String postCode) {
//		Scanner scan = new Scanner(System.in);
//		String read = scan.nextLine();
//		System.out.println(read);
//		scan.close();
		
		if(cookieStore == null) {
			cookieStore = new BasicCookieStore();
		}
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		Map<String,String> params = new HashMap<String,String>();
		params.put("c", postCode);
		params.put("r", "/" + Utils.getLastPath(freezeUrl));
		params.put("v", "5");
		// TODO test
		log.debug(params.toString());
		HttpResult result = HttpHelper.getInstance().httpPost("http://weixin.sogou.com/antispider/thank.php", params, null, httpContext);
		log.info(result.getContent());
		String content = result.getContent();
		String id = null;
		try {
			JsonNode root = MAPPER.readTree(content);
			int code = root.get("code").asInt();
			if(code != 0) {
				log.warn("unfreeze failed. ");
				return false;
			} else {
				id = root.get("id").asText();
			}
		} catch (IOException e) {
			log.warn("unfreeze failed. ", e);
			return false;
		}
		BasicClientCookie cookieSNUID = new BasicClientCookie("SNUID", id);
		cookieSNUID.setDomain(".sogou.com");
		cookieSNUID.setPath("/");
		cookieSNUID.setAttribute("domain", "");
		cookieStore.addCookie(cookieSNUID);
		BasicClientCookie cookieRight = new BasicClientCookie("seccodeRight", "success");
		cookieRight.setDomain(".weixin.sogou.com");
		cookieRight.setPath("/");
		cookieRight.setAttribute("domain", "");
		cookieStore.addCookie(cookieRight);
		BasicClientCookie cookieCount = new BasicClientCookie("successCount", "1|" + Utils.toGMTString(new Date()));
		cookieCount.setDomain(".weixin.sogou.com");
		cookieCount.setPath("/");
		cookieCount.setAttribute("domain", "");
		cookieStore.addCookie(cookieCount);
		
		Header header = new BasicHeader("Referer", antispiderUrl);
		Header upgrade = new BasicHeader("Upgrade-Insecure-Requests", "1");
		Header[] headers = new Header[2];
		headers[0] = header;
		headers[1] = upgrade;
		HttpResult successResult = HttpHelper.getInstance().httpGet(freezeUrl, headers, false, null, httpContext);
		if(successResult.getResponse().getFirstHeader("Location") != null) {
			log.warn("unfreeze failed. location: " + successResult.getResponse().getFirstHeader("Location").getValue());
			return false;
		} else {
			isBan = false;
			saveCookieStore();
			log.info("unfreeze success.");
			return true;
		}
	}
	
	public static void saveCookieStore() {
		ObjectOutputStream output = null;
		try {
			output = new ObjectOutputStream(new FileOutputStream(new File(Config.getProperty("weixin_cookie_save"))));
			output.writeObject(cookieStore);
		} catch (IOException e) {
			log.error("save cookie store failed. ", e);
		} finally {
			if(output != null) {
				try {
					output.close();
				} catch (IOException e) {
					log.error("close output failed. ", e);
				}
			}
		}
	}
	
	private static void readCookieStore() {
		ObjectInputStream input = null;
		try {
			input = new ObjectInputStream(new FileInputStream(new File(Config.getProperty("weixin_cookie_save"))));
			cookieStore = (BasicCookieStore)input.readObject();
		} catch (IOException | ClassNotFoundException e) {
			log.warn("read cookie store failed. ", e);
		} finally {
			if(input != null) {
				try {
					input.close();
				} catch (IOException e) {
					log.error("close input failed.", e);
				}
			}
		}
	}
	
	public static void main(String[] args) {
//		SubTask subTask = new SubTask();
//		subTask.setUrl("http://weixin.sogou.com/weixin?type=2&query=%E6%B5%B7%E8%B4%BC%E7%8E%8B&ie=utf8");
//		SearchFetcher fetcher = new SogouWeixinFetcher(subTask, new CommonResultHandle());
//		new Thread(fetcher).start();
		String url = "http://weixin.sogou.com/weixin?type=2&query=%E6%B5%B7%E8%B4%BC%E7%8E%8B&ie=utf8&page=1";
		readCookieStore();
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		log.info("start.........");
		for(int i=0; i<1000; i++) {
//			HttpHelper.getInstance().get("http://weixin.sogou.com/weixin?type=2&query=%E6%B5%B7%E8%B4%BC%E7%8E%8B&ie=utf8&page=1");
			HttpResult result = HttpHelper.getInstance().httpGet(url, null, true, null, httpContext);
			httpPvRequest(url);
			RedirectLocations locations = (RedirectLocations)result.getContext().getAttribute(HttpClientContext.REDIRECT_LOCATIONS);
			if(locations == null || locations.size() <= 0) {
				log.info("success.");
			} else {
				log.warn("freezed!!!");
			}
			Utils.randomSleep(7, 10);
		}
		
		saveCookieStore();
		
//		unFreeze("http://weixin.sogou.com/weixin?type=2&query=%E6%B5%B7%E8%B4%BC%E7%8E%8B&ie=utf8&page=1");
//		unFreeze("http://weixin.sogou.com/weixin?type=2&query=%E6%B5%B7%E8%B4%BC%E7%8E%8B&ie=utf8&page=1");
//		System.out.println(Utils.toGMTString(new Date()));
	}
}
