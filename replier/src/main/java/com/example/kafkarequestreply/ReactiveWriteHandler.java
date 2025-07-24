package com.example.kafkarequestreply;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
public class ReactiveWriteHandler {
    private static final Logger log = LogManager.getLogger(ReactiveWriteHandler.class);

    @Autowired
    @Qualifier("reactiveRequestWriteReceiver")
    private KafkaReceiver<Integer, String> reactiveRequestWriteReceiver;

    @Autowired
    @Qualifier("reactiveRequestReadReceiver")
    private KafkaReceiver<Integer, String> reactiveRequestReadReceiver;

    @Autowired
    private KafkaSender<Integer, String> kafkaSender;

    @Autowired
    private GreetingMessageRepository greetingRepo;

    @PostConstruct
    public void init() {
        reactiveRequestReadReceiver.receive()
                .flatMap(record -> {
                    String messageId = record.value();
                    String correlationId = getHeader(record, KafkaHeaders.CORRELATION_ID);
                    String replyTo = getHeader(record, KafkaHeaders.REPLY_TOPIC);

                    return processReadMessage(Long.valueOf(messageId)) // Business logic execution
                            .map(response -> {
                                ProducerRecord<Integer, String> replyRecord = new ProducerRecord<>(replyTo, response);
                                replyRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId.getBytes(StandardCharsets.UTF_8));
                                return SenderRecord.create(replyRecord, correlationId);
                            });
                })
                .as(kafkaSender::send) // reactively send
                .doOnError(err -> System.err.println("Error: " + err.getMessage()))
                .subscribe();

        reactiveRequestWriteReceiver.receive()
                .flatMap(record -> {
                    String message = record.value();
                    String correlationId = getHeader(record, KafkaHeaders.CORRELATION_ID);
                    String replyTo = getHeader(record, KafkaHeaders.REPLY_TOPIC);

                    return processWriteMessage(message) // Business logic execution
                            .map(response -> {
                                ProducerRecord<Integer, String> replyRecord = new ProducerRecord<>(replyTo, response);
                                replyRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId.getBytes(StandardCharsets.UTF_8));
                                return SenderRecord.create(replyRecord, correlationId);
                            });
                })
                .as(kafkaSender::send) // reactively send
                .doOnError(err -> System.err.println("Error: " + err.getMessage()))
                .subscribe();
    }

    private Mono<String> processWriteMessage(String in) {
        log.info("Reactive Server write received: {}", in);

        GreetingMessage greeting = new GreetingMessage();
        greeting.setMessage(in);
        greeting.setSender("Requester Service");
        greeting.setCreatedAt(LocalDateTime.now());

        final GreetingMessage saved = greetingRepo.save(greeting);

        return Mono.just("Message saved | id: " + saved.getId());
    }

    private Mono<String> processReadMessage(Long in) {
        log.info("Reactive Server read received: {}", in);

        String dbMessage = greetingRepo.findById(in)
                .map(GreetingMessage::getMessage)
                .orElse("No greeting found in DB");

        return Mono.just("Response Message saved: " + dbMessage);
    }

    private String getHeader(ConsumerRecord<Integer, String> record, String headerKey) {
        Header header = record.headers().lastHeader(headerKey);
        return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
    }

    @Bean
    public NewTopic kRequestsWriteReactive() {
        return TopicBuilder.name("kRequests-write-reactive")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic kRequestsReadReactive() {
        return TopicBuilder.name("kRequests-read-reactive")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic kRepliesReactive() {
        return TopicBuilder.name("kReplies-reactive")
                .partitions(2)
                .replicas(1)
                .build();
    }
}
