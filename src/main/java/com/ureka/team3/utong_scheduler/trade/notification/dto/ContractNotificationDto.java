package com.ureka.team3.utong_scheduler.trade.notification.dto;

import java.time.LocalDateTime;
import com.ureka.team3.utong_scheduler.auth.entity.Account;
import com.ureka.team3.utong_scheduler.trade.alert.ContractDto;
import com.ureka.team3.utong_scheduler.trade.global.entity.Contract;
import com.ureka.team3.utong_scheduler.trade.notification.enums.ContractType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractNotificationDto {
    
    private String accountId;
    private String email;
    private String nickname;
    private Boolean isMailEnabled;
    
    private String purchaseOrderId;
    private String saleOrderId;
    private String dataCode;
    private Long quantity;
    private Long price;
    private Long totalAmount;
    private LocalDateTime contractedAt;
    private ContractType contractType; 
    
    private String buyerNickname;
    private String sellerNickname;
    
    public static ContractNotificationDto from(ContractDto contractDto) {
        return ContractNotificationDto.builder()
                .purchaseOrderId(contractDto.getPurchaseOrderId())
                .saleOrderId(contractDto.getSaleOrderId())
                .dataCode(contractDto.getDataCode())
                .quantity(contractDto.getQuantity())
                .price(contractDto.getPrice())
                .totalAmount(contractDto.getPrice() * contractDto.getQuantity())
                .contractedAt(contractDto.getContractedAt())
                .build();
    }
    

    public static ContractNotificationDto from(ContractDto contractDto, Account buyer, Account seller) {
        return ContractNotificationDto.builder()
                .purchaseOrderId(contractDto.getPurchaseOrderId())
                .saleOrderId(contractDto.getSaleOrderId())
                .dataCode(contractDto.getDataCode())
                .quantity(contractDto.getQuantity())
                .price(contractDto.getPrice())
                .totalAmount(contractDto.getPrice() * contractDto.getQuantity())
                .contractedAt(contractDto.getContractedAt())
                .buyerNickname(buyer != null ? buyer.getNickname() : "구매자")
                .sellerNickname(seller != null ? seller.getNickname() : "판매자")
                .build();
    }
    
  
    public static ContractNotificationDto forBuyer(ContractDto contractDto, Account buyer) {
        if (buyer == null) return null;
        
        return ContractNotificationDto.builder()
                .accountId(buyer.getId())
                .email(buyer.getEmail())
                .nickname(buyer.getNickname())
                .isMailEnabled(buyer.getIsMail())
                .purchaseOrderId(contractDto.getPurchaseOrderId())
                .saleOrderId(contractDto.getSaleOrderId())
                .dataCode(contractDto.getDataCode())
                .quantity(contractDto.getQuantity())
                .price(contractDto.getPrice())
                .totalAmount(contractDto.getPrice() * contractDto.getQuantity())
                .contractedAt(contractDto.getContractedAt())
                .contractType(ContractType.BUY)
                .buyerNickname(buyer.getNickname())
                .build();
    }
    
    public static ContractNotificationDto forSeller(ContractDto contractDto, Account seller) {
        if (seller == null) return null;
        
        return ContractNotificationDto.builder()
                .accountId(seller.getId())
                .email(seller.getEmail())
                .nickname(seller.getNickname())
                .isMailEnabled(seller.getIsMail())
                .purchaseOrderId(contractDto.getPurchaseOrderId())
                .saleOrderId(contractDto.getSaleOrderId())
                .dataCode(contractDto.getDataCode())
                .quantity(contractDto.getQuantity())
                .price(contractDto.getPrice())
                .totalAmount(contractDto.getPrice() * contractDto.getQuantity())
                .contractedAt(contractDto.getContractedAt())
                .contractType(ContractType.SALE)
                .sellerNickname(seller.getNickname())
                .build();
    }
    
  
    public static ContractNotificationDto from(Contract contract, String dataCode, Account account, ContractType contractType) {
        if (account == null || contract == null) return null;
        
        return ContractNotificationDto.builder()
                .accountId(account.getId())
                .email(account.getEmail())
                .nickname(account.getNickname())
                .isMailEnabled(account.getIsMail())
                .dataCode(dataCode)
                .quantity(contract.getAmount())
                .price(contract.getPrice())
                .totalAmount(contract.getPrice() * contract.getAmount())
                .contractedAt(contract.getCreatedAt())
                .contractType(contractType)
                .build();
    }
    

    public Contract toContract() {
        return Contract.builder()
                .price(price)
                .amount(quantity)
                .createdAt(contractedAt)
                .build();
    }
    
   
    public boolean canSendMail() {
        return isMailEnabled != null && isMailEnabled && email != null && !email.trim().isEmpty();
    }
}