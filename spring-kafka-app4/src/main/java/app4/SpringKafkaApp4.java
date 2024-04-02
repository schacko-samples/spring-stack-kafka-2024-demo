package app4;

import java.util.Collections;

import com.github.javafaker.Book;
import com.github.javafaker.Faker;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SpringKafkaApp4 {

	private static final Logger logger = LoggerFactory.getLogger(SpringKafkaApp4.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringKafkaApp4.class, args);
	}

	@RestController
	static class Controller {

		Faker faker = Faker.instance();

		@Autowired @Lazy KafkaTemplate<Long, String> longStringKafkaTemplate;

		@PostMapping(path = "/publish/demo")
		public void publishDemo() {
			final Book book = faker.book();
			for (int i = 0; i < 10; i++) {
				longStringKafkaTemplate.send("spring-kafka-app4-demo", faker.random().nextLong(),
						String.join(", ", book.title(), book.author(), book.genre(), book.publisher()));
			}
		}

		@Bean
		public KafkaTemplate<Long, String> longStringKafkaTemplate(ProducerFactory<Long, String> pf) {
			return new KafkaTemplate<>(pf,
					Collections.singletonMap(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class));
		}
	}

	static class Admin {

		@Bean
		public NewTopic springKafkaApp4DemoTopic() {
			return TopicBuilder.name("spring-kafka-app4-demo")
					.partitions(1)
					.replicas(3)
					.build();
		}

		@Bean
		public NewTopic springKafkaApp4DemoDltTopic() {
			return TopicBuilder.name("spring-kafka-app4-demo.DLT")
					.partitions(1)
					.replicas(3)
					.build();
		}

	}

	@Component
	static class Listener {

		@KafkaListener(id = "sk-app4-demo-group", topics = "spring-kafka-app4-demo",
				 		properties = "key.deserializer:org.apache.kafka.common.serialization.LongDeserializer")
		public void listen(String in, @Header(KafkaHeaders.RECEIVED_KEY) Long key,
						   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
						   @Header(KafkaHeaders.OFFSET) int offset) {
			logger.info("Data Received : {} with key {} from partition {} and offset {}.", in, key, partition, offset);
			if (offset > 0 && offset % 9 == 0) {
				throw new RuntimeException("fail");
			}
		}

		@Bean
		public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer recoverer) {
			return new DefaultErrorHandler(recoverer, new FixedBackOff(2_000, 2));
		}

		@Bean
		public DeadLetterPublishingRecoverer publisher(KafkaTemplate<Long, String> template) {
			return new DeadLetterPublishingRecoverer(template);
		}

		@KafkaListener(id = "from.dlt", topics = "spring-kafka-app4-demo.DLT",
				properties = "key.deserializer:org.apache.kafka.common.serialization.LongDeserializer")
		public void listenFromDlt(String in, @Header(KafkaHeaders.RECEIVED_KEY) Long key,
						   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
						   @Header(KafkaHeaders.OFFSET) int offset) {
			logger.info("DLT Data Received : {} with key {} from partition {} and offset {}.", in, key, partition, offset);
		}

	}
}
