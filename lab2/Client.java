import java.net.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

public class Client {
	static private Map<InetAddress, Long> hosts = new HashMap<InetAddress, Long>();
	static private BlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>();
	static private List<Message> mes = new ArrayList<Message>();

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

	static private class Message {
		InetAddress ia;
		String host;
		String nameSender;
		long time;
		int port;
		int type;

		Message(InetAddress ia, String host, String nameSender, long time,
				int type, int port) {
			this.ia = ia;
			this.host = host;
			this.nameSender = nameSender;
			this.time = time;
			this.port = port;
			this.type = type;
		}
	}

	static private class Connect extends Thread {
		Socket s;

		Connect(InetAddress ia, int port) {
			try {
				s = new Socket(ia, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			byte buf[] = new byte[10000];
			try {
				s.getInputStream().read(buf);
				InputStream in = new ByteArrayInputStream(buf);
				DataInputStream inData = new DataInputStream(in);
				int count = inData.readInt();
				for (int i = 0; i < count; ++i) {
					int len = inData.readInt();
					byte b[] = new byte[len];
					for (int j = 0; j < len; ++j) {
						b[j] = inData.readByte();
					}
					String answer = new String(b, "UTF-8");
					System.out.println(answer + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static private class Parser extends Thread {

		private void parse(byte[] data) {
			try {
				InputStream in = new ByteArrayInputStream(data);
				DataInputStream inData = new DataInputStream(in);
				int len = inData.readInt();
				byte b[] = new byte[len];
				for (int i = 0; i < len; ++i) {
					b[i] = inData.readByte();
				}
				String ip = new String(b, "UTF-8");
				InetAddress ia = InetAddress.getByName(ip);
				len = inData.readInt();
				b = new byte[len];
				for (int i = 0; i < len; ++i) {
					b[i] = inData.readByte();
				}
				String host = new String(b, "UTF-8");
				long time = inData.readLong();
				time = System.currentTimeMillis();
				len = inData.readInt();
				b = new byte[len];
				for (int i = 0; i < len; ++i) {
					b[i] = inData.readByte();
				}
				String nameSender = new String(b, "UTF-8");
				int type = inData.readInt();
				int port = inData.readInt();
				Message m = new Message(ia, host, nameSender, time, type, port);
				mes.add(m);
				hosts.put(ia, time);
				Thread c = new Connect(ia, port);
				c.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				while (true) {
					byte[] data = queue.poll(1, TimeUnit.SECONDS);
					if (data != null) {
						parse(data);
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
				Iterator<Map.Entry<InetAddress, Long>> it = hosts.entrySet()
						.iterator();
				long time = System.currentTimeMillis();
				double eps = 10000;
				while (it.hasNext()) {
					Entry<InetAddress, Long> e = it.next();
					if (e.getValue() < time - eps) {
						it.remove();
					} else {
						System.out.println(e.getKey().toString());
					}
				}
				System.out.println();
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
					queue.put(pack.getData());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		try {
			Rec r = new Rec();
			Parser p = new Parser();
			PrintHosts ph = new PrintHosts();
			r.start();
			p.start();
			ph.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
