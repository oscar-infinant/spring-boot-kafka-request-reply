package com.example.kafkarequestreply;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest
class KafkaRequestReplyApplicationTests {
	@MockBean
	private KafkaTemplate<String, String> kafkaTemplate;

	//@Test
	void contextLoads() {
	}

}
