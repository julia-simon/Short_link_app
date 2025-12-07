import models.Link;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.LinkService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LinkAppTest {
    private LinkService linkService;
    private UUID userId;
    private UUID otherUser;

    @BeforeEach
    void setUp() {
        linkService = new LinkService();
        userId = UUID.randomUUID();
        otherUser = UUID.randomUUID();
    }

    // Создание ссылки
    @Test
    void testCreateLink() {
        Link link = linkService.createLink("https://example.com", userId, 5, Duration.ofHours(24));
        assertNotNull(link.getShortUrl());
        assertEquals("https://example.com", link.getOriginalUrl());
        assertEquals(userId, link.getOwner());
    }

    // Проверка списка ссылок пользователя
    @Test
    void testFindByUser() {
        Link link1 = linkService.createLink("https://a.com", userId, 5, Duration.ofHours(24));
        Link link2 = linkService.createLink("https://b.com", userId, 10, Duration.ofHours(24));
        List<Link> links = linkService.findByUser(userId);
        assertEquals(2, links.size());
        assertTrue(links.contains(link1));
        assertTrue(links.contains(link2));
    }

    // Проверка getLink и увеличение счетчика кликов
    @Test
    void testGetLinkIncrementsClicks() {
        Link link = linkService.createLink("https://example.com", userId, 2, Duration.ofHours(24));
        Optional<Link> opt1 = linkService.getLink(link.getShortUrl());
        assertEquals(1, opt1.get().getUsedClicks());
        Optional<Link> opt2 = linkService.getLink(link.getShortUrl());
        assertEquals(2, opt2.get().getUsedClicks());
        Optional<Link> opt3 = linkService.getLink(link.getShortUrl());
        assertTrue(opt3.isEmpty());
    }

    // Проверка лимита переходов
    @Test
    void testLimitReached() {
        Link link = linkService.createLink("https://example.com", userId, 1, Duration.ofHours(24));
        linkService.getLink(link.getShortUrl());
        assertTrue(link.isLimitReached());
    }

    // Проверка TTL
    @Test
    void testIsExpired() throws InterruptedException {
        Link link = linkService.createLink("https://example.com", userId, 5, Duration.ofMillis(100));
        Thread.sleep(150);
        assertTrue(link.isExpired());
    }

    // Редактирование лимита переходов (правильный пользователь)
    @Test
    void testUpdateMaxClicksSuccess() {
        Link link = linkService.createLink("https://example.com", userId, 5, Duration.ofHours(24));
        boolean ok = linkService.updateMaxClicks(link.getShortUrl(), 10, userId);
        assertTrue(ok);
        assertEquals(10, link.getMaxClicks());
    }

    // Редактирование лимита переходов (не владелец)
    @Test
    void testUpdateMaxClicksFailWrongUser() {
        Link link = linkService.createLink("https://example.com", userId, 5, Duration.ofHours(24));
        boolean ok = linkService.updateMaxClicks(link.getShortUrl(), 10, otherUser);
        assertFalse(ok);
        assertEquals(5, link.getMaxClicks());
    }

    // Редактирование TTL (правильный пользователь)
    @Test
    void testUpdateTtlSuccess() {
        Link link = linkService.createLink("https://example.com", userId, 5, Duration.ofHours(24));
        boolean ok = linkService.updateTtl(link.getShortUrl(), Duration.ofHours(48), userId);
        assertTrue(ok);
        assertEquals(Duration.ofHours(48), link.getTtl());
    }

    // Редактирование TTL (не владелец)
    @Test
    void testUpdateTtlFailWrongUser() {
        Link link = linkService.createLink("https://example.com", userId, 5, Duration.ofHours(24));
        boolean ok = linkService.updateTtl(link.getShortUrl(), Duration.ofHours(48), otherUser);
        assertFalse(ok);
        assertEquals(Duration.ofHours(24), link.getTtl());
    }

    // Удаление ссылки
    @Test
    void testDeleteLink() {
        Link link = linkService.createLink("https://example.com", userId, 5, Duration.ofHours(24));
        linkService.delete(link.getShortUrl());
        assertTrue(linkService.findByUser(userId).isEmpty());
    }

    // Удаление протухшей ссылки
    @Test
    void testDeleteExpired() throws InterruptedException {
        Link link = linkService.createLink("https://example.com", userId, 5, Duration.ofMillis(100));
        Thread.sleep(150);
        linkService.deleteExpired();
        assertTrue(linkService.findByUser(userId).isEmpty());
    }

    // Попытка обновления несуществующей ссылки
    @Test
    void testUpdateNonExistentLink() {
        boolean ok = linkService.updateMaxClicks("nonexistent", 10, userId);
        assertFalse(ok);
        boolean ok2 = linkService.updateTtl("nonexistent", Duration.ofHours(10), userId);
        assertFalse(ok2);
    }

    // Проверка уникальности коротких ссылок
    @Test
    void testUniqueShortUrls() {
        Link link1 = linkService.createLink("https://a.com", userId, 5, Duration.ofHours(24));
        Link link2 = linkService.createLink("https://a.com", UUID.randomUUID(), 5, Duration.ofHours(24));
        assertNotEquals(link1.getShortUrl(), link2.getShortUrl());
    }

    // Попытка создания ссылки с некорректным TTL или лимитом
    @Test
    void testInvalidUpdateValues() {
        Link link = linkService.createLink("https://a.com", userId, 5, Duration.ofHours(24));
        assertFalse(linkService.updateMaxClicks(link.getShortUrl(), 0, userId));
        assertFalse(linkService.updateTtl(link.getShortUrl(), Duration.ofHours(-1), userId));
    }

    // Поиск ссылки по shortUrl
    @Test
    void testFindByShort() {
        Link link = linkService.createLink("https://example.com", userId, 5, Duration.ofHours(24));
        Link found = linkService.findByShort(link.getShortUrl());
        assertNotNull(found);
        assertEquals(link.getShortUrl(), found.getShortUrl());
    }
}
