package zunpiau.bio;

import zunpiau.ServerStarter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadServer implements Runnable {

    public static void main(String[] args) {
        new ServerStarter() {
            @Override
            public Runnable start(Config config) {
                return new MultiThreadServer();
            }
        }.start();
    }

    @Override
    public void run() {
        ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            System.out.println("MultiThreadServer start");
            ServerSocket serverSocket = new ServerSocket(65500);
            //noinspection InfiniteLoopStatement
            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.submit(new HandleClient(socket));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("MultiThreadServer end");
    }

    class HandleClient implements Runnable {

        private Socket socket;

        HandleClient(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " run");
            try {
                new SocketHandler().handle(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
