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

    // 1시간 단위로 계약 평균가를 계산하여 저장하는 메서드
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

    // 최근 1시간 동안의 계약 건수를 세는 메서드
    @Query(value = """
        SELECT COUNT(*)
        FROM contract c
        WHERE c.created_at >= :previousHour
        AND c.created_at < :currentHour
        """, nativeQuery = true)
    int countContractsByTimeRange(
            @Param("previousHour") LocalDateTime previousHour,
            @Param("currentHour") LocalDateTime currentHour
    );

    // 가장 최근의 평균가를 조회하는 메서드
    @Query(value = """
        SELECT avg_price
        FROM contract_hourly_avg_price
        WHERE aggregated_at < :beforeTime
        ORDER BY aggregated_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Long findLatestAvgPrice(@Param("beforeTime") LocalDateTime beforeTime);

    // 특정 가격을 사용하여 집계하는 메서드
    @Modifying
    @Query(value = """
        INSERT INTO contract_hourly_avg_price (id, aggregated_at, avg_price)
        VALUES (UUID(), :aggregatedAt, :avgPrice)
    """, nativeQuery = true)
    void insertHourlyAvgPriceWithValue(
            @Param("aggregatedAt") LocalDateTime aggregatedAt,
            @Param("avgPrice") Long avgPrice
    );
}
