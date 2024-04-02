package app3;

import java.util.Map;

import com.github.javafaker.Book;
import com.github.javafaker.Faker;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class SpringKafkaApp3 {

	private static final Logger logger = LoggerFactory.getLogger(SpringKafkaApp3.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringKafkaApp3.class, args);
	}

	@Bean
	public NewTopic springKafkaApp3DemoTopic() {
		return TopicBuilder.name("spring-kafka-app3-demo")
				.partitions(1)
				.replicas(3)
				.build();
	}

	@Bean
	public ApplicationRunner runner(KafkaTemplate<String, String> kafkaTemplate) {
		Faker faker = Faker.instance();
		return args -> {
			for (int i = 0; i < 100; i++) {
				final Book book = faker.book();
				kafkaTemplate.send("spring-kafka-app3-demo",
						String.join(", ", book.title(), book.author(), book.genre(), book.publisher()));
			}
		};
	}


	@Component
	static class Listener extends AbstractConsumerSeekAware {

		@KafkaListener(id = "sk-app3-demo-group", topics = "spring-kafka-app3-demo")
		public void listen(String in,  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
						   @Header(KafkaHeaders.OFFSET) int offset) {
			logger.info("Data Received : {} from partition {} and offset {}.", in, partition, offset);
		}

		@Override
		public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
			System.out.println("Assignments:" + assignments);
			super.onPartitionsAssigned(assignments, callback);
			waitUntil(1000); // wait so that we give the publisher a chance to publish record with offset 20.
			callback.seekRelative("spring-kafka-app3-demo", 0, 20, false);
		}

		private void waitUntil(long t) {
			try {
				Thread.sleep(t);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
