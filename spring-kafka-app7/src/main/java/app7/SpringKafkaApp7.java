package app7;

import com.github.javafaker.Book;
import com.github.javafaker.Faker;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.backoff.FixedBackOff;

@SpringBootApplication
public class SpringKafkaApp7 {

	private static final Logger logger = LoggerFactory.getLogger(SpringKafkaApp7.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringKafkaApp7.class, args);
	}

	@Bean
	public NewTopic springKafkaApp7DemoTopic() {
		return TopicBuilder.name("spring-kafka-app7-demo")
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
				CompletableFuture<SendResult<String, String>> send = kafkaTemplate.send("spring-kafka-app7-demo",
						String.join(", ", book.title(), book.author(), book.genre(), book.publisher()));
				send.get();
			}
		};
	}

	@KafkaListener(id = "sk-app7-demo-group", topics = "spring-kafka-app7-demo", containerFactory = "batchKafkaListenerContainerFactory")
	public void listen(List<String> in, Acknowledgment acknowledgment) {
		logger.info("Size of Data Received : " + in.size());
		in.forEach(rec -> acknowledgment.acknowledge());
	}

	static class ConsumerConfig {

		@Bean
		KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> batchKafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
			ConcurrentKafkaListenerContainerFactory<String, String> factory =
					new ConcurrentKafkaListenerContainerFactory<>();
			factory.setConsumerFactory(consumerFactory);
			factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0, 4)));
			factory.setBatchListener(true);
			factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
			return factory;
		}

	}
}
