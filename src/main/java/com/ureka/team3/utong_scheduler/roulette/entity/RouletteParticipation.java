package com.ureka.team3.utong_scheduler.roulette.entity;

import com.ureka.team3.utong_scheduler.auth.entity.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "roulette_participation",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_event_account",
           columnNames = {"event_id", "account_id"}
       ))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouletteParticipation {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private RouletteEvent event;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @Column(name = "is_winner", nullable = false)
    private Boolean isWinner;
    
    @Column(name = "participated_at", nullable = false)
    @Builder.Default
    private LocalDateTime participatedAt = LocalDateTime.now();
}