package zunpiau.bio;

import zunpiau.http.HttpRequest;
import zunpiau.http.HttpRequestProcessor;
import zunpiau.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;

class SocketHandler {

    void handle(Socket socket) throws IOException {
        byte[] bytes = new byte[10240];
        InputStream is = socket.getInputStream();
        int i = is.read(bytes);
        HttpRequest request = HttpRequest.parse(new String(bytes, 0, i));
        OutputStream outputStream = socket.getOutputStream();
        HttpResponse response = HttpRequestProcessor.getInstance().build(request);
        outputStream.write(response.headerBytes());
        outputStream.write(Files.readAllBytes(response.getDocument()));
        outputStream.flush();
        outputStream.close();
        is.close();
        socket.close();
    }

}
