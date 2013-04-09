import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class Server4 {
	static private InetAddress hostIp;
	static private String host;
	static private int port;
	static private String ip;

	static void init() {
		try {
			host = InetAddress.getLocalHost().getHostName();
			hostIp = InetAddress.getByName(host);
			port = 9999;
			ip = "255.255.255.255";
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static class Sender extends Thread {

		private void writeString(String str, DataOutputStream out)
				throws IOException {
			byte[] strB = str.getBytes();
			out.writeInt(strB.length);
			out.write(strB);
		}

		public void run() {
			while (true) {
				try {
					Thread.sleep(10000);
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					DataOutputStream outData = new DataOutputStream(out);
					writeString(hostIp.getHostAddress(), outData);
					writeString(hostIp.getHostName(), outData);
					outData.writeInt(port);
					Random rnd = new Random();
					int x = (rnd.nextInt() % 256 + 256) % 256;
					int y =  (rnd.nextInt() % 256 + 256) % 256;
					int z =  (rnd.nextInt() % 256 + 256) % 256;
					long time = System.currentTimeMillis() + 5000;
					outData.writeLong(time);
					outData.writeInt(x);
					outData.writeInt(y);
					outData.writeInt(z);
					System.out.println(x + " " + y + " " + z);
					byte data[] = out.toByteArray();
					DatagramPacket pack = new DatagramPacket(data, data.length,
							new InetSocketAddress(ip, port));
					DatagramSocket ds = new DatagramSocket();
					ds.send(pack);
					ds.close();
					System.err.println("Send");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static class Sinc extends Thread {
		Socket s;

		Sinc(Socket s) {
			this.s = s;
		}

		public void run() {
			try {
				InputStream is = s.getInputStream();
				byte data[] = new byte[10];
				is.read(data);
				long time = System.currentTimeMillis();
				OutputStream os = s.getOutputStream();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream outData = new DataOutputStream(out);
				outData.writeLong(time);
				outData.writeLong(System.currentTimeMillis());
				byte answer[] = out.toByteArray();
				os.write(answer);
				s.close();
			} catch (Exception e) {
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
				Sinc sC = new Sinc(s.accept());
				sC.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
