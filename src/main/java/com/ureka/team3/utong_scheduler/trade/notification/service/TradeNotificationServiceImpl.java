package com.ureka.team3.utong_scheduler.trade.notification.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ureka.team3.utong_scheduler.trade.alert.ContractDto;
import com.ureka.team3.utong_scheduler.trade.notification.dto.ContractNotificationDto;
import com.ureka.team3.utong_scheduler.trade.notification.enums.ContractType;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeNotificationServiceImpl implements TradeNotificationService {

    private final JavaMailSender mailSender;
    @Async
    @Override
    public boolean sendContractCompleteMessage(String to, String nickname, ContractType contractType, ContractDto contractDto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            ContractNotificationDto notificationDto = ContractNotificationDto.from(contractDto);
            helper.setTo(to);
            helper.setSubject(String.format("[유통] 데이터 %s 체결 완료 알림", contractType.getDescription()));

            String htmlContent = createContractEmailTemplate(nickname, contractType, notificationDto);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("거래 체결 메일 발송 성공 - 수신자: {}, 타입: {}", to, contractType);

            return true;
        } catch (Exception e) {
            log.error("거래 체결 메일 발송 실패 - 수신자: {}, 타입: {}, 오류: {}", to, contractType, e.getMessage(), e);
            return false;
        }
    }
    @Async
    private String createContractEmailTemplate(String nickname, ContractType contractType, ContractNotificationDto dto) {
        if(ContractType.BUY.equals(contractType))
        {
        	return String.format("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>데이터 구매 계약 체결 알림</title>
                        <style>
                            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                            .header { background-color: #007bff; color: white; padding: 20px; text-align: center; }
                            .content { padding: 20px; background-color: #f8f9fa; }
                            .info-box { background-color: white; padding: 15px; margin: 10px 0; border-radius: 5px; }
                            .footer { text-align: center; padding: 20px; color: #666; }
                            .button { background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 10px 0; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h1>데이터 구매 계약 체결 완료</h1>
                            </div>
                            <div class="content">
                                <p>안녕하세요, %s님!</p>
                                <p>요청하신 데이터 구매 계약이 성공적으로 체결되었습니다.</p>
                                
                                <div class="info-box">
                                    <h3>계약 정보</h3>
                                    <ul>
                                        <li><strong>구매 주문 ID:</strong> %s</li>
                                        <li><strong>판매 주문 ID:</strong> %s</li>
                                        <li><strong>데이터 코드:</strong> %s</li>
                                        <li><strong>구매 수량:</strong> %,dGB</li>
                                        <li><strong>단가:</strong> %,dp</li>
                                        <li><strong>총 결제 금액:</strong> %,dp</li>
                                        <li><strong>계약 일시:</strong> %s</li>
                                    </ul>
                                </div>
                                
                                <p>구매하신 데이터는 즉시 사용 가능합니다.</p>
                            </div>
                            <div class="footer">
                                <p>문의사항이 있으시면 언제든 연락해 주세요.</p>
                                <p>우통(UTONG) 팀 드림</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """,
                    dto.getSellerNickname(),
                    dto.getPurchaseOrderId(),
                    dto.getSaleOrderId(),
                    convertDataCode(dto.getDataCode()),
                    dto.getQuantity(),
                    dto.getPrice(),
                    dto.getTotalAmount(),
                    dto.getContractedAt().toString()
                );
        }
        else
        {
        	return String.format("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>데이터 판매 계약 체결 알림</title>
                        <style>
                            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                            .header { background-color: #28a745; color: white; padding: 20px; text-align: center; }
                            .content { padding: 20px; background-color: #f8f9fa; }
                            .info-box { background-color: white; padding: 15px; margin: 10px 0; border-radius: 5px; }
                            .footer { text-align: center; padding: 20px; color: #666; }
                            .button { background-color: #28a745; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 10px 0; }
                            .highlight { color: #28a745; font-weight: bold; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h1>데이터 판매 계약 체결 완료</h1>
                            </div>
                            <div class="content">
                                <p>안녕하세요, %s님!</p>
                                <p>판매 요청하신 데이터가 성공적으로 판매되었습니다.</p>
                                
                                <div class="info-box">
                                    <h3>계약 정보</h3>
                                    <ul>
                                        <li><strong>판매 주문 ID:</strong> %s</li>
                                        <li><strong>구매 주문 ID:</strong> %s</li>
                                        <li><strong>데이터 코드:</strong> %s</li>
                                        <li><strong>판매 수량:</strong> %,dGB</li>
                                        <li><strong>단가:</strong> %,dp</li>
                                        <li><strong>총 판매 금액:</strong> <span class="highlight">%,dp</span></li>
                                        <li><strong>계약 일시:</strong> %s</li>
                                    </ul>
                                </div>
                                
                                <p>판매 수익이 즉시 계정에 적립되었습니다.</p>
                            </div>
                            <div class="footer">
                                <p>지속적인 데이터 판매를 통해 더 많은 수익을 얻어보세요!</p>
                                <p>유통(UTONG) 팀 드림</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """,
                    dto.getSellerNickname(),
                    dto.getSaleOrderId(),
                    dto.getPurchaseOrderId(),
                    convertDataCode(dto.getDataCode()),
                    dto.getQuantity(),
                    dto.getPrice(),
                    dto.getTotalAmount(),
                    dto.getContractedAt().toString()
                );
        }
        
    }
    
    public String convertDataCode(String s)
    {
    	if(s.equals("001")) return "LTE";
    	else return "5G";
    }

    
}