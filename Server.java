import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Scanner;
import java.io.File;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.nio.ByteBuffer;

public class Server
{
    public static class Encoders 
    {
	static byte[] sha1(String input) throws NoSuchAlgorithmException
	{
	  MessageDigest mDigest = MessageDigest.getInstance("SHA1");
	  byte[] result = mDigest.digest(input.getBytes());
	  return result;
	}
    }

	private static ServerSocket serverSocket = null;
	private static Socket clientSocket = null;
	private static ArrayList<Player> players;

	//server starts here
	public static void main(String args[])
	{
		players = new ArrayList<Player>();
		//starting game here

		//starting websocket server here
		WebSocketServer wss = new WebSocketServer(players);
		wss.start();
		//accepting new clients and send them base html page
		int portNumber = 7777;
		try
		{
			serverSocket = new ServerSocket(portNumber);
		}
		catch (IOException e)
		{
			System.out.println(e);
		}
		while (true)
		{
			try
			{
				clientSocket = serverSocket.accept();
				ClientThread thr = new ClientThread(clientSocket);
				thr.start();
			}
			catch (IOException e)
			{
				System.out.println(e);
			}
		}
	}

	//simple class used to send base html page to clients
	private static class ClientThread extends Thread
	{
		private Socket clientSocket = null;

		public ClientThread(Socket clientSocket)
		{
			this.clientSocket = clientSocket;
		}

		public void run()
		{
			try
			{
			DataInputStream is = new DataInputStream(clientSocket.getInputStream());
			PrintStream os = new PrintStream(clientSocket.getOutputStream());
			//sending base html page
        		String content = new Scanner(new File("index.html")).useDelimiter("\\Z").next();
        		String header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + content.length() + "\r\n" +
                        "Connection: close\r\n\r\n";
        		String response = header + content;
        		os.write(response.getBytes());
        		os.flush();
        		//field update will be on its turn
			}
			catch (IOException e)
			{
				System.out.println(e);
			}
		}
	}

	//game class
	private static class GameThread extends Thread
	{
		public GameThread()
		{

		}

		public void run()
		{

		}
	}

	//class used to register new WebSocket clients
	private static class WebSocketServer extends Thread
	{
		private static ArrayList<Player> players;
		private static ServerSocket wsServerSocket;

		public WebSocketServer(ArrayList<Player> players)
		{
			this.players = players;
		}

		public void run()
		{
			int wsPortNumber = 7778;
			try
			{
				wsServerSocket = new ServerSocket(wsPortNumber);
			}
			catch (IOException e)
			{
				System.out.println(e);
			}
			while (true)
			{
				try
				{
					Socket wsClientSocket = wsServerSocket.accept();
					WebSocketThread wst = new WebSocketThread(wsClientSocket, players);
					wst.start();
				}
				catch (IOException e)
				{
					System.out.println(e);
				}
			}
		}
	}

	//simple class used to establish WebSocketConnection
	private static class WebSocketThread extends Thread
	{
		private Socket wsClientSocket = null;
		ArrayList<Player> players;
		private BufferedReader in = null;
		private PrintStream os = null;
		private InputStream inp = null;
		
		public WebSocketThread(Socket clientSocket, ArrayList<Player> players)
		{
			this.wsClientSocket = clientSocket;
			this.players = players;
		}

		public void run()
		{
			try
			{
				in = new BufferedReader(new InputStreamReader(wsClientSocket.getInputStream()));
				String inputLine;
				String rawkey = "";
				while (!(inputLine = in.readLine()).equals(""))
				{
				      String[] parts = inputLine.split(":");
				      if (parts[0].equals("Sec-WebSocket-Key"))
					  rawkey = parts[1];
				}
				try
				{
				  String key = rawkey.substring(1);
				  String toHash = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
				  byte[] hashSHA1 = Encoders.sha1(toHash);
				  String result = Base64.getEncoder().encodeToString(hashSHA1);
				  os = new PrintStream(wsClientSocket.getOutputStream());
				  String response = "HTTP/1.1 101 Switching Protocols\r\n" +
					"Upgrade: websocket\r\n" +
					"Connection: Upgrade\r\n" + 
					"Sec-WebSocket-Accept: " + result + "\r\n\r\n";
				  os.write(response.getBytes());
				  os.flush();
				  inp = wsClientSocket.getInputStream();
				  Player newOne = new Player(wsClientSocket, inp, os);
				  Point g = newOne.getTurn(1);
				  //synchronize here
				  players.add(newOne);
				}
				catch (Exception e)
				{
				  System.out.println(e);
				}
			}
			catch (IOException e)
			{
				System.out.println(e);
			}
		}
	}
	//util class
	public static class Point
	{
	  public int x;
	  public int y;
	  public Point(int x, int y)
	  {
	    this.x = x;
	    this.y = y;
	  }
	}
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for ( int j = 0; j < bytes.length; j++ ) {
        int v = bytes[j] & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
}

	public static byte xor(byte a, byte b)
	{
	  return (byte)((int)a ^ (int)b);
	}
	
	
	//player class used to represent and interact with clients
	private static class Player
	{
	    private InputStream inp = null;
	    private DataInputStream dinp = null;
	    private PrintStream os = null;
	    private Socket wsSocket = null;
	    public Player(Socket wsSocket, InputStream inp, PrintStream os)
	    {
	      this.wsSocket = wsSocket;
	      this.inp = inp;
	      this.dinp = new DataInputStream(this.inp);
	      this.os = os;
	    }
	    public void sendTurn(int id, Point p)
	    {
	      byte[] typeBytes = ByteBuffer.allocate(4).putInt(1).array();
	      byte[] idBytes = ByteBuffer.allocate(4).putInt(id).array();
	      byte[] xBytes = ByteBuffer.allocate(4).putInt(p.x).array();
	      byte[] yBytes = ByteBuffer.allocate(4).putInt(p.y).array();
	      byte[] msg = new byte[18];
	      msg[0] = (byte) 0x82;
	      msg[1] = (byte) 0x10;
	      for (int i = 0; i < 4; i++)
	      {
		msg[2 + i] = typeBytes[i];
		msg[6 + i] = idBytes[i];
		msg[10 + i] = xBytes[i];
		msg[14 + i] = yBytes[i];
	      }
	      try
	      {
		os.write(msg);
		os.flush();
	      }
	      catch (IOException e)
	      {
		System.out.println(e);
	      }
	    }
	    public void sendCleanField()
	    {
	      byte[] typeBytes = ByteBuffer.allocate(4).putInt(2).array();
	      byte[] idBytes = ByteBuffer.allocate(4).putInt(0).array();
	      byte[] xBytes = ByteBuffer.allocate(4).putInt(0).array();
	      byte[] yBytes = ByteBuffer.allocate(4).putInt(0).array();
	      byte[] msg = new byte[18];
	      msg[0] = (byte) 0x82;
	      msg[1] = (byte) 0x10;
	      for (int i = 0; i < 4; i++)
	      {
		msg[2 + i] = typeBytes[i];
		msg[6 + i] = idBytes[i];
		msg[10 + i] = xBytes[i];
		msg[14 + i] = yBytes[i];
	      }
	      try
	      {
		os.write(msg);
		os.flush();
	      }
	      catch (IOException e)
	      {
		System.out.println(e);
	      }
	    }
	    	    
	    public Point getTurn(int id)
	    {
	      byte[] typeBytes = ByteBuffer.allocate(4).putInt(3).array();
	      byte[] idBytes = ByteBuffer.allocate(4).putInt(id).array();
	      byte[] xBytes = ByteBuffer.allocate(4).putInt(0).array();
	      byte[] yBytes = ByteBuffer.allocate(4).putInt(0).array();
	      byte[] msg = new byte[18];
	      msg[0] = (byte) 0x82;
	      msg[1] = (byte) 0x10;
	      for (int i = 0; i < 4; i++)
	      {
		msg[2 + i] = typeBytes[i];
		msg[6 + i] = idBytes[i];
		msg[10 + i] = xBytes[i];
		msg[14 + i] = yBytes[i];
	      }
	      try
	      {
		os.write(msg);
		os.flush();
		//wait here for client's response
		int a = 0;
		boolean flag = true;
		while (flag)
		{
		  a = inp.available();
		  if (a > 0) // two integers are already in stream
		     flag = false;
		}
		//reading
		byte[] TBytes = new byte[a];
		if (dinp.read(TBytes, 0, a) != a)
		  System.out.println("error reading websocket!");
		byte[] lenB = new byte[4];
		lenB[3] = TBytes[1];
		ByteBuffer b1 = ByteBuffer.wrap(lenB);
		int len = (b1.getInt()) - 128; //we know it is masked, so we minus 128
		//we know len is 7 bits, because we won't send messages longer than 126 bytes
		byte[] mask = new byte[4];
		for (int i = 0; i < 4; i++)
		  mask[i] = TBytes[2 + i];
		byte[] str = new byte[len];
		for (int i = 0; i < len; i++)
		  str[i] = TBytes[6 + i];
		byte[] resstr = new byte[len];
		for (int i = 0; i < len; i++)
		  resstr[i] = xor(str[i], mask[i % 4]);
		String res = new String(resstr);
		String[] parts = res.split(":");
		int x = Integer.parseInt(parts[0]);
		int y = Integer.parseInt(parts[1]);
		System.out.println(x);
		System.out.println(y);
		return new Point(x, y);
		
	      }
	      catch (IOException e)
	      {
		System.out.println(e);
	      }
	      return new Point(0, 0);
	    }
	    public void tellWin(int id)
	    {
	      byte[] typeBytes = ByteBuffer.allocate(4).putInt(4).array();
	      byte[] idBytes = ByteBuffer.allocate(4).putInt(id).array();
	      byte[] xBytes = ByteBuffer.allocate(4).putInt(0).array();
	      byte[] yBytes = ByteBuffer.allocate(4).putInt(0).array();
	      byte[] msg = new byte[18];
	      msg[0] = (byte) 0x82;
	      msg[1] = (byte) 0x10;
	      for (int i = 0; i < 4; i++)
	      {
		msg[2 + i] = typeBytes[i];
		msg[6 + i] = idBytes[i];
		msg[10 + i] = xBytes[i];
		msg[14 + i] = yBytes[i];
	      }
	      try
	      {
		os.write(msg);
		os.flush();
		//wait here for client's response
	      }
	      catch (IOException e)
	      {
		System.out.println(e);
	      }
	    }
	    public void setPlayerId(int id)
	    {
	      byte[] typeBytes = ByteBuffer.allocate(4).putInt(5).array();
	      byte[] idBytes = ByteBuffer.allocate(4).putInt(id).array();
	      byte[] xBytes = ByteBuffer.allocate(4).putInt(0).array();
	      byte[] yBytes = ByteBuffer.allocate(4).putInt(0).array();
	      byte[] msg = new byte[18];
	      msg[0] = (byte) 0x82;
	      msg[1] = (byte) 0x10;
	      for (int i = 0; i < 4; i++)
	      {
		msg[2 + i] = typeBytes[i];
		msg[6 + i] = idBytes[i];
		msg[10 + i] = xBytes[i];
		msg[14 + i] = yBytes[i];
	      }
	      try
	      {
		os.write(msg);
		os.flush();
		//wait here for client's response
	      }
	      catch (IOException e)
	      {
		System.out.println(e);
	      }
	    }
	}
}