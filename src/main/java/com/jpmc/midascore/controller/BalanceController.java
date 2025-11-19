package com.jpmc.midascore.controller;

import com.jpmc.midascore.foundation.Balance;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/balance")
public class BalanceController {

    private final UserRepository userRepository;

    public BalanceController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public Balance getBalance(@RequestParam Long userId) {
        UserRecord user = userRepository.findById(userId);
        if (user != null) {
            return new Balance(user.getBalance());
        }
        return new Balance(0.0f);
    }
}