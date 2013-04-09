import java.net.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;


public class Receiver {
	static Map<InetAddress, Long> hosts = new HashMap<InetAddress, Long>();
	static BlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>();
	static List<Message> mes = new ArrayList<Message>();

	static String host;
	static InetAddress hostIp;
	static int port = 9999;

	static private class Message {
		InetAddress ia;
		String host;
		String nameSender;
		long time;

		Message(InetAddress ia, String host, String nameSender, long time) {
			this.ia = ia;
			this.host = host;
			this.nameSender = nameSender;
			this.time = time;
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
				len = inData.readInt();
				b = new byte[len];
				for (int i = 0; i < len; ++i) {
					b[i] = inData.readByte();
				}
				String nameSender = new String(b, "UTF-8");
				
				time = System.currentTimeMillis();
				Message m = new Message(ia, host, nameSender, time);
				mes.add(m);
				hosts.put(ia, time);
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
					queue.add(pack.getData());
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
