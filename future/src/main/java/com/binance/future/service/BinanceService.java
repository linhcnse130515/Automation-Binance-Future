package com.binance.future.service;

import com.binance.future.dto.NotificationDTO;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface BinanceService {
    boolean buyCoinFromNotification(NotificationDTO notificationDTO) throws JsonProcessingException;
}
