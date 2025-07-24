package com.example.kafkarequestreply;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@Log4j2
@EnableKafka
@EnableAsync
public class KafkaRequestReplyApplication {
	private static final Logger log = LogManager.getLogger(KafkaRequestReplyApplication.class);

	@Value("${spring.application.name}")
	private String applicationName;

	@Value("${kafka.request.message:foo}")
	private String requestMessage;

	@Value("${kafka.reply.group:repliesGroup-0}")
	private String replyGroup;

	@Value("${kafka.reply-write.group:repliesWriteGroup-0}")
	private String replyWriteGroup;

	public static void main(String[] args) {
		SpringApplication.run(KafkaRequestReplyApplication.class, args);
	}

	@Bean (name = "replyingReadTemplate")
	public ReplyingKafkaTemplate<Integer, String, String> replyingTemplate(
			ProducerFactory<Integer, String> pf,
			ConcurrentMessageListenerContainer<Integer, String> repliesContainer) {

		ReplyingKafkaTemplate<Integer, String, String> template = new ReplyingKafkaTemplate<>(pf, repliesContainer);
		template.setSharedReplyTopic(true);
		return template;
	}

	@Bean (name = "replyingWriteTemplate")
	public ReplyingKafkaTemplate<Integer, String, String> replyingWriteTemplate(
			ProducerFactory<Integer, String> pf,
			ConcurrentMessageListenerContainer<Integer, String> repliesWriteContainer) {

		ReplyingKafkaTemplate<Integer, String, String> template = new ReplyingKafkaTemplate<>(pf, repliesWriteContainer);
		template.setSharedReplyTopic(true);
		return template;
	}

	@Bean
	public ConcurrentMessageListenerContainer<Integer, String> repliesContainer(
			ConcurrentKafkaListenerContainerFactory<Integer, String> containerFactory) {

		ConcurrentMessageListenerContainer<Integer, String> repliesContainer =
				containerFactory.createContainer("kReplies");
		repliesContainer.getContainerProperties().setGroupId(replyGroup); // Overrides any `group.id` property provided by the consumer factory configuration
		repliesContainer.setAutoStartup(false);
		repliesContainer.setConcurrency(4); //
		return repliesContainer;
	}

	@Bean
	public ConcurrentMessageListenerContainer<Integer, String> repliesWriteContainer(
			ConcurrentKafkaListenerContainerFactory<Integer, String> containerFactory) {

		ConcurrentMessageListenerContainer<Integer, String> repliesContainer =
				containerFactory.createContainer("kReplies-write");
		repliesContainer.getContainerProperties().setGroupId(replyWriteGroup);
		repliesContainer.setAutoStartup(false);
		repliesContainer.setConcurrency(4); //
		return repliesContainer;
	}

	@Bean
	public NewTopic kRequests() {
		return TopicBuilder.name("kRequests")
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
}