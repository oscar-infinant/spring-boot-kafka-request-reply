package com.example.kafkarequestreply;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private KafkaRequestReplyApplication app; // contiene pendingReplies

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

    @GetMapping("/send")
    public CompletableFuture<ResponseEntity<String>> getMessage(@RequestBody String message) {
        log.info("Get Request received: {}", message);
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProducerRecord<Integer, String> record = new ProducerRecord<>("kRequests", message);
                RequestReplyFuture<Integer, String, String> future = templateRead.sendAndReceive(record);
                ConsumerRecord<Integer, String> response = future.get(10, TimeUnit.SECONDS);
                return ResponseEntity.ok(response.value());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Kafka failed");
            }
        });
    }
}
