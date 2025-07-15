package com.ureka.team3.utong_scheduler.gift.repository;

import com.ureka.team3.utong_scheduler.gift.entity.UserGifticon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


// 마이 기프티콘 목록
public interface MyGifticonRepository extends JpaRepository<UserGifticon, String> {
    // User 엔티티 안의 id를 기준으로 조회
    List<UserGifticon> findByUser_Id(String userId);

    // 기프티콘 상세
    Optional<UserGifticon> findByIdAndUser_Id(String id, String userId);

    // 활성화된 기프티콘 & 유효 기간이 지난 기프티콘 조회
    List<UserGifticon> findByIsActiveTrueAndExpiredAtBefore(LocalDateTime now);
}


