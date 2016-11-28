import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Scanner;
import java.io.File;

public class Server {

    public static void main(String[] args) throws Throwable {
        ServerSocket ss = new ServerSocket(7777);
        while (true) {
            Socket s = ss.accept();
            System.err.println("Client accepted");
            new Thread(new SocketProcessor(s)).start();
        }
    }

    public static class Game {
    
    }
    
    private static class SocketProcessor implements Runnable {

        private Socket s;
        private InputStream is;
        private OutputStream os;

        private SocketProcessor(Socket s) throws Throwable {
            this.s = s;
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
        }

        public void run() {
            try {
                String content = new Scanner(new File("index.html")).useDelimiter("\\Z").next();
                sendHTML(content);
                //main game here
                
            } catch (Throwable t) {
                /*do nothing*/
            } finally {
                try {
                   /*do nothing*/ 
                } catch (Throwable t) {
                    /*do nothing*/
                }
            }
            System.err.println("Client processing finished");
        }

        private void sendHTML(String s) throws Throwable {
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + s.length() + "\r\n" +
                    "Connection: keep-alive\r\n\r\n";
            String result = response + s;
            os.write(result.getBytes());
            os.flush();
        }
    }
}
