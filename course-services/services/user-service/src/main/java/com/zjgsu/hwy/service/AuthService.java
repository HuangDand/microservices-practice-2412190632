package com.zjgsu.hwy.service;

import com.zjgsu.hwy.model.Account;
import com.zjgsu.hwy.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Account> authenticate(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null) {
            return Optional.empty();
        }
        return accountRepository.findByUsername(username)
                .filter(account -> passwordEncoder.matches(rawPassword, account.getPasswordHash()));
    }
}
