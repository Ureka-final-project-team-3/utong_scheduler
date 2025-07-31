package com.ureka.team3.utong_scheduler.line.repository;

import com.ureka.team3.utong_scheduler.line.entity.LineData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface LineDataRepository extends JpaRepository<LineData, String> {

    @Modifying
    @Query(value = """
        INSERT INTO line_data (id, line_id, data_code, remaining, purchased, sell, month)
        SELECT 
            UUID() as id,
            l.id as line_id,
            p.data_code as data_code,
            p.data as remaining,
            0 as purchased, 
            0 as sell,
            :newMonth as month
        FROM line l 
        INNER JOIN plan p ON l.plan_id = p.id
        WHERE p.data_code IS NOT NULL
        """, nativeQuery = true)
    int renewMonthlyLineData(@Param("newMonth") LocalDate newMonth);
}
