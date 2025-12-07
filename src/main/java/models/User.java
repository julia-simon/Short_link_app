package models;

import lombok.Data;

import java.util.UUID;

@Data
public class User {
    private final UUID id;

    public User(UUID id) {
        this.id = id;
    }
}
