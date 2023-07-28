package com.binance.future.service;

import com.binance.future.dto.NotificationDTO;

public interface BinanceService {
    boolean buyCoinFromNotification(NotificationDTO notificationDTO);
}
