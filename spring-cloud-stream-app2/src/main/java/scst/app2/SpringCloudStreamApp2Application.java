package scst.app2;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SpringCloudStreamApp2Application {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudStreamApp2Application.class, args);
	}

	@Autowired
	StreamBridge streamBridge;

	@PostMapping(path = "/publish/demo")
	public void publishDemo(@RequestBody String data) {
		streamBridge.send("sensor-out-0", data);
	}

	@Bean
	Consumer<String> receive() {
		return s -> System.out.println("Received data: " + s);
	}

}
