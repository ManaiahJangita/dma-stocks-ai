package com.stockai.apipridection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockai.entity.HistoricalDataEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class StockAIService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final String PYTHON_MODEL_SCRIPT = "python/stock_prediction_model_v2.py";
    private static final String PYTHON_MODEL_FILE = "python/models/stock_prediction_model.joblib";

    public enum ModelType {
        XGBOOST,
        LIGHTGBM,
        LOGISTIC_FALLBACK
    }

    private static final ModelType DEFAULT_MODEL_TYPE = ModelType.XGBOOST;
    private static final int LOOKBACK_DAYS = 20;
    private static final int FEATURE_COUNT = 10;
    private static final int MIN_TRAINING_SAMPLES = 40;
    private static final int MIN_VALIDATION_SAMPLES = 10;
    private static final double TRAINING_FRACTION = 0.8;
    private static final int PYTHON_TIMEOUT_SECONDS = 60;

    public AnalysisResult analyze(List<HistoricalDataEntity> history) {
        return analyze(history, DEFAULT_MODEL_TYPE);
    }

    public AnalysisResult analyze(List<HistoricalDataEntity> history, ModelType modelType) {
        if (modelType == null) {
            modelType = DEFAULT_MODEL_TYPE;
        }
        if (history == null || history.isEmpty()) {
            return null;
        }
        List<HistoricalDataEntity> validHistory = sanitizeHistory(history);
        if (validHistory.size() < 10) {
            return buildFallbackAnalysis(validHistory.isEmpty() ? history : validHistory);
        }
        try {
            AnalysisResult result;
            if (modelType == ModelType.XGBOOST || modelType == ModelType.LIGHTGBM) {
                result = analyzeViaPython(validHistory, modelType);
            } else {
                result = analyzeWithLogistic(validHistory);
            }
            if (result != null) {
                return result;
            }
            return buildFallbackAnalysis(validHistory);
        } catch (Exception ex) {
            log.warn("Python/model analysis failed, using fallback analysis", ex);
            AnalysisResult fallback = analyzeWithLogistic(validHistory);
            return fallback != null ? fallback : buildFallbackAnalysis(validHistory);
        }
    }

    private AnalysisResult analyzeViaPython(List<HistoricalDataEntity> history, ModelType modelType) throws IOException, InterruptedException {
        if (history == null || history.isEmpty()) {
            return null;
        }

        Path projectRoot = Path.of(System.getProperty("user.dir"));
        Path script = projectRoot.resolve(PYTHON_MODEL_SCRIPT);
        Path modelPath = projectRoot.resolve(PYTHON_MODEL_FILE);

        if (!Files.exists(script)) {
            throw new IOException("Python model script not found: " + script);
        }

        Path csvFile = Files.createTempFile("stock_prediction_input", ".csv");
        try {
            Files.writeString(csvFile, buildCsv(history), StandardCharsets.UTF_8);

            if (!Files.exists(modelPath)) {
                runPython(script, List.of("train", "--input", csvFile.toString(), "--model-name", modelType.name().toLowerCase(Locale.ROOT)));
            }

            String jsonOutput = runPython(script, List.of(
                    "predict",
                    "--input", csvFile.toString(),
                    "--model-path", modelPath.toString(),
                    "--model-name", modelType.name().toLowerCase(Locale.ROOT)
            ));

            if (jsonOutput == null || jsonOutput.isBlank()) {
                throw new IOException("Python model returned empty output");
            }

            String sanitizedJsonOutput = extractJsonPayload(jsonOutput);

            try {
                var prediction = OBJECT_MAPPER.readTree(sanitizedJsonOutput);
                HistoricalDataEntity latest = history.get(history.size() - 1);

                double currentClose = prediction.has("current_close")
                        ? prediction.get("current_close").asDouble(latest.getClose()) : latest.getClose();
                double predictedClose = prediction.has("predicted_close")
                        ? prediction.get("predicted_close").asDouble(currentClose) : currentClose;
                double predictedHigh = prediction.has("predicted_high")
                        ? prediction.get("predicted_high").asDouble(latest.getHigh()) : latest.getHigh();
                double predictedLow = prediction.has("predicted_low")
                        ? prediction.get("predicted_low").asDouble(latest.getLow()) : latest.getLow();

                double predictedCloseChangePct = prediction.has("predicted_close_change_pct")
                        ? prediction.get("predicted_close_change_pct").asDouble(percentage(predictedClose, currentClose))
                        : percentage(predictedClose, currentClose);
                double predictedHighChangePct = prediction.has("predicted_high_change_pct")
                        ? prediction.get("predicted_high_change_pct").asDouble(percentage(predictedHigh, currentClose))
                        : percentage(predictedHigh, currentClose);
                double predictedLowChangePct = prediction.has("predicted_low_change_pct")
                        ? prediction.get("predicted_low_change_pct").asDouble(percentage(predictedLow, currentClose))
                        : percentage(predictedLow, currentClose);

                String direction = prediction.has("direction")
                        ? prediction.get("direction").asText("Down") : (predictedCloseChangePct >= 0 ? "Up" : "Down");

                String signal = prediction.has("signal") ? prediction.get("signal").asText("HOLD") : "HOLD";
                if ("HOLD".equalsIgnoreCase(signal)) {
                    direction = "Hold";
                }

                double upProbabilityPercent = prediction.has("probability_up")
                        ? prediction.get("probability_up").asDouble(0.5) * 100.0
                        : 50.0;
                double downProbabilityPercent = prediction.has("probability_down")
                        ? prediction.get("probability_down").asDouble(1.0 - upProbabilityPercent / 100.0) * 100.0
                        : 100.0 - upProbabilityPercent;

                double confidencePercent = prediction.has("confidence_percent")
                        ? prediction.get("confidence_percent").asDouble(Math.max(upProbabilityPercent, downProbabilityPercent))
                        : Math.max(upProbabilityPercent, downProbabilityPercent);

                int validationSamples = 0;
                int correctValidationPredictions = 0;
                if (prediction.has("validation_metrics") && prediction.get("validation_metrics").isObject()) {
                    var validation = prediction.get("validation_metrics");
                    validationSamples = validation.has("validation_samples")
                            ? validation.get("validation_samples").asInt(0)
                            : (validation.has("folds") ? validation.get("folds").asInt(0) : 0);
                    correctValidationPredictions = validation.has("correct_validation_predictions")
                            ? validation.get("correct_validation_predictions").asInt(0)
                            : 0;
                }

                log.info("[StockAI] Python prediction used: symbol={} signal={} direction={} predictedClose={} currentClose={} confidence={} upProbability={} downProbability={}",
                        prediction.has("symbol") ? prediction.get("symbol").asText("") : latest.getSymbol(),
                        signal, direction, predictedClose, currentClose, confidencePercent,
                        upProbabilityPercent, downProbabilityPercent);

                return new AnalysisResult(
                        predictedHigh,
                        predictedLow,
                        predictedClose,
                        predictedHighChangePct,
                        predictedLowChangePct,
                        predictedCloseChangePct,
                        direction,
                        confidencePercent,
                        upProbabilityPercent,
                        downProbabilityPercent,
                        validationSamples,
                        correctValidationPredictions
                );
            } catch (Exception parseEx) {
                log.warn("Failed to parse Python prediction JSON: {}", sanitizedJsonOutput, parseEx);
                throw parseEx;
            }
        } finally {
            try {
                Files.deleteIfExists(csvFile);
            } catch (IOException cleanupException) {
                log.warn("Failed to delete temporary prediction CSV: {}", csvFile, cleanupException);
            }
        }
    }

    private String extractJsonPayload(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return rawOutput;
        }
        String output = rawOutput.trim();
        int start = output.lastIndexOf('{');
        int end = output.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return output.substring(start, end + 1);
        }
        return output;
    }

    private String runPython(Path script, List<String> args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        String pythonCommand = findPythonExecutable();
        command.add(pythonCommand);
        command.add(script.toString());
        command.addAll(args);

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

        boolean finished = process.waitFor(PYTHON_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Python process timed out after " + PYTHON_TIMEOUT_SECONDS + " seconds");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("Python process exited with code " + exitCode + ": " + output);
        }
        return output.toString().trim();
    }

    private String findPythonExecutable() {
        String[] candidates = {"python", "python3"};
        for (String candidate : candidates) {
            try {
                Process process = new ProcessBuilder(candidate, "--version").start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return candidate;
                }
            } catch (Exception ignored) {
                // continue to next candidate
            }
        }
        throw new IllegalStateException("No python executable found in PATH");
    }

    private List<HistoricalDataEntity> sanitizeHistory(List<HistoricalDataEntity> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<HistoricalDataEntity> valid = new ArrayList<>();
        for (HistoricalDataEntity row : history) {
            if (row == null) {
                continue;
            }
            if (row.getCreatedDate() == null || row.getSymbol() == null || row.getSymbol().isBlank()) {
                continue;
            }
            if (!Double.isFinite(row.getOpen()) || !Double.isFinite(row.getHigh()) || !Double.isFinite(row.getLow()) || !Double.isFinite(row.getClose()) || !Double.isFinite(row.getVolume())) {
                continue;
            }
            if (row.getOpen() <= 0 || row.getHigh() <= 0 || row.getLow() <= 0 || row.getClose() <= 0) {
                continue;
            }
            valid.add(row);
        }
        return valid;
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

    public int countValidPredictionSamples(List<HistoricalDataEntity> history) {
        if (history == null || history.isEmpty()) {
            return 0;
        }

        int validSampleCount = 0;
        for (int index = LOOKBACK_DAYS; index < history.size() - 1; index++) {
            double[] features = features(history, index);
            HistoricalDataEntity current = history.get(index);
            HistoricalDataEntity next = history.get(index + 1);
            if (features == null || current.getClose() <= 0 || next.getClose() <= 0) {
                continue;
            }
            validSampleCount++;
        }
        return validSampleCount;
    }

    private AnalysisResult analyzeWithLogistic(List<HistoricalDataEntity> history) {
        List<Sample> samples = new ArrayList<>();
        for (int index = LOOKBACK_DAYS; index < history.size() - 1; index++) {
            double[] features = features(history, index);
            HistoricalDataEntity current = history.get(index);
            HistoricalDataEntity next = history.get(index + 1);
            if (features == null || current.getClose() <= 0 || next.getClose() <= 0) {
                continue;
            }

            double close = current.getClose();
            samples.add(new Sample(features, next.getClose() > close,
                    percentage(next.getHigh(), close),
                    percentage(next.getLow(), close),
                    percentage(next.getClose(), close)));
        }

        int splitIndex = (int) (samples.size() * TRAINING_FRACTION);
        int validationCount = samples.size() - splitIndex;
        log.debug("Model validation debug: rowCount={}, validSampleCount={}, splitIndex={}, validationCount={}, minTrainingSamples={}, minValidationSamples={}",
                history.size(), samples.size(), splitIndex, validationCount, MIN_TRAINING_SAMPLES, MIN_VALIDATION_SAMPLES);
        if (splitIndex < MIN_TRAINING_SAMPLES || validationCount < MIN_VALIDATION_SAMPLES) {
            log.debug("Skipping model analysis due to insufficient samples. rowCount={}, validSampleCount={}, splitIndex={}, validationCount={}",
                    history.size(), samples.size(), splitIndex, validationCount);
            return null;
        }

        double[][] trainingFeatures = samples.subList(0, splitIndex).stream()
                .map(Sample::features).toArray(double[][]::new);
        Standardizer validationStandardizer = Standardizer.fit(trainingFeatures);
        double[][] normalizedTraining = validationStandardizer.transform(trainingFeatures);
        double[] trainingLabels = samples.subList(0, splitIndex).stream()
                .mapToDouble(sample -> sample.up ? 1.0 : 0.0).toArray();

        LogisticModel validationModel = LogisticModel.train(normalizedTraining, trainingLabels);
        int correctPredictions = 0;
        for (int index = splitIndex; index < samples.size(); index++) {
            Sample sample = samples.get(index);
            double upProbability = validationModel.probability(validationStandardizer.transform(sample.features));
            if ((upProbability >= 0.5) == sample.up) {
                correctPredictions++;
            }
        }

        double[][] allFeatures = samples.stream().map(Sample::features).toArray(double[][]::new);
        Standardizer finalStandardizer = Standardizer.fit(allFeatures);
        double[][] normalizedAll = finalStandardizer.transform(allFeatures);
        double[] allLabels = samples.stream().mapToDouble(sample -> sample.up ? 1.0 : 0.0).toArray();
        double[][] priceTargets = samples.stream()
                .map(sample -> new double[]{sample.highChangePercent, sample.lowChangePercent, sample.closeChangePercent})
                .toArray(double[][]::new);

        double[] latestFeatures = features(history, history.size() - 1);
        if (latestFeatures == null) {
            log.debug("Skipping model analysis because latest feature vector is null. rowCount={}, validSampleCount={}", history.size(), samples.size());
            return null;
        }

        LogisticModel finalModel = LogisticModel.train(normalizedAll, allLabels);
        LinearModel priceModel = LinearModel.train(normalizedAll, priceTargets);
        double upProbability = finalModel.probability(finalStandardizer.transform(latestFeatures));
        double[] predictedChanges = priceModel.predict(finalStandardizer.transform(latestFeatures));

        double currentClose = history.get(history.size() - 1).getClose();
        double predictedClose = positivePrice(currentClose, predictedChanges[2]);
        double predictedHigh = Math.max(predictedClose, positivePrice(currentClose, predictedChanges[0]));
        double predictedLow = Math.min(predictedClose, positivePrice(currentClose, predictedChanges[1]));

        return new AnalysisResult(
                predictedHigh,
                predictedLow,
                predictedClose,
                percentage(predictedHigh, currentClose),
                percentage(predictedLow, currentClose),
                percentage(predictedClose, currentClose),
                upProbability >= 0.5 ? "Up" : "Down",
                Math.max(upProbability, 1.0 - upProbability) * 100.0,
                upProbability * 100.0,
                (1.0 - upProbability) * 100.0,
                validationCount,
                correctPredictions);
    }

    private AnalysisResult buildFallbackAnalysis(List<HistoricalDataEntity> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }

        HistoricalDataEntity latest = history.get(history.size() - 1);
        HistoricalDataEntity previous = history.size() > 1 ? history.get(history.size() - 2) : latest;
        double currentClose = latest.getClose();
        double previousClose = previous.getClose();
        double trendPercent = previousClose > 0 ? percentage(currentClose, previousClose) : 0.0;
        double predictedClose = currentClose * (1.0 + (trendPercent / 100.0));
        double predictedHigh = Math.max(currentClose, predictedClose) * 1.01;
        double predictedLow = Math.min(currentClose, predictedClose) * 0.99;
        String direction = trendPercent >= 0.0 ? "Up" : "Down";
        double confidence = Math.min(86.0, 50.0 + Math.abs(trendPercent) * 2.0);
        double upProbability = direction.equals("Up") ? confidence / 100.0 : (100.0 - confidence) / 100.0;
        double downProbability = 1.0 - upProbability;

        return new AnalysisResult(
                predictedHigh,
                predictedLow,
                predictedClose,
                percentage(predictedHigh, currentClose),
                percentage(predictedLow, currentClose),
                percentage(predictedClose, currentClose),
                direction,
                confidence,
                upProbability * 100.0,
                downProbability * 100.0,
                0,
                0
        );
    }

    private static double[] features(List<HistoricalDataEntity> history, int index) {
        if (index < LOOKBACK_DAYS || index >= history.size()) {
            return null;
        }
        HistoricalDataEntity current = history.get(index);
        HistoricalDataEntity previous = history.get(index - 1);
        HistoricalDataEntity dayFive = history.get(Math.max(0, index - 5));
        HistoricalDataEntity dayTen = history.get(Math.max(0, index - 10));
        HistoricalDataEntity dayTwenty = history.get(Math.max(0, index - LOOKBACK_DAYS));
        double close = current.getClose();
        double open = current.getOpen();
        if (close <= 0 || open <= 0 || previous.getClose() <= 0) {
            return null;
        }

        double averageVolume = 0;
        for (int day = index - LOOKBACK_DAYS; day < index; day++) {
            averageVolume += history.get(day).getVolume();
        }
        averageVolume /= LOOKBACK_DAYS;

        double shortMomentum = percentage(close, previous.getClose());
        double mediumMomentum = percentage(close, dayFive.getClose());
        double longMomentum = percentage(close, dayTwenty.getClose());
        double rangePercent = (current.getHigh() - current.getLow()) / close * 100.0;
        double openGap = percentage(open, previous.getClose());
        double closeGap = percentage(close, open);
        double volumeSignal = averageVolume > 0 ? current.getVolume() / averageVolume - 1.0 : 0.0;
        double volatility = Math.abs(shortMomentum) + Math.abs(mediumMomentum);
        double trendStrength = percentage(current.getHigh(), dayTen.getClose()) + percentage(current.getLow(), dayTen.getClose());

        return new double[]{
                shortMomentum,
                mediumMomentum,
                longMomentum,
                rangePercent,
                openGap,
                closeGap,
                volumeSignal,
                volatility,
                trendStrength,
                percentage(current.getClose(), dayTwenty.getClose())
        };
    }

    private static double percentage(double value, double base) {
        return base == 0 ? 0 : (value / base - 1.0) * 100.0;
    }

    private static double positivePrice(double base, double changePercent) {
        return Math.max(0.01, base * (1.0 + changePercent / 100.0));
    }

    @Getter
    @AllArgsConstructor
    public static class AnalysisResult {
        private final double predictedHigh;
        private final double predictedLow;
        private final double predictedClose;
        private final double predictedHighPercentage;
        private final double predictedLowPercentage;
        private final double predictedClosePercentage;
        private final String direction;
        private final double confidencePercent;
        private final double upProbabilityPercent;
        private final double downProbabilityPercent;
        private final int validationSamples;
        private final int correctValidationPredictions;
    }

    private static final class Sample {
        private final double[] features;
        private final boolean up;
        private final double highChangePercent;
        private final double lowChangePercent;
        private final double closeChangePercent;

        private Sample(double[] features, boolean up, double highChangePercent,
                       double lowChangePercent, double closeChangePercent) {
            this.features = features;
            this.up = up;
            this.highChangePercent = highChangePercent;
            this.lowChangePercent = lowChangePercent;
            this.closeChangePercent = closeChangePercent;
        }

        private double[] features() {
            return features;
        }
    }

    private static final class Standardizer {
        private final double[] means;
        private final double[] scales;

        private Standardizer(double[] means, double[] scales) {
            this.means = means;
            this.scales = scales;
        }

        private static Standardizer fit(double[][] values) {
            double[] means = new double[FEATURE_COUNT];
            double[] scales = new double[FEATURE_COUNT];
            for (double[] row : values) {
                for (int feature = 0; feature < FEATURE_COUNT; feature++) {
                    means[feature] += row[feature];
                }
            }
            for (int feature = 0; feature < FEATURE_COUNT; feature++) {
                means[feature] /= values.length;
            }
            for (double[] row : values) {
                for (int feature = 0; feature < FEATURE_COUNT; feature++) {
                    double difference = row[feature] - means[feature];
                    scales[feature] += difference * difference;
                }
            }
            for (int feature = 0; feature < FEATURE_COUNT; feature++) {
                scales[feature] = Math.sqrt(scales[feature] / values.length);
                if (scales[feature] < 1e-9) {
                    scales[feature] = 1.0;
                }
            }
            return new Standardizer(means, scales);
        }

        private double[][] transform(double[][] values) {
            return Arrays.stream(values).map(this::transform).toArray(double[][]::new);
        }

        private double[] transform(double[] values) {
            double[] normalized = new double[FEATURE_COUNT];
            for (int feature = 0; feature < FEATURE_COUNT; feature++) {
                normalized[feature] = (values[feature] - means[feature]) / scales[feature];
            }
            return normalized;
        }
    }

    private static final class LogisticModel {
        private final double[] weights;

        private LogisticModel(double[] weights) {
            this.weights = weights;
        }

        private static LogisticModel train(double[][] features, double[] labels) {
            double[] weights = new double[FEATURE_COUNT + 1];
            double learningRate = 0.03;
            for (int epoch = 0; epoch < 1200; epoch++) {
                double[] gradient = new double[weights.length];
                for (int row = 0; row < features.length; row++) {
                    double error = sigmoid(score(weights, features[row])) - labels[row];
                    gradient[0] += error;
                    for (int feature = 0; feature < FEATURE_COUNT; feature++) {
                        gradient[feature + 1] += error * features[row][feature];
                    }
                }
                weights[0] -= learningRate * gradient[0] / features.length;
                for (int feature = 1; feature < weights.length; feature++) {
                    weights[feature] -= learningRate * (gradient[feature] / features.length + 0.001 * weights[feature]);
                }
            }
            return new LogisticModel(weights);
        }

        private double probability(double[] features) {
            return sigmoid(score(weights, features));
        }

        private static double score(double[] weights, double[] features) {
            double result = weights[0];
            for (int feature = 0; feature < FEATURE_COUNT; feature++) {
                result += weights[feature + 1] * features[feature];
            }
            return result;
        }

        private static double sigmoid(double value) {
            double bounded = Math.max(-35.0, Math.min(35.0, value));
            return 1.0 / (1.0 + Math.exp(-bounded));
        }
    }

    private static final class LinearModel {
        private final double[][] weights;

        private LinearModel(double[][] weights) {
            this.weights = weights;
        }

        private static LinearModel train(double[][] features, double[][] targets) {
            int targetCount = targets[0].length;
            double[][] weights = new double[targetCount][FEATURE_COUNT + 1];
            double learningRate = 0.01;
            for (int epoch = 0; epoch < 1600; epoch++) {
                double[][] gradient = new double[targetCount][FEATURE_COUNT + 1];
                for (int row = 0; row < features.length; row++) {
                    for (int target = 0; target < targetCount; target++) {
                        double error = predict(weights[target], features[row]) - targets[row][target];
                        gradient[target][0] += error;
                        for (int feature = 0; feature < FEATURE_COUNT; feature++) {
                            gradient[target][feature + 1] += error * features[row][feature];
                        }
                    }
                }
                for (int target = 0; target < targetCount; target++) {
                    weights[target][0] -= learningRate * gradient[target][0] / features.length;
                    for (int feature = 1; feature < weights[target].length; feature++) {
                        weights[target][feature] -= learningRate * (gradient[target][feature] / features.length
                                + 0.001 * weights[target][feature]);
                    }
                }
            }
            return new LinearModel(weights);
        }

        private double[] predict(double[] features) {
            double[] result = new double[weights.length];
            for (int target = 0; target < weights.length; target++) {
                result[target] = predict(weights[target], features);
            }
            return result;
        }

        private static double predict(double[] weights, double[] features) {
            double result = weights[0];
            for (int feature = 0; feature < FEATURE_COUNT; feature++) {
                result += weights[feature + 1] * features[feature];
            }
            return result;
        }
    }
}

