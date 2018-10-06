package zunpiau.nio;

import zunpiau.Config;
import zunpiau.http.HttpRequest;
import zunpiau.http.HttpRequestProcessor;
import zunpiau.http.HttpResponse;
import zunpiau.nio.template.BasicReactorDesign;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * implement {@link BasicReactorDesign}
 */
public class BasicReactor extends Reactor {

    final HttpRequestProcessor processor;
    final ServerSocketChannel serverSocketChannel;

    BasicReactor() throws IOException {
        Config config = new Config();
        processor = new HttpRequestProcessor(config);

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(config.serverPort));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, getAcceptor());
    }

    public static void main(String[] args) throws IOException {
        new BasicReactor().run();
    }

    Acceptor getAcceptor() {
        return new Acceptor();
    }

    class Acceptor implements Runnable {

        @Override
        public void run() {
            try {
                SocketChannel socketChannel = serverSocketChannel.accept();
                getHandler(BasicReactor.this, socketChannel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Handler getHandler(Reactor reactor, SocketChannel socketChannel) throws IOException {
            return new Handler(reactor, socketChannel);
        }
    }

    public class Handler implements Runnable {

        final byte[] EOF = "\r\n\r\n".getBytes();
        final SocketChannel socketChannel;
        final SelectionKey key;
        private final Sender sender;
        ByteBuffer input;

        Handler(Reactor reactor, SocketChannel socketChannel) throws IOException {
            input = ByteBuffer.allocate(64);
            sender = new Sender();
            this.socketChannel = socketChannel;
            socketChannel.configureBlocking(false);
            key = registerChannel(reactor, socketChannel);
            read();
            if (inputIsComplete()) {
                System.out.println(socketChannel + " read complete at accept");
                onInputIsComplete();
            } else {
                registerHandler();
            }
        }

        protected SelectionKey registerChannel(Reactor reactor, SocketChannel socketChannel) throws ClosedChannelException {
            return socketChannel.register(reactor.selector, 0);
        }

        void registerHandler() {
            key.attach(Handler.this);
            key.interestOps(SelectionKey.OP_READ);
            key.selector().wakeup();
        }

        void read() {
            long length;
            while (true) {
                ensureCapacity();
                try {
                    length = socketChannel.read(input);
                } catch (IOException e) {
                    e.printStackTrace();
                    closeChannel();
                    return;
                }
                if (length == 0) break;
                if (length < 0) {
                    closeChannel();
                    return;
                }
            }
        }

        void ensureCapacity() {
            if (input.remaining() < 32) {
                ByteBuffer newBuffer = ByteBuffer.allocate(input.capacity() * 2);
                input.flip();
                newBuffer.put(input);
                input = newBuffer;
            }
        }

        boolean inputIsComplete() {
            int position = input.position();
            if (position < 4) return false;
            input.position(position - 4);
            byte[] bytes = new byte[4];
            input.get(bytes);
            input.position(position);
            for (int i = 0; i < 4; i++) {
                if (bytes[i] != EOF[i])
                    return false;
            }
            return true;
        }

        void onInputIsComplete() {
            try {
                String data = getInputAsString();
                HttpRequest request = HttpRequest.parse(data);
                HttpResponse response = processor.process(request);
                sender.prepareWrite(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            System.out.println(socketChannel + " isReadable");
            read();
            if (inputIsComplete()) {
                onInputIsComplete();
            }
        }

        String getInputAsString() {
            input.flip();
            byte[] bytes = new byte[input.remaining()];
            input.get(bytes);
            input.clear();
            String data = new String(bytes);
            System.out.println(">>>>>>>>>>>>>>>");
            System.out.println(data);
            System.out.println(">>>>>>>>>>>>>>>");
            return data;
        }

        void close(Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void closeChannel() {
            System.out.println(socketChannel + " Closing");
            close(socketChannel);
            key.cancel();
        }

        class Sender implements Runnable {

            HttpResponse response;
            boolean hasWriteHeader = false;
            FileChannel fileChannel;
            long transferPos = 0;
            long transferSize = 0;

            void prepareWrite(HttpResponse response) {
                registerSender();
                this.response = response;
                setupFileChannel(response);
            }

            @Override
            public void run() {
                System.out.println(socketChannel + " isWritable");
                write();
                if (outputIsComplete()) {
                    onOutputComplete();
                }
            }

            void registerSender() {
                key.attach(this);
                key.interestOps(SelectionKey.OP_WRITE);
                key.selector().wakeup();
            }

            void setupFileChannel(HttpResponse response) {
                if (response.getDocument() != null) {
                    try {
                        fileChannel = FileChannel.open(response.getDocument());
                        transferSize = fileChannel.size();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            boolean outputIsComplete() {
                return hasWriteHeader && transferPos == transferSize;
            }

            void onOutputComplete() {
                System.out.println(socketChannel + " write completed");
                cleanup();
                registerHandler();
            }

            private void cleanup() {
                response = null;
                if (fileChannel != null) {
                    close(fileChannel);
                    fileChannel = null;
                }
                transferPos = 0;
                transferSize = 0;
                hasWriteHeader = false;
            }

            void write() {
                try {
                    if (!hasWriteHeader) {
                        System.out.println("<<<<<<<<<<<<<<<");
                        System.out.println(response);
                        System.out.println("<<<<<<<<<<<<<<<");
                        socketChannel.write(ByteBuffer.wrap(response.headerBytes()));
                        hasWriteHeader = true;
                    }
                    if (fileChannel != null) {
                        long l = fileChannel.transferTo(transferPos, transferSize, socketChannel);
                        System.out.println(socketChannel + " write " + l + " bytes");
                        transferPos += l;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    closeChannel();
                    cleanup();
                }
            }

        }

    }
}
