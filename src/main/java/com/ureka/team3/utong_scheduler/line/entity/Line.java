package com.ureka.team3.utong_scheduler.line.entity;

import com.ureka.team3.utong_scheduler.auth.entity.User;
import com.ureka.team3.utong_scheduler.plan.entity.Plan;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "line")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Line {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "phone_number", length = 20, nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "country_code")
    private Integer countryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", referencedColumnName = "id", nullable = false)
    private Plan plan;

}