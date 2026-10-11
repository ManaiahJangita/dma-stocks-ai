# Pattern Types - Quick Reference

## 📊 Total Patterns: 3

---

## Pattern #1: 🔴 Breakout with Volume Confirmation

```
┌─────────────────────────────────────────────────────┐
│ BREAKOUT WITH VOLUME CONFIRMATION                   │
├─────────────────────────────────────────────────────┤
│                                                      │
│  Type: Bullish  |  Signal: Breakout                 │
│  Filter: bullish_breakout                           │
│                                                      │
│  CONDITIONS:                                         │
│  ✓ Breakout Flag = TRUE                             │
│  ✓ Volume Ratio >= 1.2  (20% above average)        │
│  ✓ Trend = Bullish                                  │
│                                                      │
│  SCORE FORMULA:                                      │
│  50 + (RSI - 50) × 0.8 + (Volume - 1) × 30         │
│                                                      │
│  LINE RANGE: 93-109                                 │
│                                                      │
│  KEY METRICS USED:                                  │
│  └─ breakoutFlag, volumeRatio, trendLabel, rsi14   │
│                                                      │
│  SIGNAL: 🟢 BUY (Strong breakout with volume)      │
│                                                      │
└─────────────────────────────────────────────────────┘
```

---

## Pattern #2: 🟠 Trend Pullback

```
┌─────────────────────────────────────────────────────┐
│ TREND PULLBACK                                      │
├─────────────────────────────────────────────────────┤
│                                                      │
│  Type: Bullish  |  Signal: Trend                    │
│  Filter: trend_pullback                             │
│                                                      │
│  CONDITIONS:                                         │
│  ✓ Close > SMA20  (price above short-term avg)     │
│  ✓ SMA20 > SMA50  (short-term > long-term)         │
│  ✓ RSI14 > 50     (positive momentum)               │
│  ✓ Trend = Bullish                                  │
│                                                      │
│  SCORE FORMULA:                                      │
│  55 + (RSI - 50) × 0.5 + (Close-SMA20)/SMA20 × 100 │
│                                                      │
│  LINE RANGE: 111-127                                │
│                                                      │
│  KEY METRICS USED:                                  │
│  └─ close, sma20, sma50, rsi14, trendLabel         │
│                                                      │
│  SIGNAL: 🟡 MOMENTUM BUY (Trend continuation)       │
│                                                      │
└─────────────────────────────────────────────────────┘
```

---

## Pattern #3: 🔵 Momentum Shift

```
┌─────────────────────────────────────────────────────┐
│ MOMENTUM SHIFT                                      │
├─────────────────────────────────────────────────────┤
│                                                      │
│  Type: Bullish  |  Signal: Momentum                 │
│  Filter: momentum_shift                             │
│                                                      │
│  CONDITIONS:                                         │
│  ✓ RSI14 > 50      (positive momentum)              │
│  ✓ RSI14 < 70      (not overbought)                 │
│  ✓ MACD > Signal   (MACD crossover)                 │
│  ✓ Trend = Neutral (setting up to change)          │
│                                                      │
│  SCORE FORMULA:                                      │
│  60 + (RSI - 50) × 0.7                              │
│                                                      │
│  LINE RANGE: 129-146                                │
│                                                      │
│  KEY METRICS USED:                                  │
│  └─ rsi14, macd, macdSignal, trendLabel             │
│                                                      │
│  SIGNAL: 🔵 EARLY ENTRY (Start of uptrend)         │
│                                                      │
└─────────────────────────────────────────────────────┘
```

---

## 📈 Pattern Comparison

```
╔═════════════════════════════════════════════════════════════════╗
║ ASPECT          │ BREAKOUT │ TREND PULLBACK │ MOMENTUM SHIFT    ║
╠═════════════════╪══════════╪════════════════╪══════════════════╣
║ Type            │ Bullish  │ Bullish        │ Bullish           ║
║ Base Score      │ 50       │ 55             │ 60                ║
║ Main Driver     │ Volume   │ Moving Avgs    │ RSI + MACD         ║
║ Risk Level      │ Medium   │ Low            │ High              ║
║ Profit Potential│ High     │ Medium         │ Very High         ║
║ Frequency       │ Medium   │ High           │ Medium            ║
║ Reliability     │ High     │ Medium-High    │ Medium            ║
║ Best For        │ Trending │ Trending       │ Neutral→Bullish   ║
╚═════════════════╩══════════╩════════════════╩══════════════════╝
```

---

## 🔄 Scheduler Execution Flow

```
┌─────────────────────────────────────────────────────────────┐
│ PatternSyncScheduler executes at 6:30 PM (Mon-Fri)         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ ① Fetch 2 years of market data                              │
│    └─ Filter by date range                                  │
│                                                              │
│ ② Group by Symbol                                           │
│    └─ Min 20 records per symbol                             │
│                                                              │
│ ③ For each symbol, check ALL 3 patterns:                    │
│    ├─ Pattern #1: Breakout with Volume Confirmation?        │
│    ├─ Pattern #2: Trend Pullback?                           │
│    └─ Pattern #3: Momentum Shift?                           │
│                                                              │
│ ④ Calculate scores for matched patterns                     │
│    └─ Multiple patterns possible per symbol per day         │
│                                                              │
│ ⑤ Sort by score DESC                                        │
│    └─ Keep top 200 patterns                                 │
│                                                              │
│ ⑥ Save to patterns table                                    │
│    └─ Replace all patterns (fresh daily)                    │
│                                                              │
│ ⑦ Log results                                               │
│    └─ "Pattern sync completed. Saved X rows"                │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Score Ranges

### Breakout Pattern
```
0-25:   Weak
  ▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░

25-50:  Moderate
  ▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░

50-75:  Strong
  ▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░

75-100: Very Strong
  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░
```

### Trend Pullback
```
0-30:   Weak
  ▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░

30-60:  Moderate
  ▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░

60-85:  Strong
  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░

85-100: Very Strong
  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░
```

### Momentum Shift
```
0-30:   Weak
  ▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░

30-60:  Weak-Moderate
  ▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░

60-80:  Strong
  ▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░

80-100: Very Strong
  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░
```

---

## 🔍 Pattern Detection Logic (Decision Tree)

```
Market Data Point
      │
      ├─ Breakout Flag = TRUE?
      │  ├─ YES → Volume Ratio >= 1.2?
      │  │        ├─ YES → Trend = "Bullish"?
      │  │        │         └─ YES → 🔴 BREAKOUT PATTERN ✓
      │  │        └─ NO → Skip
      │  └─ NO → Continue
      │
      ├─ Close > SMA20?
      │  ├─ YES → SMA20 > SMA50?
      │  │        ├─ YES → RSI > 50?
      │  │        │         ├─ YES → Trend = "Bullish"?
      │  │        │         │         └─ YES → 🟠 TREND PULLBACK ✓
      │  │        │         └─ NO → Skip
      │  │        └─ NO → Skip
      │  └─ NO → Continue
      │
      └─ RSI > 50 AND RSI < 70?
         ├─ YES → MACD > Signal Line?
         │        ├─ YES → Trend = "Neutral"?
         │        │         └─ YES → 🔵 MOMENTUM SHIFT ✓
         │        └─ NO → Skip
         └─ NO → Skip
```

---

## 📋 Pattern Storage

```
patterns table
├─ patternName: "Breakout with Volume Confirmation"
├─ patternType: "Bullish"
├─ signalType: "Breakout" / "Trend" / "Momentum"
├─ filterValue: "bullish_breakout" / "trend_pullback" / "momentum_shift"
├─ symbol: "TCS" / "RELIANCE" / etc
├─ score: 75.5 (0-100)
├─ changePercent: 2.35
├─ volumeRatio: 1.45
├─ rsi14: 65.5
├─ sma20: 2150.00
├─ sma50: 2100.00
├─ trendLabel: "Bullish"
├─ breakoutFlag: true
├─ dataDate: 2024-10-04
└─ createdDate: 2024-10-04 18:30:00
```

---

## 🎯 Quick Summary Table

```
┌────────────┬────────────────┬────────┬─────────┬─────────────┐
│ #          │ Pattern Name   │ Lines  │ Signals │ Use Case    │
├────────────┼────────────────┼────────┼─────────┼─────────────┤
│ 1 (🔴)     │ Breakout       │ 93-109 │ 3-4     │ Strong move │
│ 2 (🟠)     │ Trend Pullback │111-127 │ 4       │ Continuation│
│ 3 (🔵)     │ Momentum Shift │129-146 │ 4       │ Early entry │
└────────────┴────────────────┴────────┴─────────┴─────────────┘

Total Patterns: 3
All Types: Bullish/Buy Signals
Processing: 2-Year Historical Data
Output: Top 200 patterns daily
Update: 6:30 PM (Mon-Fri trading days)
```

---

## 🚀 Quick Commands

### View Pattern Types
```java
// In PatternSyncScheduler.java
Lines 93-109:   Pattern #1
Lines 111-127:  Pattern #2
Lines 129-146:  Pattern #3
```

### Query Patterns
```sql
-- Count by type
SELECT filter_value, COUNT(*) 
FROM patterns 
GROUP BY filter_value;

-- Expected output:
-- bullish_breakout   | ~50-100
-- trend_pullback     | ~50-150
-- momentum_shift     | ~50-100
```

---

## ✅ Summary

| Item | Details |
|------|---------|
| **Total Pattern Types** | **3** |
| **All Bullish?** | ✅ YES |
| **Time to Detect** | Real-time (during sync) |
| **Data Period** | 2 years |
| **Frequency** | Daily (6:30 PM) |
| **Max Patterns Stored** | 200 (top by score) |
| **Storage Table** | dmastock.patterns |
| **API Available** | ✅ YES (via PatternDto) |

---

## 📌 Key Takeaways

1. **3 distinct pattern types** are detected
2. **All are bullish signals** (buy opportunities)
3. **Different triggers** (Volume, Moving Avgs, RSI+MACD)
4. **Different scores** (base: 50, 55, 60)
5. **Different risk-reward** (Medium/Low/High risk)
6. **Combined output** of ~200 top patterns daily
7. **Used for** dashboard display and trading signals

🎯 These patterns help identify the **BEST trading opportunities** in the market! 📊

