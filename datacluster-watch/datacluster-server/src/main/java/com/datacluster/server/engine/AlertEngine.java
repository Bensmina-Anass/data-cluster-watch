package com.datacluster.server.engine;

import com.datacluster.common.constants.AppConstants;
import com.datacluster.common.model.Alert;
import com.datacluster.common.model.ThresholdConfig;
import com.datacluster.server.dao.AlertDAO;
import com.datacluster.server.dao.ThresholdDAO;
import com.datacluster.server.model.NodeState;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Moteur d'alertes basé sur le pattern Strategy.
 * Évalue toutes les stratégies d'alerte enregistrées à chaque nouvelle métrique.
 * Maintient un cooldown par (nodeId, alertType) pour éviter les doublons.
 */
public class AlertEngine {

    private static final Logger LOGGER = Logger.getLogger(AlertEngine.class.getName());

    /** Délai minimum entre deux alertes identiques pour le même nœud (ms). */
    private static final long COOLDOWN_MS = 60_000L;

    private final AlertDAO     alertDAO;
    private final ThresholdDAO thresholdDAO;

    private final List<AlertStrategy>         strategies;
    private final Map<String, ThresholdConfig> thresholdCache;
    /** Clé : nodeId + "_" + alertType → timestamp du dernier déclenchement */
    private final Map<String, Long>            cooldownMap;

    /**
     * @param alertDAO     DAO de persistance des alertes
     * @param thresholdDAO DAO d'accès aux seuils configurés
     */
    public AlertEngine(AlertDAO alertDAO, ThresholdDAO thresholdDAO) {
        this.alertDAO      = alertDAO;
        this.thresholdDAO  = thresholdDAO;
        this.thresholdCache = new ConcurrentHashMap<>();
        this.cooldownMap    = new ConcurrentHashMap<>();

        this.strategies = List.of(
                new CpuAlertStrategy(),
                new RamAlertStrategy(),
                new DiskAlertStrategy(),
                new FailedJobsAlertStrategy()
        );

        refreshThresholds();
    }

    /**
     * Évalue toutes les stratégies sur l'état courant d'un nœud.
     * Les alertes générées sont persistées en base.
     *
     * @param state état courant du nœud
     */
    public void evaluate(NodeState state) {
        for (AlertStrategy strategy : strategies) {
            String metricKey = resolveMetricKey(strategy);
            ThresholdConfig threshold = thresholdCache.getOrDefault(metricKey,
                    defaultThreshold(metricKey));

            strategy.evaluate(state, threshold).ifPresent(alert -> {
                String cooldownKey = state.getNode().getId() + "_" + alert.getType().name();
                long now = System.currentTimeMillis();
                long lastFired = cooldownMap.getOrDefault(cooldownKey, 0L);

                if (now - lastFired >= COOLDOWN_MS) {
                    cooldownMap.put(cooldownKey, now);
                    persistAlert(alert);
                    LOGGER.info("Alert generated: " + alert.getLevel()
                            + " [" + alert.getType() + "] on " + state.getNode().getId());
                }
            });
        }
    }

    /**
     * Recharge les seuils depuis la base de données.
     */
    public void refreshThresholds() {
        try {
            List<ThresholdConfig> configs = thresholdDAO.findAll();
            thresholdCache.clear();
            configs.forEach(c -> thresholdCache.put(c.getMetric(), c));
            LOGGER.info("Thresholds reloaded: " + thresholdCache.size() + " entries");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to reload thresholds", e);
        }
    }

    /**
     * Met à jour un seuil en cache et en base.
     *
     * @param config nouvelle configuration de seuil
     */
    public void updateThreshold(ThresholdConfig config) {
        thresholdCache.put(config.getMetric(), config);
        try {
            thresholdDAO.upsert(config);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to persist threshold update", e);
        }
    }

    /**
     * Retourne tous les seuils courants.
     *
     * @return liste des {@link ThresholdConfig}
     */
    public List<ThresholdConfig> getThresholds() {
        return new ArrayList<>(thresholdCache.values());
    }

    private void persistAlert(Alert alert) {
        try {
            alertDAO.save(alert);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to persist alert: " + alert.getId(), e);
        }
    }

    private String resolveMetricKey(AlertStrategy strategy) {
        return switch (strategy.getClass().getSimpleName()) {
            case "CpuAlertStrategy"        -> "cpu";
            case "RamAlertStrategy"        -> "ram";
            case "DiskAlertStrategy"       -> "disk";
            case "FailedJobsAlertStrategy" -> "failed_jobs";
            default -> "unknown";
        };
    }

    private ThresholdConfig defaultThreshold(String metric) {
        return switch (metric) {
            case "cpu"  -> new ThresholdConfig("cpu",
                    AppConstants.CPU_WARNING_THRESHOLD, AppConstants.CPU_CRITICAL_THRESHOLD);
            case "ram"  -> new ThresholdConfig("ram",
                    AppConstants.RAM_WARNING_THRESHOLD, AppConstants.RAM_CRITICAL_THRESHOLD);
            case "disk" -> new ThresholdConfig("disk",
                    AppConstants.DISK_WARNING_THRESHOLD, AppConstants.DISK_CRITICAL_THRESHOLD);
            case "failed_jobs" -> new ThresholdConfig("failed_jobs", 1.0, 3.0);
            default -> new ThresholdConfig(metric, 80.0, 95.0);
        };
    }
}
