package com.stockai.config;

import com.stockai.utils.StockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.util.NumberUtils;

import java.util.List;
import java.util.Objects;


/**
 * Application configuration.
 *
 * @author sudhakar.badugu Oct 22, 2019
 */
@Configuration
@PropertySources({@PropertySource(ignoreResourceNotFound = true, value = "classpath:stocks-server.properties"),
        @PropertySource(ignoreResourceNotFound = true, value = "classpath:stocks.properties")})
public class ApplicationConfig {

    @Autowired
    private Environment env;

    public String getProperty(String propertyKey) {
        return env.getProperty(propertyKey, "");
    }

    public String getProperty(String propertyKey, String defaultValue) {
        return env.getProperty(propertyKey, defaultValue);
    }

    public List<String> getListProperties(String propertyKey) {
        String propValue = env.getProperty(propertyKey);
        return StockUtil.split(propValue, ",");
    }

    public Integer getIntProperty(String propertyKey) {
        return NumberUtils.parseNumber(env.getProperty(propertyKey), Integer.class);
    }

    public int getIntProperty(String propertyKey, int defaultValue) {
        String propValue = env.getProperty(propertyKey);
        int value = 0;
        if (Objects.isNull(propValue)) {
            value = defaultValue;
        } else {
            value = NumberUtils.parseNumber(propValue, Integer.class);
        }

        return value;
    }

    public Long getLongProperty(String propertyKey) {
        return NumberUtils.parseNumber(env.getProperty(propertyKey), Long.class);
    }

    public Long getLongProperty(String propertyKey, long defaultValue) {
        String propValue = env.getProperty(propertyKey);
        long value = 0;
        if (Objects.isNull(propValue)) {
            value = defaultValue;
        } else {
            value = NumberUtils.parseNumber(propValue, Long.class);
        }

        return value;
    }

    public Double getDoubleProperty(String propertyKey) {
        return NumberUtils.parseNumber(env.getProperty(propertyKey), Double.class);
    }

    public Double getDoubleProperty(String propertyKey, long defaultValue) {
        String propValue = env.getProperty(propertyKey);
        double value = 0;
        if (Objects.isNull(propValue)) {
            value = defaultValue;
        } else {
            value = NumberUtils.parseNumber(propValue, Double.class);
        }

        return value;
    }

    public Boolean getBooleanProperty(String propertyKey) {
        return Boolean.valueOf(env.getProperty(propertyKey));
    }

    public boolean getBooleanProperty(String propertyKey, boolean defaultValue) {
        Boolean value = this.getBooleanProperty(propertyKey);
        if (Objects.isNull(value)) {
            value = defaultValue;
        }

        return value;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfig() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}