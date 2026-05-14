package slit.slitserver;

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;
import slit.slitserver.service.ExchangeRateService;

@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class SlitServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SlitServerApplication.class, args);
    }

    /** Shared RestClient bean — used by ExchangeRateService and ScanService. */
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

    /** Shared Jackson ObjectMapper bean — used by ScanService. */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /** On startup: fetch exchange rates if the table is empty. */
    @Bean
    public ApplicationRunner fetchRatesOnStartup(ExchangeRateService exchangeRateService) {
        return args -> exchangeRateService.fetchIfEmpty();
    }
}
