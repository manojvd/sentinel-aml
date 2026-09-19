package com.sentinel.aml.anomaly;

/**
 * Per-column z-score standardization, fit once on a training matrix and reused for
 * scoring new points. Isolation Forest doesn't strictly need this, but K-Means does
 * (raw Euclidean distance is dominated by whichever feature has the largest units,
 * e.g. amount in currency units vs. a 0/1 flag) — both implementations share it so a
 * training set can be swapped between algorithms without behavior surprises.
 */
final class FeatureScaler {

    private final double[] mean;
    private final double[] std;

    private FeatureScaler(double[] mean, double[] std) {
        this.mean = mean;
        this.std = std;
    }

    static FeatureScaler fit(double[][] data) {
        int n = data.length;
        int dims = data[0].length;
        double[] mean = new double[dims];
        double[] std = new double[dims];

        for (double[] row : data) {
            for (int j = 0; j < dims; j++) {
                mean[j] += row[j];
            }
        }
        for (int j = 0; j < dims; j++) {
            mean[j] /= n;
        }

        for (double[] row : data) {
            for (int j = 0; j < dims; j++) {
                double d = row[j] - mean[j];
                std[j] += d * d;
            }
        }
        for (int j = 0; j < dims; j++) {
            std[j] = Math.sqrt(std[j] / n);
            if (std[j] < 1e-9) {
                std[j] = 1.0; // constant column: leave it as a shift-only, non-blowing-up dimension
            }
        }

        return new FeatureScaler(mean, std);
    }

    double[] transform(double[] x) {
        double[] out = new double[x.length];
        for (int j = 0; j < x.length; j++) {
            out[j] = (x[j] - mean[j]) / std[j];
        }
        return out;
    }

    double[][] transform(double[][] data) {
        double[][] out = new double[data.length][];
        for (int i = 0; i < data.length; i++) {
            out[i] = transform(data[i]);
        }
        return out;
    }
}
