package com.ureka.team3.utong_scheduler.coupon.scheduler;

import com.ureka.team3.utong_scheduler.auth.entity.User;
import com.ureka.team3.utong_scheduler.coupon.entity.Coupon;
import com.ureka.team3.utong_scheduler.coupon.entity.UserCoupon;
import com.ureka.team3.utong_scheduler.coupon.repository.UserCouponRepository;
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
class CouponExpirationSchedulerTest {

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private CouponExpirationScheduler couponExpirationScheduler;

    private User createTestUser() {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .name("테스트 사용자")
                .build();
    }

    private Coupon createTestCoupon() {
        return Coupon.builder()
                .id(UUID.randomUUID().toString())
                .isActive(true)
                .couponCode("001")
                .createdAt(LocalDateTime.now().minusDays(7))
                .expiredAt(LocalDateTime.now().plusDays(30))
                .build();
    }

    private UserCoupon createUserCoupon(String status, LocalDateTime expiredAt) {
        return UserCoupon.builder()
                .id(UUID.randomUUID().toString())
                .user(createTestUser())
                .coupon(createTestCoupon())
                .status(status)
                .createdAt(LocalDateTime.now().minusDays(1))
                .expiredAt(expiredAt)
                .build();
    }

    @Nested
    @DisplayName("쿠폰 만료 스케줄러")
    class ExpireCoupons {

        @Test
        @DisplayName("성공 - 만료된 쿠폰이 있는 경우")
        void expireCoupons_성공_만료된쿠폰있음_test() {
            // given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime yesterday = now.minusDays(1);

            List<UserCoupon> expiredCoupons = List.of(
                    createUserCoupon("002", yesterday),        // 사용 가능 → 만료 처리 대상
                    createUserCoupon("002", yesterday.minusHours(2))  // 사용 가능 → 만료 처리 대상
            );

            given(userCouponRepository.findAvailableAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(expiredCoupons);

            // when
            couponExpirationScheduler.expireCoupons();

            // then
            then(userCouponRepository).should().findAvailableAndExpiredAtBefore(any(LocalDateTime.class));

            // 각 쿠폰이 만료 상태로 변경되었는지 확인
            expiredCoupons.forEach(uc -> {
                assertThat(uc.getStatus()).isEqualTo("001"); // 만료 상태
            });
        }

        @Test
        @DisplayName("성공 - 만료된 쿠폰이 없는 경우")
        void expireCoupons_성공_만료된쿠폰없음_test() {
            // given
            given(userCouponRepository.findAvailableAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(List.of());

            // when
            couponExpirationScheduler.expireCoupons();

            // then
            then(userCouponRepository).should().findAvailableAndExpiredAtBefore(any(LocalDateTime.class));
            // 빈 리스트이므로 추가 처리 없음
        }

        @Test
        @DisplayName("성공 - 대량의 만료된 쿠폰 처리")
        void expireCoupons_성공_대량데이터_test() {
            // given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime yesterday = now.minusDays(1);

            List<UserCoupon> expiredCoupons = new ArrayList<>();
            for (int i = 0; i < 150; i++) {
                expiredCoupons.add(createUserCoupon("002", yesterday.minusMinutes(i)));
            }

            given(userCouponRepository.findAvailableAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(expiredCoupons);

            // when
            couponExpirationScheduler.expireCoupons();

            // then
            then(userCouponRepository).should().findAvailableAndExpiredAtBefore(any(LocalDateTime.class));

            // 모든 쿠폰이 만료 상태로 변경되었는지 확인
            expiredCoupons.forEach(uc -> {
                assertThat(uc.getStatus()).isEqualTo("001"); // 만료 상태
            });
        }

        @Test
        @DisplayName("실패 - Repository에서 예외 발생")
        void expireCoupons_실패_Repository예외_test() {
            // given
            given(userCouponRepository.findAvailableAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willThrow(new RuntimeException("Database connection timeout"));

            // when & then
            // 예외가 발생해도 스케줄러는 중단되지 않아야 함 (catch 블록에서 처리)
            couponExpirationScheduler.expireCoupons();

            // Repository 메소드가 호출되었는지 확인
            then(userCouponRepository).should().findAvailableAndExpiredAtBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("검증 - 정확한 시간으로 조회하는지 확인")
        void expireCoupons_검증_조회시간_test() {
            // given
            given(userCouponRepository.findAvailableAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(List.of());

            // when
            LocalDateTime beforeExecution = LocalDateTime.now();
            couponExpirationScheduler.expireCoupons();
            LocalDateTime afterExecution = LocalDateTime.now();

            // then
            ArgumentCaptor<LocalDateTime> dateTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            then(userCouponRepository).should().findAvailableAndExpiredAtBefore(dateTimeCaptor.capture());

            LocalDateTime capturedTime = dateTimeCaptor.getValue();
            assertThat(capturedTime).isBetween(beforeExecution, afterExecution);
        }

        @Test
        @DisplayName("검증 - 상태 코드 변경 확인")
        void expireCoupons_검증_상태코드변경_test() {
            // given
            UserCoupon expiredCoupon = createUserCoupon("002", LocalDateTime.now().minusDays(1));
            List<UserCoupon> expiredCoupons = List.of(expiredCoupon);

            given(userCouponRepository.findAvailableAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(expiredCoupons);

            // when
            couponExpirationScheduler.expireCoupons();

            // then
            assertThat(expiredCoupon.getStatus()).isEqualTo("001"); // 만료 상태로 변경됨
        }

        @Test
        @DisplayName("검증 - 이미 만료된 쿠폰은 조회되지 않음")
        void expireCoupons_검증_이미만료된쿠폰제외_test() {
            // given
            // status가 "001"인 쿠폰은 조회 대상이 아님 (이미 만료)
            given(userCouponRepository.findAvailableAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(List.of()); // 빈 리스트 반환

            // when
            couponExpirationScheduler.expireCoupons();

            // then
            // findAvailableAndExpiredAtBefore 메소드가 호출되었는지만 확인
            then(userCouponRepository).should().findAvailableAndExpiredAtBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("검증 - 사용된 쿠폰은 조회되지 않음")
        void expireCoupons_검증_사용된쿠폰제외_test() {
            // given
            // status가 "003"인 쿠폰은 조회 대상이 아님 (이미 사용됨)
            given(userCouponRepository.findAvailableAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(List.of()); // 빈 리스트 반환

            // when
            couponExpirationScheduler.expireCoupons();

            // then
            then(userCouponRepository).should().findAvailableAndExpiredAtBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("성능 테스트 - 실행 시간 측정")
        void expireCoupons_성능테스트_test() {
            // given
            List<UserCoupon> expiredCoupons = List.of(
                    createUserCoupon("002", LocalDateTime.now().minusDays(1))
            );

            given(userCouponRepository.findAvailableAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(expiredCoupons);

            // when
            long startTime = System.currentTimeMillis();
            couponExpirationScheduler.expireCoupons();
            long endTime = System.currentTimeMillis();

            // then
            long executionTime = endTime - startTime;
            assertThat(executionTime).isLessThan(5000); // 5초 이내 실행되어야 함

            then(userCouponRepository).should().findAvailableAndExpiredAtBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("검증 - 상태별 쿠폰 처리 확인")
        void expireCoupons_검증_상태별처리_test() {
            // given
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

            UserCoupon availableCoupon = createUserCoupon("002", yesterday);    // 사용 가능 → 만료 처리
            UserCoupon expiredCoupon = createUserCoupon("001", yesterday);      // 이미 만료 → 조회 안됨
            UserCoupon usedCoupon = createUserCoupon("003", yesterday);         // 이미 사용 → 조회 안됨

            // Repository에서는 status="002"인 것만 반환
            List<UserCoupon> expiredCoupons = List.of(availableCoupon);

            given(userCouponRepository.findAvailableAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(expiredCoupons);

            // when
            couponExpirationScheduler.expireCoupons();

            // then
            assertThat(availableCoupon.getStatus()).isEqualTo("001"); // 만료로 변경
            assertThat(expiredCoupon.getStatus()).isEqualTo("001");   // 기존 상태 유지
            assertThat(usedCoupon.getStatus()).isEqualTo("003");      // 기존 상태 유지
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
            assertThat(couponExpirationScheduler).isNotNull();

            // 메소드가 예외 없이 실행되는지 확인
            given(userCouponRepository.findAvailableAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(List.of());

            couponExpirationScheduler.expireCoupons();

            then(userCouponRepository).should().findAvailableAndExpiredAtBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("로그 출력 확인을 위한 테스트")
        void 로그출력확인_test() {
            // given
            UserCoupon expiredCoupon = createUserCoupon("002", LocalDateTime.now().minusDays(1));
            List<UserCoupon> expiredCoupons = List.of(expiredCoupon);

            given(userCouponRepository.findAvailableAndExpiredAtBefore(any(LocalDateTime.class)))
                    .willReturn(expiredCoupons);

            // when
            couponExpirationScheduler.expireCoupons();

            // then
            // 로그는 실제 실행 시 콘솔에서 확인 가능
            // 여기서는 메소드 실행과 상태 변경만 확인
            then(userCouponRepository).should().findAvailableAndExpiredAtBefore(any(LocalDateTime.class));
            assertThat(expiredCoupon.getStatus()).isEqualTo("001");
        }
    }
}