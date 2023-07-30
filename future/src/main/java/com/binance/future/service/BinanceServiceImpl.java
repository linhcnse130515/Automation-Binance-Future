package com.binance.future.service;

import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.binance.future.config.PrivateConfig;
import com.binance.future.dto.NotificationDTO;
import com.binance.future.dto.PositionDTO;
import com.binance.future.dto.PriceDTO;
import com.binance.future.dto.WalletDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Service
public class BinanceServiceImpl implements BinanceService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceServiceImpl.class);

    private static final double PERCENT = 0.01; // 1 % balance

    private static final int LEVERAGE = 20;

    private static final String MARKET = "MARKET";

    private static final String TAKE_PROFIT_MARKET = "TAKE_PROFIT_MARKET";

    private static final String STOP_MARKET = "STOP_MARKET";

    private final UMFuturesClientImpl client;

    public BinanceServiceImpl() {
        this.client = new UMFuturesClientImpl(PrivateConfig.TESTNET_API_KEY, PrivateConfig.TESTNET_SECRET_KEY, PrivateConfig.TESTNET_BASE_URL);;
    }

    @Override
    public boolean buyCoinFromNotification(NotificationDTO notificationDTO) throws JsonProcessingException {
        //Get all position
        PositionDTO positionDTO = this.getPosition(notificationDTO.getSymbol());
        if (Objects.nonNull(positionDTO)) {
            BigDecimal balance = this.getBalance();
            BigDecimal currentPrice = positionDTO.getMarkPrice();
            if (currentPrice.equals(BigDecimal.ZERO)) {
                currentPrice = this.getCurrentPrice(notificationDTO.getSymbol());
            }
            if (currentPrice != null && balance != null) {
                BigDecimal quantity = balance.multiply(BigDecimal.valueOf(PERCENT)).divide(currentPrice, 0, RoundingMode.HALF_UP);
                BigDecimal curQuantity = positionDTO.getPositionAmt();
                BigDecimal topProfitPrice = notificationDTO.getTopProfit();
                if (notificationDTO.getComment().contains("Exit")) {
                    this.exitPosition(curQuantity, notificationDTO, notificationDTO.getPosition());
                    this.cancelOrder(notificationDTO.getSymbol());
                } else if (positionDTO.getPositionAmt().compareTo(BigDecimal.ZERO) == 0) {
                    this.newOrder(quantity, notificationDTO, MARKET, null, currentPrice);
                    this.newOrder(quantity, notificationDTO, TAKE_PROFIT_MARKET, null, topProfitPrice);
                } else {
                    this.exitPosition(curQuantity, notificationDTO, notificationDTO.getPosition());
                    this.cancelOrder(notificationDTO.getSymbol());
                    this.newOrder(quantity, notificationDTO, MARKET, null, currentPrice);
                    this.newOrder(quantity, notificationDTO, TAKE_PROFIT_MARKET, null, topProfitPrice);
                }
            }
        }

        return false;
    }

    private BigDecimal getCurrentPrice(String symbol) throws JsonProcessingException {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", symbol);

        try {
            String price = client.market().markPrice(parameters);
            logger.info(price);
            ObjectMapper objectMapper = new ObjectMapper();
            PriceDTO priceDTO = objectMapper.readValue(price, PriceDTO.class);
            return priceDTO.getMarkPrice();
        } catch (BinanceConnectorException e) {
            logger.error("fullErrMessage: {}", e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error("fullErrMessage: {} \nerrMessage: {} \nerrCode: {} \nHTTPStatusCode: {}",
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
        return null;
    }

    private PositionDTO getPosition(String symbol) throws JsonProcessingException {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", symbol);
        try {
            String allPosition = client.account().positionInformation(parameters);
            ObjectMapper objectMapper = new ObjectMapper();
            List<PositionDTO> positionListDTO = objectMapper.readValue(allPosition, new TypeReference<>() {});
            logger.info(allPosition);
            if (!CollectionUtils.isEmpty(positionListDTO)) {
                return positionListDTO.get(0);
            }
        } catch (BinanceConnectorException e) {
            logger.error("fullErrMessage: {}", e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error("fullErrMessage: {} \nerrMessage: {} \nerrCode: {} \nHTTPStatusCode: {}",
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
        return null;
    }

    private void exitPosition(BigDecimal quantity, NotificationDTO notificationDTO, String side) throws JsonProcessingException {
        if (quantity == null) {
            PositionDTO positionDTO = this.getPosition(notificationDTO.getSymbol());
            assert positionDTO != null;
            quantity = positionDTO.getPositionAmt();
        }
        if (side != null) {
            this.newOrder(quantity.abs(), notificationDTO, MARKET, side, BigDecimal.ZERO);
        }
    }

    private BigDecimal getBalance() throws JsonProcessingException {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        try {
            String response = client.account().futuresAccountBalance(parameters);
            ObjectMapper objectMapper = new ObjectMapper();
            List<WalletDTO> walletDTOS = objectMapper.readValue(response, new TypeReference<>() {});
            logger.info(response);
            if (!CollectionUtils.isEmpty(walletDTOS)) {
                WalletDTO walletDTO = walletDTOS.stream()
                        .filter(wallet -> wallet.getAsset().equals("USDT"))
                        .findFirst()
                        .orElseThrow(null);
                if (!Objects.isNull(walletDTO)) {
                    return walletDTO.getAvailableBalance();
                }
            }

        } catch (BinanceConnectorException e) {
            logger.error("fullErrMessage: {}", e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error("fullErrMessage: {} \nerrMessage: {} \nerrCode: {} \nHTTPStatusCode: {}",
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
        return null;
    }
    private void newOrder(BigDecimal quantity, NotificationDTO notificationDTO, String type, String side, BigDecimal stopPrice) throws JsonProcessingException {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        parameters.put("symbol", notificationDTO.getSymbol());
        parameters.put("side", side != null ? side : notificationDTO.getPosition());
        parameters.put("type", type);
        if (type.equals(TAKE_PROFIT_MARKET)) {
            parameters.put("timestamp", System.currentTimeMillis());
            parameters.put("side", notificationDTO.getOppositeSide().toUpperCase());
            parameters.put("stopPrice", stopPrice);
        }
        parameters.put("quantity", quantity);


        try {
            String result = client.account().newOrder(parameters);
            logger.info(result);
        } catch (BinanceConnectorException e) {
            logger.error("fullErrMessage: {}", e.getMessage(), e);
            this.exitPosition(null, notificationDTO, notificationDTO.getOppositeSide());
        } catch (BinanceClientException e) {
            logger.error("fullErrMessage: {} \nerrMessage: {} \nerrCode: {} \nHTTPStatusCode: {}",
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
            this.exitPosition(null, notificationDTO, notificationDTO.getOppositeSide());
        }
    }

    private void cancelOrder(String symbol) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", symbol);
        //parameters.put("orderId", this.getOrderId(symbol));

        try {
            String result = client.account().cancelAllOpenOrders(parameters);
            logger.info(result);
        } catch (BinanceConnectorException e) {
            logger.error("fullErrMessage: {}", e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error("fullErrMessage: {} \nerrMessage: {} \nerrCode: {} \nHTTPStatusCode: {}",
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
    }
}
