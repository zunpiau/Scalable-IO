package zunpiau;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

public class Config {

    public final int serverPort;
    public final Path webapp;
    public final Path errorPage;
    public final String homepageIndex;

    public Config() {
        Properties properties = new Properties();
        URL resource = getClass().getClassLoader().getResource("server.properties");
        Path path = Paths.get(Objects.requireNonNull(resource).getPath());
        try {
            properties.load(new FileInputStream(path.toFile()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        serverPort = Integer.parseInt(properties.getProperty("server.port"));
        webapp = path.resolveSibling(properties.getProperty("dir.webapp"));
        errorPage = path.resolveSibling(properties.getProperty("dir.error"));
        homepageIndex = properties.getProperty("homepage.index");
    }
}
