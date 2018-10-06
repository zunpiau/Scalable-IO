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
                doSelect();
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

    int doSelect() throws IOException {
        return selector.select();
    }

    private void dispatch(SelectionKey selectionKey) {
        ((Runnable) selectionKey.attachment()).run();
    }
}
