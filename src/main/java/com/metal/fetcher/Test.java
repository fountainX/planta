package com.metal.fetcher;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.network.Ip4;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

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
}
