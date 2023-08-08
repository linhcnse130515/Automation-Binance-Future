package com.binance.future.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PositionDTO {
    private String symbol;
    private BigDecimal positionAmt;
    private BigDecimal entryPrice;
    private BigDecimal markPrice;
    private BigDecimal unRealizedProfit;
    private BigDecimal liquidationPrice;
    private int leverage;
    private int maxNotionalValue;
    private String marginType;
    private BigDecimal isolatedMargin;
    private String isAutoAddMargin;
    private String positionSide;
    private BigDecimal notional;
    private BigDecimal isolatedWallet;
    private BigDecimal updateTime;

    public PositionDTO() {
    }

    public PositionDTO(String symbol, BigDecimal positionAmt, BigDecimal entryPrice, BigDecimal markPrice, BigDecimal unRealizedProfit, BigDecimal liquidationPrice, int leverage, int maxNotionalValue, String marginType, BigDecimal isolatedMargin, String isAutoAddMargin, String positionSide, BigDecimal notional, BigDecimal isolatedWallet, BigDecimal updateTime) {
        this.symbol = symbol;
        this.positionAmt = positionAmt;
        this.entryPrice = entryPrice;
        this.markPrice = markPrice;
        this.unRealizedProfit = unRealizedProfit;
        this.liquidationPrice = liquidationPrice;
        this.leverage = leverage;
        this.maxNotionalValue = maxNotionalValue;
        this.marginType = marginType;
        this.isolatedMargin = isolatedMargin;
        this.isAutoAddMargin = isAutoAddMargin;
        this.positionSide = positionSide;
        this.notional = notional;
        this.isolatedWallet = isolatedWallet;
        this.updateTime = updateTime;
    }
}
