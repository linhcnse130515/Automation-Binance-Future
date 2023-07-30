package com.binance.future.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PriceDTO {
    private String symbol;
    private BigDecimal markPrice;
    private BigDecimal indexPrice;
    private BigDecimal estimatedSettlePrice;
    private BigDecimal lastFundingRate;
    private BigDecimal interestRate;
    private BigDecimal nextFundingTime;
    private BigDecimal time;

    public PriceDTO() {
    }

    public PriceDTO(String symbol, BigDecimal markPrice, BigDecimal indexPrice, BigDecimal estimatedSettlePrice, BigDecimal lastFundingRate, BigDecimal interestRate, BigDecimal nextFundingTime, BigDecimal time) {
        this.symbol = symbol;
        this.markPrice = markPrice;
        this.indexPrice = indexPrice;
        this.estimatedSettlePrice = estimatedSettlePrice;
        this.lastFundingRate = lastFundingRate;
        this.interestRate = interestRate;
        this.nextFundingTime = nextFundingTime;
        this.time = time;
    }
}
