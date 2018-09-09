package zunpiau.nio.basicreactor.template;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Copy from <a href="gee.cs.oswego.edu/dl/cpjslides/nio.pdf">Basic Reactor Design</a>
 */
public class BasicReactorDesign {

    private final static int MAXIN = 65535;
    private final static int MAXOUT = 65535;
    private final static int PORT = 65500;

    public static void main(String[] args) throws IOException {
        new Thread(new Reactor(PORT)).start();
    }

    static class Reactor implements Runnable {

        private final Selector selector;
        private final ServerSocketChannel serverSocket;

        Reactor(int port) throws IOException {
            selector = Selector.open();
            serverSocket = ServerSocketChannel.open();
            serverSocket.socket().bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false);
            SelectionKey sk = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            sk.attach(new Acceptor());
        }

        public void run() {
            try {
                while (!Thread.interrupted()) {
                    selector.select();
                    Set selected = selector.selectedKeys();
                    Iterator it = selected.iterator();
                    while (it.hasNext())
                        dispatch((SelectionKey) (it.next()));
                    selected.clear();
                }
            } catch (IOException ex) { /* ... */ }
        }

        void dispatch(SelectionKey k) {
            Runnable r = (Runnable) (k.attachment());
            if (r != null)
                r.run();
        }

        class Acceptor implements Runnable {

            // inner
            public void run() {
                try {
                    SocketChannel c = serverSocket.accept();
                    if (c != null)
                        new Handler(selector, c);
                } catch (IOException ex) { /* ... */ }
            }
        }
    }

    static class Handler implements Runnable {

        final SocketChannel socket;
        final SelectionKey sk;
        ByteBuffer input = ByteBuffer.allocate(MAXIN);
        ByteBuffer output = ByteBuffer.allocate(MAXOUT);

        Handler(Selector selector, SocketChannel channel) throws IOException {
            socket = channel;
            channel.configureBlocking(false);
// Optionally try first read now
            sk = socket.register(selector, 0);
            sk.attach(this);
            sk.interestOps(SelectionKey.OP_READ);
            selector.wakeup();
        }

        boolean inputIsComplete() {
            return false;
        }

        boolean outputIsComplete() {
            return false;
        }

        void process() { /* ... */ }

        class Sender implements Runnable {

            public void run() { // ...
                try {
                    socket.write(output);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (outputIsComplete()) sk.cancel();
            }
        }

        public void run() { // initial state is reader
            try {
                socket.read(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (inputIsComplete()) {
                process();
                sk.attach(new Sender());
                sk.interestOps(SelectionKey.OP_WRITE);
                sk.selector().wakeup();
            }
        }


    }
}

