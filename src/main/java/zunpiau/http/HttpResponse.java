package zunpiau.http;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.nio.file.Path;

public class HttpResponse {

    @SuppressWarnings("FieldCanBeLocal")
    private final String version = "HTTP/1.1";
    private final HttpStatus statusCode;
    private final HttpHeaders headers;
    private final Path document;

    HttpResponse(HttpStatus statusCode, HttpHeaders headers, Path document) {
        this.statusCode = statusCode;
        this.document = document;
        this.headers = headers;
    }

    @Override
    public String toString() {
        return (version + " "
                + statusCode.value() + " "
                + statusCode.name() + "\r\n" +
                headers + "\r\n");
    }

    public byte[] headerBytes() {
        return toString().getBytes();
    }

    public Path getDocument() {
        return document;
    }
}
