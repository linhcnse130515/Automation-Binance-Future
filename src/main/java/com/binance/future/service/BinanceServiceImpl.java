package com.binance.future.service;

import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.binance.future.config.PrivateConfig;
import com.binance.future.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.binance.future.config.CommonConfig.*;

@Service
public class BinanceServiceImpl implements BinanceService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceServiceImpl.class);

    private static final double PERCENT = 0.1;

    private static final int LEVERAGE = 20;

    private BigDecimal currentPrice = BigDecimal.ZERO;

    private int places = 0;

    private static ExchangeInfoDTO exchangeInfoDTO = null;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final UMFuturesClientImpl client;

    public BinanceServiceImpl() {
        this.client = new UMFuturesClientImpl(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY, PrivateConfig.UM_BASE_URL);
    }

    @Override
    public boolean buyCoinFromNotification(NotificationDTO notificationDTO) throws IOException {

        // create thread to get current price while get balance
        Thread thread = new Thread(() -> {
            try {
                currentPrice = this.getCurrentPrice(notificationDTO.getSymbol());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();

        if (exchangeInfoDTO == null) {
            exchangeInfoDTO = getExchangeInfo(client);
        }

        // Get balance
        BigDecimal balance = this.getBalance();
        places = getPlaces(notificationDTO.getSymbol());

        if (Objects.nonNull(balance) && balance.compareTo(BigDecimal.ZERO) > 0) {
            //Get all position
            PositionDTO positionDTO = Objects.requireNonNull(this.getPosition(notificationDTO.getSymbol())).get(0);
            BigDecimal curQuantity = BigDecimal.ZERO;
            if (Objects.nonNull(positionDTO)) {
                curQuantity = positionDTO.getPositionAmt();
                // create thread to set leverage = 20 if not have
                thread = new Thread(() -> {
                    if (positionDTO.getLeverage() != LEVERAGE) {
                        this.setUpLeverage(positionDTO.getSymbol());
                    }
                });
                thread.start();
            }
            
            if (!currentPrice.equals(BigDecimal.ZERO)) {
                BigDecimal quantity = balance.multiply(BigDecimal.valueOf(PERCENT * LEVERAGE)).divide(currentPrice, 0, RoundingMode.HALF_UP);
                Double topProfitPrice = notificationDTO.getTakeProfit();
                Double stopLossPrice = notificationDTO.getStopLoss();


                if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                    if (notificationDTO.getComment().contains(EXIT_ALERT)) {
                        this.closeAlert(curQuantity, notificationDTO);
                    } else if (curQuantity.equals(BigDecimal.ZERO)) {
                        this.setUpNewAlert(quantity, notificationDTO, topProfitPrice, stopLossPrice);
                    } else {
                        this.closeAlert(curQuantity, notificationDTO);
                        this.setUpNewAlert(quantity, notificationDTO, topProfitPrice, stopLossPrice);
                    }
                }
            }
        }

        return false;
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    private static ExchangeInfoDTO getExchangeInfo(UMFuturesClientImpl client) throws IOException {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.reader().readValue(client.market().exchangeInfo(), ExchangeInfoDTO.class);
    }

    private int getPlaces(String symbol) {
        SymbolDTO currentSymbol = exchangeInfoDTO.getSymbols()
                .stream().filter(sym -> sym.getSymbol().equals(symbol))
                .findFirst()
                .orElse(null);
        assert currentSymbol != null;
        FilterDTO filterCurrentSymbol = currentSymbol.getFilters()
                .stream()
                .filter(filter -> PRICE_FILTER.equals(filter.getFilterType())).findFirst()
                .orElse(null);
        assert filterCurrentSymbol != null;

        return stripDecimalZeros(filterCurrentSymbol.getTickSize()).scale();
    }

    public static BigDecimal stripDecimalZeros(BigDecimal value) {
        if (value == null)
            return null;

        // Strip only values with decimal digits
        BigDecimal striped = (value.scale() > 0) ? value.stripTrailingZeros() : value;
        // Unscale only values with ten exponent
        return (striped.scale() < 0) ? striped.setScale(0, RoundingMode.HALF_UP) : striped;
    }

    private void closeAlert(BigDecimal curQuantity, NotificationDTO notificationDTO) throws JsonProcessingException {
        this.exitPosition(curQuantity, notificationDTO, notificationDTO.getPosition());
        this.cancelOrder(notificationDTO.getSymbol());
    }

    private void setUpNewAlert(BigDecimal quantity, NotificationDTO notificationDTO, Double topProfitPrice, Double stopLossPrice) throws JsonProcessingException {
        boolean orderSuccess = this.newOrder(quantity, notificationDTO, LIMIT, null, null);
        if (orderSuccess) {
            orderSuccess = this.newOrder(quantity, notificationDTO, TAKE_PROFIT_MARKET, null, topProfitPrice);
            orderSuccess = orderSuccess && this.newOrder(quantity, notificationDTO, STOP_LOSS_MARKET, null, stopLossPrice);
        }
        if (!orderSuccess) {
            this.closeAlert(null, notificationDTO);
        }
    }

    private BigDecimal getCurrentPrice(String symbol) throws JsonProcessingException {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put(SYMBOL_PARAM, symbol);

        try {
            String price = client.market().tickerSymbol(parameters);
            logger.info(price);
            PriceDTO priceDTO = objectMapper.readValue(price, PriceDTO.class);
            return priceDTO.getPrice();
        } catch (BinanceConnectorException e) {
            logger.error(CONNECT_ERR_MESS, e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error(CLIENT_ERR_MESS,
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
        return BigDecimal.ZERO;
    }

    private void setUpLeverage(String symbol) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        parameters.put(SYMBOL_PARAM, symbol);
        parameters.put("leverage", LEVERAGE);

        try {
            String result = client.account().changeInitialLeverage(parameters);
            logger.info(result);
        } catch (BinanceConnectorException e) {
            logger.error(CONNECT_ERR_MESS, e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error(CLIENT_ERR_MESS,
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
    }

    private List<PositionDTO> getPosition(String symbol) throws JsonProcessingException {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        if (Objects.nonNull(symbol)) {
            parameters.put(SYMBOL_PARAM, symbol);
        }
        try {
            String allPosition = client.account().positionInformation(parameters);
            ObjectMapper objectMapper = new ObjectMapper();
            List<PositionDTO> positionListDTO = objectMapper.readValue(allPosition, new TypeReference<>() {
            });
            logger.info(allPosition);
            if (!CollectionUtils.isEmpty(positionListDTO)) {
                return positionListDTO;
            }
        } catch (BinanceConnectorException e) {
            logger.error(CONNECT_ERR_MESS, e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error(CLIENT_ERR_MESS,
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
        return null;
    }

    private void exitPosition(BigDecimal quantity, NotificationDTO notificationDTO, String side) throws JsonProcessingException {
        if (quantity == null) {
            PositionDTO positionDTO = Objects.requireNonNull(this.getPosition(notificationDTO.getSymbol())).get(0);
            assert positionDTO != null;
            quantity = positionDTO.getPositionAmt();
        }
        if (side != null && quantity.compareTo(BigDecimal.ZERO) != 0) {
            this.newOrder(quantity.abs(), notificationDTO, MARKET, side, 0.0);
        }
    }

    private BigDecimal getBalance() throws JsonProcessingException {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        try {
            String response = client.account().futuresAccountBalance(parameters);
            ObjectMapper objectMapper = new ObjectMapper();
            List<WalletDTO> walletDTOS = objectMapper.readValue(response, new TypeReference<>() {
            });
            logger.info(response);
            if (!CollectionUtils.isEmpty(walletDTOS)) {
                WalletDTO walletDTO = walletDTOS.stream()
                        .filter(wallet -> wallet.getAsset().equals(FUTURE_WALLET))
                        .findFirst()
                        .orElseThrow(null);
                if (!Objects.isNull(walletDTO)) {
                    return walletDTO.getAvailableBalance();
                }
            }

        } catch (BinanceConnectorException e) {
            logger.error(CONNECT_ERR_MESS, e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error(CLIENT_ERR_MESS,
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
        return null;
    }

    private boolean newOrder(BigDecimal quantity, NotificationDTO notificationDTO, String type, String side, Double stopPrice) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        parameters.put(SYMBOL_PARAM, notificationDTO.getSymbol());
        parameters.put(SIDE_PARAM, side != null ? side : notificationDTO.getPosition());
        parameters.put("type", type);
        parameters.put("timestamp", System.currentTimeMillis());
        switch (type) {
            case TAKE_PROFIT_MARKET, STOP_LOSS_MARKET -> {
                parameters.put(SIDE_PARAM, notificationDTO.getOppositeSide().toUpperCase());
                parameters.put("stopPrice", this.round(stopPrice, places));
                parameters.put("closePosition", true);
            }
            case LIMIT -> {
                parameters.put("timeInForce", "GTC");
                parameters.put("price", this.round(notificationDTO.getEntry(), places));
                parameters.put("quantity", quantity);
            }
            default -> parameters.put("quantity", quantity);
        }

        try {
            String result = client.account().newOrder(parameters);
            logger.info(result);
            return true;
        } catch (BinanceConnectorException e) {
            logger.error(CONNECT_ERR_MESS, e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error(CLIENT_ERR_MESS,
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
        return false;
    }

    private void cancelOrder(String symbol) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put(SYMBOL_PARAM, symbol);

        try {
            String result = client.account().cancelAllOpenOrders(parameters);
            logger.info(result);
        } catch (BinanceConnectorException e) {
            logger.error(CONNECT_ERR_MESS, e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error(CLIENT_ERR_MESS,
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
    }

    @Scheduled(fixedDelay = 900000)
    public void scheduleFixedDelayTask() throws JsonProcessingException {
        List<OrderDTO> allOrders = this.getAllOrders();
        if (!CollectionUtils.isEmpty(allOrders)) {
            Map<String, Integer> map = new LinkedHashMap<>();
            allOrders.forEach(orderDTO -> {
                int num;
                if (map.containsKey(orderDTO.getSymbol())) {
                    num = map.get(orderDTO.getSymbol());
                    num++;
                } else {
                    num = 1;
                }
                map.put(orderDTO.getSymbol(), num);
            });
            map.forEach((symbol, num) -> {
                if (num == 1) {
                    this.cancelOrder(symbol);
                }
            });
        }
    }

    private List<OrderDTO> getAllOrders() throws JsonProcessingException {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
//        parameters.put("status", "FILLED");
        parameters.put("timestamp", System.currentTimeMillis());
        try {
            String allOrders = client.account().currentAllOpenOrders(parameters);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            List<OrderDTO> orderDTOS = objectMapper.readValue(allOrders, new TypeReference<>() {
            });
            logger.info(allOrders);
            if (!CollectionUtils.isEmpty(orderDTOS)) {
                return orderDTOS;
            }
        } catch (BinanceConnectorException e) {
            logger.error(CONNECT_ERR_MESS, e.getMessage(), e);
        } catch (BinanceClientException e) {
            logger.error(CLIENT_ERR_MESS,
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
        return null;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        return scheduler;
    }
}
