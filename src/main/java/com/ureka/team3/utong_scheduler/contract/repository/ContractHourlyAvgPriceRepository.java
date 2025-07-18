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
        INSERT INTO contract_hourly_avg_price (id, aggregated_at, avg_price, data_code)
        SELECT 
            UUID() AS id,
            :currentHour AS aggregated_at,
            COALESCE(SUM(c.price * c.amount) / NULLIF(SUM(c.amount), 0), 0) AS avg_price,
            :dataCode AS data_code
        FROM contract c
        INNER JOIN buy_data_request bdr ON c.buy_data_request_id = bdr.id
        WHERE c.created_at >= :previousHour
        AND c.created_at < :currentHour
        AND bdr.data_code = :dataCode
    """, nativeQuery = true)
    void insertHourlyAvgPrice(
            @Param("previousHour") LocalDateTime previousHour,
            @Param("currentHour") LocalDateTime currentHour,
            @Param("dataCode") String dataCode
    );

    // 최근 1시간 동안의 계약 건수를 세는 메서드
    @Query(value = """
        SELECT COUNT(*)
        FROM contract c
        INNER JOIN buy_data_request bdr ON c.buy_data_request_id = bdr.id
        WHERE c.created_at >= :previousHour
        AND c.created_at < :currentHour
        AND (:dataCode IS NULL OR bdr.data_code = :dataCode)
    """, nativeQuery = true)
    int countContractsByTimeRange(
            @Param("previousHour") LocalDateTime previousHour,
            @Param("currentHour") LocalDateTime currentHour,
            @Param("dataCode") String dataCode
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM ContractHourlyAvgPrice chap
        WHERE chap.aggregatedAt >= :previousHour
        AND chap.aggregatedAt <= :currentHour
        AND (:dataCode IS NULL OR chap.dataCode = :dataCode)
    """)
    int countContractHourlyAvgPriceByTimeRange(
            @Param("previousHour") LocalDateTime previousHour,
            @Param("currentHour") LocalDateTime currentHour,
            @Param("dataCode") String dataCode
    );

    // 가장 최근의 평균가를 조회하는 메서드
    @Query(value = """
        SELECT avg_price
        FROM contract_hourly_avg_price
        WHERE aggregated_at <= :beforeTime
        AND data_code = :dataCode
        ORDER BY aggregated_at DESC
        LIMIT 1
    """, nativeQuery = true)
    Long findLatestAvgPrice(
            @Param("beforeTime") LocalDateTime beforeTime,
            @Param("dataCode") String dataCode
    );

    // 특정 가격을 사용하여 집계하는 메서드
    @Modifying
    @Query(value = """
        INSERT INTO contract_hourly_avg_price (id, aggregated_at, avg_price, data_code)
        VALUES (UUID(), :aggregatedAt, :avgPrice, :dataCode)
    """, nativeQuery = true)
    void insertHourlyAvgPriceWithValue(
            @Param("aggregatedAt") LocalDateTime aggregatedAt,
            @Param("avgPrice") Long avgPrice,
            @Param("dataCode") String dataCode
    );
}
