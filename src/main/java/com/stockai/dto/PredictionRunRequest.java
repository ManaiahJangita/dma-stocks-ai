package com.stockai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionRunRequest {
	private LocalDate startDate;
	private LocalDate endDate;
	private Integer limit;
}