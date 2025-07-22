package com.ureka.team3.utong_scheduler.trade.queue.dto;

import com.ureka.team3.utong_scheduler.trade.global.entity.Contract;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ContractDto {

    private Long price;

    private Long quantity;

    private String dataCode;

    private LocalDateTime contractedAt;

    public static ContractDto of(Contract contract, String dataCode) {
        ContractDto contractDto = new ContractDto();

        contractDto.price = contract.getPrice();
        contractDto.quantity = contract.getAmount();
        contractDto.dataCode = dataCode;  // 파라미터로 받은 값 사용
        contractDto.contractedAt = contract.getCreatedAt();

        return contractDto;
    }

}
