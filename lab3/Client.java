import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Client {
	static private Map<InetAddress, byte[]> hosts = new HashMap<InetAddress, byte[]>();
	static private Map<InetAddress, List<byte[]>> Allhash = new HashMap<InetAddress, List<byte[]>>();
	static private BlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>();

	static private String host;
	static private InetAddress hostIp;
	static private int port = 9998;

	static {
		try {
			host = InetAddress.getLocalHost().getHostName();
			hostIp = InetAddress.getByName(host);
		} catch (UnknownHostException ex) {
			ex.printStackTrace();
		}
	}

	private static void askList(InetAddress ia) {
		Socket s;
		try {
			s = new Socket(ia, port);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream outData = new DataOutputStream(out);
			outData.writeInt(0);
			byte data[] = out.toByteArray();
			OutputStream ans = s.getOutputStream();
			ans.write(data);
			byte buf[] = new byte[1024];
			s.getInputStream().read(buf);
			InputStream in = new ByteArrayInputStream(buf);
			DataInputStream inData = new DataInputStream(in);
			int count = inData.readInt();
			List<byte[]> list = new ArrayList<byte[]>();
			for (int i = 0; i < count; ++i) {
				int len = inData.readInt();
				byte b[] = new byte[len];
				inData.read(b);
				list.add(b);
			}
			Allhash.put(ia, list);
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void printmd5(byte[] b) {
		for (int i = 0; i < b.length; ++i) {
			int t = 15;
			int r1 = b[i] & t;
			if (r1 <= 9) {
				System.out.print(r1 + "");
			} else {
				System.out.print((char) (r1 - 10 + 'a') + "");
			}
			int r2 = (b[i] >> 4);
			r2 = r2 & t;
			if (r2 <= 9) {
				System.out.print(r2 + "");
			} else {
				System.out.print((char) (r2 - 10 + 'a') + "");
			}
		}
		System.out.println();
	}

	private static void getList() {
		Iterator<Map.Entry<InetAddress, byte[]>> it = hosts.entrySet()
				.iterator();
		while (it.hasNext()) {
			InetAddress ia = it.next().getKey();
			try {
				Socket s = new Socket(ia, port);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream outData = new DataOutputStream(out);
				outData.writeInt(0);
				byte data[] = out.toByteArray();
				OutputStream ans = s.getOutputStream();
				ans.write(data);
				byte buf[] = new byte[1024];
				s.getInputStream().read(buf);
				InputStream in = new ByteArrayInputStream(buf);
				DataInputStream inData = new DataInputStream(in);
				int count = inData.readInt();
				System.out.println(ia);
				for (int i = 0; i < count; ++i) {
					int len = inData.readInt();
					byte b[] = new byte[len];
					inData.read(b);
					String str = new String(b);
					// System.out.println(str);
					printmd5(b);
					// System.out.println(Arrays.toString(b));
				}
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static private void getFile(byte[] hash) {
		Iterator<Map.Entry<InetAddress, byte[]>> it = hosts.entrySet()
				.iterator();
		while (it.hasNext()) {
			InetAddress ia = it.next().getKey();
			Iterator<byte[]> it2 = Allhash.get(ia).iterator();
			Boolean flag = false;
			while (it2.hasNext()) {
				String l = Arrays.toString(it2.next());
				if (l.equals(Arrays.toString(hash))) {
					flag = true;
					continue;
				}
			}
			if (!flag)
				continue;
			try {
				Socket s = new Socket(ia, port);
				try {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					DataOutputStream outData = new DataOutputStream(out);
					outData.writeInt(1);
					outData.writeInt(hash.length);
					outData.write(hash);
					byte data[] = out.toByteArray();
					OutputStream ans = s.getOutputStream();
					ans.write(data);
					byte buf[] = new byte[1024];
					s.getInputStream().read(buf);
					InputStream in = new ByteArrayInputStream(buf);
					DataInputStream inData = new DataInputStream(in);
					int have = inData.read();
					if (have == 0) {
						continue;
					}
					int length = inData.readInt();
					byte answer[] = new byte[length];
					inData.read(answer);
					System.out.println(new String(answer, "UTF-8"));
					s.close();
					return;
				} catch (Exception e) {
					e.printStackTrace();
					s.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("no contains");
	}

	static private void put(InetAddress ia, byte data[]) {
		try {
			Socket s = new Socket(ia, port);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream outData = new DataOutputStream(out);
			outData.writeInt(2);
			outData.writeInt(data.length);
			outData.write(data);
			byte answer[] = out.toByteArray();
			OutputStream ans = s.getOutputStream();
			ans.write(answer);
			byte b[] = new byte[10];
			s.getInputStream().read(b);
			InputStream in = new ByteArrayInputStream(b);
			DataInputStream inData = new DataInputStream(in);
			System.out.println(inData.read());
		} catch (Exception e) {
			e.printStackTrace();
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
				len = inData.readInt();
				b = new byte[len];
				for (int i = 0; i < len; ++i) {
					b[i] = inData.readByte();
				}
				if ((!hosts.containsKey(ia)) || (!hosts.get(ia).equals(b))) {
					askList(ia);
					hosts.put(ia, b);
				}
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
			} catch (Exception e) {
				e.printStackTrace();
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

		private byte[] getbytesmd5(String s) {
			byte result[] = new byte[s.length() / 2];
			for (int i = 0; i < s.length() / 2; i += 1) {
				char ch = s.charAt(i * 2);
				int t1;
				if (ch >= 'a' && ch <= 'f') {
					t1 = ch - 'a' + 10;
				} else {
					t1 = ch - '0';
				}
				ch = s.charAt(i * 2 + 1);
				int t2;
				if (ch >= 'a' && ch <= 'f') {
					t2 = ch - 'a' + 10;
				} else {
					t2 = ch - '0';
				}
				result[i] = (byte) ((t2 << 4) | t1);
			}
			return result;
		}

		public void run() {
			while (true) {
				try {
					InputStreamReader in = new InputStreamReader(System.in);
					BufferedReader br = new BufferedReader(in);
					int t = br.read();
					t = t - '0';
					if (t == 0) {
						System.err.println("GetList");
						getList();
					} else if (t == 1) {
						String s = br.readLine();
						s = br.readLine();
						byte b[] = getbytesmd5(s);
						getFile(b);
					} else if (t == 2) {
						String s = br.readLine();
						s = br.readLine();
						byte ip[] = new byte[4];
						getIp(ip, s);
						InetAddress ia = InetAddress.getByAddress(ip);
						byte file[] = br.readLine().getBytes();
						put(ia, file);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) {
		try {
			Rec r = new Rec();
			Parser p = new Parser();
			Requester req = new Requester();
			r.start();
			p.start();
			req.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
