package com.meryt.demographics.math;

import java.util.Random;
import org.apache.commons.math3.distribution.BetaDistribution;

public class FunkyBetaDistribution extends BetaDistribution {

    private final Random randomGen = new Random();

    private final double ga;
    private final double bu;
    private final double zo;
    private final double meu;

    public FunkyBetaDistribution(double alpha, double beta) {
        this(alpha, beta, 0.55, 0.6, 0.8, 1.0);
    }

    FunkyBetaDistribution(double alpha, double beta, double ga, double bu, double zo, double meu) {
        super(alpha, beta);
        this.ga = ga;
        this.bu = bu;
        this.zo = zo;
        this.meu = meu;
    }

    /**
     * Gets a value on a funky distribution with a slight hump at the low end
     *
     * @return single funky-beta deviate yo
     */
    @Override
    public double sample() {
        double randBeta1 = (1 - super.sample());
        if (randBeta1 < gabu()) {
            double randBeta = super.sample();
            if ((1 - randBeta) < zomeu()) {
                return randBeta;
            }
            return 1 - randBeta;
        } else {
            return randBeta1;
        }
    }

    /**
     * Get a random percent between ga and bu
     * @return number between 0.0 and 1.0
     */
    private double gabu() {
        int intBu = (int) (bu * 100.0);
        int intGa = (int) (ga * 100.0);
        return (randomGen.nextInt(intBu - intGa) + intGa) / 100.0;
    }

    private double zomeu() {
        int intZo = (int) (zo * 100.0);
        int intMeu = (int) (meu * 100.0);
        return (randomGen.nextInt(intMeu - intZo) + intZo) / 100.0;
    }
}
