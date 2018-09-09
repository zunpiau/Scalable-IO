package zunpiau.bio;

import zunpiau.ServerStarter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SignalThreadServer implements Runnable {

    public static void main(String[] args) {
        new ServerStarter() {
            @Override
            public Runnable start(Config config) {
                return new SignalThreadServer();
            }
        }.start();
    }

    @Override
    public void run() {
        try {
            System.out.println("SignalThreadServer start");
            ServerSocket serverSocket = new ServerSocket(65500);
            //noinspection InfiniteLoopStatement
            while (true) {
                Socket socket = serverSocket.accept();
                new SocketHandler().handle(socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("SignalThreadServer end");
    }
}
