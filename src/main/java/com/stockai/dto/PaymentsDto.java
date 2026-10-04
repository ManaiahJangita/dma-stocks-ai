package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentsDto {
	private UUID id;
	private UUID planId;
	private String planUserName;
	private String recordType; //
	private String planName;
	private String planDescription;
	private String paymentType; //
	private BigDecimal amount;
	private String currency;   //
	private String status;
	private String transactionReference;
	private LocalDateTime createdDate;
	private LocalDateTime planUpdatedDate;
	private LocalDate fromDate;
	private LocalDate toDate;
	private BigDecimal minimumAmount;
	private BigDecimal maximumAmount;
}
