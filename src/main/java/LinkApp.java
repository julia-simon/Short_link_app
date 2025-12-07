import models.Link;
import models.User;
import service.LinkService;
import service.UserService;

import java.awt.*;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;


public class LinkApp {
    private final Scanner scanner = new Scanner(System.in);
    private final LinkService linkService;
    private final UserService userService;
    UUID userId;

    public LinkApp() {
        linkService = new LinkService();
        userService = new UserService();
    }

    public void run() {
        User currentUser = userService.getCurrentUser();
        this.userId = currentUser.getId();
        System.out.println("Ваш UUID: " + userId);

        System.out.println("Добро пожаловать в сервис коротких ссылок!");

        while (true) {
            showMainMenu();
            System.out.print("> ");
            String command = scanner.nextLine().trim();

            switch (command) {
                case "1":
                    handleCreate();
                    break;
                case "2":
                    handleList();
                    break;
                case "3":
                    handleOpen();
                    break;
                case "4":
                    handleEditParams();
                    break;
                case "5":
                    handleDelete();
                    break;
                case "6":
                    System.out.println("До свидания!");
                    return;
                case "help":
                    showHelp();
                    break;
                default:
                    System.out.println("Неизвестная команда. Введите 'help' для справки.");
            }

            // Авто-удаление протухших ссылок
            linkService.deleteExpired();
        }
    }

    private void showMainMenu() {
        System.out.println("Доступные команды:");
        System.out.println("1 - Создать новую короткую ссылку");
        System.out.println("2 - Показать все ваши ссылки");
        System.out.println("3 - Перейти по короткой ссылке");
        System.out.println("4 - Редактировать параметры ссылки");
        System.out.println("5 - Удалить ссылку");
        System.out.println("6 - Выход из программы");
        System.out.println("help - Показать справку");
    }

    private void handleCreate() {
        System.out.print("Введите оригинальный URL: ");
        String url = scanner.nextLine().trim();

        if (!isValidUrl(url)) {
            System.out.println("Ошибка: введён некорректный URL.");
            return;
        }

        System.out.print("Введите лимит переходов: ");
        int maxClicks;
        try {
            maxClicks = Integer.parseInt(scanner.nextLine().trim());
            if (maxClicks <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: лимит переходов должен быть положительным числом.");
            return;
        }

        Duration ttl = Duration.ofHours(config.AppConfig.getTtlHours());

        Link link = linkService.createLink(url, userId, maxClicks, ttl);
        System.out.println("Короткая ссылка создана: " + link.getShortUrl());
        System.out.println("Срок жизни: " + ttl.toHours() + " часа, лимит переходов: " + maxClicks);
    }

    private boolean isValidUrl(String url) {
        try {
            if (url == null || url.isBlank()) return false;
            new java.net.URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void handleList() {
        List<Link> links = linkService.findByUser(userId);
        if (links.isEmpty()) {
            System.out.println("У вас пока нет ссылок.");
            return;
        }
        System.out.println("Ваши ссылки:");
        for (Link link : links) {
            System.out.printf("%s -> %s | Переходов: %d/%d | Просрочено: %s%n",
                    link.getShortUrl(),
                    link.getOriginalUrl(),
                    link.getUsedClicks(),
                    link.getMaxClicks(),
                    link.isExpired() ? "да" : "нет");
        }
    }

    private void handleOpen() {
        System.out.print("Введите короткую ссылку: ");
        String shortUrl = scanner.nextLine().trim();

        if (shortUrl.isBlank() || shortUrl.isEmpty()) {
            System.out.println("Ошибка: ссылка не может быть пустой.");
            return;
        }

        Optional<Link> optLink = linkService.getLink(shortUrl);
        if (optLink.isEmpty()) {
            Link link = linkService.findByShort(shortUrl);
            if (link == null) {
                System.out.println("Ошибка: ссылка не найдена.");
            } else if (link.isExpired()) {
                System.out.println("Ошибка: срок жизни ссылки истек.");
            } else if (link.isLimitReached()) {
                System.out.println("Ошибка: лимит переходов по ссылке исчерпан.");
            } else {
                System.out.println("Ссылка недоступна.");
            }
            return;
        }

        Link link = optLink.get();
        try {
            Desktop.getDesktop().browse(new URI(link.getOriginalUrl()));
            System.out.println("Перенаправление на: " + link.getOriginalUrl());
        } catch (Exception e) {
            System.out.println("Не удалось открыть ссылку: " + e.getMessage());
        }

        if (link.isLimitReached()) {
            System.out.println("Внимание: лимит переходов по этой ссылке исчерпан!");
        }
    }

    private void handleEditParams() {
        while (true) {
            System.out.println("Редактирование ссылки:");
            System.out.println("1 - Редактировать переходов");
            System.out.println("2 - Редактировать время жизни ссылки (в часах)");
            System.out.println("3 - Назад");
            System.out.print("> ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    editMaxClicksFlow();
                    break;
                case "2":
                    editTtlFlow();
                    break;
                case "3":
                    return;
                default:
                    System.out.println("Неизвестная команда. Введите 1, 2 или 3.");
            }
        }
    }

    private void editMaxClicksFlow() {
        System.out.print("Введите короткую ссылку для редактирования лимита: ");
        String shortUrl = scanner.nextLine().trim();

        System.out.print("Введите новый лимит переходов (положительное целое): ");
        int newLimit;
        try {
            newLimit = Integer.parseInt(scanner.nextLine().trim());
            if (newLimit <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: нужно положительное целое число.");
            return;
        }

        boolean ok = linkService.updateMaxClicks(shortUrl, newLimit, userId);

        if (ok) System.out.println("Лимит успешно обновлён до " + newLimit);
        else System.out.println("Не удалось обновить лимит (ссылка не найдена или нет прав).");
    }

    private void editTtlFlow() {
        System.out.print("Введите короткую ссылку для редактирования время жизни: ");
        String shortUrl = scanner.nextLine().trim();

        System.out.print("Введите новый время жизни в часах (положительное число): ");
        long hours;
        try {
            hours = Long.parseLong(scanner.nextLine().trim());
            if (hours <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: время жизни должен быть положительным числом.");
            return;
        }

        boolean ok = linkService.updateTtl(shortUrl, Duration.ofHours(hours), userId);

        if (ok) System.out.println("время жизни успешно обновлён на " + hours + " часов.");
        else System.out.println("Не удалось обновить время жизни (ссылка не найдена или нет прав).");
    }

    private void handleDelete() {
        System.out.print("Введите короткую ссылку для удаления: ");
        String shortUrl = scanner.nextLine().trim();

        Optional<Link> linkOpt = Optional.ofNullable(linkService.findByShort(shortUrl));
        if (linkOpt.isEmpty() || !linkOpt.get().getOwner().equals(userId)) {
            System.out.println("Ссылка не найдена или принадлежит другому пользователю.");
            return;
        }

        linkService.delete(shortUrl);
        System.out.println("Ссылка удалена.");
    }

    private void showHelp() {
        System.out.println("\n--- Справка по командам ---");
        System.out.println("Эта программа позволяет создавать короткие ссылки,");
        System.out.println("управлять ими, открывать, редактировать и удалять.");
        System.out.println();
        System.out.println("Доступные команды:");
        System.out.println("1 - Создать новую короткую ссылку");
        System.out.println("    • Введите оригинальный URL");
        System.out.println("    • Введите лимит переходов");
        System.out.println("    • Ссылка будет жить 24 часа по умолчанию");
        System.out.println();
        System.out.println("2 - Показать все ваши ссылки");
        System.out.println("    • Показывает список: короткая ссылка → оригинальная");
        System.out.println("    • Показывает лимит переходов и использованные клики");
        System.out.println("    • Показывает истекла ссылка или ещё работает");
        System.out.println();
        System.out.println("3 - Перейти по короткой ссылке");
        System.out.println("    • Если ссылка не просрочена и лимит не исчерпан, то откроется браузер");
        System.out.println();
        System.out.println("4 - Редактировать параметры ссылки");
        System.out.println("    Подменю:");
        System.out.println("       1 — Редактировать лимит переходов");
        System.out.println("       2 — Редактировать время жизни ссылки в часах");
        System.out.println("       3 — Назад");
        System.out.println();
        System.out.println("5 - Удалить ссылку");
        System.out.println("    • Можно удалять только свои ссылки");
        System.out.println();
        System.out.println("6 - Выход");
        System.out.println();
        System.out.println("help - Показать данную справку");
    }

    // Для запуска CLI
    public static void main(String[] args) { new LinkApp().run();
    }
}
