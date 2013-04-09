import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class Client4 {
	static private Map<InetAddress, Long> hosts = new HashMap<InetAddress, Long>();
	static private BlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>();

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

	private static class Rec extends Thread {
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

	private static class Parser extends Thread {
		private static JFrame mainWindow = new JFrame("=)");
		
		static {
			mainWindow.setBounds(100,100,500,500);
		    mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		
		private static void vizual(int x, int y, int z) {
			System.out.println("create window");
		    Color c = new Color(x, y, z);
		    mainWindow.setBackground(c);
		    JPanel panel = new JPanel();
		    panel.setBackground(c);
		    mainWindow.setContentPane(panel);
		    mainWindow.setVisible(true);
		}

		private void getDel(Socket s, InetAddress ia) {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream outData = new DataOutputStream(out);
				long time1 = System.currentTimeMillis();
				outData.writeLong(time1);
				byte data[] = out.toByteArray();
				OutputStream ans = s.getOutputStream();
				ans.write(data);
				byte buf[] = new byte[1024];
				s.getInputStream().read(buf);
				InputStream in = new ByteArrayInputStream(buf);
				DataInputStream inData = new DataInputStream(in);
				long time2 = inData.readLong();
				long time3 = inData.readLong();
				long time4 = System.currentTimeMillis();
				long delta = ((time2 - time1) + (time3 - time4)) / 2;
				System.err.println(time1 + " " + time2 + " " + time3 + " " + time4 + " " + delta);
				hosts.put(ia, delta);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

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
				//System.err.println(ip);
				InetAddress ia = InetAddress.getByName(ip);
				len = inData.readInt();
				b = new byte[len];
				for (int i = 0; i < len; ++i) {
					b[i] = inData.readByte();
				}
				int port = inData.readInt();
				String host = new String(b, "UTF-8");
				//System.err.println(host);
				long time = inData.readLong();
				int x = inData.readInt();
				int y = inData.readInt();
				int z = inData.readInt();
				System.out.println(ia + " " + port + " " + x + " " + y + " " + z);
				if (!hosts.containsKey(ia)) {
					Socket s = new Socket(ia, port);
					getDel(s, ia);
				}
				long time2 = System.currentTimeMillis() + hosts.get(ia);
				System.out.println(time + "  " + time2 + "  " + hosts.get(ia));
				if (time2 < time) {
					vizual(x, y, z);
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

	public static void main(String[] args) {
		try {
			Rec r = new Rec();
			Parser p = new Parser();
			r.start();
			p.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
