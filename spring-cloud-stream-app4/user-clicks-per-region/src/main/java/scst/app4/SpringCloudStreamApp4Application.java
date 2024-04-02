package scst.app4;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SpringCloudStreamApp4Application {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudStreamApp4Application.class, args);
	}

	public static final String CLICK_UPDATES = "click-updates";

	protected final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private InteractiveQueryService interactiveQueryService;


	@Bean
	public BiFunction<KStream<String, Long>, KTable<String, String>, KStream<String, Long>> clicks() {
		return (userClicksStream, userRegionsTable) -> (userClicksStream
				.leftJoin(userRegionsTable, (clicks, region) -> new RegionWithClicks(region == null ?
								"UNKNOWN" : region, clicks),
						Joined.with(Serdes.String(), Serdes.Long(), null))
				.map((user, regionWithClicks) -> new KeyValue<>(regionWithClicks.getRegion(),
						regionWithClicks.getClicks()))
				.groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
				.reduce(Long::sum, Materialized.as(CLICK_UPDATES))
				.toStream());
	}

	@Bean
	public Consumer<KStream<String, Long>> updates() {
		return c -> c.foreach((key, value) -> {
			logger.info(String.format("Current updates: %s \t %d", key, value));
		});
	}

	@RequestMapping(value = "/updates/{region}")
	@ResponseBody
	public RegionWithClicks region(@PathVariable("region") String region) {

		final ReadOnlyKeyValueStore<Object, Object> queryableStore = interactiveQueryService.getQueryableStore(CLICK_UPDATES,
				QueryableStoreTypes.keyValueStore());
		final Long clicks = (Long)queryableStore.get(region);
		return new RegionWithClicks(region, clicks);
	}

	/**
	 * Tuple for a region and its associated number of clicks.
	 */
	private static final class RegionWithClicks {

		private final String region;
		private final long clicks;

		RegionWithClicks(String region, long clicks) {
			if (region == null || region.isEmpty()) {
				throw new IllegalArgumentException("region must be set");
			}
			if (clicks < 0) {
				throw new IllegalArgumentException("clicks must not be negative");
			}
			this.region = region;
			this.clicks = clicks;
		}

		public String getRegion() {
			return region;
		}

		public long getClicks() {
			return clicks;
		}

	}
}
