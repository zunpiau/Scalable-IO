package zunpiau.bio;

import zunpiau.Config;
import zunpiau.http.HttpRequestProcessor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SignalThreadServer implements Runnable {

    private static HttpRequestProcessor processor;

    public static void main(String[] args) {
        processor = new HttpRequestProcessor(new Config());
        new Thread(new SignalThreadServer()).start();
    }

    @Override
    public void run() {
        try {
            System.out.println("SignalThreadServer start");
            ServerSocket serverSocket = new ServerSocket(65500);
            //noinspection InfiniteLoopStatement
            while (true) {
                Socket socket = serverSocket.accept();
                new SocketHandler().handle(socket, processor);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("SignalThreadServer end");
    }
}
