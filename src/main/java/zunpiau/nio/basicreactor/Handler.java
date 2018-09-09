package zunpiau.nio.basicreactor;

import zunpiau.http.HttpRequest;
import zunpiau.http.HttpRequestProcessor;
import zunpiau.http.HttpResponse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class Handler implements Runnable {

    private static final byte[] EOF = "\r\n\r\n".getBytes();
    private final SocketChannel socketChannel;
    private final SelectionKey key;
    private ByteBuffer input = ByteBuffer.allocate(64);

    Handler(Selector selector, SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        socketChannel.configureBlocking(false);
        key = socketChannel.register(selector, 0);
        read();
        if (inputIsComplete()) {
            System.out.println(socketChannel + " complete read at accept");
            registerReader();
        } else {
            key.attach(this);
            key.interestOps(SelectionKey.OP_READ);
            selector.wakeup();
        }
    }

    public void run() {
        System.out.println(socketChannel + " isReadable");
        read();
        if (inputIsComplete()) {
            registerReader();
        }
    }

    private void registerReader() {
        try {
            new Sender(process());
        } catch (Exception e) {
            e.printStackTrace();
            closeSilently(socketChannel);
        }
    }

    private void read() {
        long length;
        while (true) {
            if (input.remaining() < 32) {
                ByteBuffer newBuffer = ByteBuffer.allocate(input.capacity() * 2);
                input.flip();
                newBuffer.put(input);
                input = newBuffer;
            }
            try {
                length = socketChannel.read(input);
            } catch (IOException e) {
                e.printStackTrace();
                closeSilently(socketChannel);
                return;
            }
            if (length == 0) break;
            if (length < 0) {
                closeSilently(socketChannel);
                return;
            }
        }
    }

    private boolean inputIsComplete() {
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

    private HttpResponse process() throws IOException {
        input.flip();
        byte[] bytes = new byte[input.remaining()];
        input.get(bytes);
        input.clear();
        String data = new String(bytes);
        System.out.println(">>>>>>>>>>>>>>>");
        System.out.println(data);
        System.out.println(">>>>>>>>>>>>>>>");
        HttpRequest request = HttpRequest.parse(data);
        return HttpRequestProcessor.getInstance().build(request);
    }

    private void closeSilently(SocketChannel channel) {
        try {
            System.out.println(socketChannel + " Closing");
            channel.close();
            key.cancel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class Sender implements Runnable {

        private HttpResponse response;
        private boolean hasWriteHeader = false;
        private FileChannel fileChannel;
        private long transferPos = 0;
        private long transferSize = 0;

        Sender(HttpResponse response) {
            key.attach(this);
            key.interestOps(SelectionKey.OP_WRITE);
            key.selector().wakeup();
            this.response = response;
            if (response.getDocument() != null) {
                try {
                    fileChannel = FileChannel.open(response.getDocument());
                    transferSize = fileChannel.size();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean outputIsComplete() {
            return hasWriteHeader && transferPos == transferSize;
        }

        @Override
        public void run() {
            System.out.println(socketChannel + " isWritable");
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
                if (outputIsComplete()) {
                    response = null;
                    if (fileChannel != null)
                        fileChannel.close();
                    key.attach(Handler.this);
                    key.interestOps(SelectionKey.OP_READ);
                    key.selector().wakeup();
                }
            } catch (IOException e) {
                e.printStackTrace();
                closeSilently(socketChannel);
            }
        }


    }

}
