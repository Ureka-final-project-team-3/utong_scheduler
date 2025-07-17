package com.ureka.team3.utong_scheduler.contract.repository;

import com.ureka.team3.utong_scheduler.contract.entity.ContractHourlyAvgPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ContractHourlyAvgPriceRepository extends JpaRepository<ContractHourlyAvgPrice, String> {

    @Modifying
    @Query(value = """
        INSERT INTO contract_hourly_avg_price (id, aggregated_at, avg_price)
        SELECT 
            UUID() AS id,
            :previousHour AS aggregated_at,
            COALESCE(SUM(c.price * c.amount) / NULLIF(SUM(c.amount), 0), 0) AS avg_price
        FROM contract c
        WHERE c.created_at >= :previousHour
        AND c.created_at < :currentHour
        HAVING COUNT(*) > 0
        """, nativeQuery = true)
    void insertHourlyAvgPrice(
            @Param("previousHour") LocalDateTime previousHour,
            @Param("currentHour") LocalDateTime currentHour
    );
}
