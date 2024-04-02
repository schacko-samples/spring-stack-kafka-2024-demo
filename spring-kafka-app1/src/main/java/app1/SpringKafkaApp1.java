package app1;

import java.util.Collections;
import java.util.UUID;

import com.github.javafaker.Book;
import com.github.javafaker.Faker;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SpringKafkaApp1 {

	private static final Logger logger = LoggerFactory.getLogger(SpringKafkaApp1.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringKafkaApp1.class, args);
	}

	@Bean
	public KafkaAdmin.NewTopics springKafkaApp1Demo1Topics() {
		return new KafkaAdmin.NewTopics(
				TopicBuilder.name("spring-kafka-app1-demo1")
						.partitions(10)
						.replicas(3)
						.build(),
				TopicBuilder.name("spring-kafka-app1-demo2")
						.partitions(10)
						.replicas(3)
						.build());
	}

	@KafkaListener(id = "sk-app1-demo1-group", topicPartitions =
	@TopicPartition(topic = "spring-kafka-app1-demo1",
			partitionOffsets = {
					@PartitionOffset(partition = "0-2", initialOffset = "0"),
					@PartitionOffset(partition = "6-9", initialOffset = "0")
			}),
			properties = "key.deserializer:org.apache.kafka.common.serialization.LongDeserializer")
	public void listen1(String in, @Header(KafkaHeaders.RECEIVED_KEY) Long key,
						@Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
						@Header(KafkaHeaders.OFFSET) int offset) {
		logger.info("Listener-1:: Data Received : {} with key {} from partition {} and offset {}.", in, key, partition, offset);
	}

	@KafkaListener(id = "sk-app1-demo2-group", topicPartitions = {
			@TopicPartition(topic = "spring-kafka-app1-demo2", partitions = "0-1, 4, 6, 8-9",
					partitionOffsets = @PartitionOffset(partition = "*", initialOffset = "0")
			)},
			properties = "key.deserializer:org.apache.kafka.common.serialization.UUIDDeserializer")
	public void listen2(String in, @Header(KafkaHeaders.RECEIVED_KEY) UUID key,
						@Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
						@Header(KafkaHeaders.OFFSET) int offset) {
		logger.info("Listener-2:: Data Received : {} with key {} from partition {} and offset {}.", in, key, partition, offset);
	}

	@RestController
	static class Controller {

		Faker faker = Faker.instance();

		@Autowired @Lazy KafkaTemplate<Long, String> longStringKafkaTemplate;
		@Autowired @Lazy KafkaTemplate<UUID, String> uuidStringKafkaTemplate;

		@PostMapping(path = "/publish/demo1")
		public void publishDemo1() {
			final Book book = faker.book();
			for (int i = 0; i < 100; i++) {
				longStringKafkaTemplate.send("spring-kafka-app1-demo1", faker.random().nextLong(),
						String.join(", ", book.title(), book.author(), book.genre(), book.publisher()));
			}
		}

		@PostMapping(path = "/publish/demo2")
		public void publishDemo2() {
			final Book book = faker.book();
			for (int i = 0; i < 100; i++) {
				uuidStringKafkaTemplate.send("spring-kafka-app1-demo2", UUID.randomUUID(),
						String.join(", ", book.title(), book.author(), book.genre(), book.publisher()));
			}
		}

		@Bean
		public KafkaTemplate<Long, String> longStringKafkaTemplate(ProducerFactory<Long, String> pf) {
			return new KafkaTemplate<>(pf,
					Collections.singletonMap(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class));
		}

		@Bean
		public KafkaTemplate<UUID, String> uuidStringKafkaTemplate(ProducerFactory<UUID, String> pf) {
			return new KafkaTemplate<>(pf,
					Collections.singletonMap(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class));
		}
	}
}
