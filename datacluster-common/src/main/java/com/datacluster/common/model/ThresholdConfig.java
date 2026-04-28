package com.datacluster.common.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Configuration d'un seuil d'alerte pour une métrique donnée.
 */
public class ThresholdConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String metric;
    private double warningValue;
    private double criticalValue;

    public ThresholdConfig() {}

    /**
     * @param metric         nom de la métrique (cpu, ram, disk)
     * @param warningValue   valeur déclenchant une alerte WARNING
     * @param criticalValue  valeur déclenchant une alerte CRITICAL
     */
    public ThresholdConfig(String metric, double warningValue, double criticalValue) {
        this.metric         = metric;
        this.warningValue   = warningValue;
        this.criticalValue  = criticalValue;
    }

    // ─── Getters / Setters ─────────────────────────────────────────────────────

    public String getMetric()                { return metric; }
    public void   setMetric(String metric)   { this.metric = metric; }

    public double getWarningValue()                    { return warningValue; }
    public void   setWarningValue(double warningValue) { this.warningValue = warningValue; }

    public double getCriticalValue()                     { return criticalValue; }
    public void   setCriticalValue(double criticalValue) { this.criticalValue = criticalValue; }

    @Override
    public String toString() {
        return "ThresholdConfig{metric='" + metric
                + "', warning=" + warningValue
                + ", critical=" + criticalValue + '}';
    }
}
