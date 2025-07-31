package com.ureka.team3.utong_scheduler.trade.notification.service;

import com.ureka.team3.utong_scheduler.trade.alert.ContractDto;
import com.ureka.team3.utong_scheduler.trade.notification.enums.ContractType;

public interface TradeNotificationService {


	boolean sendContractCompleteMessage(String to, String nickname, ContractType contractType, ContractDto contractDto);

}
