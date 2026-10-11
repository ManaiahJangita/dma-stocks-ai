package com.stockai.apiconstants;

import java.util.List;

public class ApiConstants {
    public static final String ADMIN = "/admin/v1";
    public static final String USER = "/api/v1";

    public static final String AUTHENTICATION_COOKIE_NAME = "X-Client-Key";
    public static final String USER_ID_COOKIE_NAME = "X-Auth-User-Key";

    public static final String USER_INFO_DELIMITER = "#@#";

    public static final String DELIMITER = ",";

    public static String ANGEL_ONE_API_KEY = "stocks.angelone.apiKey";
    public static String ANGEL_ONE_2FACTOR_SECRET_API_KEY = "stocks.angelone.2factor.secretKey";
    public static String ANGEL_ONE_USER_ID_KEY = "stocks.angelone.userId";
    public static String ANGEL_ONE_PIN_KEY = "stocks.angelone.pin";

    public static String EQUITY_TRADING_SPECIAL_WORKING_DAYS_LIST_KEY = "stocks.ind.equity.special.workingdays";

   public static final  List<String> nextDayHigh = List.of("Highest10","Highest20","Highest30","Highest40","Highest50","Highest60","Highest70","Highest80","Highest90","Highest100");
    public static final List<String> nextDayLow = List.of("Lowest10","Lowest20","Lowest30","Lowest40","Lowest50","Lowest60","Lowest70","Lowest80","Lowest90","Lowest100");
    public static final List<String> confidence = List.of("High","Medium","Low");

}
