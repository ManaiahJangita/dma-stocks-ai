# AI Stock Analyzer

Starter Spring Boot project for an Indian stock-analysis application.

## Stack

- Java 21
- Spring Boot 3.5.x
- PostgreSQL
- Flyway
- Angel One SmartAPI integration
- Spring AI/OpenAI integration (optional)
- REST API

## 1. Start PostgreSQL

Requires Docker Desktop:

```bash
docker compose up -d
```

Or use your own PostgreSQL and update `application.yml`.

## 2. Configure Angel One

Create your SmartAPI credentials and set environment variables:

Windows PowerShell:

```powershell
$env:ANGELONE_API_KEY="your-api-key"
$env:ANGELONE_CLIENT_CODE="your-client-code"
$env:ANGELONE_PASSWORD="your-pin"
$env:ANGELONE_TOTP="your-totp"
```

Do not commit credentials.

## 3. Optional AI configuration

```powershell
$env:OPENAI_API_KEY="your-key"
```

The application starts without an OpenAI key. AI explanation endpoint will return a clear configuration error until the key is supplied.

## 4. Run

```bash
mvn spring-boot:run
```

Health:

```text
GET http://localhost:8080/api/health
```

## 5. Load a historical stock

The first version exposes a provider-neutral endpoint:

```text
POST /api/market-data/historical
```

Example body:

```json
{
  "symbolToken": "2885",
  "exchange": "NSE",
  "fromDate": "2025-01-01",
  "toDate": "2026-01-01"
}
```

The service obtains an Angel One auth token using the configured credentials, calls the historical candle endpoint, and stores the candles in PostgreSQL.

> Angel One authentication/TOTP and API rules can change. Verify your current SmartAPI account/API documentation if authentication differs from your account setup.

## Database

Flyway creates:

- stocks
- stock_prices
- technical_indicators
- predictions
- stock_analysis

The stock token is currently supplied by the API request so that you can test one instrument first. A later phase can import Angel One's instrument master and automatically map symbols to tokens.

## Next phases

1. Instrument master ingestion
2. 200+ NSE stocks
3. RSI/SMA/EMA/MACD/Bollinger calculations
4. Backtesting
5. ML prediction service
6. News sentiment
7. Spring AI explanation
8. React + TypeScript dashboard
