package com.stockai.apipridection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockai.entity.HistoricalDataEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
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
public class StockAIService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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

    public AnalysisResult analyze(List<HistoricalDataEntity> history) {
        return analyze(history, DEFAULT_MODEL_TYPE);
    }

    public AnalysisResult analyze(List<HistoricalDataEntity> history, ModelType modelType) {
        if (modelType == null) {
            modelType = DEFAULT_MODEL_TYPE;
        }
        try {
            if (modelType == ModelType.XGBOOST || modelType == ModelType.LIGHTGBM) {
                return analyzeViaPython(history, modelType);
            }
            return analyzeWithLogistic(history);
        } catch (Exception ex) {
            return analyzeWithLogistic(history);
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

        var prediction = OBJECT_MAPPER.readTree(jsonOutput);
        double predictedHigh = prediction.has("predicted_high") && !prediction.get("predicted_high").isNull()
                ? prediction.get("predicted_high").asDouble() : history.get(history.size() - 1).getHigh();
        double predictedHighChangePct = prediction.has("predicted_high_change_pct")
                ? prediction.get("predicted_high_change_pct").asDouble() : 0.0;
        double currentClose = prediction.has("current_close")
                ? prediction.get("current_close").asDouble() : history.get(history.size() - 1).getClose();
        
        String direction = predictedHighChangePct > 0.5 ? "Up" : "Down";
        double confidence = Math.min(100.0, Math.abs(predictedHighChangePct) + 50.0);

        return new AnalysisResult(
                predictedHigh,
                Math.max(0.01, currentClose * 0.98),
                currentClose,
                predictedHighChangePct,
                -0.5,
                0.0,
                direction,
                confidence,
                predictedHighChangePct > 0.5 ? confidence : (100.0 - confidence),
                predictedHighChangePct > 0.5 ? (100.0 - confidence) : confidence,
                0,
                0
        );
    }

    private String runPython(Path script, List<String> args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        String pythonCommand = findPythonExecutable();
        command.add(pythonCommand);
        command.add(script.toString());
        command.addAll(args);

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();
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
        if (splitIndex < MIN_TRAINING_SAMPLES || validationCount < MIN_VALIDATION_SAMPLES) {
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