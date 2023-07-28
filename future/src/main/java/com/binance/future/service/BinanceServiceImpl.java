package com.binance.future.service;

import com.binance.connector.futures.client.enums.HttpMethod;
import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.binance.future.config.PrivateConfig;
import com.binance.future.dto.NotificationDTO;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;

@Service
public class BinanceServiceImpl implements BinanceService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceServiceImpl.class);

    private static final double PERCENT = 0.01; // 1 % balance

    private static final int LEVERAGE = 20;

    private final UMFuturesClientImpl client;

    public BinanceServiceImpl() {
        this.client = new UMFuturesClientImpl(PrivateConfig.TESTNET_API_KEY, PrivateConfig.TESTNET_SECRET_KEY, PrivateConfig.TESTNET_BASE_URL);;
    }

    @Override
    public boolean buyCoinFromNotification(NotificationDTO notificationDTO) {
        //Get all position
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", notificationDTO.getSymbol());
        String allPosition = client.account().positionInformation(parameters);
        JSONArray jsonArray = new JSONArray(allPosition);
        if (!jsonArray.isNull(0)) {
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            BigDecimal balance = this.getBalance();
            BigDecimal currentPrice = new BigDecimal(jsonObject.getString("markPrice"));
            BigDecimal quantity = balance.multiply(BigDecimal.valueOf(PERCENT * LEVERAGE)).divide(currentPrice, 0, RoundingMode.HALF_UP);
            if (notificationDTO.getComment().contains("Exit")) {
                this.cancelOrder(notificationDTO.getSymbol());
            } else if (jsonObject.get("entryPrice").equals("0.0")) {
                this.newOrder(quantity, notificationDTO);
            } else {
                this.cancelOrder(notificationDTO.getSymbol());
                this.newOrder(quantity, notificationDTO);
            }
        }

        return false;
    }

    private BigDecimal getBalance() {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        try {
            String response = client.account().futuresAccountBalance(parameters);
            logger.info(response);
            if (Strings.isNotEmpty(response)) {
                JSONArray jsonArray = new JSONArray(response);
                if (!jsonArray.isNull(3)) {
                    JSONObject jsonObject = jsonArray.getJSONObject(3);
                    String amount = jsonObject.getString("availableBalance");
                    return new BigDecimal(amount);
                }
            }

        } catch (BinanceConnectorException e) {
            logger.error("fullErrMessage: {}", e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error("fullErrMessage: {} \nerrMessage: {} \nerrCode: {} \nHTTPStatusCode: {}",
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
        return BigDecimal.ZERO;
    }
    private void newOrder(BigDecimal quantity, NotificationDTO notificationDTO) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        parameters.put("symbol", notificationDTO.getSymbol());
        parameters.put("side", notificationDTO.getPosition().toUpperCase());
        parameters.put("type", "MARKET");
        //parameters.put("timeInForce", "GTC");
        parameters.put("quantity", quantity);
        //parameters.put("price", 0.8);

        try {
            String result = client.account().newOrder(parameters);
            logger.info(result);
        } catch (BinanceConnectorException e) {
            logger.error("fullErrMessage: {}", e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error("fullErrMessage: {} \nerrMessage: {} \nerrCode: {} \nHTTPStatusCode: {}",
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
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

    private Object getOrderId(String symbol) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", symbol);
        try {
            return client.account().currentAllOpenOrders(parameters);
        } catch (BinanceConnectorException e) {
            logger.error("fullErrMessage: {}", e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error("fullErrMessage: {} \nerrMessage: {} \nerrCode: {} \nHTTPStatusCode: {}",
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
        return null;
    }
}
