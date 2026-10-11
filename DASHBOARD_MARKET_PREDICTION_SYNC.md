# Dashboard Market and Prediction Data Sync Implementation

## Overview
This document describes the implementation of dashboard service logic that merges market and prediction data into a unified dashboard table. The system fetches 2 years of historical market data and combines it with the latest prediction data, calculating differences for analysis.

## Architecture

### Data Flow
```
Market Data (2 years) + Prediction Data (latest)
         ↓
    Merge by Symbol
         ↓
Calculate Diffs (Market - Prediction)
         ↓
Store in Dashboard Table
         ↓
Dashboard API returns merged results
```

## Implementation Details

### 1. Dashboard Service Implementation
**File:** `src/main/java/com/stockai/service/impl/DashboardServiceImpl.java`

#### Key Methods:

##### `syncMarketAndPredictionData()`
Main orchestration method that:
- Fetches market data from the last 2 years using `marketRepository.findAllMarketData()`
- Fetches all latest prediction data using `predictionRepository.findAllPredictions()`
- Creates a symbol-indexed map for quick prediction lookups
- Iterates through market records and merges with corresponding prediction data
- Calculates market-prediction differences
- Stores all merged records in the dashboard table

**Process Flow:**
```java
1. Fetch MarketEntity records (last 2 years)
   └─ Filter by marketDate >= LocalDate.now().minusYears(2)
   
2. Fetch PredictionEntity records (latest)
   └─ Get all predictions from prediction table
   
3. Build prediction lookup map
   └─ Map<symbol, PredictionEntity>
   
4. For each market record:
   └─ Copy market data to DashboardEntity
   └─ If prediction exists for symbol:
       ├─ Copy prediction data
       └─ Calculate diffs (market - prediction)
   └─ Set marketDataType = "MARKET_PREDICTION"
   └─ Add to dashboard records list
   
5. Store all records
   └─ dashboardRepository.replaceDashboardData(records)
```

##### `copyMarketToDashboard(MarketEntity, DashboardEntity)`
Copies all market data fields to dashboard entity:
- **Price Data:** open, high, low, close
- **Volume:** volume, volumeRatio, avgVolume
- **Percentages:** per, ptsC, changePercent
- **Technical Indicators:** sma5, sma10, sma20, sma50, ema12, ema26, rsi14, macd, macdSignal, atr14, volatility20d
- **Analytics:** return1d, return5d, return10d, return30d, distanceFrom52wHighPct, distanceFrom52wLowPct, distanceFromSma20Pct
- **Flags & Signals:** breakoutFlag, breakdownFlag, trendLabel
- **Next Day Predictions:** nextDayHigh, nextDayReturnPct, nextDayLow, nextDayLowReturnPct
- **Metadata:** symbol, name, status, sector, stockType, marketSegment, watchlistName, marketDate, dates

##### `copyPredictionToDashboard(PredictionEntity, DashboardEntity)`
Copies prediction-specific fields to dashboard entity:
- `predictedHigh` (from `predicted_high`)
- `predictedLow` (from `predicted_low`)
- `predictionClose` (from `prediction_close`)
- `predictionDirection` (from `prediction_direction`)
- `confidencePercent` (from `confidence_percent`)

##### `calculateDifferences(MarketEntity, PredictionEntity, DashboardEntity)`
Calculates market-prediction diffs for analysis:
```
Market High - Predicted High = marketPredictionHighDiff
Market Low - Predicted Low   = marketPredictionLowDiff
```

These differences help identify:
- **Positive Diffs:** Market performed better than prediction
- **Negative Diffs:** Market underperformed prediction
- **Zero Diffs:** Prediction was accurate

##### `fetchDashBoardData(DashboardFilterDto)`
Retrieves dashboard data and converts entities to DTOs:
- Fetches all dashboard records
- Streams through records and converts each entity to DTO using `BeanUtils.copyProperties()`
- Returns list of DashboardDto objects

### 2. Entity Updates
**Files:** 
- `src/main/java/com/stockai/entity/DashboardEntity.java`
- `src/main/java/com/stockai/dto/DashboardDto.java`

#### Added Fields:
```java
// High-side analytics
private Double nextDayHigh;
private Double nextDayReturnPct;

// Low-side analytics (NEW)
private Double nextDayLow;
private Double nextDayLowReturnPct;

// Technical signals (NEW)
private Boolean breakdownFlag;

// Market vs Prediction comparison
private Double marketPredictionHighDiff;
private Double marketPredictionLowDiff;

// Prediction data from latest run
private Double predictedHigh;
private Double predictedLow;
private Double predictionClose;
private String predictionDirection;
private Double confidencePercent;

// Data type marker
private String marketDataType; // "MARKET_PREDICTION"
```

### 3. Scheduler Integration
**File:** `src/main/java/com/stockai/schedulars/DashboardSyncScheduler.java`

Existing scheduler already integrated:
- **Cron:** `0 0 18 ? * MON-FRI` (6 PM on trading days)
- **Check:** Validates if current day is a trading day using `StockUtil.isTradingDay()`
- **Execution:** Calls `dashboardService.processDashboardData()`
- **Logging:** Logs number of synchronized rows and any errors

## Field Mappings

### Market to Dashboard
| Market Entity | Dashboard Entity | Purpose |
|---------------|-----------------|---------|
| high | high | Current market high |
| low | low | Current market low |
| close | close | Market close price |
| sma20 | sma20 | 20-day moving average |
| rsi14 | rsi14 | RSI indicator |
| breakoutFlag | breakoutFlag | Breakout detection |
| breakdownFlag | breakdownFlag | Breakdown detection (NEW) |
| trendLabel | trendLabel | Trend classification |
| nextDayHigh | nextDayHigh | Predicted next-day high |
| nextDayReturnPct | nextDayReturnPct | Predicted next-day return % |
| nextDayLow | nextDayLow | Predicted next-day low (NEW) |
| nextDayLowReturnPct | nextDayLowReturnPct | Predicted next-day low return % (NEW) |
| confidenceScore | confidenceScore | Market prediction confidence |

### Prediction to Dashboard
| Prediction Entity | Dashboard Entity | Purpose |
|-------------------|-----------------|---------|
| predicted_high | predictedHigh | ML-predicted high |
| predicted_low | predictedLow | ML-predicted low |
| prediction_close | predictionClose | ML-predicted close |
| prediction_direction | predictionDirection | UP/DOWN direction |
| confidence_percent | confidencePercent | ML model confidence |

### Calculated in Dashboard
| Calculation | Dashboard Field | Formula |
|------------|-----------------|---------|
| High Difference | marketPredictionHighDiff | market.high - prediction.predicted_high |
| Low Difference | marketPredictionLowDiff | market.low - prediction.predicted_low |

## Data Storage

### Dashboard Table Structure
```
dmastock.dashboard
├── Market Data (symbol, open, high, low, close, volume, etc.)
├── Technical Indicators (sma20, rsi14, macd, etc.)
├── Analytics (volumeRatio, returns, distances, etc.)
├── Signals (breakoutFlag, breakdownFlag, trendLabel)
├── Market Predictions (nextDayHigh, nextDayLow, etc.)
├── Prediction Data (predictedHigh, predictedLow, predictionClose, etc.)
├── Differences (marketPredictionHighDiff, marketPredictionLowDiff)
└── Metadata (symbol, sector, stockType, marketDate, etc.)
```

### Data Retention
- **Market Data:** Last 2 years of historical data
- **Prediction Data:** Latest predictions only (per symbol)
- **Dashboard Records:** All merged market records with latest predictions
- **Update Frequency:** Daily at 6 PM (configurable via cron)

## Usage Examples

### Start Dashboard Sync Manually
```java
// In controller or service
@Autowired
private DashboardService dashboardService;

// Trigger sync
int recordsSaved = dashboardService.processDashboardData();
System.out.println("Saved " + recordsSaved + " dashboard records");
```

### Fetch Dashboard Data
```java
@Autowired
private DashboardService dashboardService;

// Fetch all dashboard data (with optional filters)
DashboardFilterDto filters = new DashboardFilterDto();
List<DashboardDto> dashboardData = dashboardService.fetchDashBoardData(filters);

// Use in API response
return ResponseEntity.ok(dashboardData);
```

### Query Dashboard in Controller
```java
@GetMapping("/api/dashboard")
public ResponseEntity<List<DashboardDto>> getDashboard(
    @RequestParam(required = false) String symbol,
    @RequestParam(required = false) String trend) {
    
    DashboardFilterDto filters = new DashboardFilterDto();
    filters.setSymbol(symbol);
    filters.setTrendLabel(trend);
    
    List<DashboardDto> data = dashboardService.fetchDashBoardData(filters);
    return ResponseEntity.ok(data);
}
```

## Error Handling

The implementation includes comprehensive error handling:
```java
try {
    // Sync process
    return savedCount;
} catch (Exception e) {
    log.error("Error while syncing dashboard data", e);
    return 0; // Returns 0 on failure
}
```

**Logged Errors:**
- Dashboard sync start/completion
- Market record fetch count
- Prediction record fetch count
- Merge completion count
- Save status
- Any exceptions during processing

## Testing Checklist

- [ ] Market data syncs successfully (verify row count in logs)
- [ ] Prediction data loads correctly
- [ ] Merge creates dashboard records for each market record
- [ ] Prediction data is correctly merged (check predictions for known symbols)
- [ ] Diffs calculate correctly:
  - [ ] marketPredictionHighDiff = market.high - prediction.predicted_high
  - [ ] marketPredictionLowDiff = market.low - prediction.predicted_low
- [ ] Dashboard API returns all fields (market + prediction + diffs)
- [ ] Records persist in dashboard table
- [ ] Scheduler runs at scheduled time (6 PM)
- [ ] No duplicate records are created
- [ ] All null values handled gracefully

## Database Verification

```sql
-- Check dashboard records count
SELECT COUNT(*) FROM dmastock.dashboard;

-- Verify market data present
SELECT COUNT(DISTINCT symbol) FROM dmastock.dashboard 
WHERE market_date >= DATE_SUB(NOW(), INTERVAL 2 YEAR);

-- Check prediction merge
SELECT COUNT(*) FROM dmastock.dashboard 
WHERE predicted_high IS NOT NULL;

-- Verify diffs calculated
SELECT COUNT(*) FROM dmastock.dashboard 
WHERE market_prediction_high_diff IS NOT NULL;

-- Sample merged record
SELECT symbol, high, predicted_high, market_prediction_high_diff, 
       low, predicted_low, market_prediction_low_diff,
       trend_label, prediction_direction, confidence_percent
FROM dmastock.dashboard
WHERE symbol = 'TCS'
LIMIT 5;
```

## Performance Considerations

- **2-Year Data Load:** May take time depending on market data size
- **Prediction Lookup:** Uses HashMap for O(1) lookup by symbol
- **Batch Processing:** All records processed and stored in single batch
- **Memory Usage:** Loads all market and prediction data into memory
- **Optimization:** For large datasets, consider pagination or incremental updates

## Configuration

### Cron Schedule
Current: `0 0 18 ? * MON-FRI` (6 PM, Mon-Fri)

To modify, update in `DashboardSyncScheduler.java`:
```java
@Scheduled(cron = "0 30 18 ? * MON-FRI") // 6:30 PM instead
```

### Dashboard Limit
Current: `20` (configurable)
```properties
# application.properties
dashboard.top-limit=20
```

## Future Enhancements

1. **Incremental Updates:** Only sync new/updated records instead of full refresh
2. **Filtering by Date:** Sync only recent market data for faster processing
3. **Parallel Processing:** Use multi-threading for large datasets
4. **Prediction Confidence Filter:** Only merge predictions above certain confidence threshold
5. **Historical Tracking:** Keep historical prediction versions for analysis
6. **Real-time Updates:** Sync market data as it arrives instead of batch daily

## Related Files

- Market Service: `src/main/java/com/stockai/service/impl/MarketsServiceImpl.java`
- Pattern Scheduler: `src/main/java/com/stockai/schedulars/PatternSyncScheduler.java`
- Dashboard Controller: `src/main/java/com/stockai/controller/DashboardController.java`
- Dashboard Repository: `src/main/java/com/stockai/repository/DashboardRepository.java`

