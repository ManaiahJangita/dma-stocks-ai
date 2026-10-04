package com.stockai.filter;

import com.stockai.apiconstants.ApiConstants;
import com.stockai.dto.UserInfoToken;
import com.stockai.utils.AuthUtils;
import com.stockai.utils.CookieUtils;
import com.stockai.utils.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals(ApiConstants.USER + "/auth") || path.startsWith(ApiConstants.USER + "/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extractTokenFromRequest(request);
        if (StringUtils.hasText(token) && jwtTokenUtil.validateToken(token)) {
            try {
                String username = jwtTokenUtil.extractUsername(token);
                String userId = jwtTokenUtil.extractUserId(token);
                List<String> roles = jwtTokenUtil.extractAuthorities(token);
                String authorities = roles.stream().collect(Collectors.joining(ApiConstants.DELIMITER));
                UserInfoToken userInfoToken = new UserInfoToken(
                        username, userId, jwtTokenUtil.extractExpiration(token), authorities);
                List<SimpleGrantedAuthority> grantedAuthorities = AuthUtils.getSimpleGrantedAuthority(roles);
                AuthUtils.setAuthentication(userInfoToken, token, grantedAuthorities);
            } catch (RuntimeException exception) {
                AuthUtils.removeAuthentication();
            }
        }

        chain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return bearerToken.substring(7);
        }
        return CookieUtils.getValueFromRequest(ApiConstants.USER_ID_COOKIE_NAME, request);
    }
}