package com.stockai.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;

import java.util.Objects;

public class CookieUtils {
    /**
     * Adds the cookie with given details.
     * Also adds the header with the same name for backup purpose
     *
     * @param cookieName
     * @param cookieValue
     * @param response
     */
    public static void addCookie(String cookieName, String cookieValue, HttpServletResponse response) {
        int hour = 3600;
        addCookie(cookieName, cookieValue, hour, response, false);
    }

    /**
     * Adds the cookie with given details.
     * Also adds the header with the same name for backup purpose
     *
     * @param cookieName
     * @param cookieValue
     * @param cookieMaxAge
     * @param response
     */
    public static void addCookie(String cookieName, String cookieValue, int cookieMaxAge, HttpServletResponse response) {
        addCookie(cookieName, cookieValue, cookieMaxAge, response, false);
    }

    public static void addCookie(String cookieName, String cookieValue, int cookieMaxAge,
                                 HttpServletResponse response, boolean secure) {
        if (StringUtils.hasText(cookieName)) {
            Cookie cookie = new Cookie(cookieName, cookieValue);
            cookie.setMaxAge(cookieMaxAge);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setSecure(secure);
            cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);
        }
    }

    /**
     * Remove the cookie with given details.
     *
     * @param cookieName
     * @param response
     */
    public static void removeCookie(String cookieName, HttpServletResponse response) {
        addCookie(cookieName, null, 0, response);
    }

    public static void removeCookie(String cookieName, HttpServletResponse response, boolean secure) {
        addCookie(cookieName, null, 0, response, secure);
    }

    /**
     * Returns the value from header. If value is not available in the header then get it from the cookie.
     *
     * @param name        - cookie name or header name
     * @param httpRequest
     * @return
     */
    public static String getValueFromRequest(String name, HttpServletRequest httpRequest) {
        String accessToken = httpRequest.getHeader(name);

        //If value is not available in the header then get from cookie
        if (!StringUtils.hasText(accessToken) && StringUtils.hasText(name)) {
            Cookie[] cookies = httpRequest.getCookies();
            if (Objects.nonNull(cookies)) {
                for (Cookie cookie : cookies) {
                    if (name.equalsIgnoreCase(cookie.getName())) {
                        accessToken = cookie.getValue();
                        break;
                    }
                }
            }
        }

        return accessToken;
    }

}
