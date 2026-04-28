package com.datacluster.server.stats;

import com.datacluster.common.model.Metric;
import com.datacluster.server.dao.MetricDAO;
import com.datacluster.server.model.NodeState;
import com.datacluster.server.processor.MetricsProcessor;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Moteur de statistiques avancées.
 * Calcule les moyennes mobiles et z-scores en Java sur les données du cache
 * ou de la base, sans déléguer ces calculs à SQL.
 */
public class StatisticsEngine {

    private static final Logger LOGGER = Logger.getLogger(StatisticsEngine.class.getName());

    private final MetricsProcessor processor;
    private final MetricDAO        metricDAO;

    /**
     * @param processor processeur de métriques (cache en mémoire)
     * @param metricDAO DAO pour les requêtes historiques
     */
    public StatisticsEngine(MetricsProcessor processor, MetricDAO metricDAO) {
        this.processor = processor;
        this.metricDAO = metricDAO;
    }

    /**
     * Calcule les moyennes mobiles CPU/RAM/disk sur les {@code windowSize} dernières métriques.
     *
     * @param nodeId     identifiant du nœud
     * @param windowSize taille de la fenêtre
     * @return map {cpu, ram, disk} → moyenne mobile
     */
    public Map<String, Double> getMovingAverages(String nodeId, int windowSize) {
        NodeState state = processor.getNodeState(nodeId);
        Metric[] history = (state != null) ? state.getMetricHistorySnapshot() : new Metric[0];

        int size = Math.min(history.length, windowSize);
        if (size == 0) return emptyStats();

        // On prend les [size] plus récentes (fin du tableau)
        int start = history.length - size;
        double sumCpu = 0, sumRam = 0, sumDisk = 0;
        for (int i = start; i < history.length; i++) {
            sumCpu  += history[i].getCpu();
            sumRam  += history[i].getRam();
            sumDisk += history[i].getDisk();
        }
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("cpu",  round2(sumCpu  / size));
        result.put("ram",  round2(sumRam  / size));
        result.put("disk", round2(sumDisk / size));
        return result;
    }

    /**
     * Calcule les z-scores de la dernière métrique par rapport à l'historique du nœud.
     * Un z-score > 2 ou < -2 signale une anomalie statistique.
     *
     * @param nodeId identifiant du nœud
     * @return map {cpu, ram, disk} → z-score
     */
    public Map<String, Double> getZScores(String nodeId) {
        NodeState state = processor.getNodeState(nodeId);
        if (state == null || state.getLastMetric() == null) return emptyStats();

        Metric[] history = state.getMetricHistorySnapshot();
        if (history.length < 2) return emptyStats();

        double[] cpuValues  = Arrays.stream(history).mapToDouble(Metric::getCpu).toArray();
        double[] ramValues  = Arrays.stream(history).mapToDouble(Metric::getRam).toArray();
        double[] diskValues = Arrays.stream(history).mapToDouble(Metric::getDisk).toArray();

        Metric last = state.getLastMetric();
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("cpu",  round2(zScore(last.getCpu(),  mean(cpuValues),  stdDev(cpuValues))));
        result.put("ram",  round2(zScore(last.getRam(),  mean(ramValues),  stdDev(ramValues))));
        result.put("disk", round2(zScore(last.getDisk(), mean(diskValues), stdDev(diskValues))));
        return result;
    }

    /**
     * Retourne l'historique des métriques d'un nœud sur les dernières {@code minutes} minutes.
     *
     * @param nodeId  identifiant du nœud
     * @param minutes fenêtre temporelle
     * @return liste triée chronologiquement
     */
    public List<Metric> getMetricHistory(String nodeId, int minutes) {
        long toMs   = System.currentTimeMillis();
        long fromMs = toMs - (long) minutes * 60_000L;
        try {
            return metricDAO.findByNodeIdAndTimeRange(nodeId, fromMs, toMs);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load metric history for " + nodeId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Compare les métriques moyennes de tous les nœuds connus.
     *
     * @return map nodeId → {cpu, ram, disk}
     */
    public Map<String, Map<String, Double>> getClusterComparison() {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();
        for (NodeState state : processor.getAllNodeStates()) {
            String nodeId = state.getNode().getId();
            result.put(nodeId, getMovingAverages(nodeId, 20));
        }
        return result;
    }

    // ─── Calculs statistiques ──────────────────────────────────────────────────

    private double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private double stdDev(double[] values) {
        double m = mean(values);
        double variance = 0;
        for (double v : values) variance += (v - m) * (v - m);
        return Math.sqrt(variance / values.length);
    }

    private double zScore(double value, double mean, double stdDev) {
        return stdDev == 0 ? 0 : (value - mean) / stdDev;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Map<String, Double> emptyStats() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("cpu", 0.0);
        m.put("ram", 0.0);
        m.put("disk", 0.0);
        return m;
    }
}
