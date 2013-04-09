import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;


public class Server {
	static private InetAddress hostIp;
	static private String host;	
	static private String nameSender;
	static private int port;
	static private int type;
	static private String ip;
	static private String path;
	
	static void init () {
		try{
			host = InetAddress.getLocalHost().getHostName();
			hostIp = InetAddress.getByName(host);
			nameSender = "Nastik";
			port = 9999;
			type = 0;
			ip = "255.255.255.255";
			path = "C:\\Users\\Asus\\Documents\\network\\";
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static class Sender extends Thread {
		
		private void writeString(String str, DataOutputStream out) throws IOException {
			byte[] strB = str.getBytes();
			out.writeInt(strB.length);
			out.write(strB);
		}
		
		public void run () {
			while (true) {
				try {
					Thread.sleep(1000);
					System.err.println("Sent\n");
					long time = System.currentTimeMillis();
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					DataOutputStream outData = new DataOutputStream(out);
					writeString(hostIp.getHostAddress(), outData);
					writeString(hostIp.getHostName(), outData);
					outData.writeLong(time);
					writeString(nameSender, outData);
					outData.writeInt(type);
					outData.writeInt(port);
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
		}
	}
	
	static class SendInfo extends Thread {
		Socket s;
		
		SendInfo(Socket s) {
			this.s = s;
		}
		
		public void run() {
			 try {
				 String list[] = new File(path).list();
				 boolean flag = false;
				 int index = 0;
				 for(int i = 0; i < list.length; ++i) {
					 String fp = path + list[i];
					 File f = new File(fp);
					 if (f.isFile()) {
						 flag = true;
						 index = i;
						 break;
					 }
				 }
				 ByteArrayOutputStream out = new ByteArrayOutputStream();
				 DataOutputStream outData = new DataOutputStream(out);
				 if (flag) {
					 FileInputStream inFile = new FileInputStream(path + list[index]);
					 int length = inFile.available();
					 outData.writeInt(1);
					 outData.writeInt(length);
					 byte file[] = new byte [length];
					 inFile.read(file, 0, length);
					 outData.write(file, 0, length);
					 inFile.close();
				 } else {
					 outData.writeInt(0);
				 }
				 OutputStream os = s.getOutputStream();
				 os.write(out.toByteArray());
				 os.flush();
				 s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		init();
		Thread sender = new Sender();
		sender.start();
		try {
			ServerSocket s = new ServerSocket();
			s.setReuseAddress(true);
			s.bind(new InetSocketAddress(hostIp, port));
			while (true) {
				SendInfo sC = new SendInfo(s.accept());
				sC.start();
			}
		} catch (Exception e) {
			e.printStackTrace(); 
		}
	}
}