package com.example.kafkarequestreply;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.messaging.handler.annotation.SendTo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;

@SpringBootApplication
@Log4j2
public class KafkaRequestReplyApplication {
	private static final Logger log = LogManager.getLogger(KafkaRequestReplyApplication.class);
	@Autowired
	private GreetingMessageRepository greetingRepo;

	public static void main(String[] args) {
		SpringApplication.run(KafkaRequestReplyApplication.class, args);
	}

	@KafkaListener(id = "server-read", topics = "kRequests", concurrency = "4")
	@SendTo("kReplies")
	public String listenReads(String in) {
		log.info("Server read received: {}", in);

		String dbMessage = greetingRepo.findById(1L)
				.map(GreetingMessage::getMessage)
				.orElse("No greeting found in DB");

		return "Input message: " + in + " | Response Message: " + dbMessage;
	}

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
