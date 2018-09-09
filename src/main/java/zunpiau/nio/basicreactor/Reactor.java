package zunpiau.nio.basicreactor;

import zunpiau.ServerStarter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * implement {@link zunpiau.nio.basicreactor.template.BasicReactorDesign}
 */
public class Reactor implements Runnable {

    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;

    private Reactor(int port) throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, new Acceptor());
    }

    public static void main(String[] args) {
        new ServerStarter() {
            @Override
            public Runnable start(Config config) {
                try {
                    return new Reactor(config.getServerPort());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
    }

    public void run() {
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    dispatch(selectionKey);
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dispatch(SelectionKey selectionKey) {
        ((Runnable) selectionKey.attachment()).run();
    }

    class Acceptor implements Runnable {

        @Override
        public void run() {
            try {
                SocketChannel socketChannel = serverSocketChannel.accept();
                new Handler(selector, socketChannel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
