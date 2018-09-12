package zunpiau.bio;

import zunpiau.Config;
import zunpiau.http.HttpRequestProcessor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadServer implements Runnable {

    private static HttpRequestProcessor processor;

    public static void main(String[] args) {
        processor = new HttpRequestProcessor(new Config());
        new Thread(new MultiThreadServer()).start();
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
                threadPool.submit(new HandleClient(socket, processor));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("MultiThreadServer end");
    }

    class HandleClient implements Runnable {

        private Socket socket;
        private HttpRequestProcessor processor;

        HandleClient(Socket socket, HttpRequestProcessor processor) {
            this.socket = socket;
            this.processor = processor;
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " run");
            try {
                new SocketHandler().handle(socket, processor);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
