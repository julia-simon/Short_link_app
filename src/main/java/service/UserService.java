package service;

import models.User;

import java.util.UUID;

public class UserService {
    private User currentUser;

    public User getCurrentUser() {
        if (currentUser == null) {
            currentUser = new User(UUID.randomUUID());
        }
        return currentUser;
    }
}
