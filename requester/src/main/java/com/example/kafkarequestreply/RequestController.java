package com.example.kafkarequestreply;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class RequestController {
    private static final Logger log = LogManager.getLogger(RequestController.class);

    @Autowired
    @Qualifier("replyingReadTemplate")
    private ReplyingKafkaTemplate<Integer, String, String> templateRead;

    @Autowired
    @Qualifier("replyingWriteTemplate")
    private ReplyingKafkaTemplate<Integer, String, String> templateWrite;

    @Autowired
    private ReactiveRequestReplyHandler reactiveRequestReplyHandler;

    /**
     * Begin: Handles a reactive request-reply interaction over Kafka using non-blocking I/O.
     * Sends a message to the specified request topic and awaits a correlated response on the reply topic.
     * Returns an HTTP 200 with the Kafka response, or 500 if any error occurs during the process.
     * **/

    @PostMapping("/reactive/send")
    public Mono<ResponseEntity<String>> sendReactiveMessage(@RequestBody String message) {
        log.info("Reactive Post Request received: {}", message);
        return reactiveRequestReplyHandler.sendAndReceive("kRequests-write-reactive", "kReplies-reactive", message)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Kafka error: " + e.getMessage())));
    }

    @GetMapping("/reactive/send/{messageId}")
    public Mono<ResponseEntity<String>> getReactiveMessage(@PathVariable String messageId) {
        log.info("Reactive Get Request received: {}", messageId);
        return reactiveRequestReplyHandler.sendAndReceive("kRequests-read-reactive", "kReplies-reactive", messageId)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Kafka error: " + e.getMessage())));
    }

    /** End: Non-blocking implementation **/

    /**
     * Begin: use of CompletableFuture delegates work to another
     * thread but may still involve blocking operations.
     * Using it alone does not ensure that the application behaves in a non-blocking manner.
     * **/

    @PostMapping("/send")
    public CompletableFuture<ResponseEntity<String>> sendMessage(@RequestBody String message) {
        log.info("Post Request received: {}", message);
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProducerRecord<Integer, String> record = new ProducerRecord<>("kRequests-write", message);
                RequestReplyFuture<Integer, String, String> future = templateWrite.sendAndReceive(record);
                ConsumerRecord<Integer, String> response = future.get(10, TimeUnit.SECONDS);
                return ResponseEntity.ok(response.value());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Kafka failed");
            }
        });
    }

    @GetMapping("/send/{messageId}")
    public CompletableFuture<ResponseEntity<String>> getMessage(@PathVariable String messageId) {
        log.info("Get Request received: {}", messageId);
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProducerRecord<Integer, String> record = new ProducerRecord<>("kRequests", messageId);
                RequestReplyFuture<Integer, String, String> future = templateRead.sendAndReceive(record);
                ConsumerRecord<Integer, String> response = future.get(10, TimeUnit.SECONDS);
                return ResponseEntity.ok(response.value());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Kafka failed");
            }
        });
    }
}
