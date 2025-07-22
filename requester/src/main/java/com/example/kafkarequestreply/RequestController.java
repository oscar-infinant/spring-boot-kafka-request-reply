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
    private ReplyingKafkaTemplate<Integer, String, String> template;

    @Autowired
    private KafkaRequestReplyApplication app; // contiene pendingReplies

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody String message) {
        log.info("Post Request received: {}", message);
        try {
            // Construir el ProducerRecord con topic y payload
            ProducerRecord<Integer, String> record = new ProducerRecord<>(
                    "kRequests",
                    "App: " + "RequesterApp " + " | Message: " + message + "-" + LocalTime.now()
            );

            // (opcional) añadir headers si deseas trazabilidad
            record.headers().add(new RecordHeader("requestId", UUID.randomUUID().toString().getBytes()));

            // Enviar y esperar respuesta
            RequestReplyFuture<Integer, String, String> future = template.sendAndReceive(record);

            // Obtener la respuesta con timeout
            ConsumerRecord<Integer, String> response = future.get(10, TimeUnit.SECONDS);

            return ResponseEntity.ok(response.value());

        } catch (TimeoutException e) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Kafka reply timed out");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Kafka send/receive failed");
        }
    }
}
