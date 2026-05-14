package slit.slitserver.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import slit.slitserver.entity.ExchangeRate;
import slit.slitserver.repository.ExchangeRateRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    // ── Paste your ExchangeRate-API key here or set EXCHANGE_API_KEY env var ──
    // Sign up for free at https://www.exchangerate-api.com/  (1 500 req/month free)
    @Value("${exchange.api.key:}")
    private String apiKey;

    private final ExchangeRateRepository rateRepository;
    private final RestClient restClient;

    // ── Scheduled fetch — every day at 07:00 server time ─────────────────────

    @Scheduled(cron = "0 0 7 * * *")
    public void scheduledFetch() {
        fetchAndStore();
    }

    // ── Manual trigger (called once on startup if table is empty) ─────────────

    @Transactional
    public void fetchAndStore() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Exchange API key not configured — skipping rate fetch. " +
                     "Set exchange.api.key in application.properties or EXCHANGE_API_KEY env var.");
            return;
        }

        try {
            String url = "https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/USD";
            ExchangeRateApiResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(ExchangeRateApiResponse.class);

            if (response == null || !"success".equals(response.result()) ||
                    response.conversionRates() == null) {
                log.error("Invalid response from ExchangeRate-API");
                return;
            }

            Instant now = Instant.now();

            // Upsert all rates (base = USD)
            for (Map.Entry<String, Double> entry : response.conversionRates().entrySet()) {
                String target = entry.getKey();
                BigDecimal rate = BigDecimal.valueOf(entry.getValue());

                rateRepository.findByBaseCurrencyAndTargetCurrency("USD", target)
                        .ifPresentOrElse(
                                existing -> {
                                    existing.setRate(rate);
                                    existing.setFetchedAt(now);
                                    rateRepository.save(existing);
                                },
                                () -> rateRepository.save(ExchangeRate.builder()
                                        .baseCurrency("USD")
                                        .targetCurrency(target)
                                        .rate(rate)
                                        .fetchedAt(now)
                                        .build())
                        );
            }

            log.info("Fetched {} exchange rates (base USD)", response.conversionRates().size());
        } catch (Exception e) {
            log.error("Failed to fetch exchange rates: {}", e.getMessage(), e);
        }
    }

    // ── Get rates for a given base currency ───────────────────────────────────
    // All stored rates have USD as base. Cross-rate formula:
    //   base→target = (USD→target) / (USD→base)

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getRatesForBase(String baseCurrency) {
        String base = baseCurrency.toUpperCase();

        // Get all USD-based rates
        Map<String, BigDecimal> usdRates = new HashMap<>();
        rateRepository.findByBaseCurrency("USD")
                .forEach(r -> usdRates.put(r.getTargetCurrency(), r.getRate()));

        if (usdRates.isEmpty()) {
            log.warn("No exchange rates in database. Returning identity map.");
            return Map.of(base, BigDecimal.ONE);
        }

        BigDecimal usdToBase = usdRates.getOrDefault(base, BigDecimal.ONE);
        if (usdToBase.compareTo(BigDecimal.ZERO) == 0) usdToBase = BigDecimal.ONE;

        Map<String, BigDecimal> result = new HashMap<>();
        for (Map.Entry<String, BigDecimal> e : usdRates.entrySet()) {
            BigDecimal crossRate = e.getValue()
                    .divide(usdToBase, 6, RoundingMode.HALF_UP);
            result.put(e.getKey(), crossRate);
        }

        // Ensure the base currency maps to exactly 1
        result.put(base, BigDecimal.ONE);
        return result;
    }

    // ── Fetch on startup if table is empty ────────────────────────────────────

    public void fetchIfEmpty() {
        if (rateRepository.count() == 0) {
            log.info("Exchange rate table empty — fetching now.");
            fetchAndStore();
        }
    }

    // ── Response DTO (ExchangeRate-API v6) ───────────────────────────────────
    // The API returns snake_case keys; @JsonProperty maps them to the record fields.

    record ExchangeRateApiResponse(
            String result,
            @JsonProperty("base_code")         String baseCode,
            @JsonProperty("conversion_rates")  Map<String, Double> conversionRates
    ) {}
}
