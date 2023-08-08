package com.binance.future.config;

public final class CommonConfig {
    public static final String LIMIT = "LIMIT";

    public static final String MARKET = "MARKET";
    public static final String TAKE_PROFIT_MARKET = "TAKE_PROFIT_MARKET";
    public static final String STOP_LOSS_MARKET = "STOP_MARKET";
    public static final String EXIT_ALERT = "Exit";
    public static final String SPLIT_REGEX = " - ";
    public static final String ENTRY_DETECT = "Entry: ";
    public static final String TP_DETECT = "TP: ";
    public static final String STL_DETECT = "STL: ";
    public static final String FUTURE_WALLET = "USDT";
    public static final String SYMBOL_PARAM = "symbol";
    public static final String SIDE_PARAM = "side";
    public static final String CONNECT_ERR_MESS = "fullErrMessage: {}";
    public static final String CLIENT_ERR_MESS = "fullErrMessage: {} \nerrMessage: {} \nerrCode: {} \nHTTPStatusCode: {}";
    public static final String REGEX_PRICE_ENTRY = ".+Entry:\\s([\\d\\.]+).+";
    public static final String REGEX_PRICE_TP = ".+TP:\\s([\\d\\.]+).+";
    public static final String REGEX_PRICE_STL = ".+STL:\\s([\\d\\.]+).+";
    public static final String PRICE_FILTER = "PRICE_FILTER";
}
