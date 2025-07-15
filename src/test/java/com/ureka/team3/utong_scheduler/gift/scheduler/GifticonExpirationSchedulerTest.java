package com.ureka.team3.utong_scheduler.gift.scheduler;

import com.ureka.team3.utong_scheduler.auth.entity.User;
import com.ureka.team3.utong_scheduler.gift.entity.Gifticon;
import com.ureka.team3.utong_scheduler.gift.entity.UserGifticon;
import com.ureka.team3.utong_scheduler.gift.repository.MyGifticonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class GifticonExpirationSchedulerTest {

    @Mock
    private MyGifticonRepository myGifticonRepository;

    @InjectMocks
    private GifticonExpirationScheduler gifticonExpirationScheduler;

    private User testUser;
    private Gifticon testGifticon;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .name("테스트 사용자")
                .build();

        testGifticon = Gifticon.builder()
                .id(UUID.randomUUID().toString())
                .name("테스트 기프티콘")
                .price(10000L)
                .build();
    }

    private UserGifticon createUserGifticon(boolean isActive, LocalDateTime expiredAt) {
        return UserGifticon.builder()
                .id(UUID.randomUUID().toString())
                .user(testUser)
                .gifticon(testGifticon)
                .isActive(isActive)
                .createdAt(LocalDateTime.now().minusDays(1))
                .expiredAt(expiredAt)
                .build();
    }

    @Nested
    @DisplayName("기프티콘 만료 스케줄러")
    class ExpireGifticons {

        @Test
        @DisplayName("성공 - 만료된 기프티콘이 있는 경우")
        void expireGifticons_성공_만료된기프티콘있음_test() {
            // given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime yesterday = now.minusDays(1);

            List<UserGifticon> expiredGifticons = List.of(
                    createUserGifticon(true, yesterday),
                    createUserGifticon(true, yesterday.minusHours(1))
            );

            given(myGifticonRepository.findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(expiredGifticons);

            // when
            gifticonExpirationScheduler.expireGifticons();

            // then
            then(myGifticonRepository).should().findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class));

            // 각 기프티콘이 비활성화되었는지 확인
            expiredGifticons.forEach(ug -> {
                assertThat(ug.getIsActive()).isFalse();
            });
        }

        @Test
        @DisplayName("성공 - 만료된 기프티콘이 없는 경우")
        void expireGifticons_성공_만료된기프티콘없음_test() {
            // given
            given(myGifticonRepository.findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(List.of());

            // when
            gifticonExpirationScheduler.expireGifticons();

            // then
            then(myGifticonRepository).should().findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class));
            // 빈 리스트이므로 추가 처리 없음
        }

        @Test
        @DisplayName("성공 - 대량의 만료된 기프티콘 처리")
        void expireGifticons_성공_대량데이터_test() {
            // given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime yesterday = now.minusDays(1);

            List<UserGifticon> expiredGifticons = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                expiredGifticons.add(createUserGifticon(true, yesterday.minusMinutes(i)));
            }

            given(myGifticonRepository.findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(expiredGifticons);

            // when
            gifticonExpirationScheduler.expireGifticons();

            // then
            then(myGifticonRepository).should().findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class));

            // 모든 기프티콘이 비활성화되었는지 확인
            expiredGifticons.forEach(ug -> {
                assertThat(ug.getIsActive()).isFalse();
            });
        }

        @Test
        @DisplayName("실패 - Repository에서 예외 발생")
        void expireGifticons_실패_Repository예외_test() {
            // given
            given(myGifticonRepository.findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willThrow(new RuntimeException("Database connection error"));

            // when & then
            // 예외가 발생해도 스케줄러는 중단되지 않아야 함 (catch 블록에서 처리)
            gifticonExpirationScheduler.expireGifticons();

            // Repository 메소드가 호출되었는지 확인
            then(myGifticonRepository).should().findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("검증 - 정확한 시간으로 조회하는지 확인")
        void expireGifticons_검증_조회시간_test() {
            // given
            given(myGifticonRepository.findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(List.of());

            // when
            LocalDateTime beforeExecution = LocalDateTime.now();
            gifticonExpirationScheduler.expireGifticons();
            LocalDateTime afterExecution = LocalDateTime.now();

            // then
            ArgumentCaptor<LocalDateTime> dateTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            then(myGifticonRepository).should().findByIsActiveTrueAndExpiredAtBefore(dateTimeCaptor.capture());

            LocalDateTime capturedTime = dateTimeCaptor.getValue();
            assertThat(capturedTime).isBetween(beforeExecution, afterExecution);
        }

        @Test
        @DisplayName("검증 - isActive 값 변경 확인")
        void expireGifticons_검증_isActive변경_test() {
            // given
            UserGifticon expiredGifticon = createUserGifticon(true, LocalDateTime.now().minusDays(1));
            List<UserGifticon> expiredGifticons = List.of(expiredGifticon);

            given(myGifticonRepository.findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(expiredGifticons);

            // when
            gifticonExpirationScheduler.expireGifticons();

            // then
            assertThat(expiredGifticon.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("검증 - 이미 만료된 기프티콘은 조회되지 않음")
        void expireGifticons_검증_이미만료된기프티콘제외_test() {
            // given
            // isActive가 false인 기프티콘은 조회 대상이 아님
            given(myGifticonRepository.findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(List.of()); // 빈 리스트 반환

            // when
            gifticonExpirationScheduler.expireGifticons();

            // then
            // findByIsActiveTrueAndExpiredAtBefore 메소드가 호출되었는지만 확인
            then(myGifticonRepository).should().findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("성능 테스트 - 실행 시간 측정")
        void expireGifticons_성능테스트_test() {
            // given
            List<UserGifticon> expiredGifticons = List.of(
                    createUserGifticon(true, LocalDateTime.now().minusDays(1))
            );

            given(myGifticonRepository.findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(expiredGifticons);

            // when
            long startTime = System.currentTimeMillis();
            gifticonExpirationScheduler.expireGifticons();
            long endTime = System.currentTimeMillis();

            // then
            long executionTime = endTime - startTime;
            assertThat(executionTime).isLessThan(5000); // 5초 이내 실행되어야 함

            then(myGifticonRepository).should().findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("스케줄러 통합 테스트")
    class SchedulerIntegrationTest {

        @Test
        @DisplayName("스케줄러 메소드 존재 확인")
        void 스케줄러메소드존재확인_test() {
            // given & when & then
            // 스케줄러 클래스가 올바르게 생성되었는지 확인
            assertThat(gifticonExpirationScheduler).isNotNull();

            // 메소드가 예외 없이 실행되는지 확인
            given(myGifticonRepository.findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(List.of());

            gifticonExpirationScheduler.expireGifticons();

            then(myGifticonRepository).should().findByIsActiveTrueAndExpiredAtBefore(any(LocalDateTime.class));
        }
    }
}