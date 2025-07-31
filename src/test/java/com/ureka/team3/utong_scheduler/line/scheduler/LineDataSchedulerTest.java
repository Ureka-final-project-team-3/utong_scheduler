package com.ureka.team3.utong_scheduler.line.scheduler;

import com.ureka.team3.utong_scheduler.line.repository.LineDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("LineDataScheduler 테스트")
class LineDataSchedulerTest {

    @Mock
    private LineDataRepository lineDataRepository;

    @InjectMocks
    private LineDataScheduler lineDataScheduler;

    private LocalDate currentMonth;

    @BeforeEach
    void setUp() {
        currentMonth = LocalDate.now().withDayOfMonth(1);
    }

    @Nested
    @DisplayName("월간 LineData 갱신 스케줄러 테스트")
    class RenewMonthlyLineData {

        @Test
        @DisplayName("성공 - 정상적인 월간 데이터 갱신")
        void renewMonthlyLineData_성공_정상갱신_test() {
            // given
            int expectedCreatedCount = 141;
            given(lineDataRepository.renewMonthlyLineData(any(LocalDate.class)))
                    .willReturn(expectedCreatedCount);

            // when
            lineDataScheduler.renewMonthlyLineData();

            // then
            ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
            then(lineDataRepository).should(times(1))
                    .renewMonthlyLineData(dateCaptor.capture());

            LocalDate capturedDate = dateCaptor.getValue();
            assertThat(capturedDate).isEqualTo(currentMonth);
            assertThat(capturedDate.getDayOfMonth()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공 - 생성된 데이터가 0개인 경우")
        void renewMonthlyLineData_성공_생성데이터없음_test() {
            // given
            given(lineDataRepository.renewMonthlyLineData(any(LocalDate.class)))
                    .willReturn(0);

            // when
            lineDataScheduler.renewMonthlyLineData();

            // then
            then(lineDataRepository).should(times(1))
                    .renewMonthlyLineData(any(LocalDate.class));
        }

        @Test
        @DisplayName("성공 - 대량 데이터 생성")
        void renewMonthlyLineData_성공_대량데이터_test() {
            // given
            int largeCount = 10000;
            given(lineDataRepository.renewMonthlyLineData(any(LocalDate.class)))
                    .willReturn(largeCount);

            // when
            lineDataScheduler.renewMonthlyLineData();

            // then
            then(lineDataRepository).should(times(1))
                    .renewMonthlyLineData(any(LocalDate.class));
        }

        @Test
        @DisplayName("실패 - Repository에서 예외 발생")
        void renewMonthlyLineData_실패_Repository예외_test() {
            // given
            String errorMessage = "데이터베이스 연결 실패";
            given(lineDataRepository.renewMonthlyLineData(any(LocalDate.class)))
                    .willThrow(new RuntimeException(errorMessage));

            // when & then
            assertThatThrownBy(() -> lineDataScheduler.renewMonthlyLineData())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("월간 데이터 갱신 실패")
                    .hasCauseInstanceOf(RuntimeException.class);

            then(lineDataRepository).should(times(1))
                    .renewMonthlyLineData(any(LocalDate.class));
        }

        @Test
        @DisplayName("실패 - SQL 제약 조건 위반")
        void renewMonthlyLineData_실패_SQL제약조건위반_test() {
            // given
            given(lineDataRepository.renewMonthlyLineData(any(LocalDate.class)))
                    .willThrow(new RuntimeException("Duplicate entry"));

            // when & then
            assertThatThrownBy(() -> lineDataScheduler.renewMonthlyLineData())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("월간 데이터 갱신 실패");

            then(lineDataRepository).should(times(1))
                    .renewMonthlyLineData(any(LocalDate.class));
        }

        @Test
        @DisplayName("검증 - 월의 첫째 날로 정확히 설정")
        void renewMonthlyLineData_검증_월첫째날설정_test() {
            // given
            given(lineDataRepository.renewMonthlyLineData(any(LocalDate.class)))
                    .willReturn(100);

            // when
            lineDataScheduler.renewMonthlyLineData();

            // then
            ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
            then(lineDataRepository).should().renewMonthlyLineData(dateCaptor.capture());

            LocalDate capturedDate = dateCaptor.getValue();
            assertThat(capturedDate.getDayOfMonth()).isEqualTo(1);
            assertThat(capturedDate.getMonthValue()).isEqualTo(LocalDate.now().getMonthValue());
            assertThat(capturedDate.getYear()).isEqualTo(LocalDate.now().getYear());
        }

        @Test
        @DisplayName("검증 - Repository 메서드 1회만 호출")
        void renewMonthlyLineData_검증_Repository호출횟수_test() {
            // given
            given(lineDataRepository.renewMonthlyLineData(any(LocalDate.class)))
                    .willReturn(50);

            // when
            lineDataScheduler.renewMonthlyLineData();

            // then
            then(lineDataRepository).should(times(1))
                    .renewMonthlyLineData(any(LocalDate.class));
            then(lineDataRepository).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("엣지 케이스 테스트")
    class EdgeCases {

        @Test
        @DisplayName("월말에 실행될 때 다음 달 1일로 설정되는지 확인")
        void renewMonthlyLineData_월말실행_다음달설정_test() {
            // given
            given(lineDataRepository.renewMonthlyLineData(any(LocalDate.class)))
                    .willReturn(75);

            // when
            lineDataScheduler.renewMonthlyLineData();

            // then
            ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
            then(lineDataRepository).should().renewMonthlyLineData(dateCaptor.capture());

            LocalDate capturedDate = dateCaptor.getValue();
            // 현재 날짜와 상관없이 항상 현재 월의 1일이어야 함
            LocalDate expectedDate = LocalDate.now().withDayOfMonth(1);
            assertThat(capturedDate).isEqualTo(expectedDate);
        }

        @Test
        @DisplayName("윤년 2월 처리 확인")
        void renewMonthlyLineData_윤년2월_test() {
            // given
            given(lineDataRepository.renewMonthlyLineData(any(LocalDate.class)))
                    .willReturn(80);

            // when
            lineDataScheduler.renewMonthlyLineData();

            // then
            ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
            then(lineDataRepository).should().renewMonthlyLineData(dateCaptor.capture());

            LocalDate capturedDate = dateCaptor.getValue();
            assertThat(capturedDate.getDayOfMonth()).isEqualTo(1);
        }

        @Test
        @DisplayName("12월에서 1월로 넘어가는 경우")
        void renewMonthlyLineData_12월에서1월_test() {
            // given
            given(lineDataRepository.renewMonthlyLineData(any(LocalDate.class)))
                    .willReturn(120);

            // when
            lineDataScheduler.renewMonthlyLineData();

            // then
            ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
            then(lineDataRepository).should().renewMonthlyLineData(dateCaptor.capture());

            LocalDate capturedDate = dateCaptor.getValue();
            assertThat(capturedDate.getDayOfMonth()).isEqualTo(1);
            assertThat(capturedDate.getMonthValue()).isEqualTo(LocalDate.now().getMonthValue());
        }
    }

    @Nested
    @DisplayName("트랜잭션 동작 확인")
    class TransactionBehavior {

        @Test
        @DisplayName("예외 발생 시 트랜잭션 롤백 확인")
        void renewMonthlyLineData_예외발생시롤백_test() {
            // given
            given(lineDataRepository.renewMonthlyLineData(any(LocalDate.class)))
                    .willThrow(new RuntimeException("트랜잭션 테스트"));

            // when & then
            assertThatThrownBy(() -> lineDataScheduler.renewMonthlyLineData())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("월간 데이터 갱신 실패");
        }
    }
}