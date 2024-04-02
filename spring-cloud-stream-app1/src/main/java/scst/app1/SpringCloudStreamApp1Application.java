package scst.app1;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringCloudStreamApp1Application {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudStreamApp1Application.class, args);
	}

	@Bean
	public Supplier<Long> currentTime() {
		return System::currentTimeMillis;
	}

	@Bean
	public Function<Long, String> convertToUTC() {
		return l -> Instant.ofEpochMilli(l).atZone(ZoneOffset.UTC).toString();
	}

	@Bean
	public Consumer<String> print() {
		return utc -> System.out.println("Current UTC Time: " + utc);
	}

}
