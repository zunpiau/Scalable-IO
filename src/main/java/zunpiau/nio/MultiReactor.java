package zunpiau.nio;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class MultiReactor extends ReactorWithWorker {

    private final SubReactor[] subReactors;
    private final ExecutorService rectorExecutor;

    private MultiReactor() throws IOException {
        super();
        int cpus = Runtime.getRuntime().availableProcessors() * 2;
        subReactors = new SubReactor[cpus];
        for (int i = 0; i < cpus; i++) {
            subReactors[i] = new SubReactor();
        }
        rectorExecutor = Executors.newFixedThreadPool(cpus);
    }

    public static void main(String[] args) throws IOException {
        new MultiReactor().run();
    }

    @Override
    public void run() {
        for (SubReactor subReactor : subReactors) {
            rectorExecutor.submit(subReactor);
        }
        super.run();
    }

    @Override
    Acceptor getAcceptor() {
        return new Acceptor();
    }

    class SubReactor extends Reactor {

        private final ReentrantLock selectLock = new ReentrantLock();

        SubReactor() throws IOException {
            super();
        }

        @Override
        int doSelect() throws IOException {
            selectLock.lock();
            selectLock.unlock();
            return super.doSelect();
        }
    }

    class Acceptor extends ReactorWithWorker.Acceptor {

        @Override
        public void run() {
            try {
                SocketChannel socketChannel = serverSocketChannel.accept();
                SubReactor rector = subReactors[socketChannel.hashCode() % subReactors.length];
                getHandler(rector, socketChannel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        Handler getHandler(Reactor reactor, SocketChannel socketChannel) throws IOException {
            return new Handler(reactor, socketChannel);
        }
    }

    class Handler extends ReactorWithWorker.Handler {

        Handler(Reactor reactor, SocketChannel socketChannel) throws IOException {
            super(reactor, socketChannel);
        }

        protected SelectionKey registerChannel(Reactor reactor, SocketChannel socketChannel) throws ClosedChannelException {
            SubReactor subReactor = (SubReactor) reactor;
            try {
                subReactor.selectLock.lock();
                subReactor.selector.wakeup();
                return super.registerChannel(subReactor, socketChannel);
            } finally {
                subReactor.selectLock.unlock();
            }
        }
    }

}
