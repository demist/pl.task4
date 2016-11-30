import java.io.DataInputStream;
import java.io.PrintStream;
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
					WebSocketThread wst = new WebSocketThread(wsClientSocket);
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
		
		public WebSocketThread(Socket clientSocket)
		{
			this.wsClientSocket = clientSocket;
		}

		public void run()
		{
			try
			{
				BufferedReader in = new BufferedReader(new InputStreamReader(wsClientSocket.getInputStream()));
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
				  PrintStream os = new PrintStream(wsClientSocket.getOutputStream());
				  String response = "HTTP/1.1 101 Switching Protocols\r\n" +
					"Upgrade: websocket\r\n" +
					"Connection: Upgrade\r\n" + 
					"Sec-WebSocket-Accept: " + result + "\r\n\r\n";
				  os.write(response.getBytes());
				  os.flush();
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

	//player class used to represent and interact with clients
	private static class Player
	{

	}
}