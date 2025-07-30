package com.ureka.team3.utong_scheduler.product.scheduler;

import com.ureka.team3.utong_scheduler.product.coupon.service.UserCouponService;
import com.ureka.team3.utong_scheduler.product.dto.ExpirationResult;
import com.ureka.team3.utong_scheduler.product.gift.service.UserGifticonService;
import com.ureka.team3.utong_scheduler.product.global.enums.ProductType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductExpirationScheduler {

    private final UserCouponService userCouponService;
    private final UserGifticonService userGifticonService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Scheduled(cron = "0 * * * * *")
    public void expireProducts() {
        long startTime = System.currentTimeMillis();

        log.info("상품 만료 처리 스케줄러 실행");

        try {
            CompletableFuture<ExpirationResult> couponFuture = expireCouponsAsync();
            CompletableFuture<ExpirationResult> gifticonFuture = expireGifticonsAsync();

            CompletableFuture.allOf(couponFuture, gifticonFuture).join();

            long duration = System.currentTimeMillis() - startTime;

            logSchedulerResults(couponFuture.get(), gifticonFuture.get(), duration);
        } catch (Exception e) {
            log.error("상품 만료 처리 스케줄러 실행 중 오류 발생 - 오류 : {}", e.getMessage(), e);
        }
    }

    private CompletableFuture<ExpirationResult> expireCouponsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("쿠폰 만료 처리 시작");
                return userCouponService.expireCouponsAsync();
            } catch (Exception e) {
                log.error("쿠폰 만료 처리 중 오류 발생 : {}", e.getMessage(), e);
                return ExpirationResult.builder()
                        .processedCount(0)
                        .duration(0)
                        .type(ProductType.COUPON)
                        .errorMessage(e.getMessage())
                        .build();
            }
        }, executorService);
    }



    private CompletableFuture<ExpirationResult> expireGifticonsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("기프티콘 만료 처리 시작");
                return userGifticonService.expireGifticonsAsync();
            } catch (Exception e) {
                log.error("기프티콘 만료 처리 중 오류 발생 : {}", e.getMessage(), e);
                return ExpirationResult.builder()
                        .processedCount(0)
                        .duration(0)
                        .type(ProductType.GIFTICON)
                        .errorMessage(e.getMessage())
                        .build();
            }
        }, executorService);
    }

    private void logSchedulerResults(ExpirationResult couponResult, ExpirationResult gifticonResult, long totalDuration) {
        log.info("통합 상품 만료 처리 스케줄러 완료");

        // 쿠폰 결과 로깅
        if (couponResult.getErrorMessage() != null) {
            log.error("쿠폰: 처리 실패 - 오류: {}", couponResult.getErrorMessage());
        } else {
            log.info("쿠폰: {}개 처리 ({}ms)", couponResult.getProcessedCount(), couponResult.getDuration());
        }

        // 기프티콘 결과 로깅
        if (gifticonResult.getErrorMessage() != null) {
            log.error("기프티콘: 처리 실패 - 오류: {}", gifticonResult.getErrorMessage());
        } else {
            log.info("기프티콘: {}개 처리 ({}ms)", gifticonResult.getProcessedCount(), gifticonResult.getDuration());
        }

        // 전체 통계
        int totalProcessed = couponResult.getProcessedCount() + gifticonResult.getProcessedCount();
        log.info("총 소요시간: {}ms", totalDuration);
        log.info("총 처리량: {}개", totalProcessed);
    }

}
