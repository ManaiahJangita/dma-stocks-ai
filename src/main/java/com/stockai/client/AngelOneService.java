package com.stockai.client;

import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.models.HistoricalData;
import com.angelbroking.smartapi.models.Instrument;
import com.angelbroking.smartapi.models.User;
import com.stockai.apiconstants.ApiConstants;
import com.stockai.config.ApplicationConfig;
import com.stockai.data.feed.AngelOneRestService;
import com.stockai.data.feed.AngelOneTimeFrameInterval;
import com.stockai.dto.StockV3Dto;
import com.stockai.entity.InstrumentsEntity;
import com.stockai.enums.Exchange;
import com.stockai.repository.InstrumentsRepository;
import com.stockai.service.impl.HistoricalDataService;
import com.stockai.service.interf.DailyStockService;
import com.stockai.utils.DateUtil;
import com.stockai.utils.EncryptUtil;
import com.stockai.utils.MathUtil;
import com.stockai.utils.OtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.stockai.data.feed.util.ClientHelperMethods.getAllInstrumentsAndSaveHistoricalData;
import static com.stockai.data.feed.util.ClientHelperMethods.updateDailyStockData;

@Slf4j
@Service
public class AngelOneService implements AngelOneRestService {
    @Autowired
    private ApplicationConfig applicationConfig;


    @Autowired
    private InstrumentsRepository instrumentsRepository;

    @Autowired
    private HistoricalDataService historicalDataService;

    @Autowired
    private DailyStockService dailyStockService;

    private static SmartConnect smartConnect = null;
    private User user;
    private List<Instrument> instruments = Collections.emptyList();
    private static final Map<String, String> instrumentMap = new HashMap<>();

    private static final Map<AngelOneTimeFrameInterval, Integer> TIME_FRAME_MAP = new EnumMap<>(AngelOneTimeFrameInterval.class);

    static {
        TIME_FRAME_MAP.put(AngelOneTimeFrameInterval.ONE_DAY, 730);
    }

    public void login() {
        log.info("Generate new session through login AngelOne.");
        generateSession();
        logout();
    }

    public void logout() {
        log.info("Logout AngelOne user and kill session.");
        if (Objects.nonNull(smartConnect)) {
            smartConnect.logout();
        }

        user = null;
        instruments = Collections.emptyList();
    }

    private synchronized void getSmartConnect() {
        if (Objects.isNull(smartConnect)) {
            String apiKey = EncryptUtil.decryptThirdParty(applicationConfig.getProperty(ApiConstants.ANGEL_ONE_API_KEY));
            smartConnect = new SmartConnect(apiKey);
        }
    }

    public synchronized void generateSession() {
        String clientID = EncryptUtil.decryptThirdParty(applicationConfig.getProperty(ApiConstants.ANGEL_ONE_USER_ID_KEY));
        String clientPass = EncryptUtil.decryptThirdParty(applicationConfig.getProperty(ApiConstants.ANGEL_ONE_PIN_KEY));

        // 2FA code
        String secretKeyFor2FactAuth = EncryptUtil.decryptThirdParty(applicationConfig.getProperty(ApiConstants.ANGEL_ONE_2FACTOR_SECRET_API_KEY));
        String totp = OtpUtil.getTOTPCode(secretKeyFor2FactAuth);
        getSmartConnect();

        if (Objects.isNull(user)) {
            log.debug("Generating session from AngelOne");
            user = smartConnect.generateSession(clientID, clientPass, totp);
            boolean isSessionGenerated = Objects.nonNull(user);
            if (isSessionGenerated) {
                smartConnect.setUserId(user.getUserId());
                smartConnect.setAccessToken(user.getAccessToken());
                smartConnect.setRefreshToken(user.getRefreshToken());
                instruments = Collections.emptyList();
                log.debug("Session is generated from AngelOne: Session status {}", Objects.nonNull(user));
            } else {
                log.error("Session is not generated from AngelOne");
            }

            if (isSessionGenerated) {
                getAllInstruments();
            }

            // Set session expiry callback.
            smartConnect.setSessionExpiryHook(() -> {
                log.info("Angelone session is expired. Generate the new session from hook.");
                user = null;
                instruments = Collections.emptyList();
                generateSession();
            });
        }
    }

    public synchronized List<Instrument> getAllInstruments() {
        // Get all instruments list. This call is very expensive as it involves
        // downloading of large data dump.
        // Hence, it is recommended that this call be made once and the results stored
        // locally once every morning before market opening.
        // Get instruments for an exchange.
        if (CollectionUtils.isEmpty(instruments)) {
            try {
                instruments = smartConnect.getInstruments();
                log.debug("instruments size: {}", instruments.size());

                if (!CollectionUtils.isEmpty(instruments)) {
                    instrumentMap.clear();
                    //filter instruments by exchange
                    Map<String, String> newInstrumentMap = instruments.stream()
                            .filter(instrument -> instrument.getExchSeg().equalsIgnoreCase(Exchange.NSE.name()))
                            .collect(Collectors.toMap(
                                    instrument -> instrument.getExchSeg() + "-" + instrument.getSymbol(),
                                    Instrument::getToken
                            ));
                    if (Objects.nonNull(instruments)) {
                        List<Instrument> instrumentsNSE = instruments.stream().filter(instrument -> instrument.getExchSeg().equalsIgnoreCase(Exchange.NSE.name())).toList();
                        List<InstrumentsEntity> instrumentsEntityList = new ArrayList<>();
                        for (Instrument instrument : instrumentsNSE) {
                            InstrumentsEntity entity = new InstrumentsEntity();
                            BeanUtils.copyProperties(instrument, entity);
                            instrumentsEntityList.add(entity);
                        }
                        instrumentsRepository.truncateInstrumentsTable();
                        saveInstrument(instrumentsEntityList);
                    }
                    this.instrumentMap.putAll(newInstrumentMap);
                    //getAllInstrumentsAndSaveHistoricalData(instrumentsRepository,historicalDataService, this);
                    updateDailyStockData(instrumentsRepository, dailyStockService, this);
                }
            } catch (Exception e) {
                log.error("Error while fetching instruments from angelone", e);
            }
        } else {
            log.debug("Returns angelone instruments from cache");
        }

        return instruments;
    }

    public User getProfile() {
        return smartConnect.getProfile();
    }

    public void saveInstrument(List<InstrumentsEntity> instrument) {
        log.info("Saving instruments to database, size: {}", instrument.size());
        instrumentsRepository.saveAllInstrumetns(instrument);
        log.info("Saved instruments to database successfully, size: {}", instrument.size());
    }

    public HistoricalData getHistoricalData(Exchange exchange, String symbol, AngelOneTimeFrameInterval interval, LocalDateTime startDate,
                                            LocalDateTime endDate) {
        HistoricalData historicalData;
        try {
            String symbolToken = getToken(exchange.name(), symbol);
            if (!StringUtils.hasLength(symbolToken)) {
                return new HistoricalData();
            }

            JSONObject requestObejct = new JSONObject();
            requestObejct.put("exchange", exchange);
            requestObejct.put("symboltoken", symbolToken);
            requestObejct.put("interval", interval);

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date from = DateUtil.convertToDateViaInstant(startDate);
            Date to = DateUtil.convertToDateViaInstant(endDate);
            requestObejct.put("fromdate", format.format(from));
            requestObejct.put("todate", format.format(to));

            historicalData = smartConnect.getHistoricalData(requestObejct);
            log.info("Fetched historical data for symbol: {}, interval: {}, startDate: {}, endDate: {}, size: {}", symbol, interval, startDate, endDate, Objects.nonNull(historicalData) ? historicalData.dataArrayList.size() : 0);
        } catch (Exception e) {
            log.error("Error while getting candle history", e);
            historicalData = new HistoricalData();
        }

        return historicalData;
    }

    public StockV3Dto getLTP(Exchange exchange, String tradingSymbol) {
        String symbolToken = getToken(exchange.name(), tradingSymbol);
        StockV3Dto stockDto = null;

        try {
            JSONObject ltpData = smartConnect.getLTP(exchange.name(), tradingSymbol, symbolToken);
            //convert JSONObject to StockV2Dto
            if (Objects.nonNull(ltpData)) {
                stockDto = new StockV3Dto();
                stockDto.setSymbol(tradingSymbol);
                stockDto.setOpen(ltpData.getDouble("open"));
                stockDto.setHigh(ltpData.getDouble("high"));
                stockDto.setLow(ltpData.getDouble("low"));
                stockDto.setClose(ltpData.getDouble("close"));
                stockDto.setLtP(ltpData.getDouble("ltp"));
                log.debug("LTP data: {}", ltpData);
            }
        } catch (Exception e) {
            log.error("Error while fetching LTP from angelone", e);
        }

        return stockDto;
    }

    private String getToken(String exchange, String symbol) {
        if (CollectionUtils.isEmpty(instruments)) {
            getAllInstruments();
        }

        List<String> symbolSuffixes = Arrays.asList("EQ", "BE", "BZ", "MKT", "ST", "SM");
        for (String suffix : symbolSuffixes) {
            String key = exchange + "-" + symbol + "-" + suffix;
            if (instrumentMap.containsKey(key)) {
                return instrumentMap.get(key);
            }
        }

        return null;
    }

    @Override
    public List<StockV3Dto> getOHLCQuote(List<String> symbols) {
        List<StockV3Dto> stockList = new ArrayList<>();
        try {
            if (!CollectionUtils.isEmpty(symbols)) {
                for (String symbol : symbols) {
                    StockV3Dto dto = getOHLCQuote(symbol);
                    if (Objects.nonNull(dto)) {
                        stockList.add(dto);
                    }

                    // Pause the thread for 100ms to avoid the rate limit.
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        log.error("Thread interrupted while fetching OHLC quotes for symbols: {}", symbols, e);
                        Thread.currentThread().interrupt(); // Restore interrupted status
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while fetching OHLC quotes for symbols: {}", symbols, e);
        }
        return stockList;
    }

    @Override
    public StockV3Dto getOHLCQuote(String symbol) {
        return getLTP(Exchange.NSE, symbol);
    }

    @Override
    public List<StockV3Dto> getHistoricalData(String symbol, AngelOneTimeFrameInterval timeFrame) {
        //AngelOne API allows to fetch only some days based on time frame.
        Integer historyDays = TIME_FRAME_MAP.getOrDefault(timeFrame, 730);
        LocalDateTime startDate = LocalDateTime.now().minusDays(historyDays);
        LocalDateTime endDate = LocalDateTime.now();
        List<StockV3Dto> fullList = new ArrayList<>();
        List<StockV3Dto> historicalData;
        do {
            historicalData = getHistoricalData(symbol, timeFrame, startDate, endDate);
            fullList.addAll(historicalData);
            endDate = startDate;
            startDate = startDate.minusDays(historyDays);

            // Pause the thread for 350ms to avoid the rate limit.
            try {
                TimeUnit.MILLISECONDS.sleep(350);
            } catch (InterruptedException e) {
                log.error("Thread interrupted while fetching historical data for symbol: {}", symbol, e);
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        } while (!historicalData.isEmpty());
        return fullList;
    }

    @Override
    public List<StockV3Dto> getHistoricalData(String symbol, AngelOneTimeFrameInterval timeFrame,
                                              LocalDateTime startDate, LocalDateTime endDate) {
        List<StockV3Dto> stockList = new ArrayList<>();
        try {
            if (Objects.isNull(startDate)) {
                startDate = LocalDateTime.now();
            }

            if (Objects.isNull(endDate)) {
                endDate = LocalDateTime.now();
            }

            HistoricalData historicalData = getHistoricalData(Exchange.NSE, symbol, timeFrame, startDate, endDate);
            if (Objects.nonNull(historicalData) && !CollectionUtils.isEmpty(historicalData.dataArrayList)) {
                for (HistoricalData entry : historicalData.dataArrayList) {
                    StockV3Dto dto = getStockV3Dto(symbol, entry);
                    stockList.add(dto);
                }
            } else {
                log.warn("No historical data found for symbol: {}, timeFrame: {}, startDate: {}, endDate: {}", symbol, timeFrame, startDate, endDate);
            }
        } catch (Throwable e) {
            log.error("Error while loading historical data from angelone", e);
        }

        // Reverse the list to get the data in descending order.
        stockList.sort((o1, o2) -> o2.getCreatedDate().compareTo(o1.getCreatedDate()));
        log.info("Fetched data and started store process {} historical data entries for symbol: {}, timeFrame: {}, startDate: {}, endDate: {}", stockList.size(), symbol, timeFrame, startDate, endDate);
        log.info("Saved data in db {} historical data entries for symbol: {}, timeFrame: {}, startDate: {}, endDate: {}", stockList.size(), symbol, timeFrame, startDate, endDate);
        return stockList;
    }

    private static StockV3Dto getStockV3Dto(String symbol, HistoricalData entry) {
        StockV3Dto dto = new StockV3Dto();
        try {
            dto.setSymbol(symbol);
            dto.setOpen(entry.open);
            dto.setHigh(entry.high);
            dto.setLow(entry.low);
            dto.setClose(entry.close);
            dto.setVolume(entry.volume);

            dto.setCreatedDate(LocalDate.parse(entry.timeStamp, DateTimeFormatter.ISO_DATE_TIME));

            dto.setPtsC(MathUtil.roundToTwoDigits(entry.close - entry.open));
            double pChange = MathUtil.roundToTwoDigits((dto.getPtsC() / entry.open) * 100);
            dto.setPer(pChange);
        } catch (Exception e) {
            log.error("Error while loading historical data from angelone", e);
        }
        return dto;
    }


}
