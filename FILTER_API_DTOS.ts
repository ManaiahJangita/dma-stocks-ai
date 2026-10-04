export type UUID = string;
export type LocalDate = string;
export type LocalDateTime = string;

export const API_BASE_URL = "http://localhost:8081";

export const FILTER_OPTION_ENDPOINTS = {
  reports: {
    method: "GET",
    path: "/api/v1/filter/reports-filters",
    responseDto: "ReportFilterDto",
  },
  patterns: {
    method: "GET",
    path: "/api/v1/filter/patterns-filters",
    responseDto: "PatternFilterDto",
  },
  stockMarket: {
    method: "GET",
    path: "/api/v1/filter/stock-market-filters",
    responseDto: "MarketFiltersDto[]",
  },
  stockAnalyzer: {
    method: "GET",
    path: "/api/v1/filter/stock-analyzer-filters",
    responseDto: "StockAnalyzerFiltersDto[]",
  },
  dashboard: {
    method: "GET",
    path: "/api/v1/filter/dashboard-filters",
    responseDto: "MarketFiltersOverviewDto[]",
  },
  customers: {
    method: "GET",
    path: "/api/v1/filter/customer-filters",
    responseDto: "CustomerDto[]",
  },
  payments: {
    method: "GET",
    path: "/api/v1/filter/payment-filters",
    responseDto: "PaymentFiltersDto[]",
  },
  auditLogs: {
    method: "GET",
    path: "/api/v1/filter/audit-log-filters",
    responseDto: "AuditLogFiltersDto[]",
  },
} as const;

export const FILTERED_DATA_ENDPOINTS = {
  authenticationLogs: {
    method: "GET",
    path: "/admin/v1/audit/authentication-logs",
    queryDto: "AuthAuditLogFilterDto",
  },
  customers: {
    method: "GET",
    path: "/api/v1/customers/customer-data",
    queryDto: "CustomerDto",
  },
  dashboard: {
    method: "GET",
    path: "/api/v1/dashboard/dashboard-data",
    queryDto: "DashboardDto",
  },
  markets: {
    method: "GET",
    path: "/api/v1/markets/markets-data",
    queryDto: "MarketDto",
  },
  patterns: {
    method: "GET",
    path: "/api/v1/patterns/patterns-data",
    queryDto: "PatternDto",
  },
  payments: {
    method: "GET",
    path: "/api/v1/payments/payments-data",
    queryDto: "PaymentsDto",
  },
  paymentPlans: {
    method: "GET",
    path: "/api/v1/payments/plans",
    queryDto: "PlanDto",
  },
  plans: {
    method: "GET",
    path: "/api/v1/plans/get-plans",
    queryDto: "PlanDto",
  },
  predictionRuns: {
    method: "GET",
    path: "/api/prediction/runs",
    queryDto: "PredictionRunRequest",
  },
  predictions: {
    method: "GET",
    path: "/api/prediction/runs/{runId}/predictions",
    queryDto: "PredictionDto",
  },
  reports: {
    method: "GET",
    path: "/api/v1/reports/reports-data",
    queryDto: "ReportsDto",
  },
  statements: {
    method: "GET",
    paths: [
      "/api/v1/statements",
      "/api/v1/statements/filter",
      "/api/v1/statements/statements-data",
      "/api/v1/statements/all",
    ],
    queryDto: "StatementDto",
  },
  stockAnalyzer: {
    method: "GET",
    path: "/api/v1/stock-analyzer/stock-analyzer-data",
    queryDto: "StockAnalyzerDto",
  },
} as const;

export interface ReportFilterDto {
  reports?: string[];
}

export interface PatternFilterDto {
  patterns?: string[];
}

export interface MarketFiltersDto {
  id?: UUID;
  market_indices?: string;
  market_performance?: string;
  price_movement?: string;
  volume_delivery?: string;
  technical_signals?: string;
  breakouts?: string;
  ai_prediction?: string;
  market_sentiment?: string;
  sector_performance?: string;
  time_period?: string;
}

export interface StockAnalyzerFiltersDto {
  id?: UUID;
  stock_search?: string;
  stock_overview?: string;
  price_analysis?: string;
  Technical_analysis?: string;
  volume_analysis?: string;
  ai_prediction?: string;
  historical_performance?: string;
  risk_analysis?: string;
  signals?: string;
  news_events?: string;
}

export interface MarketFiltersOverviewDto {
  id?: UUID;
  market_summary?: string;
  market_movers?: string;
  market_status?: string;
  market_signals?: string;
  ai_predictions_summary?: string;
  sector_performance?: string;
  mywatchlist_summary?: string;
}

export interface PaymentFiltersDto {
  id?: UUID;
  paymentType?: string;
  status?: string;
  createdDate?: LocalDateTime;
}

export interface AuditLogFiltersDto {
  id?: UUID;
  action?: string;
  performedBy?: string;
  performedDate?: LocalDateTime;
}

export interface AuthAuditLogFilterDto {
  userId?: UUID;
  email?: string;
  eventType?: string;
  fromDate?: LocalDate;
  toDate?: LocalDate;
  page?: number;
  size?: number;
}

export interface CustomerDto {
  id?: UUID;
  name?: string;
  email?: string;
  phone?: string;
  status?: string;
  createdDate?: LocalDateTime;
  fromDate?: LocalDate;
  toDate?: LocalDate;
}

export interface DashboardDto {
  id?: UUID;
  title?: string;
  description?: string;
  status?: string;
  createdDate?: LocalDateTime;
  fromDate?: LocalDate;
  toDate?: LocalDate;
}

export interface MarketDto {
  id?: UUID;
  name?: string;
  description?: string;
  status?: string;
  createdDate?: LocalDateTime;
  historicalDataId?: number;
  symbol?: string;
  open?: number;
  high?: number;
  low?: number;
  close?: number;
  previousClose?: number;
  volume?: number;
  per?: number;
  ptsC?: number;
  marketDate?: LocalDate;
  recordType?: string;
  predictionId?: number;
  predictionRunId?: UUID;
  predictionDate?: LocalDate;
  predictionClose?: number;
  predictionDirection?: string;
  predictionClosePercentage?: number;
  predictedHigh?: number;
  predictedHighPercentage?: number;
  predictedLow?: number;
  predictedLowPercentage?: number;
  confidencePercent?: number;
  upProbabilityPercent?: number;
  downProbabilityPercent?: number;
  fromDate?: LocalDate;
  toDate?: LocalDate;
  predictionDirectionFilter?: string;
  minimumConfidence?: number;
  maximumConfidence?: number;
}

export interface PatternDto {
  id?: UUID;
  name?: string;
  description?: string;
  status?: string;
  createdDate?: LocalDateTime;
  fromDate?: LocalDate;
  toDate?: LocalDate;
}

export interface PaymentsDto {
  id?: UUID;
  customerId?: UUID;
  planId?: UUID;
  recordType?: string;
  planName?: string;
  planDescription?: string;
  paymentType?: string;
  amount?: number;
  currency?: string;
  status?: string;
  transactionReference?: string;
  createdDate?: LocalDateTime;
  planUpdatedDate?: LocalDateTime;
  fromDate?: LocalDate;
  toDate?: LocalDate;
  minimumAmount?: number;
  maximumAmount?: number;
  minimumPrice?: number;
  maximumPrice?: number;
}

export interface PlanDto {
  id?: UUID;
  name?: string;
  description?: string;
  price?: number;
  status?: string;
  createdDate?: LocalDateTime;
  updatedDate?: LocalDateTime;
  fromDate?: LocalDate;
  toDate?: LocalDate;
  minimumPrice?: number;
  maximumPrice?: number;
}

export interface PredictionRunRequest {
  startDate?: LocalDate;
  endDate?: LocalDate;
  limit?: number;
}

export interface PredictionDto {
  id?: number;
  runId?: UUID;
  symbol?: string;
  open?: number;
  high?: number;
  low?: number;
  close?: number;
  volume?: number;
  per?: number;
  ptsC?: number;
  createdDate?: string;
  todayValue?: number;
  mode?: string;
  predictionDate?: string;
  predictionClose?: number;
  predictionDirection?: string;
  predictionClosePercentage?: number;
  predictedHigh?: number;
  predictedHighPercentage?: number;
  predictedLow?: number;
  predictedLowPercentage?: number;
  increasePercentage?: number;
  decreasePercentage?: number;
  confidencePercent?: number;
  upProbabilityPercent?: number;
  downProbabilityPercent?: number;
  fromDate?: LocalDate;
  toDate?: LocalDate;
  minimumConfidence?: number;
  maximumConfidence?: number;
}

export interface ReportsDto {
  id?: UUID;
  title?: string;
  description?: string;
  status?: string;
  createdDate?: LocalDateTime;
  fromDate?: LocalDate;
  toDate?: LocalDate;
}

export interface StatementDto {
  id?: UUID;
  title?: string;
  description?: string;
  status?: string;
  createdDate?: LocalDateTime;
  userId?: UUID;
  symbol?: string;
  stockType?: string;
  tradeType?: string;
  quantity?: number;
  unitPrice?: number;
  totalAmount?: number;
  tradeDate?: LocalDate;
  fromDate?: LocalDate;
  toDate?: LocalDate;
  page?: number;
  size?: number;
}

export interface StockAnalyzerDto {
  id?: UUID;
  symbol?: string;
  predictionId?: number;
  predictionRunId?: UUID;
  open?: number;
  high?: number;
  low?: number;
  latestClose?: number;
  previousClose?: number;
  tradingVolume?: number;
  per?: number;
  ptsC?: number;
  historyDate?: LocalDate;
  predictionDate?: LocalDate;
  predictionClose?: number;
  predictionDirection?: string;
  predictionClosePercentage?: number;
  predictedHigh?: number;
  predictedHighPercentage?: number;
  predictedLow?: number;
  predictedLowPercentage?: number;
  confidencePercent?: number;
  upProbabilityPercent?: number;
  downProbabilityPercent?: number;
  analysisResult?: string;
  status?: string;
  createdDate?: LocalDateTime;
  fromDate?: LocalDate;
  toDate?: LocalDate;
  predictionDirectionFilter?: string;
  minimumConfidence?: number;
  maximumConfidence?: number;
}