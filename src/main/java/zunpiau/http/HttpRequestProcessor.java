package zunpiau.http;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import zunpiau.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpRequestProcessor {

    private final Path WEB_DIR;
    private final Path ERROR_PAGE_DIR;
    private final String HOMEPAGE_INDEX;

    public HttpRequestProcessor(Config config) {
        WEB_DIR = config.webapp;
        ERROR_PAGE_DIR = config.errorPage;
        HOMEPAGE_INDEX = config.homepageIndex;
    }

    public HttpResponse process(HttpRequest request) throws IOException {
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
