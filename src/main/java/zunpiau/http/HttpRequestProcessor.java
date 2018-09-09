package zunpiau.http;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpRequestProcessor {

    private static HttpRequestProcessor instance;
    private final Path WEB_DIR;
    private final Path ERROR_PAGE_DIR;
    private final String HOMEPAGE_INDEX;

    private HttpRequestProcessor(Path webDir, Path errorPageDir, String homepageIndex) {
        WEB_DIR = webDir;
        ERROR_PAGE_DIR = errorPageDir;
        HOMEPAGE_INDEX = homepageIndex;
    }

    public static void createInstance(Path webDir, Path errorPageDir, String homepageIndex) {
        if (instance == null)
            instance = new HttpRequestProcessor(webDir, errorPageDir, homepageIndex);
        else
            throw new IllegalStateException("HttpRequestProcessor has been created");
    }

    public static HttpRequestProcessor getInstance() {
        if (instance != null)
            return instance;
        else
            throw new IllegalStateException("HttpRequestProcessor not yet created");
    }

    public HttpResponse build(HttpRequest request) throws IOException {
        Path path = Paths.get(WEB_DIR.toString(), request.getPath());
        if (path.compareTo(WEB_DIR) < 0) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, new HttpHeaders());
        }
        if (Files.isDirectory(path)) {
            path = path.resolve(HOMEPAGE_INDEX);
        }
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, new HttpHeaders());
        }
        if (!Files.isReadable(path)) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, new HttpHeaders());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(path)));
        headers.put(HttpHeaders.CONTENT_TYPE, Files.probeContentType(path));
        return new HttpResponse(HttpStatus.OK, headers, path);
    }

    private HttpResponse buildErrorResponse(HttpStatus status, HttpHeaders headers) throws IOException {
        Path path = ERROR_PAGE_DIR.resolve(status.value() + ".html");
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(path)));
            return new HttpResponse(status, headers, path);
        } else {
            headers.put(HttpHeaders.CONTENT_LENGTH, "0");
            return new HttpResponse(status, headers, null);
        }
    }

}
