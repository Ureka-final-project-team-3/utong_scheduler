package com.ureka.team3.utong_scheduler.gift.scheduler;

import com.ureka.team3.utong_scheduler.gift.entity.UserGifticon;
import com.ureka.team3.utong_scheduler.gift.repository.MyGifticonRepository;
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
public class GifticonExpirationScheduler {

    private final MyGifticonRepository myGifticonRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireGifticons() {
        long startTime = System.currentTimeMillis();
        try {
            log.info("기프티콘 만료 스케줄러가 실행되었습니다.");
            LocalDateTime now = LocalDateTime.now();

            List<UserGifticon> expiredGifticons = myGifticonRepository
                    .findByIsActiveTrueAndExpiredAtBefore(now);

            if (!expiredGifticons.isEmpty()) {
                expiredGifticons.forEach(ug -> {
                    ug.setIsActive(false);
                });

                long duration = System.currentTimeMillis() - startTime;
                log.info("기프티콘 만료 처리 완료. 만료된 기프티콘 수: {}, 실행 시간: {}ms", expiredGifticons.size(), duration);
            } else {
                log.info("만료된 기프티콘이 없습니다.");
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("기프티콘 만료 스케줄러 실행 중 오류가 발생했습니다. 오류: {}, 실행 시간: {}ms", e.getMessage(), duration);
        }
    }
}
