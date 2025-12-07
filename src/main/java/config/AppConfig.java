package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) {
                throw new RuntimeException("config.properties не найден");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить config.properties", e);
        }
    }

    public static String getBaseUrl() {
        return props.getProperty("base.url");
    }

    public static int getShortUrlLength() {
        return Integer.parseInt(props.getProperty("short.url.length"));
    }

    public static long getTtlHours() {
        return Long.parseLong(props.getProperty("default.ttl.hours"));
    }
}
