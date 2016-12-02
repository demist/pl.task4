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
		GameThread game = new GameThread(players);
		game.start();
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
	    private ArrayList<Player> players;
	    private int lastInGame = -1;
	    private ArrayList<ArrayList<Integer>> game = null;
		public GameThread(ArrayList<Player> players)
		{
		  this.players = players;
		}

		private void newGame()
		{
			game = new ArrayList<ArrayList<Integer>>(); // 20 - start size
			//preparing game field
			for (int i = 0; i < 20; i++)
			{
				ArrayList<Integer> inner = new ArrayList<Integer>(game.size());
				for (int k = 0; k < 20; k++)
					inner.add(-1);
				game.add(inner);
			}
		}

		private int checkFive(int a, int b, int c, int d, int e)
    	{
      		if (a == -1)
        		return -1;
      		if (a != b)
        		return -1;
      		if (a != c)
        		return -1;
      		if (a != d)
        		return -1;
      		if (a != e)
        		return -1;
      		return a;
    	}

		//very simple, we can make lazy check with knowledge of the last turn
    	private int checkWin()
    	{
      		//horizontal case _
      		for (int i = 0; i < game.size(); i++)
      		{
        		ArrayList<Integer> cur = game.get(i);
        		for (int j = 0; j < cur.size() - 4; j++)
        		{
          			int a = checkFive(cur.get(j), cur.get(j + 1), cur.get(j + 2), cur.get(j + 3), cur.get(j + 4));
          			if (a != -1)
            			return a;
        		}
      		}
      		//vertical case |
      		for (int i = 0; i < game.size() - 4; i++)
      		{
        		for (int j = 0; j < game.size(); j++)
        		{
          			int a = checkFive(game.get(i).get(j), game.get(i + 1).get(j), game.get(i + 2).get(j), game.get(i + 3).get(j), game.get(i + 4).get(j));
          			if (a != -1)
            			return a;
        		}
      		}
      		//diagonal case 1 /
      		for (int i = 0; i < game.size() - 4; i++)
      		{
        		for (int j = 0; j < game.size() - 4; j++)
        		{
          			int a = checkFive(game.get(i + 4).get(j), game.get(i + 3).get(j + 1), game.get(i + 2).get(j + 2), game.get(i + 1).get(j + 3), game.get(i).get(j + 4));
          			if (a != -1)
            			return a;
        		}
      		}
      		//diagonal case 2 \
      		for (int i = 0; i < game.size() - 4; i++)
      		{
        		for (int j = 0; j < game.size() - 4; j++)
        		{
          			int a = checkFive(game.get(i).get(j), game.get(i + 1).get(j + 1), game.get(i + 2).get(j + 2), game.get(i + 3).get(j + 3), game.get(i + 4).get(j + 4));
          			if (a != -1)
            			return a;
        		}
      		}
      		//default
      		return -1;
    	}

    	private void nextTurn(int id, Point p)
    	{
			//field always square
			//x < 0 or y < 0 means cells on left (on top) of current field
			//agreement is that x can be only -1 or size(), y -1 or size()
			//player can enlarge field only for one cell(one turn - one cell)
			//optimization - we enlarge game field for 5 cells on every side
			if (p.x == -1 || p.x == game.size() || p.y == -1 || p.y == game.size())
			{
		  		ArrayList<ArrayList<Integer>> ngame = new ArrayList<ArrayList<Integer>>();
		  		for (int i = 0; i < game.size() + 10; i++)
				{
					ArrayList<Integer> inner = new ArrayList<Integer>(game.size() + 10);
					for (int k = 0; k < game.size() + 10; k++)
						inner.add(-1);
					ngame.add(inner);
				}
		  		//retrieve already done turns
		  		for (int i = 0; i < game.size(); i++)
		    		for (int k = 0; k < game.size(); k++)
		      			ngame.get(i + 5).set(k + 5, game.get(i).get(k)); 
		  		//swap fields
		    	game = ngame;
		  		//do turn
				game.get(p.x + 5).set(p.y + 5, id); 
			}
			else
			{
		  		ArrayList<Integer> tmp = game.get(p.x); //always square
		  		tmp.set(p.y, id);
      		}
    	}

		public void run()
		{
			//waiting for 2 players min to start the game
			int size = 0;
			while (size < 2)
			{
				try
				{
					Thread.sleep(1000);
					synchronized(players)
					{
					  size = players.size();
					}
				}
				catch (Exception e)
				{
					System.out.println(e);
				}
			}
			//start game process
			newGame();
			int cur = 0;
			Point newTurn;
			Player curPl;
			int win;
			while (true)
			{
			    synchronized(players)
			    {
			      curPl = players.get(cur);
			    }
			  	//adding new players and sending them current game info
			  	//new players get game status at their turn
			  
			  	if (cur > lastInGame)
			  	{
			  		lastInGame = cur;
			    	curPl.setPlayerId(cur);
			    	for (int i = 0; i < game.size(); i++)
			      	for (int k = 0; k < game.size(); k++)
						if (game.get(i).get(k) != -1)
				    		curPl.sendTurn(game.get(i).get(k), new Point(i, k));
			  	}
			  	//getting turn
			  	newTurn = curPl.getTurn();
			  	nextTurn(cur, newTurn);
			  	synchronized(players)
			  	{
			    	for (int i = 0; i < players.size(); i++)
						players.get(i).sendTurn(cur, newTurn);
			  	}
			  	win = checkWin();
			  	if (win != -1)
			  	{
			    	synchronized(players)
			    	{
			      		for (int i = 0; i < players.size(); i++)
			      		{
							players.get(i).tellWin(win);
							players.get(i).sendCleanField();
			      		}
			    	}
			    	newGame();
			    	cur = -1;
			  	}
			  	synchronized(players)
			  	{
			    	size = players.size();
			  	}
			  	if (cur == size - 1)
			    	cur = 0;
			  	else
			    	cur++;
			}
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
				  	synchronized(players)
				  	{
				    	players.add(newOne);
				  	}
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
	    	    
	    public Point getTurn()
	    {
	    	byte[] typeBytes = ByteBuffer.allocate(4).putInt(3).array();
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
				//wait here for client's response
				int a = 0;
				boolean flag = true;
				while (flag)
				{
		  			a = inp.available();
		  			if (a > 0) // response is already in stream
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
	      	}
	      	catch (IOException e)
	      	{
				System.out.println(e);
	      	}
	    }
	}
}