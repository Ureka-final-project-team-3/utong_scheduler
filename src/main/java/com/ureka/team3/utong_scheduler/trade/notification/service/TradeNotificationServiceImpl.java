package com.ureka.team3.utong_scheduler.trade.notification.service;

import com.ureka.team3.utong_scheduler.trade.notification.enums.ContractType;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeNotificationServiceImpl implements TradeNotificationService{

    private final JavaMailSender mailSender;

    @Override
    public boolean sendContractCompleteMessage(String to, String nickname, ContractType contractType) {
        try {
            MimeMessage message =  mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("[유통] 데이터 구매 완료 알림");

            String htmlContent = createTestEmailTemplate(nickname, contractType);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML 이메일 발송 성공 : {}", to);

            return true;
        } catch (Exception e) {
            log.error("HTML 이메일 발송 실패 : {}, 에러: {}", to, e.getMessage(), e);
            return false;
        }
    }

    private String createTestEmailTemplate(String nickname, ContractType contractType) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    .container { max-width: 600px; margin: 0 auto; font-family: Arial, sans-serif; }
                    .header { background: #2196F3; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f9f9f9; }
                    .info-box { background: white; padding: 15px; margin: 10px 0; border-radius: 5px; border: 1px solid #ddd; }
                    .highlight { color: #2196F3; font-weight: bold; }
                    .footer { background: #333; color: white; padding: 10px; text-align: center; font-size: 12px; }
                    .success { background: #4CAF50; color: white; padding: 10px; border-radius: 5px; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📧 %s 완료 알림</h1>
                    </div>
                    <div class="content">
                        <div class="success">
                            <strong>✅ 고객님의 %s 주문이 정상적으로 체결되었습니다</strong>
                        </div>

                        <p>안녕하세요, <strong>%s</strong>님!</p>
                        <p>너로 통하다, 유통입니다</p>

                        <div class="info-box">
                            <h3>📋 테스트 정보</h3>
                            <p><strong>발송 시간:</strong> <span class="highlight">%s</span></p>
                            <p><strong>메일 시스템:</strong> <span class="highlight">Spring Boot Mail</span></p>
                            <p><strong>템플릿:</strong> <span class="highlight">HTML 템플릿</span></p>
                        </div>

                        <div class="info-box">
                            <h3>🚀 다음 단계</h3>
                            <p>✓ SMTP 설정 완료</p>
                            <p>✓ HTML 템플릿 렌더링 성공</p>
                            <p>✓ 메일 발송 기능 정상 작동</p>
                        </div>

                        <p>메일 발송 기능이 정상적으로 작동하고 있습니다. 🎉</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 유통(UTONG) - %s 완료 알림</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                contractType.getDescription(),
                contractType.getDescription(),
                nickname != null ? nickname : "테스터",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm:ss")),
                contractType.getDescription()
        );
    }
}
