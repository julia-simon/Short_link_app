package models;

import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Link {
    private final String originalUrl; // исходный URL
    private final String shortUrl;    // короткая ссылка
    private final UUID owner;         // UUID владельца

    private int maxClicks;      // лимит переходов
    private int usedClicks = 0;       // сколько раз уже переходили

    private final LocalDateTime createdAt; // время создания
    private Duration ttl;            // время жизни

    public Link(String originalUrl, String shortUrl, UUID owner, int maxClicks, Duration ttl) {
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
        this.owner = owner;
        this.maxClicks = maxClicks;
        this.ttl = ttl;
        this.createdAt = LocalDateTime.now();
    }

    public void incrementClicks() {
        usedClicks++;
    }

    public boolean isExpired() {
        return createdAt.plus(ttl).isBefore(LocalDateTime.now());
    }

    public boolean isLimitReached() {
        return usedClicks >= maxClicks;
    }
}
