# Dashboard Market-Prediction Sync - Quick Reference

## What Was Implemented

### ✅ DashboardServiceImpl
**File:** `src/main/java/com/stockai/service/impl/DashboardServiceImpl.java`

**Key Method:** `syncMarketAndPredictionData()`

```
STEP 1: Fetch Data
├─ Get last 2 years of market data from MarketEntity
└─ Get latest predictions from PredictionEntity

STEP 2: Build Index
└─ Create Map<Symbol, PredictionEntity> for O(1) lookups

STEP 3: Merge Records
├─ For each market record:
│  ├─ Copy all market data to DashboardEntity
│  ├─ Find matching prediction by symbol
│  ├─ Copy prediction data if found
│  ├─ Calculate diffs (market - prediction)
│  └─ Set marketDataType = "MARKET_PREDICTION"
└─ Total: One DashboardEntity per market record

STEP 4: Store & Return
└─ Save all merged records to dashboard table
```

### ✅ Entity Fields Added

**DashboardEntity & DashboardDto - New Fields:**
```java
private Boolean breakdownFlag;        // Breakdown detection
private Double nextDayLow;            // Predicted low for next day
private Double nextDayLowReturnPct;   // Predicted low return %
```

**Plus existing prediction fields:**
```java
private Double predictedHigh;             // ML prediction
private Double predictedLow;              // ML prediction
private Double predictionClose;           // ML prediction
private Double marketPredictionHighDiff;  // Market High - Predicted High
private Double marketPredictionLowDiff;   // Market Low - Predicted Low
private String predictionDirection;       // UP/DOWN
private Double confidencePercent;         // ML confidence
```

### ✅ Scheduler Integration
**File:** `src/main/java/com/stockai/schedulars/DashboardSyncScheduler.java`

Already calls: `dashboardService.processDashboardData()`

**Schedule:** 6 PM every trading day (Mon-Fri)

---

## Data Flow Example

### Input Data
```
MarketEntity: {
  symbol: "TCS",
  high: 3500.00,
  low: 3480.00,
  close: 3495.50,
  sma20: 3450.00,
  rsi14: 65.5,
  breakoutFlag: true,
  trendLabel: "Strong Uptrend"
}

PredictionEntity: {
  symbol: "TCS",
  predicted_high: 3520.00,
  predicted_low: 3470.00,
  prediction_close: 3500.00,
  prediction_direction: "UP",
  confidence_percent: 78.5
}
```

### Processing
```
1. Copy market data → DashboardEntity
2. Copy prediction data → DashboardEntity
3. Calculate diffs:
   - marketPredictionHighDiff = 3500.00 - 3520.00 = -20.00
   - marketPredictionLowDiff = 3480.00 - 3470.00 = +10.00
4. Set marketDataType = "MARKET_PREDICTION"
```

### Output DashboardEntity
```
{
  symbol: "TCS",
  high: 3500.00,
  low: 3480.00,
  close: 3495.50,
  sma20: 3450.00,
  rsi14: 65.5,
  breakoutFlag: true,
  trendLabel: "Strong Uptrend",
  
  predictedHigh: 3520.00,
  predictedLow: 3470.00,
  predictionClose: 3500.00,
  predictionDirection: "UP",
  confidencePercent: 78.5,
  
  marketPredictionHighDiff: -20.00,   // Market underperformed
  marketPredictionLowDiff: +10.00,    // Market outperformed on low
  
  marketDataType: "MARKET_PREDICTION"
}
```

---

## Interpretation Guide

### Market vs Prediction Diffs

**High Difference:**
- **Positive (+):** Market high > Predicted high → Market stronger than predicted ✅
- **Negative (-):** Market high < Predicted high → Market weaker than predicted ⚠️
- **Zero (0):** Perfect prediction 🎯

**Low Difference:**
- **Positive (+):** Market low > Predicted low → Market didn't fall as much ✅
- **Negative (-):** Market low < Predicted low → Market fell more ⚠️

### Example Analysis
```
If marketPredictionHighDiff = -20 and marketPredictionLowDiff = +10:
├─ Market couldn't reach predicted high (-20)
└─ But market held better at lows (+10)
→ Market more defensive than predicted
```

---

## Testing the Implementation

### 1. Verify Service is Running
```bash
# Check logs for sync execution at 6 PM
tail -f logs/app.log | grep "Dashboard sync"
```

### 2. Check Dashboard Records
```sql
-- Count records
SELECT COUNT(*) FROM dmastock.dashboard;

-- Show sample with market & prediction data
SELECT symbol, high, predicted_high, market_prediction_high_diff,
       low, predicted_low, market_prediction_low_diff,
       market_data_type
FROM dmastock.dashboard
WHERE market_data_type = 'MARKET_PREDICTION'
LIMIT 10;
```

### 3. Verify Diffs Calculated
```sql
-- Check records with diffs
SELECT COUNT(*) as records_with_diffs
FROM dmastock.dashboard
WHERE market_prediction_high_diff IS NOT NULL 
  AND market_prediction_low_diff IS NOT NULL;

-- Sample calculations verification
SELECT symbol,
       high as market_high,
       predicted_high,
       (high - predicted_high) as calculated_diff,
       market_prediction_high_diff as stored_diff,
       CASE WHEN (high - predicted_high) = market_prediction_high_diff 
            THEN 'OK' ELSE 'MISMATCH' END as validation
FROM dmastock.dashboard
WHERE market_prediction_high_diff IS NOT NULL
LIMIT 10;
```

### 4. API Testing
```bash
# Fetch dashboard data
curl -X GET "http://localhost:8080/api/dashboard" \
     -H "Content-Type: application/json"

# Response should include:
# - Market fields (high, low, close, sma20, rsi14, etc.)
# - Prediction fields (predictedHigh, predictedLow, etc.)
# - Diff fields (marketPredictionHighDiff, marketPredictionLowDiff)
```

---

## Common Issues & Solutions

### Issue: No Records in Dashboard
**Cause:** Market or prediction data not available
**Solution:** 
- Check market data exists: `SELECT COUNT(*) FROM dmastock.markets;`
- Check predictions exist: `SELECT COUNT(*) FROM dmastock.prediction;`
- Run sync manually and check logs

### Issue: Prediction Data Not Merging
**Cause:** Symbol mismatch between market and prediction tables
**Solution:**
- Compare symbols: 
  ```sql
  SELECT DISTINCT m.symbol FROM dmastock.markets m
  WHERE m.symbol NOT IN (SELECT DISTINCT symbol FROM dmastock.prediction);
  ```

### Issue: Null Diffs
**Cause:** Market or prediction values missing
**Solution:**
- Check for nulls:
  ```sql
  SELECT * FROM dmastock.dashboard
  WHERE market_prediction_high_diff IS NULL
  AND predicted_high IS NOT NULL;
  ```

### Issue: Scheduler Not Running
**Cause:** Not a trading day or scheduler disabled
**Solution:**
- Check if trading day: `SELECT com.stockai.utils.StockUtil.isTradingDay()`
- Verify scheduler enabled in Spring config
- Check application logs for scheduling errors

---

## Code Usage Examples

### Manually Trigger Sync
```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @Autowired
    private DashboardService dashboardService;
    
    @PostMapping("/dashboard/sync")
    public ResponseEntity<Integer> syncDashboard() {
        int recordsSaved = dashboardService.processDashboardData();
        return ResponseEntity.ok(recordsSaved);
    }
}
```

### Filter Dashboard Data
```java
@GetMapping("/api/dashboard")
public ResponseEntity<List<DashboardDto>> getDashboard(
    @RequestParam(required = false) String symbol,
    @RequestParam(required = false) String trendLabel,
    @RequestParam(required = false) Double minConfidence) {
    
    List<DashboardDto> allData = dashboardService.fetchDashBoardData(null);
    
    List<DashboardDto> filtered = allData.stream()
        .filter(d -> symbol == null || d.getSymbol().equals(symbol))
        .filter(d -> trendLabel == null || d.getTrendLabel().equals(trendLabel))
        .filter(d -> minConfidence == null || d.getConfidencePercent() >= minConfidence)
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(filtered);
}
```

### Analyze Market vs Prediction
```java
public List<String> analyzePredictionAccuracy(List<DashboardDto> dashboard) {
    return dashboard.stream()
        .filter(d -> d.getMarketPredictionHighDiff() != null)
        .map(d -> String.format(
            "%s: High diff=%.2f (Market %s prediction), " +
            "Low diff=%.2f (Market %s prediction)",
            d.getSymbol(),
            d.getMarketPredictionHighDiff(),
            d.getMarketPredictionHighDiff() > 0 ? "exceeded" : "missed",
            d.getMarketPredictionLowDiff(),
            d.getMarketPredictionLowDiff() > 0 ? "held" : "broke"
        ))
        .collect(Collectors.toList());
}
```

---

## Performance Notes

- **Processing Time:** Depends on 2-year dataset size
- **Memory Usage:** Loads all market + prediction data in memory
- **Batch Insert:** All records inserted in single transaction
- **Lookup:** O(1) symbol-based prediction lookup using HashMap
- **Frequency:** Daily (configurable via cron)

---

## Files Modified

| File | Changes |
|------|---------|
| `DashboardServiceImpl.java` | Added syncMarketAndPredictionData(), copyMarketToDashboard(), copyPredictionToDashboard(), calculateDifferences(), convertEntityToDto() |
| `DashboardEntity.java` | Added breakdownFlag, nextDayLow, nextDayLowReturnPct |
| `DashboardDto.java` | Added breakdownFlag, nextDayLow, nextDayLowReturnPct |

---

## Next Steps

1. ✅ Verify compilation (no syntax errors)
2. 📅 Wait for next scheduled run (6 PM) OR manually trigger sync
3. 🔍 Check dashboard table for merged records
4. 📊 Verify API returns market + prediction data
5. 📈 Analyze diffs for market accuracy
6. 🎯 Use in frontend dashboard UI

---

## Architecture Context

```
Scheduler (DashboardSyncScheduler)
    ↓ 6 PM Daily ↓
DashboardService.processDashboardData()
    ↓
syncMarketAndPredictionData()
    ├─ MarketRepository.findAllMarketData() → Last 2 years
    └─ PredictionRepository.findAllPredictions() → Latest
    ↓
Merge by Symbol + Calculate Diffs
    ↓
DashboardRepository.replaceDashboardData()
    ↓
Dashboard Table (market + prediction + diffs)
    ↓
DashboardController / Dashboard API
    ↓
Frontend Dashboard UI
```

