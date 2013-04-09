import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Client5 {
	static private Map<InetAddress, Pair> servers = new HashMap<InetAddress, Pair>();
	static private BlockingQueue<DatagramPacket> queue = new LinkedBlockingQueue<DatagramPacket>();
	static private Map<InetAddress, Long> clients = new HashMap<InetAddress, Long>();

	static private String host;
	static private InetAddress hostIp;
	static private int port = 9999;

	static {
		try {
			host = InetAddress.getLocalHost().getHostName();
			hostIp = InetAddress.getByName(host);
		} catch (UnknownHostException ex) {
			ex.printStackTrace();
		}
	}

	public class Pair {

		private Long time;
		private Short port;

		public Pair() {
		}

		public Pair(Long t, short p) {
			time = t;
			port = p;
		}

		public Long getTime() {
			return time;
		}

		public Short getPort() {
			return port;
		}

	}

	static private class Parser extends Thread {

		private void parse(byte[] data, InetAddress ia) {
			try {
				InputStream in = new ByteArrayInputStream(data);
				DataInputStream inData = new DataInputStream(in);
				long time = System.currentTimeMillis();
				byte ans = inData.readByte();
				if (ans == 0) {
					clients.put(ia, time);
				} else if (ans == 1) {
					byte len = inData.readByte();
					byte[] b = new byte[len];
					for (int i = 0; i < len; ++i) {
						b[i] = inData.readByte();
					}
					String ip = new String(b, "UTF-8");
					InetAddress ia2 = InetAddress.getByName(ip);
					short port = inData.readShort();
					Pair p = new Client5().new Pair(time, port);
					servers.put(ia2, p);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				while (true) {
					if (queue.isEmpty()) {
						sleep(100);
						continue;
					}
					DatagramPacket pc = queue.poll(1, TimeUnit.SECONDS);
					byte[] data = pc.getData();
					InetAddress ia = pc.getAddress();
					if (data != null) {
						parse(data, ia);
					}
				}
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
	}

	private static class PrintHosts extends Thread {

		public void run() {
			while (true) {
				long time = System.currentTimeMillis();
				double eps = 10000;
				Iterator<Map.Entry<InetAddress, Pair>> it = servers.entrySet()
						.iterator();
				while (it.hasNext()) {
					Entry<InetAddress, Pair> e = it.next();
					if (e.getValue().getTime() < time - eps) {
						it.remove();
					}
				}
				Iterator<Map.Entry<InetAddress, Long>> it2 = clients.entrySet()
						.iterator();
				while (it2.hasNext()) {
					Entry<InetAddress, Long> e = it2.next();
					if (e.getValue() < time - eps) {
						it2.remove();
					}
				}
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	static private class Rec extends Thread {
		public void run() {
			try {
				DatagramSocket ds = new DatagramSocket(new InetSocketAddress(
						hostIp, port));
				while (true) {
					DatagramPacket pack = new DatagramPacket(new byte[1000],
							1000);
					ds.receive(pack);
					queue.put(pack);

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	static private class Connect extends Thread {
		Socket s;
		InetAddress destia;
		byte[] message;

		Connect(InetAddress ia, InetAddress dest, int port, byte[] m) {
			try {
				s = new Socket(ia, port);
				destia = dest;
				message = m;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void writeString(String str, DataOutputStream out)
				throws IOException {
			byte[] strB = str.getBytes();
			out.writeByte((byte) strB.length);
			out.write(strB);
		}

		public void run() {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream outData = new DataOutputStream(out);
			try {
				outData.writeByte(0);
				writeString(hostIp.getHostAddress(), outData);
				writeString(destia.getHostAddress(), outData);
				byte ask[] = out.toByteArray();
				OutputStream ans = s.getOutputStream();
				ans.write(ask);
				byte buf[] = new byte[1000];
				s.getInputStream().read(buf);
				InputStream in = new ByteArrayInputStream(buf);
				DataInputStream inData = new DataInputStream(in);
				byte flag = inData.readByte();
				if (flag == 0) {
					byte len = inData.readByte();
					byte[] sid = new byte[len];
					inData.read(sid);
					len = inData.readByte();
					byte[] dest = new byte[len];
					inData.read(dest);
					Socket ds = new Socket(destia, port);
					ByteArrayOutputStream out2 = new ByteArrayOutputStream();
					DataOutputStream outData2 = new DataOutputStream(out2);
					outData2.writeByte((byte)0xFF);
					outData2.writeByte((byte) sid.length);
					outData2.write(sid);
					outData2.writeShort((short) message.length);
					outData2.write(message);
					byte[] ask2 = out2.toByteArray();
					OutputStream ans2 = ds.getOutputStream();
					ans2.write(ask2);
				} else {
					byte len = inData.readByte();
					byte[] dest = new byte[len];
					inData.read(dest);
					if (!dest.equals(destia.getHostAddress())) {
						return;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	private static void sendMessage(InetAddress ia, byte[] message) {
		Iterator<Map.Entry<InetAddress, Pair>> it = servers.entrySet()
				.iterator();
		Entry<InetAddress, Pair> e;
		if (it.hasNext()) {
			e = it.next();
		} else {
			return;
		}
		Thread c = new Connect(e.getKey(), ia, e.getValue().getPort(), message);
		c.start();
	}

	static private class Requester extends Thread {

		private void getIp(byte ip[], String s) {
			int index = 0;
			for (int i = 0; i < s.length(); ++i) {
				int a = 0;
				while ((i < s.length()) && (s.charAt(i) != '.')) {
					a = a * 10 + s.charAt(i) - '0';
					++i;
				}
				ip[index] = (byte) a;
				++index;
			}
		}

		public void run() {
			while (true) {
				try {
					InputStreamReader in = new InputStreamReader(System.in);
					BufferedReader br = new BufferedReader(in);
					int t = br.read();
					t = t - '0';
					if (t == 0) {
						// Prints hosts
						Iterator<Map.Entry<InetAddress, Pair>> it = servers
								.entrySet().iterator();
						while (it.hasNext()) {
							Entry<InetAddress, Pair> e = it.next();
							System.err.println(e.getKey());
						}
					}
					if (t == 1) {
						// Prints clients
						Iterator<Map.Entry<InetAddress, Long>> it = clients
								.entrySet().iterator();
						while (it.hasNext()) {
							Entry<InetAddress, Long> e = it.next();
							System.err.println(e.getKey());
						}
					}
					if (t == 2) {
						// request
						String s = br.readLine();
						s = br.readLine();
						byte ip[] = new byte[4];
						getIp(ip, s);
						InetAddress ia = InetAddress.getByAddress(ip);
						byte message[] = br.readLine().getBytes();
						sendMessage(ia, message);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	static class ReciveMes extends Thread {
		Socket s;

		ReciveMes(Socket s) {
			this.s = s;
		}

		public void run() {
			try {
				byte buf[] = new byte[1000];
				s.getInputStream().read(buf);
				InputStream in = new ByteArrayInputStream(buf);
				DataInputStream inData = new DataInputStream(in);
				byte flag = inData.readByte();				
				if (flag != (byte) 0xFF) {
					//s.close();
					return;
				}
				byte len = inData.readByte();
				byte[] sid = new byte[len];
				inData.read(sid);
				short lenm = inData.readShort();
				byte[] message = new byte[lenm];
				inData.read(message);
				Iterator<Map.Entry<InetAddress, Pair>> it = servers.entrySet()
						.iterator();
				Entry<InetAddress, Pair> e;
				if (it.hasNext()) {
					e = it.next();
				} else {
					return;
				}
				Socket ser = new Socket(e.getKey(), e.getValue().getPort());
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream outData = new DataOutputStream(out);
				outData.writeByte((byte)0x01);
				outData.writeByte((byte) sid.length);
				outData.write(sid);
				outData.writeByte((byte) hostIp.getHostAddress().length());
				outData.write(hostIp.getHostAddress().getBytes());
				byte ask[] = out.toByteArray();
				OutputStream ans = ser.getOutputStream();
				ans.write(ask);
				byte buf2[] = new byte[1000];
				ser.getInputStream().read(buf2);
				InputStream in2 = new ByteArrayInputStream(buf2);
				DataInputStream inData2 = new DataInputStream(in2);
				flag = inData2.readByte();
				if (flag == (byte) 0x02) {
					len = inData2.readByte();
					byte[] rsid = new byte[len];
					inData2.read(rsid);
					if (Arrays.toString(rsid).equals(Arrays.toString(sid))) {
						String answer = new String(message, "UTF-8");
						System.out.println(answer);
					} else {
						System.err.println("Sid not equal");
					}
				} else {
					System.err.println("Server fail");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static class Listener extends Thread {
		public void run() {
			try {
				ServerSocket s = new ServerSocket();
				s.setReuseAddress(true);
				s.bind(new InetSocketAddress(hostIp, port));
				while (true) {
					ReciveMes sC = new ReciveMes(s.accept());
					sC.start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static class udpSender extends Thread {
		public void run() {
			while (true) {
				try {
					byte[] data = new byte[1];
					data[0] = 0;
					DatagramPacket pack = new DatagramPacket(data, 1,
							new InetSocketAddress("255.255.255.255", port));
					DatagramSocket ds = new DatagramSocket();
					ds.setBroadcast(true);
					ds.send(pack);
					ds.close();
					sleep(10000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) {
		try {
			System.err.println(hostIp.getHostAddress());
			Rec r = new Rec();
			Parser p = new Parser();
			PrintHosts ph = new PrintHosts();
			Requester rec = new Requester();
			udpSender d = new udpSender();
			Listener l = new Listener();
			l.start();
			d.start();
			r.start();
			p.start();
			ph.start();
			rec.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
