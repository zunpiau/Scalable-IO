package zunpiau.http;

import org.springframework.http.HttpMethod;

import java.util.HashMap;

@SuppressWarnings({"WeakerAccess", "unused", "FieldCanBeLocal"})
public class HttpRequest {

    private HttpMethod method;
    private String path;
    private HashMap<String, String> headers;
    private String version;

    private HttpRequest(HttpMethod method,
            String path,
            String version,
            HashMap<String, String> headers) {
        this.method = method;
        this.path = path;
        this.version = version;
        this.headers = headers;
    }

    public static HttpRequest parse(String header) {
        String[] lines = header.split("\r\n");
        String[] requestLine = lines[0].split(" ");
        HashMap<String, String> headers = new HashMap<>();
        for (int l = 1; l < lines.length; l++) {
            String[] kv = lines[l].split(":");
            headers.put(kv[0], kv[1]);
        }
        return new HttpRequest(HttpMethod.valueOf(requestLine[0]),
                requestLine[1],
                requestLine[2],
                headers);
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        return "zunpiau.http.HttpRequest{" +
               "method=" + method +
               ", path='" + path + '\'' +
               '}';
    }
}
