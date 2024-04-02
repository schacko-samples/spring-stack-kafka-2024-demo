package app0;

import com.github.javafaker.Book;
import com.github.javafaker.Faker;
import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

@SpringBootApplication
public class SpringKafkaApp0 {

	public static void main(String[] args) {
		SpringApplication.run(SpringKafkaApp0.class, args);
	}

	@Bean
	public NewTopic springKafkaApp0Demo1Topic() {
		return TopicBuilder.name("spring-kafka-app0-demo1")
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
				CompletableFuture<SendResult<String, String>> send = kafkaTemplate.send("spring-kafka-app0-demo1",
						String.join(", ", book.title(), book.author(), book.genre(), book.publisher()));
				send.get();
			}
		};
	}

	@KafkaListener(id = "sk-app0-demo1-group", topics = "spring-kafka-app0-demo1")
	public void listen(String in) {
		System.out.println("Data Received : " + in);
	}
}
