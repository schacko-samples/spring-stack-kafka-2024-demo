package app2;

import com.github.javafaker.Book;
import com.github.javafaker.Faker;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootApplication
public class SpringKafkaApp2 {

	private static final Logger logger = LoggerFactory.getLogger(SpringKafkaApp2.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringKafkaApp2.class, args);
	}

	@Bean
	public NewTopic springKafkaApp2DemoTopic() {
		return TopicBuilder.name("spring-kafka-app2-demo")
				.partitions(3)
				.replicas(3)
				.build();
	}

	@Bean
	public ApplicationRunner runner(KafkaTemplate<String, Foo> kafkaTemplate) {
		Faker faker = Faker.instance();
		return args -> {
			for (int i = 0; i < 10; i++) {
				final Book book = faker.book();
				Foo foo = new Foo(book.title(), book.author(), book.genre(), book.publisher());
				kafkaTemplate.send("spring-kafka-app2-demo", foo.title(), foo);
				Thread.sleep(100);
			}
		};
	}

	@KafkaListener(id = "sk-app2-demo-group", topics = "spring-kafka-app2-demo")
	public void listen(Foo in) {
		logger.info("Data Received : " + in);
	}

	public record Foo(String title, String author, String genre, String publisher){}

}
