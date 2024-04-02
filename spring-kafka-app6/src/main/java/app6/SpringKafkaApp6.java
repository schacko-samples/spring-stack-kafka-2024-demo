package app6;

import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicHeaders;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class SpringKafkaApp6 {

	public static void main(String[] args) {
		SpringApplication.run(SpringKafkaApp6.class, args).close();
	}

	@Bean
	NewTopic topic() {
		return TopicBuilder.name("s1-app6").partitions(1).replicas(1).build();
	}

	@Bean
	ApplicationRunner runner(KafkaTemplate<String, String> template) {
		return args -> {
			Thread.sleep(5_000);
			template.send("s1-app6", "one").get(10, TimeUnit.SECONDS);
			template.send("s1-app6", "two").get(10, TimeUnit.SECONDS);
			Thread.sleep(30_000);
		};
	}

}

@Component
class Listener {

	private static final Logger log = LoggerFactory.getLogger(Listener.class);

	@KafkaListener(id = "app6", topics = "s1-app6")
	@RetryableTopic(attempts = "5", backoff = @Backoff(delay = 2000, multiplier = 1.5))
	void listen(String in, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
			@Header(name = RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS, required = false) Integer attempt) {

		log.info(in + " received from " + topic + ", attempt=" + (attempt == null ? 1 : attempt));
		if (in.equals("one")) {
			throw new RuntimeException("test");
		}
	}

	@DltHandler
	void dlt(String in, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
			@Header(RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS) Integer attempt) {

		log.info(in + " received from " + topic + ", attempt=" + attempt);
	}

}
