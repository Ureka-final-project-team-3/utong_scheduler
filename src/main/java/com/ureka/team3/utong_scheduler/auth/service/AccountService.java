package com.ureka.team3.utong_scheduler.auth.service;

import com.ureka.team3.utong_scheduler.auth.entity.Account;

public interface AccountService {
    Account findById(String userId) throws IllegalAccessException;
}
