package service;

import utils.Base62Encoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

public class UrlShortenerService {
    private final String baseUrl = config.AppConfig.getBaseUrl();
    private final int shortUrlLength = config.AppConfig.getShortUrlLength();

    /**
     * Генерация короткой ссылки через SHA-256 + Base62
     */
    public String generateShortUrl(String originalUrl, UUID userId) {
        try {
            String input = originalUrl + userId.toString();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            String base62 = Base62Encoder.encode(hash);
            String shortPart = base62.substring(0, shortUrlLength);

            return baseUrl + shortPart;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось сгенерировать короткую ссылку", e);
        }
    }
}
