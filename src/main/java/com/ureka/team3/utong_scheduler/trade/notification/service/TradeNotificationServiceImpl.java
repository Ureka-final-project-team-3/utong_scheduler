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
public class TradeNotificationServiceImpl implements TradeNotificationService {

    private final JavaMailSender mailSender;

    @Override
    public boolean sendContractCompleteMessage(String to, String nickname, ContractType contractType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(String.format("[유통] 데이터 %s 체결 완료 알림", contractType.getDescription()));

            String htmlContent = createContractEmailTemplate(nickname, contractType);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("거래 체결 메일 발송 성공 - 수신자: {}, 타입: {}", to, contractType);

            return true;
        } catch (Exception e) {
            log.error("거래 체결 메일 발송 실패 - 수신자: {}, 타입: {}, 오류: {}", to, contractType, e.getMessage(), e);
            return false;
        }
    }

    private String createContractEmailTemplate(String nickname, ContractType contractType) {
        String actionText = contractType == ContractType.BUY ? "구매" : "판매";
        String successIcon = contractType == ContractType.BUY ? "📦" : "💰";
        String bgColor = contractType == ContractType.BUY ? "#4CAF50" : "#2196F3";
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    .container { 
                        max-width: 600px; 
                        margin: 0 auto; 
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background-color: #f8f9fa;
                    }
                    .header { 
                        background: linear-gradient(135deg, %s, %s); 
                        color: white; 
                        padding: 30px 20px; 
                        text-align: center;
                        border-radius: 8px 8px 0 0;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 24px;
                        font-weight: 600;
                    }
                    .content { 
                        padding: 30px 20px; 
                        background: white;
                        border-radius: 0 0 8px 8px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    .success-banner { 
                        background: %s; 
                        color: white; 
                        padding: 20px; 
                        border-radius: 8px; 
                        text-align: center;
                        margin-bottom: 25px;
                        font-size: 18px;
                        font-weight: 500;
                    }
                    .info-section {
                        background: #f8f9fa;
                        padding: 20px;
                        border-radius: 8px;
                        margin: 20px 0;
                        border-left: 4px solid %s;
                    }
                    .info-section h3 {
                        margin: 0 0 15px 0;
                        color: #333;
                        font-size: 16px;
                    }
                    .info-item {
                        display: flex;
                        justify-content: space-between;
                        margin: 8px 0;
                        padding: 8px 0;
                        border-bottom: 1px solid #eee;
                    }
                    .info-item:last-child {
                        border-bottom: none;
                    }
                    .info-label {
                        font-weight: 600;
                        color: #555;
                    }
                    .info-value {
                        color: %s;
                        font-weight: 500;
                    }
                    .next-steps {
                        background: #e3f2fd;
                        padding: 20px;
                        border-radius: 8px;
                        margin: 20px 0;
                    }
                    .next-steps h3 {
                        margin: 0 0 15px 0;
                        color: #1976d2;
                    }
                    .step-item {
                        display: flex;
                        align-items: center;
                        margin: 10px 0;
                    }
                    .step-icon {
                        background: #1976d2;
                        color: white;
                        width: 24px;
                        height: 24px;
                        border-radius: 50%%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin-right: 12px;
                        font-size: 12px;
                        font-weight: bold;
                    }
                    .footer { 
                        background: #333; 
                        color: #ccc; 
                        padding: 20px; 
                        text-align: center; 
                        font-size: 12px;
                        margin-top: 20px;
                    }
                    .footer strong {
                        color: white;
                    }
                    .greeting {
                        font-size: 16px;
                        line-height: 1.6;
                        margin-bottom: 25px;
                    }
                    .highlight {
                        color: %s;
                        font-weight: 600;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s %s 체결 완료!</h1>
                    </div>
                    
                    <div class="content">
                        <div class="success-banner">
                            <strong>%s 축하합니다! 거래가 성공적으로 체결되었습니다.</strong>
                        </div>

                        <div class="greeting">
                            안녕하세요, <strong>%s</strong>님!<br>
                            <span class="highlight">너로 통하다, 유통</span>에서 알려드립니다.
                        </div>

                        <div class="info-section">
                            <h3>📋 거래 정보</h3>
                            <div class="info-item">
                                <span class="info-label">거래 유형</span>
                                <span class="info-value">데이터 %s</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">체결 시간</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">처리 상태</span>
                                <span class="info-value">✅ 체결 완료</span>
                            </div>
                        </div>

                        <div class="next-steps">
                            <h3>🚀 다음 단계</h3>
                            <div class="step-item">
                                <div class="step-icon">1</div>
                                <span>거래 내역은 마이페이지에서 확인하실 수 있습니다</span>
                            </div>
                            <div class="step-item">
                                <div class="step-icon">2</div>
                                <span>%s 완료 후 관련 자료가 제공됩니다</span>
                            </div>
                            <div class="step-item">
                                <div class="step-icon">3</div>
                                <span>문의사항이 있으시면 고객센터로 연락해 주세요</span>
                            </div>
                        </div>

                        <p style="margin-top: 30px; font-size: 14px; color: #666; line-height: 1.6;">
                            항상 <strong>유통</strong>을 이용해 주셔서 감사합니다.<br>
                            더 나은 서비스로 보답하겠습니다.
                        </p>
                    </div>
                    
                    <div class="footer">
                        <p><strong>© 2025 유통(UTONG)</strong> - 데이터 거래 플랫폼</p>
                        <p>이 메일은 거래 체결 시 자동으로 발송되는 메일입니다.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                bgColor, adjustBrightness(bgColor, -20), 
                bgColor,
                bgColor,
                bgColor,
                bgColor,
                successIcon, actionText,
                successIcon,
                nickname != null ? nickname : "고객",
                actionText,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm")),
                actionText
            );
    }
    

    private String adjustBrightness(String hexColor, int adjustment) {
        if (hexColor.equals("#4CAF50")) {
            return "#45a049";
        } else if (hexColor.equals("#2196F3")) {
            return "#1976d2";
        }
        return hexColor;
    }
}