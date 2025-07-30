package com.example.kafkarequestreply;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/blocking/send/{messageId}")
    public ResponseEntity<String> getBlockingMessage(@PathVariable String messageId) {
        log.info("Blocking Get Request received: {}", messageId);
        try {
            // Bloquea la ejecución del Mono hasta obtener la respuesta
            String response = reactiveRequestReplyHandler
                    .sendAndReceive("kRequests-read-reactive", "kReplies-reactive", messageId)
                    .block();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Kafka error: " + e.getMessage());
        }
    }


    @PostMapping("/blocking/send")
    public ResponseEntity<String> sendBlockingMessage(@RequestBody String message) {
        log.info("Blocking Post Request received: {}", message);
        try {
            // Block until the response is obtained
            String response = reactiveRequestReplyHandler
                    .sendAndReceive("kRequests-write-reactive", "kReplies-reactive", message)
                    .block();  // Block the execution

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Kafka error: " + e.getMessage());
        }
    }
}
