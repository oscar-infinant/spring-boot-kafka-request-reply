package com.example.kafkarequestreply;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class GreetingMessage {

    @Id
    private Long id;

    private String message;

    public GreetingMessage() {}

    public GreetingMessage(Long id, String message) {
        this.id = id;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }
}
