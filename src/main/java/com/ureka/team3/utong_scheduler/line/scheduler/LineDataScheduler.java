package com.ureka.team3.utong_scheduler.line.scheduler;

import com.ureka.team3.utong_scheduler.line.repository.LineDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class LineDataScheduler {

    private final LineDataRepository lineDataRepository;

    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void renewMonthlyLineData() {
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        try {
            int count = lineDataRepository.renewMonthlyLineData(currentMonth);

            log.info("월간 LineData 갱신 완료 - 대상 월 : {}, 생성 : {}개", currentMonth, count);
        } catch (Exception e) {
            log.error("월간 LineData 갱신 실패 - 대상 월 : {}, 오류 : {}", currentMonth, e.getMessage());
            throw new RuntimeException("월간 데이터 갱신 실패", e);
        }
    }

}
