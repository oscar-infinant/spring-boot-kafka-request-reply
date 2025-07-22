package com.example.kafkarequestreply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GreetingMessageRepository extends JpaRepository<GreetingMessage, Long> {
}
