package app5;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class SpringKafkaApp5 {

	private static final Logger logger = LoggerFactory.getLogger(SpringKafkaApp5.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringKafkaApp5.class, args);
	}

	static class Producer {

		@Bean
		public ApplicationRunner runner(KafkaTemplate<String, Object> kafkaTemplate) {
			return args -> {
				Map<Class<?>, Serializer<?>> delegates = new HashMap<>();
				delegates.put(byte[].class, new ByteArraySerializer());
				delegates.put(String.class, new StringSerializer());
				((DefaultKafkaProducerFactory<String, Object>) kafkaTemplate.getProducerFactory())
						.setValueSerializer(new DelegatingByTypeSerializer(delegates));

				kafkaTemplate.send("spring-kafka-app5-demo", "hello");

			};
		}
	}

	static class Admin {

		@Bean
		public NewTopic springKafkaApp5DemoTopic() {
			return TopicBuilder.name("spring-kafka-app5-demo")
					.partitions(1)
					.replicas(3)
					.build();
		}

		@Bean
		public NewTopic springKafkaApp5DemoDltTopic() {
			return TopicBuilder.name("spring-kafka-app5-demo.DLT")
					.partitions(1)
					.replicas(3)
					.build();
		}

	}

	@Component
	static class Listener {

		@KafkaListener(id = "sk-app5-demo-group", topics = "spring-kafka-app5-demo")
		public void listen(Integer in) {
			logger.info("Data Received : " + in);
		}

		@Bean
		public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer recoverer) {
			return new DefaultErrorHandler(recoverer);
		}

		@Bean
		public DeadLetterPublishingRecoverer publisher(KafkaTemplate<String, Object> template) {
			return new DeadLetterPublishingRecoverer(template);
		}

		@KafkaListener(id = "from.dlt-app5", topics = "spring-kafka-app5-demo.DLT",
				properties = "value.deserializer:org.apache.kafka.common.serialization.ByteArrayDeserializer")
		public void listenFromDlt(byte[] in,
								  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
								  @Header(KafkaHeaders.OFFSET) int offset) {
			logger.info("DLT Data Received : {} from partition {} and offset {}.", in, partition, offset);
		}

	}

}
