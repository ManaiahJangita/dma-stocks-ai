package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockV2Dto {
    private String symbol;
    private LocalDate createdDate;
    private double open;
    private double high;
    private double low;
    private double close;
    private Long volume;
    private Long dvolume;    //Delivery volume
}
