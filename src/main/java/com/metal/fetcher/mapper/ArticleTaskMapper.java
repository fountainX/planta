package com.metal.fetcher.mapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metal.fetcher.common.Constants;
import com.metal.fetcher.model.Article;
import com.metal.fetcher.model.SubTask;
import com.metal.fetcher.model.Task;
import com.metal.fetcher.utils.DBHelper;
import com.metal.fetcher.utils.DBUtils;

public class ArticleTaskMapper {
	private static Logger log = LoggerFactory.getLogger(ArticleTaskMapper.class);
	
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

	private static final String QUERY_TASK_BY_STATUS = "select task_id,key_word,status,start_time,end_time from task where status=? limit ?";
	
	private static final String QUERY_SUB_TASK_BY_STATUS = "select sub_task_id,task_id,platform,url,status,start_time,end_time from sub_task where status=? limit ?";
	
	private static final String INSERT_TASK_SQL = "insert into task (key_word,status,start_time) values (?,?,?)";
	
	private static final String INSERT_SUB_TASK_SQL = "insert into sub_task (task_id,platform,url,status) values (?,?,?,?) on DUPLICATE key UPDATE status=?";
	
	private static final String QUERY_ARTICLEID_BY_URL = "select article_id from article where url=?";
	
	private static final String QUERY_ARTICLEID_BY_PLATFORM_AND_TITLE = "select article_id from article where platform=? and title=?";
	
	private static final String INSERT_ARTICLE = "insert into article(url, platform,title,description,author_id,author_name,publish_time,status) values(?,?,?,?,?,?,?,?)";
	
	private static final String INSERT_ARTICLE_CONTENT = "insert into article_content(article_id,content) values(?,?)";
	
	private static final String INSERT_TASK_ARTICLE = "insert ignore into task_article(task_id,article_id) values(?,?)";
	
	private static final String UPDATE_SUB_TASK_STATUS = "update sub_task set status=? where sub_task_id=?";
	
	private static final String RESET_SUB_TASK = "update sub_task set status=0,start_time=now() where sub_task_id=?";
	
	private static final String UPDATE_TASK_STATUS = "update task set status=? where task_id=?";
	
	private static final String QUERY_SUB_TASK_BY_TASKID = "select sub_task_id,status from sub_task where task_id=?";
	
	public static void insertTask(String keyword) {
		DBUtils.update(INSERT_TASK_SQL, keyword, Constants.TASK_STATUS_INIT);
	}
	
	public static void insertSubTask(SubTask subTask) {
		DBUtils.update(INSERT_SUB_TASK_SQL, subTask.getTask_id(), subTask.getPlatform(), subTask.getUrl(), Constants.TASK_STATUS_INIT, Constants.TASK_STATUS_INIT);
	}

	public static void insertArticle(long taskId, Article article) {
		Connection conn = null;
		try {
			conn = DBHelper.getInstance().getConnection();
			conn.setAutoCommit(false);
			QueryRunner qr = new QueryRunner();
			List<Integer> articleIds = qr.query(conn, QUERY_ARTICLEID_BY_URL, new ColumnListHandler<Integer>(), article.getUrl());
			long articleId = 0;
			if(articleIds == null || articleIds.size() == 0) {
				if(StringUtils.isNotBlank(article.getTitle())) {
					articleIds = qr.query(conn, QUERY_ARTICLEID_BY_PLATFORM_AND_TITLE, new ColumnListHandler<Integer>(), article.getPlatform(), article.getTitle());
				}
				if(articleIds == null || articleIds.size() == 0) {
					articleId = qr.insert(conn, INSERT_ARTICLE, new ScalarHandler<Long>(), article.getUrl(), article.getPlatform(), article.getTitle(), 
							article.getDescription(), article.getAuthor_id(), article.getAuthor_name().getBytes(), article.getPublish_time(), 1);
					qr.update(conn, INSERT_ARTICLE_CONTENT, articleId, article.getContent().getBytes());
				} else {
					articleId = articleIds.get(0);
				}
			} else {
				articleId = articleIds.get(0);
			}
			qr.update(conn, INSERT_TASK_ARTICLE, taskId, articleId);
			conn.commit();
			conn.setAutoCommit(true);
		} catch (final SQLException e) {
			log.error("insert article failed. article: " + article.toString(), e);
		} finally {
			DBHelper.release(conn);
		}
	}
	
	public static void subTaskFinish(SubTask subTask) {
		Connection conn = null;
		try {
			conn = DBHelper.getInstance().getConnection();
			conn.setAutoCommit(false);
			QueryRunner qr = new QueryRunner();

			qr.update(conn, UPDATE_SUB_TASK_STATUS, Constants.TASK_STATUS_FINISH, subTask.getSub_task_id());

			List<SubTask> subTasks = qr.query(conn, QUERY_SUB_TASK_BY_TASKID, new BeanListHandler<SubTask>(SubTask.class), subTask.getTask_id());

			// check video status
			int status = Constants.TASK_STATUS_FINISH;
			for(SubTask bean : subTasks) {
				if(bean.getStatus() == Constants.TASK_STATUS_INIT || bean.getStatus() == Constants.TASK_STATUS_RUNNING) {
					status = Constants.TASK_STATUS_RUNNING;
					break;
				}
				if(bean.getStatus() == Constants.TASK_STATUS_STOP) {
					status = Constants.TASK_STATUS_STOP;
					continue;
				}
				if(bean.getStatus() == Constants.TASK_STATUS_EXSTOP) {
					status = Constants.TASK_STATUS_EXSTOP;
					continue;
				}
			}
			if(status != Constants.TASK_STATUS_RUNNING) {
				qr.update(conn, UPDATE_TASK_STATUS, status, subTask.getTask_id());
			}
			conn.commit();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			log.error("subTaskFinish:", e);
		} finally {
			DBHelper.release(conn);
		}
	}
	
	public static void subTaskReset(SubTask subTask) {
		DBUtils.update(RESET_SUB_TASK, subTask.getSub_task_id());
	}
	
	public static List<Task> getInitTasks(int limit) {
		Connection conn = null;
		List<Task> tasks = null;
		try {
			conn = DBHelper.getInstance().getConnection();
			conn.setAutoCommit(false);
			QueryRunner qr = new QueryRunner();
			tasks = qr.query(conn, QUERY_TASK_BY_STATUS, new BeanListHandler<Task>(Task.class), Constants.TASK_STATUS_INIT, limit);
			for(Task task : tasks) {
				qr.update(conn, UPDATE_TASK_STATUS, Constants.TASK_STATUS_RUNNING, task.getTask_id());				
			}
			conn.commit();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			log.error("get init tasks failed", e);
		} finally {
			DBHelper.release(conn);
		}
		return tasks;
	}
	
	public static List<SubTask> getInitSubTasks(int limit) {
		Connection conn = null;
		List<SubTask> subTasks = null;
		try {
			conn = DBHelper.getInstance().getConnection();
			conn.setAutoCommit(false);
			QueryRunner qr = new QueryRunner();
			subTasks = qr.query(conn, QUERY_SUB_TASK_BY_STATUS, new BeanListHandler<SubTask>(SubTask.class), Constants.TASK_STATUS_INIT, limit);
			for(SubTask subTask : subTasks) {
				qr.update(conn, UPDATE_SUB_TASK_STATUS, Constants.TASK_STATUS_RUNNING, subTask.getSub_task_id());		
			}
			conn.commit();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			log.error("get init sub task failed.", e);
		} finally {
			DBHelper.release(conn);
		}
		return subTasks;
	}
	
	public static void checkAndReSetSubTasks() {
		Connection conn = null;
		try {
			conn = DBHelper.getInstance().getConnection();
			conn.setAutoCommit(false);
			QueryRunner qr = new QueryRunner();
			// TODO
			DateTime now = new DateTime();
			int hour = now.getHourOfDay();
			DateTime hourTime = DateTime.parse(now.toString("yyyy-MM-dd HH:00:00"), DATE_FORMAT);
			List<Integer> taskIds = qr.query(conn, "select task_id from sub_task where status!=0 and reset_hour>0 and start_time<? and ?%reset_hour=0", new ColumnListHandler<Integer>(), hourTime.toString(DATE_FORMAT), hour);
			if(taskIds != null && taskIds.size() > 0) {
				log.info("task id: " + taskIds);
				for(int id : taskIds) {
					qr.update(conn, "update task set status=?,start_time=now() where task_id=? and status!=?", Constants.TASK_STATUS_RUNNING, id, Constants.TASK_STATUS_RUNNING);
				}
				qr.update(conn, "update sub_task set status=?, start_time=now() where status!=0 and reset_hour>0 and start_time<? and ?%reset_hour=0", Constants.TASK_STATUS_INIT, hourTime.toString(DATE_FORMAT), hour);
			} else {
				log.info("no sub task need to reset");
			}
			conn.commit();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			log.error("get check sub task failed.", e);
		} finally {
			DBHelper.release(conn);
		}
	}
	
	public static void main(String[] args) {
		System.out.println(DateTime.now().toString());
	}
}
