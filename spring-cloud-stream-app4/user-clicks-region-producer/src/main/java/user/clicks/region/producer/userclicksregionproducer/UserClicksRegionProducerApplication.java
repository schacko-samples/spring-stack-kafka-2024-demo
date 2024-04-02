package user.clicks.region.producer.userclicksregionproducer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class UserClicksRegionProducerApplication {

	@Autowired
	StreamBridge streamBridge;

	public static void main(String[] args) {
		SpringApplication.run(UserClicksRegionProducerApplication.class, args);
	}

	@RequestMapping(value = "/user-region/{user}/{region}", method = RequestMethod.POST)
	@ResponseBody
	public void region(@PathVariable("user") String user, @PathVariable("region") String region) {
		streamBridge.send("regions", MessageBuilder.withPayload(region)
				.setHeader(KafkaHeaders.KEY, user).build());
	}

	@RequestMapping(value = "/user-clicks/{user}/{clicks}", method = RequestMethod.POST)
	@ResponseBody
	public void clicks(@PathVariable("user") String user, @PathVariable("clicks") long clicks) {
		streamBridge.send("clicks", MessageBuilder.withPayload(clicks)
				.setHeader(KafkaHeaders.KEY, user).build());
	}

}
