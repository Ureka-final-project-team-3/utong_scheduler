package com.ureka.team3.utong_scheduler.product.coupon.repository;

import com.ureka.team3.utong_scheduler.product.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, String> {

    // 배치 크기만큼 만료된 쿠폰 비활성화
    @Modifying
    @Query(value = """
    UPDATE user_coupon 
    SET status = '001'
    WHERE id IN (
        SELECT temp.id FROM (
            SELECT id 
            FROM user_coupon 
            WHERE status = '002' 
            AND expired_at < :now 
            ORDER BY expired_at ASC 
            LIMIT :batchSize
        ) AS temp
    )
    """, nativeQuery = true)
    int bulkUpdateExpiredCoupons(@Param("now") LocalDateTime now, @Param("batchSize") int batchSize);
}
