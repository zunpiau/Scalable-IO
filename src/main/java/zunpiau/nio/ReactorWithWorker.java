package zunpiau.nio;

import zunpiau.http.HttpRequest;
import zunpiau.http.HttpResponse;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ReactorWithWorker extends BasicReactor {

    private final ExecutorService worker;

    private ReactorWithWorker() throws IOException {
        super();
        worker = Executors.newFixedThreadPool(4);
    }

    public static void main(String[] args) throws IOException {
        new ReactorWithWorker().run();
    }

    @Override
    Acceptor getAcceptor() {
        return new Acceptor();
    }

    class Acceptor extends BasicReactor.Acceptor {

        @Override
        Handler getHandler(Selector selector, SocketChannel socketChannel) throws IOException {
            return new Handler(selector, socketChannel);
        }
    }

    public class Handler extends BasicReactor.Handler {

        Handler(Selector selector, SocketChannel socketChannel) throws IOException {
            super(selector, socketChannel);
        }

        void onInputIsComplete() {
            String data = getInputAsString();
            Future<HttpResponse> responseFuture = worker.submit(() -> {
                HttpRequest request = HttpRequest.parse(data);
                try {
                    return (processor.process(request));
                } catch (Exception e) {
                    e.printStackTrace();
                    closeChannel();
                }
                return null;
            });
            new Sender().prepareWrite(responseFuture);
        }

        class Sender extends BasicReactor.Handler.Sender {

            private Future<HttpResponse> responseFuture;

            @Override
            public void run() {
                if (!responseFuture.isDone()) return;
                try {
                    response = responseFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    closeChannel();
                }
                if (response == null) {
                    closeChannel();
                    return;
                }
                setupFileChannel(response);
                super.run();
            }

            void prepareWrite(Future<HttpResponse> response) {
                registerSender();
                this.responseFuture = response;
            }
        }
    }

}
