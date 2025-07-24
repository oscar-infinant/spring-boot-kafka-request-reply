package com.example.kafkarequestreply;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReactiveRequestReplyHandler {

    @Autowired
    private KafkaSender<Integer, String> kafkaSender;

    @Autowired
    private KafkaReceiver<Integer, String> replyReceiver;

    private final Map<String, MonoSink<String>> pendingReplies = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        replyReceiver.receive()
                .doOnNext(record -> {
                    String correlationId = record.headers().lastHeader(KafkaHeaders.CORRELATION_ID) != null
                            ? new String(record.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value(), StandardCharsets.UTF_8)
                            : null;

                    if (correlationId != null && pendingReplies.containsKey(correlationId)) {
                        pendingReplies.remove(correlationId).success(record.value());
                    }
                })
                .subscribe();
    }

    public Mono<String> sendAndReceive(String topic, String replyTopic, String message) {
        String correlationId = UUID.randomUUID().toString();

        return Mono.create((MonoSink<String> sink) -> {
                    pendingReplies.put(correlationId, sink);

                    ProducerRecord<Integer, String> record = new ProducerRecord<>(topic, message);
                    record.headers().add(KafkaHeaders.CORRELATION_ID, correlationId.getBytes(StandardCharsets.UTF_8));
                    record.headers().add(KafkaHeaders.REPLY_TOPIC, replyTopic.getBytes(StandardCharsets.UTF_8));

                    SenderRecord<Integer, String, String> senderRecord = SenderRecord.create(record, correlationId);

                    kafkaSender.send(Mono.just(senderRecord))
                            .doOnError(sink::error)
                            .subscribe();
                }).timeout(Duration.ofSeconds(10)) // timeout configurable
                .onErrorResume(e -> Mono.error(new RuntimeException("Timeout or failure waiting for reply", e)));
    }
}