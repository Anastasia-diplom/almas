import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;

public class Server {
	static private InetAddress hostIp;
	static private String host;
	static private int port;
	static private String ip;
	static private String path;
	static private byte[] contentid;

	static void init() {
		try {
			host = InetAddress.getLocalHost().getHostName();
			hostIp = InetAddress.getByName(host);
			port = 9998;
			ip = "255.255.255.255";
			path = "C:\\Users\\Asus\\Documents\\network\\";
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static public byte[] getHash(File f)  {
		byte b[];
		try {
			FileInputStream ifs = new FileInputStream(f);
			MessageDigest md = MessageDigest.getInstance("MD5");
			int len = ifs.available();
			b = new byte [len];
			ifs.read(b, 0, len); 
			ifs.close();
			b = Arrays.copyOf(b, len);
			byte hash[] = md.digest(b);
			return hash;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	static public byte[] getHash(byte b[]) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte hash[] = md.digest(b);
			return hash;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void writeString(String str, DataOutputStream out)
			throws IOException {
		byte[] strB = str.getBytes();
		out.writeInt(strB.length);
		out.write(strB);
	}
	
	static class Sender extends Thread {

		public void run() {
			while (true) {
				try {
					Thread.sleep(1000);					
					String list[] = new File(path).list();
					int length = 0;
					for (int i = 0; i < list.length; ++i) {
						String fp = path + list[i];
						File f = new File(fp);
						if (f.isFile()) {
							FileInputStream ifs = new FileInputStream(f);
							length += ifs.available();
							ifs.close();
						}
						
					}
					byte all[] = new byte[length];
					int index = 0;
					for (int i = 0; i < list.length; ++i) {
						String fp = path + list[i];
						File f = new File(fp);
						if (f.isFile()) {
							FileInputStream ifs = new FileInputStream(f);
							int len = ifs.available();
							ifs.read(all, index, len);
							index += len;
							ifs.close();
						}
					}
					contentid = getHash(all);
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					DataOutputStream outData = new DataOutputStream(out);
					writeString(hostIp.getHostAddress(), outData);
					writeString(hostIp.getHostName(), outData);
					outData.writeInt(contentid.length);
					outData.write(contentid);
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

	static class ProcRequest extends Thread {
		Socket s;

		ProcRequest(Socket s) {
			this.s = s;
		}

		private void commandList(DataOutputStream outData,
				ByteArrayOutputStream out) {
			try {
				String list[] = new File(path).list();
				int countFiles = 0;
				for (int i = 0; i < list.length; ++i) {
					String fp = path + list[i];
					File f = new File(fp);
					if (f.isFile()) {
						++countFiles;
					}
				}
				outData.writeInt(countFiles);
				for (int i = 0; i < list.length; ++i) {
					String fp = path + list[i];
					File f = new File(fp);
					if (f.isFile()) {
						byte hash[] = getHash(f);
						outData.writeInt(hash.length);
						outData.write(hash);
					}
				}
				byte answer[] = out.toByteArray();
				OutputStream ans = s.getOutputStream();
				ans.write(answer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void commandGet(DataInputStream inData,
				ByteArrayOutputStream out, DataOutputStream outData) {
			try {
				int length = inData.readInt();
				byte findhash[] = new byte [length];
				inData.read(findhash, 0, length);
				String fhash = Arrays.toString(findhash);
				String list[] = new File(path).list();
				boolean flag = false;
				for (int i = 0; i < list.length; ++i) {
					String fp = path + list[i];
					File f = new File(fp);
					if (f.isFile()) {
						String hash = Arrays.toString(getHash(f));
						if (hash.equals(fhash)) {
							FileInputStream inFile = new FileInputStream(f);
							int len = inFile.available();
							outData.write(1);
							outData.writeInt(len);
							byte file[] = new byte[len];
							inFile.read(file);
							outData.write(file);
							flag = true;
							System.out.println("find");
							inFile.close();
							break;
						}
					}
				}
				if (!flag) {
					outData.write(0);
				}
				byte answer[] = out.toByteArray();
				OutputStream ans = s.getOutputStream();
				ans.write(answer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void commandPut(DataInputStream inData,
				ByteArrayOutputStream out, DataOutputStream outData) {
			try {
				int length = inData.readInt();
				byte addfile[] = new byte[length];
				inData.read(addfile);
				byte[] findhash = getHash(addfile);
				String list[] = new File(path).list();
				boolean flag = false;
				for (int i = 0; i < list.length; ++i) {
					String fp = path + list[i];
					File f = new File(fp);
					if (f.isFile()) {
						byte[] hash = getHash(f);
						if (hash.equals(findhash)) {
							flag = true;
							break;
						}
					}
				}
				if (flag) {
					outData.write(0);
				} else {
					outData.write(1);
					int index = list.length + 1;
					String fname = new String("newFile" + index + ".txt");
					System.out.println(fname);
					File f = new File(path + fname);
					OutputStream outfile = new FileOutputStream(f);
					outfile.write(addfile);
					outfile.close();
				}
				byte answer[] = out.toByteArray();
				OutputStream ans = s.getOutputStream();
				ans.write(answer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				InputStream is = s.getInputStream();
				byte data[] = new byte[1024];
				is.read(data);
				InputStream in = new ByteArrayInputStream(data);
				DataInputStream inData = new DataInputStream(in);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream outData = new DataOutputStream(out);
				int command = inData.readInt();
				if (command == 0) {
					System.out.println("List");
					commandList(outData, out);
				} else if (command == 1) {
					System.out.println("Get");
					commandGet(inData, out, outData);
				} else if (command == 2) {
					System.out.println("Put");
					commandPut(inData, out, outData);
				}
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
				ProcRequest sC = new ProcRequest(s.accept());
				sC.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}