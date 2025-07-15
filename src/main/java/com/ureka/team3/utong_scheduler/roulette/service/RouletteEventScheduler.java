package com.ureka.team3.utong_scheduler.roulette.service;

import com.ureka.team3.utong_scheduler.roulette.entity.RouletteEvent;
import com.ureka.team3.utong_scheduler.roulette.repository.RouletteEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouletteEventScheduler {
    
    private final RouletteEventRepository rouletteEventRepository;
    
    
    @Scheduled(fixedRate = 60000) 
    @Transactional
    public void deactivateExpiredEvents() {
        LocalDateTime now = LocalDateTime.now();
        
        List<RouletteEvent> expiredEvents = rouletteEventRepository.findExpiredActiveEvents(now);
        
        if (!expiredEvents.isEmpty()) {
            log.info("만료된 활성 이벤트 {}개 발견", expiredEvents.size());
            
            for (RouletteEvent event : expiredEvents) {
                log.info("이벤트 비활성화: {} (ID: {}, 종료시간: {})", 
                        event.getTitle(), event.getId(), event.getEndDate());
                
                RouletteEvent updatedEvent = RouletteEvent.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .startDate(event.getStartDate())
                        .endDate(event.getEndDate())
                        .maxWinners(event.getMaxWinners())
                        .currentWinners(event.getCurrentWinners())
                        .winProbability(event.getWinProbability())
                        .isActive(false)
                        .createdAt(event.getCreatedAt())
                        .build();
                
                rouletteEventRepository.save(updatedEvent);
            }
            
            log.info("만료된 이벤트 {}개 비활성화 완료", expiredEvents.size());
        } else {
            log.debug("만료된 활성 이벤트 없음 (현재 시간: {})", now);
        }
    }
    
    
    @Scheduled(cron = "0 0 0 * * ?") 
    @Transactional
    public void dailyEventCleanup() {
        LocalDateTime now = LocalDateTime.now();
        
        log.info("일일 이벤트 정리 작업 시작 ({})", now);
        
        List<RouletteEvent> expiredEvents = rouletteEventRepository.findExpiredActiveEvents(now);
        
        if (!expiredEvents.isEmpty()) {
            for (RouletteEvent event : expiredEvents) {
                RouletteEvent updatedEvent = RouletteEvent.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .startDate(event.getStartDate())
                        .endDate(event.getEndDate())
                        .maxWinners(event.getMaxWinners())
                        .currentWinners(event.getCurrentWinners())
                        .winProbability(event.getWinProbability())
                        .isActive(false)
                        .createdAt(event.getCreatedAt())
                        .build();
                
                rouletteEventRepository.save(updatedEvent);
            }
            
            log.info("일일 정리: 만료된 이벤트 {}개 비활성화", expiredEvents.size());
        }
        
        List<RouletteEvent> activeEvents = rouletteEventRepository.findAllActiveEvents();
        log.info("일일 정리 완료: 현재 활성 이벤트 {}개", activeEvents.size());
        
        for (RouletteEvent event : activeEvents) {
            log.info("활성 이벤트: {} (ID: {}, 종료: {}, 당첨자: {}/{})", 
                    event.getTitle(), event.getId(), event.getEndDate(),
                    event.getCurrentWinners(), event.getMaxWinners());
        }
    }
}