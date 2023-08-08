package com.binance.future.service;

import com.binance.future.dto.NotificationDTO;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface BinanceService {
    boolean buyCoinFromNotification(NotificationDTO notificationDTO) throws IOException;
}
