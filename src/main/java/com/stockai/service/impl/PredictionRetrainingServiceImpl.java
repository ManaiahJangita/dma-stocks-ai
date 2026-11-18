package com.stockai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockai.entity.HistoricalDataEntity;
import com.stockai.entity.PredictionComparisonEntity;
import com.stockai.repository.HistoricalDataRepository;
import com.stockai.repository.PredictionComparisonRepository;
import com.stockai.service.interf.RetrainingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PredictionRetrainingServiceImpl implements RetrainingService {

    private final PredictionComparisonRepository predictionComparisonRepository;
    private final HistoricalDataRepository historicalDataRepository;

    @Value("${prediction.alert.threshold-percent:90.0}")
    private double alertThresholdPercent;

    @Value("${prediction.retraining.enabled:true}")
    private boolean retrainingEnabled;

    @Value("${prediction.retraining.lookback-days:1095}")
    private int lookbackDays;

    @Value("${prediction.model.backup.keep-count:7}")
    private int modelBackupKeepCount;

    @Value("${prediction.model.name:xgboost}")
    private String predictionModelName;

    private static final String RETRAINING_TEMP_DIR = "temp/retraining";

    public PredictionRetrainingServiceImpl(
            PredictionComparisonRepository predictionComparisonRepository,
            HistoricalDataRepository historicalDataRepository) {
        this.predictionComparisonRepository = predictionComparisonRepository;
        this.historicalDataRepository = historicalDataRepository;
    }

    @Override
    public void checkAndTriggerModelRetraining(LocalDate predictionDate) {
        if (!retrainingEnabled) {
            log.info("[Retraining] Prediction retraining is disabled via configuration");
            return;
        }
        if (predictionDate == null) {
            predictionDate = LocalDate.now().minusDays(1);
            log.info("[Retraining] predictionDate was null, defaulting to previous day={}", predictionDate);
        }

        List<PredictionComparisonEntity> comparisons = predictionComparisonRepository.findByPredictionDate(predictionDate);
        log.info("[Retraining] Evaluation started for predictionDate={}. Comparison rows fetched={}", predictionDate,
                comparisons == null ? 0 : comparisons.size());
        if (comparisons == null || comparisons.isEmpty()) {
            log.info("[Retraining] No comparison data found for {}. Skipping retraining evaluation.", predictionDate);
            return;
        }

        long matched = comparisons.stream().filter(item -> Boolean.TRUE.equals(item.getDirectionMatched())).count();
        double accuracyPercent = (matched * 100.0) / comparisons.size();
        log.info("[Retraining] Accuracy check for {} => {}% (matched {} of {})", predictionDate, accuracyPercent, matched, comparisons.size());

        if (accuracyPercent >= alertThresholdPercent) {
            log.info("[Retraining] Model accuracy {}% is at/above alert threshold {}%. No retraining triggered.", accuracyPercent, alertThresholdPercent);
            return;
        }

        log.warn("[Retraining] Accuracy {}% is below threshold {}%. Triggering automatic retraining for {}.",
                accuracyPercent, alertThresholdPercent, predictionDate);
        retrainModelFromRecentHistory();
    }

    public boolean retrainModelFromRecentHistory() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(Math.max(30, lookbackDays));
        log.info("[Retraining] Starting model retraining flow. lookbackDays={}, effectiveStartDate={}, endDate={}",
                lookbackDays, startDate, endDate);

        List<HistoricalDataEntity> history = historicalDataRepository.findAll(startDate, endDate);
        log.info("[Retraining] Repository returned {} raw historical rows for {} to {}", history == null ? 0 : history.size(), startDate, endDate);

        List<HistoricalDataEntity> validHistory = sanitizeHistory(history);
        log.info("[Retraining] Sanitized historical data. validRows={} from rawRows={}",
                validHistory == null ? 0 : validHistory.size(), history == null ? 0 : history.size());

        if (validHistory == null || validHistory.isEmpty()) {
            log.warn("[Retraining] No valid historical rows available for retraining range {} to {}.", startDate, endDate);
            return false;
        }
        if (validHistory.size() < 30) {
            log.warn("[Retraining] Not enough valid data to retrain. Required >=30 rows, found {} in {} to {}.",
                    validHistory.size(), startDate, endDate);
            return false;
        }

        if (!isPythonAvailable()) {
            log.warn("[Retraining] Python executable not found; skipping automatic retraining.");
            return false;
        }

        Path projectRoot = Path.of(System.getProperty("user.dir"));
        Path script = projectRoot.resolve("python/stock_prediction_model_v2.py");
        log.info("[Retraining] Retraining script path={}", script);
        if (!Files.exists(script)) {
            log.warn("[Retraining] Python retraining script not found at {}", script);
            return false;
        }

        Path modelDir = projectRoot.resolve("python/models");
        Path latestModel = modelDir.resolve("stock_prediction_model.joblib");
        Path tempDir = projectRoot.resolve(RETRAINING_TEMP_DIR);
        Path csvFile = null;

        try {
            Files.createDirectories(modelDir);
            Files.createDirectories(tempDir);

            Path versionedModel = modelDir.resolve("stock_prediction_model_" + LocalDate.now() + ".joblib");
            if (Files.exists(latestModel)) {
                log.info("[Retraining] Backing up existing model {} to {} before retraining", latestModel, versionedModel);
                Files.copy(latestModel, versionedModel, StandardCopyOption.REPLACE_EXISTING);
            } else {
                log.info("[Retraining] No existing model found at {}. Starting fresh model training.", latestModel);
            }

            csvFile = tempDir.resolve("stock_retrain_input_" + System.currentTimeMillis() + ".csv");
            String csvContent = buildCsv(validHistory);
            Files.writeString(csvFile, csvContent, StandardCharsets.UTF_8);
            log.info("[Retraining] Prepared CSV file {} with {} historical rows for model training", csvFile, validHistory.size());

            LocalDateTime startTime = LocalDateTime.now();
            String output = runPython(script, List.of(
                    "train",
                    "--input", csvFile.toString(),
                    "--model-name", predictionModelName
            ));
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);

            if (output == null || output.isBlank()) {
                throw new IOException("Python retraining returned empty output");
            }

            log.info("[Retraining] Python retraining finished in {} seconds. Output summary={}", duration.getSeconds(), output.trim());
            writeLatestModelMetadata(output);
            pruneOldModelBackups();
            log.info("[Retraining] Model retraining succeeded and metadata was updated.");
            return true;
        } catch (Exception e) {
            log.error("[Retraining] Automatic retraining failed for range {} to {}", startDate, endDate, e);
            return false;
        } finally {
            if (csvFile != null) {
                try {
                    Files.deleteIfExists(csvFile);
                    log.info("[Retraining] Deleted temporary retraining CSV: {}", csvFile);
                } catch (IOException e) {
                    log.warn("[Retraining] Failed to delete temporary retraining CSV {}: {}", csvFile, e.getMessage());
                }
            }
            try {
                Files.list(tempDir)
                        .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".csv"))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                                log.warn("[Retraining] Could not remove stale file {} from temp directory", path);
                            }
                        });
            } catch (IOException e) {
                log.warn("[Retraining] Failed to clean stale training CSV files in {}: {}", tempDir, e.getMessage());
            }
        }
    }

    private List<HistoricalDataEntity> sanitizeHistory(List<HistoricalDataEntity> history) {
        if (history == null || history.isEmpty()) {
            log.info("[Retraining] sanitizeHistory received null or empty history input");
            return List.of();
        }

        List<HistoricalDataEntity> validRows = new ArrayList<>();
        for (HistoricalDataEntity row : history) {
            if (row == null) {
                continue;
            }
            if (row.getSymbol() == null || row.getSymbol().isBlank()) {
                continue;
            }
            if (row.getCreatedDate() == null) {
                continue;
            }
            if (!isValidNumber(row.getOpen()) || !isValidNumber(row.getHigh()) || !isValidNumber(row.getLow()) || !isValidNumber(row.getClose()) || !isValidNumber((double) row.getVolume())) {
                continue;
            }
            if (row.getOpen() <= 0 || row.getHigh() <= 0 || row.getLow() <= 0 || row.getClose() <= 0) {
                continue;
            }
            validRows.add(row);
        }
        log.debug("[Retraining] sanitizeHistory completed: {} valid rows remain from {} source rows", validRows.size(), history.size());
        return validRows;
    }

    private boolean isValidNumber(double value) {
        return Double.isFinite(value);
    }

    private String buildCsv(List<HistoricalDataEntity> history) {
        StringBuilder builder = new StringBuilder();
        builder.append("symbol,created_date,open,high,low,close,volume").append(System.lineSeparator());
        for (HistoricalDataEntity row : history) {
            if (row == null) {
                continue;
            }
            builder.append(row.getSymbol())
                    .append(',')
                    .append(row.getCreatedDate())
                    .append(',')
                    .append(row.getOpen())
                    .append(',')
                    .append(row.getHigh())
                    .append(',')
                    .append(row.getLow())
                    .append(',')
                    .append(row.getClose())
                    .append(',')
                    .append(row.getVolume())
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String runPython(Path script, List<String> args) throws IOException, InterruptedException {
        if (script == null || !Files.exists(script)) {
            throw new IOException("Python retraining script not found: " + script);
        }

        List<String> command = new ArrayList<>();
        command.add(findPythonExecutable());
        command.add(script.toString());
        command.addAll(args);
        log.info("[Retraining] Executing retraining command: {}", command);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("LOKY_MAX_CPU_COUNT", "1");
        processBuilder.environment().put("PYTHONWARNINGS", "ignore");

        Process process = processBuilder
                .redirectErrorStream(true)
                .start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        boolean finished = process.waitFor(1, TimeUnit.MINUTES);
        if (!finished) {
            process.destroy();
            throw new IOException("Python retraining process did not finish in time");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("Python retraining exited with code " + exitCode + ": " + output);
        }
        log.info("[Retraining] Python retraining process exited successfully with code {}. Output: {}",
                exitCode, output.toString().trim());
        return output.toString().trim();
    }

    private String findPythonExecutable() {
        String[] candidates = {"py", "python", "python3"};
        for (String candidate : candidates) {
            try {
                Process process = new ProcessBuilder(candidate, "--version").start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    log.info("Found Python executable: {}", candidate);
                    return candidate;
                }
            } catch (Exception ignored) {
                // continue to next candidate
            }
        }
        throw new IllegalStateException("No python executable found in PATH");
    }

    private void writeLatestModelMetadata(String output) throws IOException {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        Path metaFile = projectRoot.resolve("python/models/latest_model_metadata.json");

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Map<String, Object> metadata = Map.of(
                "updated_at", LocalDate.now(),
                "status", "trained",
                "output", output.trim().replace("\n", " ")
        );
        String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata);

        Files.writeString(metaFile, content, StandardCharsets.UTF_8);
        log.info("[Retraining] Updated model metadata file: {}", metaFile);
    }

    private boolean isPythonAvailable() {
        String[] candidates = {"py", "python", "python3"};
        for (String candidate : candidates) {
            try {
                Process process = new ProcessBuilder(candidate, "--version").start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return true;
                }
            } catch (Exception ignored) {
                // continue to next candidate
            }
        }
        return false;
    }

    private void pruneOldModelBackups() {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        Path modelDir = projectRoot.resolve("python/models");

        try {
            List<Path> backupFiles = Files.list(modelDir)
                    .filter(path -> Files.isRegularFile(path)
                            && path.getFileName().toString().startsWith("stock_prediction_model_")
                            && path.getFileName().toString().endsWith(".joblib"))
                    .sorted(Comparator.comparingLong(path -> {
                        try {
                            return -Files.getLastModifiedTime(path).toMillis();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .toList();

            if (backupFiles.size() <= modelBackupKeepCount) {
                log.info("[Retraining] Backup retention check passed. Current backups={}, keepCount={}", backupFiles.size(), modelBackupKeepCount);
                return;
            }

            for (int i = modelBackupKeepCount; i < backupFiles.size(); i++) {
                Path path = backupFiles.get(i);
                try {
                    Files.delete(path);
                    log.info("[Retraining] Deleted old model backup: {}", path);
                } catch (IOException e) {
                    log.warn("[Retraining] Failed to delete old model backup {}: {}", path, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("[Retraining] Failed to list or delete old model backups in directory {}: {}", modelDir, e.getMessage());
        }
    }
}
