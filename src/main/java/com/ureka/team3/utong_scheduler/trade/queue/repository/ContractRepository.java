package com.ureka.team3.utong_scheduler.trade.queue.repository;

import com.ureka.team3.utong_scheduler.trade.global.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, String> {

    @Query(value = """
       SELECT c.* FROM contract c
       INNER JOIN buy_data_request bdr ON c.buy_data_request_id = bdr.id
       WHERE bdr.data_code = :dataCode
       ORDER BY c.created_at DESC
       LIMIT :limit
    """, nativeQuery = true)
    List<Contract> findLatestContractByDataCode(@Param("dataCode") String dataCode, @Param("limit") int limit);
}
