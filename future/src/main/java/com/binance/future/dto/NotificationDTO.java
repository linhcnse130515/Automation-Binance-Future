package com.binance.future.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationDTO {
    private String position; // buy or sell
    private String symbol; // name coin
    private String comment;
}
