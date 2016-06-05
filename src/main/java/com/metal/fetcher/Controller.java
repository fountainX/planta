package com.metal.fetcher;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metal.fetcher.common.Config;
import com.metal.fetcher.common.QuartzManager;
import com.metal.work.impl.FetcherWorkImpl;
import com.metal.work.impl.ResetVideoTaskImpl;
import com.metal.work.impl.TaskWorkImpl;
import com.metal.work.impl.VideoFetcherWorkImpl;
import com.metal.work.impl.VideoTaskWorkImpl;

public class Controller {
	
	private static Logger log = LoggerFactory.getLogger(Controller.class);
	
	private static boolean VIDEO_TASK_RUN = Config.getBooleanProperty("video_task_run");
	private static boolean VIDEO_SUB_TASK_RUN = Config.getBooleanProperty("video_sub_task_run");
	private static boolean TASK_RUN = Config.getBooleanProperty("task_run");
	private static boolean SUB_TASK_RUN = Config.getBooleanProperty("sub_task_run");
	
	private static String VIDEO_TASK_SCHEDULE = Config.getProperty("video_task_schedule");
	private static String VIDEO_SUB_TASK_SCHEDULE = Config.getProperty("video_sub_task_schedule");
	private static String TASK_SCHEDULE = Config.getProperty("task_schedule");
	private static String SUB_TASK_SCHEDULE = Config.getProperty("sub_task_schedule");
	
	private static String CHECK_RESET_SCHEDULE = Config.getProperty("check_reset_schedule");
	
	public static void main(String[] args) {
		start();
	}
	
	private static void start() {
		if(VIDEO_TASK_RUN) {
			log.info("video task work: " + VIDEO_TASK_SCHEDULE);
			QuartzManager.addJob("video-task-work", VideoTaskWorkImpl.class, VIDEO_TASK_SCHEDULE);
		}
		if(VIDEO_SUB_TASK_RUN) {
			log.info("video fetcher work: " + VIDEO_SUB_TASK_SCHEDULE);
			QuartzManager.addJob("video-fetcher-work", VideoFetcherWorkImpl.class, VIDEO_SUB_TASK_SCHEDULE);
		}
		if(TASK_RUN) {
			log.info("task work: " + TASK_SCHEDULE);
			QuartzManager.addJob("task-work", TaskWorkImpl.class, TASK_SCHEDULE);
		}
		if(SUB_TASK_RUN) {
			log.info("sub task work: " + SUB_TASK_SCHEDULE);
			QuartzManager.addJob("sub-task-work", FetcherWorkImpl.class, SUB_TASK_SCHEDULE);
		}
		
		if(StringUtils.isNotBlank(CHECK_RESET_SCHEDULE)) {
			log.info("check-and-reset-video-task: " + CHECK_RESET_SCHEDULE);
			QuartzManager.addJob("check-and-reset-video-task", ResetVideoTaskImpl.class, CHECK_RESET_SCHEDULE);
		}
	}
}
