package zunpiau.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class Reactor implements Runnable {

    final Selector selector;

    public Reactor() throws IOException {
        selector = Selector.open();
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
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

    Selector getSelector() {
        return selector;
    }

    private void dispatch(SelectionKey selectionKey) {
        ((Runnable) selectionKey.attachment()).run();
    }
}
