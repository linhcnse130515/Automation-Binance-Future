package com.binance.future.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.binance.future.config.CommonConfig.*;

@Getter
@Setter
public class NotificationDTO {
    @JsonProperty("position")
    private String position; // buy or sell
    @JsonProperty("symbol")
    private String symbol; // name coin
    @JsonProperty("close")
    private BigDecimal close;
    @JsonProperty("comment")
    private String comment;
    private Double entry;
    private Double takeProfit;
    private Double stopLoss;

    public NotificationDTO(String position, String symbol, BigDecimal close, String comment) {
        this.position = position;
        this.symbol = symbol;
        this.close = close;
        this.comment = comment;
        this.entry = this.getValue(REGEX_PRICE_ENTRY);
        this.takeProfit = this.getValue(REGEX_PRICE_TP);
        this.stopLoss = this.getValue(REGEX_PRICE_STL);
    }

    public String getPosition() {
        return position.toUpperCase();
    }

    public String getOppositeSide() {
        if (position.equalsIgnoreCase("BUY")) {
            return "SELL";
        } else if (position.equalsIgnoreCase("SELL")) {
            return "BUY";
        }
        return null;
    }

    private Double getValue(String patternType) {
        Pattern pattern = Pattern.compile(patternType);
        Matcher matcher = pattern.matcher(comment);
        if (matcher.find()) {
            String rs = matcher.group(1);
            return Double.parseDouble(rs);
        }
        return 0.0;
    }
}
