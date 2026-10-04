package com.stockai.utils;

import com.stockai.dto.UserInfoToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


public class AuthUtils {

    //add private constructor to avoid instantiation
    private AuthUtils() {
    }

    /**
     * Set the Authentication data to SecurityContext.
     *
     * @param principle
     */
    public static void setAuthentication(Object principle) {
        setAuthentication(principle, null, null);
    }

    /**
     * Removes the Data from security context.
     */
    public static void removeAuthentication() {
        SecurityContext sc = SecurityContextHolder.getContext();

        Authentication authentication = sc.getAuthentication();
        if (Objects.nonNull(authentication) && authentication instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
            token.eraseCredentials();
        }
        SecurityContextHolder.clearContext();
    }

    /**
     * Set the Authentication data to SecurityContext.
     *
     * @param principle
     * @param credentials
     */
    public static void setAuthentication(Object principle, String credentials, List<SimpleGrantedAuthority> authorities) {
        SecurityContext sc = SecurityContextHolder.getContext();
        UsernamePasswordAuthenticationToken authReq
                = new UsernamePasswordAuthenticationToken(principle, credentials, authorities);
        sc.setAuthentication(authReq);
    }

    /**
     * Get the current UserInfoToken from security context.
     *
     * @return
     */
    public static Optional<UserInfoToken> getCurrentUser() {
        UserInfoToken userInfoToken = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.nonNull(authentication) && authentication.getPrincipal() instanceof UserInfoToken) {
            userInfoToken = ((UserInfoToken) authentication.getPrincipal());
        }
        return Optional.ofNullable(userInfoToken);
    }

    /**
     * Returns current user id from userinfo token
     *
     * @return
     */
    public static String getCurrentUserId() {
        return getCurrentUser().map(UserInfoToken::getUserId).orElse(null);
    }

    public static List<SimpleGrantedAuthority> getSimpleGrantedAuthority(List<String> authoritiesStrList) {
        return authoritiesStrList.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

}

