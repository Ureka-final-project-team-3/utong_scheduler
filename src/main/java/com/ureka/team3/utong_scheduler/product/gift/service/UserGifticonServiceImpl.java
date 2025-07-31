package com.ureka.team3.utong_scheduler.product.gift.service;

import com.ureka.team3.utong_scheduler.product.global.dto.ExpirationResult;
import com.ureka.team3.utong_scheduler.product.gift.repository.UserGifticonRepository;
import com.ureka.team3.utong_scheduler.product.global.config.ProductBatchPolicy;
import com.ureka.team3.utong_scheduler.product.global.enums.ProductType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserGifticonServiceImpl implements UserGifticonService {

    private final UserGifticonRepository userGifticonRepository;

    // 기프티콘 만료 처리 (벌크)
    @Override
    @Transactional
    public ExpirationResult expireGifticonsAsync() {
        long startTime = System.currentTimeMillis();

        int totalProcessed = 0;
        int processedCount;

        do {
            processedCount = userGifticonRepository.bulkUpdateExpiredGifticons(LocalDateTime.now(), ProductBatchPolicy.BATCH_SIZE);

            totalProcessed += processedCount;

            if(processedCount > 0) {
                try {
                    Thread.sleep(50);
                } catch(Exception e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } while (processedCount > 0);

        long endTime = System.currentTimeMillis();

        log.info("기프티콘 만료 처리 작업 완료, 처리량 : {}, 처리 시간 : {}", totalProcessed, endTime - startTime);

        return ExpirationResult.builder()
                .processedCount(totalProcessed)
                .duration(endTime - startTime)
                .type(ProductType.GIFTICON)
                .build();
    }


}
