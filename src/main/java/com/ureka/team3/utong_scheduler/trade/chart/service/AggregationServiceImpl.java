package com.ureka.team3.utong_scheduler.trade.chart.service;

import com.ureka.team3.utong_scheduler.trade.chart.repository.ContractHourlyAvgPriceRepository;
import com.ureka.team3.utong_scheduler.price.repository.PriceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AggregationServiceImpl implements AggregationService{

    private final ContractHourlyAvgPriceRepository contractHourlyAvgPriceRepository;
    private final PriceRepository priceRepository;

    private static final String PRICE_ID = "903ee67c-71b3-432e-bbd1-aaf5d5043376";

    @Transactional
    @Override
    public void aggregateHourly(LocalDateTime currentHour, LocalDateTime previousHour, String dataCode) {
        int contractCount = contractHourlyAvgPriceRepository.countContractsByTimeRange(previousHour, currentHour, dataCode);

        if (contractCount > 0) {
            contractHourlyAvgPriceRepository.insertHourlyAvgPrice(previousHour, currentHour, dataCode);
        } else {
            Long previousPrice = contractHourlyAvgPriceRepository.findLatestAvgPrice(previousHour, dataCode);

            long price = previousPrice != null
                    ? previousPrice
                    : priceRepository.findById(PRICE_ID)
                        .orElseThrow(() -> new RuntimeException("기본 가격 정보가 없습니다."))
                        .getMinimumPrice();

            contractHourlyAvgPriceRepository.insertHourlyAvgPriceWithValue(currentHour, price, dataCode);
        }
    }

    @Override
    public int getDataCountInRange(LocalDateTime from, LocalDateTime to, String code) {
        return contractHourlyAvgPriceRepository.countContractHourlyAvgPriceByTimeRange(from, to, code);
    }
}
