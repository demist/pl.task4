import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Scanner;
import java.io.File;
import java.util.ArrayList;


public class Server {
  private static ServerSocket serverSocket = null;
  private static Socket clientSocket = null;
  
  public static void main(String args[])
  {
    //game thread
    gameThread masterThread = new gameThread();
    masterThread.start();
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
	clientThread thr = new clientThread(clientSocket);
	thr.start();
      }
      catch (IOException e)
      {
      System.out.println(e);
      }
    }
  }
  
  private static class gameThread extends Thread
  {
    private ArrayList<Boolean> inGame;
    private ArrayList<ArrayList<Integer>> game;
    
    public gameThread()
    {
    }
  
    private void newGame()
    {
      game = new ArrayList<ArrayList<Integer>>(20); // 20 - start size
      //preparing game field
      for (int i = 0; i < game.size(); i++)
      {
	ArrayList<Integer> inner = new ArrayList<Integer>(game.size());
	for (int k = 0; k < inner.size(); k++)
	  inner.set(k, -1);
	game.set(i, inner);
      }
    }
  
    private boolean checkFive(int a, int b, int c, int d, int e)
    {
      if (a == -1)
	return false;
      if (a != b)
	return false;
      if (a != c)
	return false;
      if (a != d)
	return false;
      if (a != e)
	return false;
      return true;
    }
  
    //very simple, we can make lazy check with knowledge of the last turn
    private boolean checkWin()
    {
      //horizontal case _
      for (int i = 0; i < game.size(); i++)
      {
	ArrayList<Integer> cur = game.get(i);
	for (int j = 0; j < cur.size() - 4; j++)
	{
	  if (checkFive(cur.get(j), cur.get(j + 1), cur.get(j + 2), cur.get(j + 3), cur.get(j + 4)))
	    return true;
	}
      }
      //vertical case |
      for (int i = 0; i < game.size() - 4; i++)
      {
	for (int j = 0; j < game.size(); j++)
	{
	  if (checkFive(game.get(i).get(j), game.get(i + 1).get(j), game.get(i + 2).get(j), game.get(i + 3).get(j), game.get(i + 4).get(j)))
	    return true;
	}
      }
      //diagonal case 1 /
      for (int i = 0; i < game.size() - 4; i++)
      {
	for (int j = 0; j < game.size() - 4; j++)
	{
	  if (checkFive(game.get(i + 4).get(j), game.get(i + 3).get(j + 1), game.get(i + 2).get(j + 2), game.get(i + 1).get(j + 3), game.get(i).get(j + 4)))
	    return true;
	}
      }
      //diagonal case 2 \
      for (int i = 0; i < game.size() - 4; i++)
      {
	for (int j = 0; j < game.size() - 4; j++)
	{
	  if (checkFive(game.get(i).get(j), game.get(i + 1).get(j + 1), game.get(i + 2).get(j + 2), game.get(i + 3).get(j + 3), game.get(i + 4).get(j + 4)))
	    return true;
	}
      }
      //default
      return false;
    }
  
    private void nextTurn(int id)
    {
      if (inGame.get(id) == false)
	updateGame(id);
    }
  
    private void updateGame(int id)
    { 
      inGame.set(id, true);
    }
  
    public void run()
    {
    
    }
  }

  private static class clientThread extends Thread
  {
    private DataInputStream is = null;
    private PrintStream os = null;
    private Socket clientSocket = null;

    public clientThread(Socket clientSocket)
    {
      this.clientSocket = clientSocket;                                    
    }

    public void run()
    {
      try
      {
	is = new DataInputStream(clientSocket.getInputStream());
	os = new PrintStream(clientSocket.getOutputStream());
	//sending base html page
	String content = new Scanner(new File("index.html")).useDelimiter("\\Z").next();
	String header = "HTTP/1.1 200 OK\r\n" +
			"Content-Type: text/html\r\n" +
			"Content-Length: " + content.length() + "\r\n" +
			"Connection: keep-alive\r\n\r\n";
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

}

