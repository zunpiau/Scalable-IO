package zunpiau;

import zunpiau.http.HttpRequestProcessor;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

public abstract class ServerStarter {

    public void start() {
        Properties properties = new Properties();
        URL resource = ServerStarter.class.getClassLoader().getResource("server.properties");
        Path path = Paths.get(Objects.requireNonNull(resource).getPath());
        try {
            properties.load(new FileInputStream(path.toFile()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        int serverPort = Integer.parseInt(properties.getProperty("server.port"));
        Config config = new Config(serverPort);
        HttpRequestProcessor.createInstance(path.resolveSibling(properties.getProperty("dir.webapp")),
                path.resolveSibling(properties.getProperty("dir.error")),
                properties.getProperty("homepage.index"));
        new Thread(start(config)).start();
    }

    public abstract Runnable start(Config config);

    public class Config {

        private int serverPort;

        Config(int serverPort) {
            this.serverPort = serverPort;
        }

        public int getServerPort() {
            return serverPort;
        }
    }
}
