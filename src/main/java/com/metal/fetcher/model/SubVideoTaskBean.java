package com.metal.fetcher.model;

import java.util.Date;

/**
 * sub video task bean
 * @author wxp
 *
 */
public class SubVideoTaskBean {
	private long sub_vid;
	private long vid;
	private String page_url;
	private int platform;
	private String title;
	private int pd;
	private int status;
	private Date add_time;
	private Date last_update_time;
	private long tv_id;
	
	public long getSub_vid() {
		return sub_vid;
	}
	public void setSub_vid(long sub_vid) {
		this.sub_vid = sub_vid;
	}
	public long getVid() {
		return vid;
	}
	public void setVid(long vid) {
		this.vid = vid;
	}
	public String getPage_url() {
		return page_url;
	}
	public void setPage_url(String page_url) {
		this.page_url = page_url;
	}
	public int getPlatform() {
		return platform;
	}
	public void setPlatform(int platform) {
		this.platform = platform;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int getPd() {
		return pd;
	}
	public void setPd(int pd) {
		this.pd = pd;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public Date getAdd_time() {
		return add_time;
	}
	public void setAdd_time(Date add_time) {
		this.add_time = add_time;
	}
	public Date getLast_update_time() {
		return last_update_time;
	}
	public void setLast_update_time(Date last_update_time) {
		this.last_update_time = last_update_time;
	}
	public long getTv_id() {
		return tv_id;
	}
	public void setTv_id(long tv_id) {
		this.tv_id = tv_id;
	}
	@Override
	public String toString() {
		return "SubVideoTaskBean [sub_vid=" + sub_vid + ", vid=" + vid
				+ ", page_url=" + page_url + ", platform=" + platform
				+ ", title=" + title + ", status=" + status + ", add_time="
				+ add_time + ", last_update_time=" + last_update_time + "]";
	}
}
