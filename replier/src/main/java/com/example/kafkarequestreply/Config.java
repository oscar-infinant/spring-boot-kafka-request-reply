package com.example.kafkarequestreply;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableAsync
public class Config {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Begin: Refactor this implementation to a fully non-blocking, reactive approach.
     * The current use of CompletableFuture delegates work to another thread
     * but may still involve blocking operations (e.g., future.get()).
     * Consider migrating to Project Reactor (Mono/Flux) and Spring WebFlux
     * for better scalability and resource efficiency.
     * **/

    @Bean(name = "reactiveRequestReadReceiver")
    public KafkaReceiver<Integer, String> reactiveRequestReadReceiver() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "read-reactive-responder",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        );
        ReceiverOptions<Integer, String> options = ReceiverOptions.<Integer, String>create(props)
                .subscription(List.of("kRequests-read-reactive"));

        return KafkaReceiver.create(options);
    }

    @Bean(name = "reactiveRequestWriteReceiver")
    public KafkaReceiver<Integer, String> reactiveRequestWriteReceiver() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "write-reactive-responder",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        );
        ReceiverOptions<Integer, String> options = ReceiverOptions.<Integer, String>create(props)
                .subscription(List.of("kRequests-write-reactive"));

        return KafkaReceiver.create(options);
    }

    @Bean
    public KafkaSender<Integer, String> reactiveKafkaSender() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        );
        SenderOptions<Integer, String> options = SenderOptions.create(props);
        return KafkaSender.create(options);
    }

    /** End: Kafka reactive configuration **/

    @Bean
    public ProducerFactory<Integer, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

    @Bean
    public KafkaTemplate<Integer, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

}