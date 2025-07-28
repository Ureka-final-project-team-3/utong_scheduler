package com.ureka.team3.utong_scheduler.coupon.repository;

import com.ureka.team3.utong_scheduler.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, String> {

    // 활성화된 쿠폰 & 유효 기간이 지난 쿠폰 조회
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.status = '002' AND uc.expiredAt < :now")
    List<UserCoupon> findAvailableAndExpiredAtBefore(@Param("now") LocalDateTime now);

}
