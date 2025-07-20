package com.meryt.demographics.math;

import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

public class FunkyBetaDistributionTest {

    @Test
    public void testOnce() {
        testDistribution(0.55, 0.6, 0.8, 0.9, false);
    }

    public void testExpectedDistribution() {
        boolean doPrint = false;
        testDistribution(0.5, 0.9, 0.8, 1.0, false);
        testDistribution(0.6, 1.0, 0.8, 1.0, false);
        testDistribution(0.5, 0.9, 0.4, 0.6, false);
        testDistribution(0.5, 0.9, 0.2, 0.6, false);
        testDistribution(0.6, 0.8, 0.2, 0.6, false);
        testDistribution(0.7, 0.9, 0.2, 0.6, false);
        testDistribution(0.7, 0.8, 0.2, 0.6, false);
        testDistribution(0.7, 0.75, 0.2, 0.6, false);
        testDistribution(0.7, 0.75, 0.8, 1.0, false);
        testDistribution(0.7, 0.75, 0.9, 1.0, false);
        testDistribution(0.8, 0.85, 0.9, 1.0, false);
        testDistribution(0.6, 0.65, 0.9, 1.0, false);
        testDistribution(0.5, 0.55, 0.9, 1.0, false);
        testDistribution(0.55, 0.6, 0.9, 1.0, false);
        testDistribution(0.55, 0.6, 0.8, 1.0, false);
        testDistribution(0.55, 0.6, 0.8, 0.9, false);
    }

    private void testDistribution(double ga, double bu, double zo, double meu, boolean print) {
        FunkyBetaDistribution beta = new FunkyBetaDistribution(2.5, 5, ga, bu, zo, meu);
        Map<Double, Integer> results = new TreeMap<>();
        for (int i = 0; i < 1_000; i++) {
            double sample = Math.round(beta.sample() * 10.0) / 10.0;
            if (!results.containsKey(sample)) {
                results.put(sample, 1);
            } else {
                results.put(sample, results.get(sample) + 1);
            }
        }

        if (print) {
            System.out.println("[" + ga + ", " + bu + ", " + zo + ", " + meu + "]");
            for (Map.Entry<Double, Integer> entry : results.entrySet()) {
                System.out.println(" " + entry.getKey() + " " + entry.getValue());
            }
        }
    }

}
