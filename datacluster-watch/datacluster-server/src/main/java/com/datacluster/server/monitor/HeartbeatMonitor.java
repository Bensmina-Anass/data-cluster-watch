package com.datacluster.server.monitor;

import com.datacluster.common.enums.AlertLevel;
import com.datacluster.common.enums.AlertType;
import com.datacluster.common.enums.NodeStatus;
import com.datacluster.common.model.Alert;
import com.datacluster.server.dao.AlertDAO;
import com.datacluster.server.dao.NodeDAO;
import com.datacluster.server.model.NodeState;
import com.datacluster.server.processor.MetricsProcessor;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Surveille périodiquement les heartbeats des nœuds.
 * Si un nœud ne s'est pas manifesté depuis {@code heartbeatTimeoutMs}, une alerte WARNING est générée
 * et son statut est passé à {@code DOWN}.
 */
public class HeartbeatMonitor {

    private static final Logger LOGGER = Logger.getLogger(HeartbeatMonitor.class.getName());

    private final MetricsProcessor  processor;
    private final AlertDAO          alertDAO;
    private final NodeDAO           nodeDAO;
    private final long              heartbeatTimeoutMs;

    private final ScheduledExecutorService scheduler;

    /**
     * @param processor          processeur de métriques (source du cache de nœuds)
     * @param alertDAO           DAO pour persister les alertes heartbeat
     * @param nodeDAO            DAO pour mettre à jour le statut du nœud
     * @param heartbeatTimeoutMs délai au-delà duquel un nœud est considéré DOWN
     * @param checkIntervalMs    fréquence de vérification
     */
    public HeartbeatMonitor(MetricsProcessor processor, AlertDAO alertDAO, NodeDAO nodeDAO,
                            long heartbeatTimeoutMs, long checkIntervalMs) {
        this.processor          = processor;
        this.alertDAO           = alertDAO;
        this.nodeDAO            = nodeDAO;
        this.heartbeatTimeoutMs = heartbeatTimeoutMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat-monitor");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::checkHeartbeats,
                checkIntervalMs, checkIntervalMs, TimeUnit.MILLISECONDS);
        LOGGER.info("HeartbeatMonitor started (timeout=" + heartbeatTimeoutMs + "ms)");
    }

    private void checkHeartbeats() {
        long now = System.currentTimeMillis();
        for (NodeState state : processor.getAllNodeStates()) {
            long silence = now - state.getLastHeartbeat();
            if (silence > heartbeatTimeoutMs) {
                handleSilentNode(state, silence);
            }
        }
    }

    private void handleSilentNode(NodeState state, long silenceMs) {
        String nodeId   = state.getNode().getId();
        String nodeName = state.getNode().getName();
        LOGGER.warning("No heartbeat from " + nodeId + " for " + (silenceMs / 1000) + "s");

        Alert alert = new Alert(nodeId, System.currentTimeMillis(),
                AlertType.HEARTBEAT_MISSING, AlertLevel.WARNING,
                String.format("Nœud %s silencieux depuis %ds", nodeName, silenceMs / 1000));
        try {
            alertDAO.save(alert);
            nodeDAO.updateStatus(nodeId, NodeStatus.DOWN);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to persist heartbeat alert for " + nodeId, e);
        }
    }

    /** Arrête le moniteur proprement. */
    public void stop() {
        scheduler.shutdownNow();
        LOGGER.info("HeartbeatMonitor stopped");
    }
}
