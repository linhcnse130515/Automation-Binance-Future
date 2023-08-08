package com.binance.future.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WalletDTO {
    private String accountAlias;
    private String asset;
    private BigDecimal balance;
    private BigDecimal crossWalletBalance;
    private BigDecimal crossUnPnl;
    private BigDecimal availableBalance;
    private BigDecimal maxWithdrawAmount;
    private boolean marginAvailable;
    private BigDecimal updateTime;

    public WalletDTO() {
    }

    public WalletDTO(String accountAlias, String asset, BigDecimal balance, BigDecimal crossWalletBalance, BigDecimal crossUnPnl, BigDecimal availableBalance, BigDecimal maxWithdrawAmount, boolean marginAvailable, BigDecimal updateTime) {
        this.accountAlias = accountAlias;
        this.asset = asset;
        this.balance = balance;
        this.crossWalletBalance = crossWalletBalance;
        this.crossUnPnl = crossUnPnl;
        this.availableBalance = availableBalance;
        this.maxWithdrawAmount = maxWithdrawAmount;
        this.marginAvailable = marginAvailable;
        this.updateTime = updateTime;
    }
}
