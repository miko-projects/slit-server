package slit.slitserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import slit.slitserver.entity.ExchangeRate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    List<ExchangeRate> findByBaseCurrency(String baseCurrency);

    Optional<ExchangeRate> findByBaseCurrencyAndTargetCurrency(String base, String target);

    void deleteByBaseCurrency(String baseCurrency);
}
