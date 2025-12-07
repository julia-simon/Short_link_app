package service;

import models.Link;

import java.time.Duration;
import java.util.*;

public class LinkService {
    private final Map<String, Link> storage = new HashMap<>(); // shortUrl -> Link
    private final UrlShortenerService urlShortener;

    public LinkService() {
        urlShortener = new UrlShortenerService();
    }

    /**
     * Создание новой короткой ссылки
     */
    public Link createLink(String originalUrl, UUID owner, int maxClicks, Duration ttl) {
        // Генерация уникальной короткой ссылки
        String shortUrl = urlShortener.generateShortUrl(originalUrl, owner);

        Link link = new Link(originalUrl, shortUrl, owner, maxClicks, ttl);
        save(link);
        return link;
    }

    /**
     * Сохраняем ссылку
     */
    public void save(Link link) {
        storage.put(link.getShortUrl(), link);
    }

    /**
     * Получаем ссылку по короткому URL
     * Проверяем TTL и лимит переходов
     */
    public Optional<Link> getLink(String shortUrl) {
        Link link = storage.get(shortUrl);
        if (link == null) return Optional.empty();

        // Проверяем TTL
        if (link.isExpired()) {
            delete(shortUrl);
            return Optional.empty();
        }

        // Проверяем лимит переходов
        if (link.isLimitReached()) {
            return Optional.empty();
        }

        // Увеличиваем счетчик переходов
        link.incrementClicks();
        return Optional.of(link);
    }

    /**
     * Получение всех ссылок пользователя
     */
    public List<Link> findByUser(UUID userId) {
        List<Link> list = new ArrayList<>();
        for (Link link : storage.values()) {
            if (link.getOwner().equals(userId)) {
                list.add(link);
            }
        }
        return list;
    }

    /**
     * Поиск ссылки по короткому URL (без проверки TTL/лимита)
     */
    public Link findByShort(String shortUrl) {
        return storage.get(shortUrl);
    }
    /**
     * Обновление лимита кликов
     */
    public boolean updateMaxClicks(String shortUrl, int newMaxClicks, UUID userId) {
        Link link = storage.get(shortUrl);
        if (link == null) return false;
        if (!link.getOwner().equals(userId)) return false;

        if (newMaxClicks <= 0) return false;

        link.setMaxClicks(newMaxClicks);
        return true;
    }

    /**
     * Обновление время жизни сслыки
     */
    public boolean updateTtl(String shortUrl, Duration newTtl, UUID userId) {
        Link link = storage.get(shortUrl);
        if (link == null) return false;
        if (!link.getOwner().equals(userId)) return false;

        if (newTtl.isZero() || newTtl.isNegative()) return false;

        link.setTtl(newTtl);
        return true;
    }

    /**
     * Удаление ссылки
     */
    public void delete(String shortUrl) {
        storage.remove(shortUrl);
    }

    /**
     * Автоудаление всех истекших ссылок
     */
    public void deleteExpired() {
        storage.values().removeIf(Link::isExpired);
    }
}
