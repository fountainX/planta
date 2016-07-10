package com.metal.fetcher.fetcher.impl;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.metal.fetcher.common.Config;
import com.metal.fetcher.common.Constants;
import com.metal.fetcher.fetcher.SearchFetcher;
import com.metal.fetcher.handle.SearchFetchHandle;
import com.metal.fetcher.handle.impl.WeiboResultHandle;
import com.metal.fetcher.mapper.ArticleTaskMapper;
import com.metal.fetcher.model.SubTask;
import com.metal.fetcher.model.Task;
import com.metal.fetcher.model.WeiboAccount;
import com.metal.fetcher.utils.HttpHelper;
import com.metal.fetcher.utils.HttpHelper.HttpResult;
import com.metal.fetcher.utils.Utils;
import com.metal.fetcher.utils.WeiboHelper;
import com.yida.spider4j.crawler.utils.httpclient.Result;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * weibo's fetcher(search)
 * @author wxp
 *
 */
public class WeiboFetcher extends SearchFetcher {

	private static Logger log = LoggerFactory.getLogger(WeiboFetcher.class);
	
	private static final String WEIBO_SEARCH_FORMAT = "http://s.weibo.com/weibo/%s?nodup=1";
//	private static String WEIBO_SEARCH_FORMAT = "http://www.weibo.cn/search/mblog?hideSearchFrame=&keyword=%s";

	private static final int WEIBO_PAGE_COUNT = Config.getIntProperty("weibo_page_count");
	
	private static final String WEIBO_ACCOUNT_SAVE_FILE = Config.getProperty("weibo_account_save");
	
	private static final List<WeiboAccount> weiboAccountList = new ArrayList<WeiboAccount>();
	
	private static final Random RANDOM = new Random();
	
	static {
		initAccount();
	}
	
	public WeiboFetcher(SubTask subTask, SearchFetchHandle handle) {
		super(subTask, handle);
	}

	public static void createSubTask(Task task) {
		String keyWord = task.getKey_word();
		try {
			keyWord = URLEncoder.encode(task.getKey_word(), "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String url = String.format(WEIBO_SEARCH_FORMAT, keyWord);
		SubTask subTask = new SubTask();
		subTask.setTask_id(task.getTask_id());
		subTask.setPlatform(Constants.PLATFORM_WEIBO);
		subTask.setUrl(url);
		ArticleTaskMapper.insertSubTask(subTask);
	}
	
	private WeiboAccount getRandomAccount() {
		return weiboAccountList.get(Math.abs(RANDOM.nextInt() % weiboAccountList.size()));
	}
	
	private static void initAccount() {
		readWeiboAccount();
		saveWeiboAccount();
	}
	
	private static void readWeiboAccount() {
		File accountFile = new File(WEIBO_ACCOUNT_SAVE_FILE);
		List<String> lines = null;
		try {
			lines = FileUtils.readLines(accountFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(String line : lines) {
			if(StringUtils.isBlank(line)) {
				continue;
			}
			String[] arr = line.split("\t");
			if(arr.length < 2) {
				continue;
			}
			String account = arr[0].trim();
			String pwd = arr[1].trim();
			String cookie = null;
			if(arr.length >= 3) {
				cookie = arr[2].trim();
			}
			WeiboAccount weiboAccount = new WeiboAccount(account, pwd, cookie);
			if(StringUtils.isBlank(cookie)) {
				weiboAccountBuildCookie(weiboAccount);
			}
			if(StringUtils.isBlank(weiboAccount.getCookie())) {
				log.warn("get weibo cookie failed. account: " + account + "; pwd: " + pwd);
				continue;
			}
			
			weiboAccountList.add(weiboAccount);
		}
		log.info("weibo account count: " + weiboAccountList.size());
	}
	
	private static void saveWeiboAccount() {
		List<String> lines = new ArrayList<String>();
		for(WeiboAccount account : weiboAccountList) {
			lines.add(account.toString());
		}
		try {
			FileUtils.writeLines(new File(WEIBO_ACCOUNT_SAVE_FILE), lines);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void weiboAccountBuildCookie(WeiboAccount account) {
		try {
			Result result = WeiboHelper.login(account.getAccount(), account.getPwd());
			account.setCookie(WeiboHelper.getSUB(result.getCookie()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void fetch() {
//		String url = String.format(WEIBO_SEARCH_FORMAT, subTask.getUrl());
//		Header agent = new BasicHeader("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0); 360Spider");
		log.info("weibo fetcher task: " + this.subTask.getUrl());
		WeiboAccount weiboAccount = getRandomAccount();
		log.info("weibo accout: " + weiboAccount);
		Header cookie = new BasicHeader("Cookie", weiboAccount.getCookie()); // TODO
		
		log.info("cookie: " + cookie.getValue());
		
		Header[] headers = new Header[]{cookie};
		for(int page=1; page<=WEIBO_PAGE_COUNT; page++) {
			Utils.randomSleep(1, 2);
			String url = subTask.getUrl() + "&page=" + page;
			log.info("weibo search url: " + url);
			
			for(int i=0; i<3; i++) {
				HttpResult httpResult = HttpHelper.getInstance().
						httpGet(url, headers, null, false, null, null);
				if(httpResult != null && httpResult.getStatusCode() == HttpStatus.SC_OK && StringUtils.isNotBlank(httpResult.getContent())) {
					String html = httpResult.getContent();
					handle.handle(subTask, url, html);
					break;
				} else {
					log.warn("http request failed. url: " + url);
					weiboAccountBuildCookie(weiboAccount);
					log.info("cookie: " + cookie.getValue());
					headers = new Header[]{cookie};
					continue;
				}
			}
			
		}
		ArticleTaskMapper.subTaskFinish(subTask);
	}
	
	public static void main(String[] args) {
//		new Thread(new WeiboFetcher("凉生我们可不可以不忧伤", new WeiboResultHandle())).start();
		
//		WeiboResultHandle handle = new WeiboResultHandle();
//		Result result = null;
//		try {
//			result = WeiboHelper.login("15911129640", "219891weibo");
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		if(result == null) {
//			return;
//		}
//		Header cookie = new BasicHeader("Cookie", result.getCookie());
//		log.info("cookie: " + result.getCookie());
//		for(int page=1; page<10;page++) {
//			String url = "http://s.weibo.com/weibo/%25E7%25BB%2586%25E9%2595%25BF%25E7%259B%25B4&page=" + page;
//			Utils.randomSleep(1, 3);
//			System.out.println(url);
//			HttpResult httpResult = HttpHelper.getInstance().httpGet(url, new Header[]{cookie}, null, false, null, null);
//			handle.handle(null, url, httpResult.getContent());
//		}
		
//		try {
//			System.out.println(URLEncoder.encode("窦靖童","utf-8"));
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		for(int i=0; i<10; i++) {
			System.out.println(Math.abs(RANDOM.nextInt() % weiboAccountList.size()));
		}
	}
}
