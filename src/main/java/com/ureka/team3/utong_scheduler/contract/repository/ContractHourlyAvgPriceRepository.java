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

    @Query("""
        INSERT INTO ContractHourlyAvgPrice (aggregatedAt, avgPrice)
        SELECT
            :previousHour AS aggregatedAt,
            SUM(c.price * c.amount) / SUM(c.amount) AS avgPrice
        FROM Contract c
        WHERE c.createdAt >= :previousHour
        AND c.createdAt < :currentHour
        """
    )
    @Modifying
    void insertHourlyAvgPrice(
            @Param("previousHour") LocalDateTime previousHour,
            @Param("currentHour") LocalDateTime currentHour
    );
}
