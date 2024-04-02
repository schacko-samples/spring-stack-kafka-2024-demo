package app8;

import io.micrometer.common.KeyValues;
import java.util.UUID;
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
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.micrometer.KafkaRecordSenderContext;
import org.springframework.kafka.support.micrometer.KafkaTemplateObservationConvention;

@SpringBootApplication
public class SpringKafkaApp8 {

    private static final Logger logger = LoggerFactory.getLogger(SpringKafkaApp8.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringKafkaApp8.class, args);
    }

    @Bean
    public NewTopic springKafkaApp0Demo1Topic() {
        return TopicBuilder.name("spring-kafka-app8-demo")
                .partitions(1)
                .replicas(3)
                .build();
    }

    @Bean
    public ApplicationRunner runner(KafkaTemplate<String, String> kafkaTemplate) {
        return args -> {
            CompletableFuture<SendResult<String, String>> send = kafkaTemplate.send("spring-kafka-app8-demo",
                    UUID.randomUUID().toString(),
                    "I am an observer!");
            send.get();
            send.whenComplete((s, e) -> {
                logger.info("data sent.");
            });

        };
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        KafkaTemplate<String, String> t = new KafkaTemplate<>(producerFactory);
        t.setObservationEnabled(true);
        t.setObservationConvention(new KafkaTemplateObservationConvention() {
            @Override
            public KeyValues getLowCardinalityKeyValues(KafkaRecordSenderContext context) {
                return KeyValues.of("topic", context.getDestination(),
                        "id", String.valueOf(context.getRecord().key()));
            }
        });
        return t;
    }

    static class ConsumerConfig {
        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, String> listenerFactory(ConsumerFactory<String, String> consumerFactory) {
            ConcurrentKafkaListenerContainerFactory<String, String> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.getContainerProperties().setObservationEnabled(true);
            factory.setConsumerFactory(consumerFactory);
            return factory;
        }
    }

    @KafkaListener(id = "spring-kafka-app8-demo-group", topics = "spring-kafka-app8-demo", containerFactory = "listenerFactory")
    public void listen(String in) {
        System.out.println("Data Received : " + in);
    }


}
