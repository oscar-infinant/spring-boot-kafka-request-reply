package com.example.kafkarequestreply;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@SpringBootApplication
@Log4j2
public class KafkaRequestReplyApplication {
	private static final Logger log = LogManager.getLogger(KafkaRequestReplyApplication.class);

	@Autowired
	private GreetingMessageRepository greetingRepo;

	@Autowired
	private KafkaTemplate<Integer, String> kafkaTemplate;

	public static void main(String[] args) {
		SpringApplication.run(KafkaRequestReplyApplication.class, args);
	}

	/******* Begin: Multi threaded message processing implementation ******/

	@KafkaListener(id = "server-read", topics = "kRequests", concurrency = "4")
	public void listenReads(Long in, @Header(KafkaHeaders.CORRELATION_ID) byte[] correlationId,
							@Header(KafkaHeaders.REPLY_TOPIC) byte[] replyTo) {
		log.info("KafkaHeaders.REPLY_TOPIC {}", replyTo);
		CompletableFuture.supplyAsync(() -> {
			log.info("Server read received: {}", in);
			String dbMessage = greetingRepo.findById(in)
					.map(GreetingMessage::getMessage)
					.orElse("No greeting found in DB");

			return "Response Message: " + dbMessage;
		}).thenAccept(result -> {
			ProducerRecord<Integer, String> reply = new ProducerRecord<>(
					new String(replyTo),
					null,
					null,
					null,
					result
			);
			reply.headers().add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId));
			kafkaTemplate.send(reply);
		});
	}

	@KafkaListener(id = "server-write", topics = "kRequests-write", concurrency = "4")
	public void listenWrites(String in,
							 @Header(KafkaHeaders.CORRELATION_ID) byte[] correlationId,
							 @Header(KafkaHeaders.REPLY_TOPIC) byte[] replyTo) {

		log.info("KafkaHeaders.REPLY_TOPIC {}", replyTo);

		CompletableFuture.supplyAsync(() -> {
			log.info("Server write received: {}", in);

			GreetingMessage greeting = new GreetingMessage();
			greeting.setMessage(in);
			greeting.setSender("Requester Service");
			greeting.setCreatedAt(LocalDateTime.now());

			GreetingMessage saved = greetingRepo.save(greeting);

			return "Message saved | id: " + saved.getId();
		}).thenAccept(result -> {
			ProducerRecord<Integer, String> reply = new ProducerRecord<>(
					new String(replyTo),
					null,
					null,
					null,
					result
			);
			reply.headers().add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId));
			kafkaTemplate.send(reply);
		});
	}

	/******* End: Multi threaded message processing implementation ******/

	/******* Begin: Sequential message processing implementation ******/

	/*

	@KafkaListener(id = "server-read", topics = "kRequests", concurrency = "4")
	@SendTo("kReplies")
	public String listenReads(Long in) {
		log.info("Server read received: {}", in);

		String dbMessage = greetingRepo.findById(in)
				.map(GreetingMessage::getMessage)
				.orElse("No greeting found in DB");

		return "Response Message: " + dbMessage;
	}
	*/

	/*
	@KafkaListener(id = "server-write", topics = "kRequests-write", concurrency = "4")
	@SendTo("kReplies-write")
	public String listenWrites(String in) {
		log.info("Server write received: {}", in);
		GreetingMessage greeting = new GreetingMessage();
		greeting.setMessage(in);
		greeting.setSender("Requester Service");
		greeting.setCreatedAt(LocalDateTime.now());

		GreetingMessage save = greetingRepo.save(greeting);

		String dbMessage = greetingRepo.findById(1L)
				.map(GreetingMessage::getMessage)
				.orElse("No greeting found in DB");

		return " Message saved | id: " + save.getId();
	}
	*/

	/******* End: Sequential message processing implementation ******/

	@Bean
	public NewTopic kRequests() {
		return TopicBuilder.name("kRequests")
				.partitions(2)
				.replicas(1)
				.build();
	}

	@Bean
	public NewTopic kRequestsWrite() {
		return TopicBuilder.name("kRequests-write")
				.partitions(2)
				.replicas(1)
				.build();
	}

	@Bean
	public NewTopic kReplies() {
		return TopicBuilder.name("kReplies")
				.partitions(2)
				.replicas(1)
				.build();
	}

	@Bean
	public NewTopic kRepliesWrite() {
		return TopicBuilder.name("kReplies-write")
				.partitions(2)
				.replicas(1)
				.build();
	}

	/*@Bean // not required if Jackson is on the classpath
	public MessagingMessageConverter simpleMapperConverter() {
		MessagingMessageConverter messagingMessageConverter = new MessagingMessageConverter();
		messagingMessageConverter.setHeaderMapper(new SimpleKafkaHeaderMapper());
		return messagingMessageConverter;
	}*/
}
