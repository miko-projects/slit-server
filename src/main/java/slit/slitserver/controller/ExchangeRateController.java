package slit.slitserver.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import slit.slitserver.service.ExchangeRateService;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    /**
     * Returns all exchange rates relative to the given base currency.
     * Example: GET /api/exchange-rates?base=PLN
     * Response: { "USD": 0.248, "EUR": 0.229, "PLN": 1.0, ... }
     */
    @GetMapping
    public Map<String, BigDecimal> getRates(
            @RequestParam(defaultValue = "USD") String base) {
        return exchangeRateService.getRatesForBase(base);
    }
}
