package com.example.kafkarequestreply;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "greeting_message")
public class GreetingMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PostgreSQL: requiere IDENTITY o SERIAL en BD
    private Long id;

    @Column(nullable = false, length = 255)
    private String message;

    @Column(name = "sender", nullable = false, length = 100)
    private String sender;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructor vacío para JPA
    public GreetingMessage() {}

    // Constructor con parámetros útiles
    public GreetingMessage(String message, String sender) {
        this.message = message;
        this.sender = sender;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "GreetingMessage{" +
                "id=" + id +
                ", message='" + message + '\'' +
                ", sender='" + sender + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
