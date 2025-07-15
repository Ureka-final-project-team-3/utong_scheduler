package com.ureka.team3.utong_scheduler.coupon.scheduler;

import com.ureka.team3.utong_scheduler.coupon.entity.UserCoupon;
import com.ureka.team3.utong_scheduler.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponExpirationScheduler {

    private final UserCouponRepository userCouponRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireCoupons() {
        long startTime = System.currentTimeMillis();
        try {
            log.info("쿠폰 만료 스케줄러가 실행되었습니다.");
            LocalDateTime now = LocalDateTime.now();

            List<UserCoupon> expiredCoupons = userCouponRepository
                    .findAvailableAndExpiredAtBefore(now);

            if (!expiredCoupons.isEmpty()) {
                expiredCoupons.forEach(uc -> {
                    uc.setStatus("001"); // 만료 상태로 변경
                });

                long duration = System.currentTimeMillis() - startTime;
                log.info("쿠폰 만료 처리 완료. 만료된 쿠폰 수: {}, 실행 시간: {}ms", expiredCoupons.size(), duration);
            } else {
                log.info("만료된 쿠폰이 없습니다.");
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("쿠폰 만료 스케줄러 실행 중 오류가 발생했습니다. 오류: {}, 실행 시간: {}ms", e.getMessage(), duration);
        }
    }
}

