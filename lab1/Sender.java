import java.net.*;
import java.io.*;

public class Sender {
	private static InetAddress hostIp;
	private static String host;	
	private static String nameSender;
	private static String ip;
	private static int port;
	
	static void init () {
		try{
			host = InetAddress.getLocalHost().getHostName();
			hostIp = InetAddress.getByName(host);
			nameSender = "Nastik";
			port = 9999;
			ip = "255.255.255.255";
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void writeString(String str, DataOutputStream out) throws IOException {
		byte[] strB = str.getBytes();
		out.writeInt(strB.length);
		out.write(strB);
	}
	
	private void sendMessage () {
		try { 
			System.err.println("Sent");
			long time = System.currentTimeMillis();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream outData = new DataOutputStream(out);
			writeString(hostIp.getHostAddress(), outData);
			writeString(hostIp.getHostName(), outData);
			outData.writeLong(time);
			writeString(nameSender, outData);
			
			byte data[] = out.toByteArray();
			DatagramPacket pack = new DatagramPacket(data, data.length, new InetSocketAddress(ip, port));
			DatagramSocket ds = new DatagramSocket();
			ds.setBroadcast(true);
			ds.send(pack);
			ds.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		init();
		Sender s = new Sender();
		while (true) {
			try {
				s.sendMessage();
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
