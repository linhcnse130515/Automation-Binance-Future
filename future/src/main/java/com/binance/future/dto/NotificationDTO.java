package com.binance.future.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class NotificationDTO {
    private String position; // buy or sell
    private String symbol; // name coin
    private String comment;

    public String getPosition() {
        return position.toUpperCase();
    }

    public String getSymbol() {
        return symbol.substring(0, symbol.length() - 2);
    }

    public BigDecimal getTopProfit() {
        return new BigDecimal(comment.substring(comment.indexOf("TP:") + 4));
    }

    public String getOppositeSide() {
        if (position.equalsIgnoreCase("BUY")) {
            return  "SELL";
        } else if (position.equalsIgnoreCase("SELL")){
            return  "BUY";
        }
        return null;
    }
}
