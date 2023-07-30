package com.binance.future.controller;

import com.binance.future.dto.NotificationDTO;
import com.binance.future.service.BinanceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("binance/feature")
public class BinanceController {
    private final BinanceService binanceService;

    public BinanceController(BinanceService binanceService) {
        this.binanceService = binanceService;
    }
    @PostMapping("/buy_coin")
    public ResponseEntity<Boolean> buyCoinFromNotification(@RequestBody NotificationDTO notificationDTO) throws JsonProcessingException {
        return ResponseEntity.status(HttpStatus.OK).body(binanceService.buyCoinFromNotification(notificationDTO));
    }
}
