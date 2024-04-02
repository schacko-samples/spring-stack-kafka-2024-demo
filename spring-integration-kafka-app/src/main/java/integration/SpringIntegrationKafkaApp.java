package integration;

import java.util.Collections;
import java.util.Map;

import com.github.javafaker.Book;
import com.github.javafaker.Faker;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

@SpringBootApplication
public class SpringIntegrationKafkaApp {

	private static final Logger logger = LoggerFactory.getLogger(SpringIntegrationKafkaApp.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringIntegrationKafkaApp.class, args);
	}


	@Bean
	public NewTopic quickTopic() {
		return TopicBuilder.name("spring-integration-kafka-app-demo")
				.partitions(1)
				.replicas(3)
				.build();
	}

	@Bean
	public ApplicationRunner runner(KafkaTemplate<String, String> kafkaTemplate, ConfigurableApplicationContext context) {
		Faker faker = Faker.instance();
		return args -> {
			MessageChannel toKafka = context.getBean("toKafka", MessageChannel.class);
			System.out.println("Sending 10 messages...");
			for (int i = 0; i < 10; i++) {
				final Book book = faker.book();
				Map<String, Object> headers = Collections.singletonMap(KafkaHeaders.TOPIC, "spring-integration-kafka-app-demo");
				toKafka.send(new GenericMessage<>(i + 1 + " : " +
						String.join(", ", book.title(), book.author(), book.genre(), book.publisher()), headers));
				Thread.sleep(100);
			}

			PollableChannel fromKafka = context.getBean("fromKafka", PollableChannel.class);
			Message<?> received = fromKafka.receive(10_000);
			int count = 0;
			while (received != null) {
				System.out.println(received);
				received = fromKafka.receive(++count < 11 ? 10000 : 1000);
			}
		};
	}

	@Bean
	public KafkaMessageListenerContainer<String, String> container(
			ConsumerFactory<String, String> kafkaConsumerFactory) {
		final ContainerProperties containerProperties = new ContainerProperties("spring-integration-kafka-app-demo");
		containerProperties.setGroupId("si-kafka-demo-group");
		return new KafkaMessageListenerContainer<>(kafkaConsumerFactory,
				containerProperties);
	}

	@Bean
	public KafkaMessageDrivenChannelAdapter<String, String> adapter(KafkaMessageListenerContainer<String, String> container) {
		KafkaMessageDrivenChannelAdapter<String, String> kafkaMessageDrivenChannelAdapter =
				new KafkaMessageDrivenChannelAdapter<>(container);
		kafkaMessageDrivenChannelAdapter.setOutputChannel(fromKafka());
		return kafkaMessageDrivenChannelAdapter;
	}

	@Bean
	public PollableChannel fromKafka() {
		return new QueueChannel();
	}

	@ServiceActivator(inputChannel = "toKafka")
	@Bean
	public MessageHandler handler(KafkaTemplate<String, String> kafkaTemplate) {
		return new KafkaProducerMessageHandler<>(kafkaTemplate);
	}

}
