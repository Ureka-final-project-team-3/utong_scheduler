package com.ureka.team3.utong_scheduler.auth.service;

import com.ureka.team3.utong_scheduler.auth.AccountRepository;
import com.ureka.team3.utong_scheduler.auth.entity.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService{
    private final AccountRepository accountRepository;

    @Override
    public Account findById(String userId) throws IllegalAccessException {
        return accountRepository.findById(userId).orElseThrow(IllegalAccessException::new);
    }
}
