package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportsDto {
	private UUID id;
	private String title;
	private String description;
	private String status;
	private LocalDateTime createdDate;
	private String filterValue;
	private String symbol;
	private Double metricValue;
	private Double changePercent;
	private Double closePrice;
	private Long volume;
	private Integer rankNo;
	private LocalDate dataDate;
    private LocalDate fromDate;
    private LocalDate toDate;
}
