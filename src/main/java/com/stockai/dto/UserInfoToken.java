package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.stockai.apiconstants.ApiConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfoToken {
    String username;
    String userId;
    LocalDateTime sessionDate;
    String authorities;

    public String generateUserInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append(username).append(ApiConstants.USER_INFO_DELIMITER)
                .append(userId).append(ApiConstants.USER_INFO_DELIMITER)
                .append(sessionDate).append(ApiConstants.USER_INFO_DELIMITER)
                .append(authorities);

        return builder.toString();
    }

    public static UserInfoToken extractUserInfo(String userInfo) {
        String[] userTokens = userInfo.split(ApiConstants.USER_INFO_DELIMITER);

        UserInfoToken token = new UserInfoToken();
        if (userTokens.length > 0) {
            token.setUsername(userTokens[0]);
        }

        if (userTokens.length > 1) {
            token.setUserId(userTokens[1]);
        }

        if (userTokens.length > 2) {
            try {
                String sessionDateStr = userTokens[2];
                token.setSessionDate(LocalDateTime.parse(sessionDateStr));
            } catch (Exception e) {
                //Ignore
            }
        }

        if (userTokens.length > 3) {
            token.setAuthorities(userTokens[3]);
        }
        return token;
    }

    public boolean isSessionValid() {
        return Objects.nonNull(this.sessionDate) && LocalDateTime.now().isBefore(this.sessionDate);
    }

}