package com.binance.future.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PriceDTO {

    private String symbol;
    private BigDecimal price;
    private BigDecimal time;

    public PriceDTO() {
    }

    public PriceDTO(String symbol, BigDecimal price, BigDecimal time) {
        this.symbol = symbol;
        this.price = price;
        this.time = time;
    }
}
