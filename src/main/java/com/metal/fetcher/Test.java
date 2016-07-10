package com.metal.fetcher;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.network.Ip4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metal.fetcher.common.Constants;
import com.metal.fetcher.model.SubVideoTaskBean;
import com.metal.fetcher.model.VideoTaskBean;
import com.metal.fetcher.task.VideoTask;
import com.metal.fetcher.utils.DBHelper;
import com.metal.fetcher.utils.DBUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

	private static Logger log = LoggerFactory.getLogger(Test.class);
	
//  private static final Pattern partern = Pattern.compile("\0x7B\".*?\0x7D\"");
  private static final Pattern partern = Pattern.compile("content\":\".*?\"");

  public static void main(String[] args) {
//    final StringBuilder errbuf = new StringBuilder(); // for any error messages
//    final String file = "/home/fountain/private/vqq.pcapng";
//    Ip4 ip = new Ip4();
//    Tcp tcp = new Tcp(); // Preallocate a TCP header
//    System.out.printf("Opening file for reading: %s%n", file);

    StringBuilder errbuf = new StringBuilder();
    Pcap pcap = Pcap.openLive("wlp1s0",3000, Pcap.MODE_PROMISCUOUS, 10000, errbuf);
//    Pcap pcap = Pcap.openOffline(file, errbuf);
    if (pcap == null) {
      System.err.printf("Error while opening device for capture: " + errbuf.toString());
      return;
    }

    PcapBpfProgram program = new PcapBpfProgram();
    String expression = "host 182.254.4.227 or host 14.17.32.235";
    int optimize = 0;         // 0 = false
    int netmask = 0xFFFFFF00; // 255.255.255.0

    if (pcap.compile(program, expression, optimize, netmask) != Pcap.OK) {
      System.err.println(pcap.getErr());
      return;
    }

    if (pcap.setFilter(program) != Pcap.OK) {
      System.err.println(pcap.getErr());
      return;
    }

    PcapPacketHandler<String> jphTcp = new PcapPacketHandler<String>() {
      public void nextPacket(PcapPacket packet, String user) {
//        int payload = ip.getOffset() + ip.size(); // Payload after Ip4 header
//
//        int dataSize = packet.size() - payload;
        int dataSize = packet.size();
        if(dataSize > 66) {
          byte[] bytes = new byte[dataSize];
          System.out.println("data size:" + dataSize);
          packet.getByteArray(0, bytes);
          String tmpData = new String(bytes);
          Matcher matcher = partern.matcher(tmpData);
          String findStr = null;
//          System.out.println(tmpData);
          while(matcher.find()) {
            findStr = matcher.group();
            System.out.println("find:" + findStr);
            insertBarrage(0L, findStr.substring(10, findStr.length() - 1)); // please input a sub vid
          }
        }
      }
    };

    try {

      pcap.loop(-1, jphTcp, "");
    } finally {
      pcap.close();
    }
  }
  
//  public static void main(String[] args) {
//	  String str = "content\":hahaha\"";
//	  insertBarrage(84L, str.substring(10, str.length() - 1));
//  }
  
  
  /**
   * 
   * @param subVid sub vid
   * @param content	barrage content
   */
  private static void insertBarrage(long subVid, String content) {
	  Connection conn = null;
		List<SubVideoTaskBean> beans = null;
		try {
			conn = DBHelper.getInstance().getConnection();
			conn.setAutoCommit(false);
			QueryRunner qr = new QueryRunner();
			SubVideoTaskBean subVideo = qr.query(conn, "select sub_vid,vid,pd,tv_id from sub_video_task where sub_vid=?", new BeanListHandler<SubVideoTaskBean>(SubVideoTaskBean.class), subVid).get(0);
			qr.update(conn, "insert into tv_barrage(v_task_id,v_sub_task_id,tv_show_id,tv_show_vidio_no,barrage_platform,barrage_content,create_time) values(?,?,?,?,?,?,?)", subVideo.getVid(), subVideo.getSub_vid(), subVideo.getTv_id(), subVideo.getPd(), Constants.VIDEO_PLATFORM_TENGXUN, content.getBytes(), new Date());
			conn.commit();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			log.error("intsert barrage failed. ", e);
		} finally {
			DBHelper.release(conn);
		}
		return;
  }
}
