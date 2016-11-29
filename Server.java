import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Scanner;
import java.io.File;
import java.util.ArrayList;

public class Server
{
	private static ServerSocket serverSocket = null;
	private static Socket clientSocket = null;

	//server starts here
	public static void main(String args[])
	{

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
		public WebSocketServer()
		{

		}

		public void run()
		{

		}
	}

	//player class used to represent and interact with clients
	private static class Player
	{

	}

	//base class which implements WebSocket Protocol
	private static class WebSocket
	{

	}
}