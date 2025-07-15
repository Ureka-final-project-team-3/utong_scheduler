package com.ureka.team3.utong_scheduler.roulette.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "roulette_event")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouletteEvent {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "title", length = 100, nullable = false)
    private String title;
    
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;
    
    @Column(name = "max_winners", nullable = false)
    private Integer maxWinners;
    
    @Column(name = "current_winners", nullable = false)
    @Builder.Default
    private Integer currentWinners = 0;
    
    @Column(name = "win_probability", precision = 5, scale = 4, nullable = false)
    private BigDecimal winProbability;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public boolean isEventActive() {
        LocalDateTime now = LocalDateTime.now();
        return isActive && 
               now.isAfter(startDate) && 
               now.isBefore(endDate);
    }
    
    public boolean hasAvailableSlots() {
        return currentWinners < maxWinners;
    }
    
    public void incrementWinners() {
        this.currentWinners++;
    }
}