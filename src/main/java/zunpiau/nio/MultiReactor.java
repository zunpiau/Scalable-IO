package zunpiau.nio;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiReactor extends ReactorWithWorker {

    private final SubRector[] subRectors;
    private final ExecutorService rectorExecutor;

    private MultiReactor() throws IOException {
        super();
        int cpus = Runtime.getRuntime().availableProcessors() * 2;
        subRectors = new SubRector[cpus];
        for (int i = 0; i < cpus; i++) {
            subRectors[i] = new SubRector();
        }
        rectorExecutor = Executors.newFixedThreadPool(cpus);
    }

    public static void main(String[] args) throws IOException {
        new MultiReactor().run();
    }

    @Override
    public void run() {
        for (SubRector subRector : subRectors) {
            rectorExecutor.submit(subRector);
        }
        super.run();
    }

    @Override
    Acceptor getAcceptor() {
        return new Acceptor();
    }

    class SubRector extends Reactor {

        SubRector() throws IOException {
            super();
        }
    }

    class Acceptor extends ReactorWithWorker.Acceptor {

        @Override
        public void run() {
            try {
                SocketChannel socketChannel = serverSocketChannel.accept();
                SubRector rector = subRectors[socketChannel.hashCode() % subRectors.length];
                getHandler(rector.getSelector(), socketChannel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
