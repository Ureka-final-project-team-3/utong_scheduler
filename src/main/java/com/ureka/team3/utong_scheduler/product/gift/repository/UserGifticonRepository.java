package com.ureka.team3.utong_scheduler.product.gift.repository;

import com.ureka.team3.utong_scheduler.product.gift.entity.UserGifticon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


// 마이 기프티콘 목록
public interface UserGifticonRepository extends JpaRepository<UserGifticon, String> {

    // 배치 크기만큼 만료된 기프티콘 비활성화
    @Modifying
    @Query(value = """
        UPDATE user_gifticon
        SET is_active = false
        WHERE id IN (
                SELECT temp.id FROM (
                    SELECT id
                    FROM user_gifticon
                    WHERE is_active = true
                    AND expired_at < :now
                    ORDER BY expired_at ASC
                    LIMIT :batchSize   
            ) AS temp
        )
    """, nativeQuery = true)
    int bulkUpdateExpiredGifticons(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );

}


