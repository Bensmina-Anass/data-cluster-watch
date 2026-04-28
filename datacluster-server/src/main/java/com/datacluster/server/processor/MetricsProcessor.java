package com.datacluster.server.processor;

import com.datacluster.common.enums.NodeStatus;
import com.datacluster.common.enums.NodeType;
import com.datacluster.common.model.Metric;
import com.datacluster.common.model.Node;
import com.datacluster.server.dao.MetricDAO;
import com.datacluster.server.dao.NodeDAO;
import com.datacluster.server.engine.AlertEngine;
import com.datacluster.server.model.NodeState;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Traite les métriques reçues, met à jour le cache en mémoire,
 * persiste en base et déclenche l'évaluation des alertes.
 */
public class MetricsProcessor {

    private static final Logger LOGGER = Logger.getLogger(MetricsProcessor.class.getName());

    private final MetricDAO  metricDAO;
    private final NodeDAO    nodeDAO;
    private final AlertEngine alertEngine;

    /** Cache principal : nodeId → état courant du nœud. */
    private final ConcurrentHashMap<String, NodeState> nodeStates = new ConcurrentHashMap<>();

    /**
     * @param metricDAO   DAO de persistance des métriques
     * @param nodeDAO     DAO de persistance des nœuds
     * @param alertEngine moteur d'alertes
     */
    public MetricsProcessor(MetricDAO metricDAO, NodeDAO nodeDAO, AlertEngine alertEngine) {
        this.metricDAO   = metricDAO;
        this.nodeDAO     = nodeDAO;
        this.alertEngine = alertEngine;
    }

    /**
     * Traite une métrique reçue depuis l'agent UDP.
     *
     * @param metric métrique à traiter
     */
    public void processMetric(Metric metric) {
        if (!isValid(metric)) {
            LOGGER.warning("Invalid metric received, ignored: " + metric);
            return;
        }

        NodeState state = ensureNodeState(metric.getNodeId());
        state.updateMetric(metric);

        try {
            metricDAO.save(metric);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to persist metric for " + metric.getNodeId(), e);
        }

        alertEngine.evaluate(state);
    }

    /**
     * Met à jour le timestamp du heartbeat d'un nœud.
     *
     * @param nodeId identifiant du nœud
     */
    public void processHeartbeat(String nodeId) {
        NodeState state = nodeStates.get(nodeId);
        if (state != null) {
            state.updateHeartbeat();
            LOGGER.fine("Heartbeat updated for: " + nodeId);
        } else {
            LOGGER.fine("Heartbeat received for unknown node: " + nodeId);
        }
    }

    /**
     * Retourne l'état d'un nœud depuis le cache.
     *
     * @param nodeId identifiant du nœud
     * @return état du nœud, ou {@code null} si inconnu
     */
    public NodeState getNodeState(String nodeId) {
        return nodeStates.get(nodeId);
    }

    /**
     * Retourne tous les états de nœuds du cache.
     *
     * @return collection de {@link NodeState}
     */
    public Collection<NodeState> getAllNodeStates() {
        return nodeStates.values();
    }

    /**
     * Retourne la dernière métrique de chaque nœud.
     *
     * @return liste de métriques
     */
    public List<Metric> getLatestMetrics() {
        List<Metric> metrics = new ArrayList<>();
        for (NodeState state : nodeStates.values()) {
            if (state.getLastMetric() != null) {
                metrics.add(state.getLastMetric());
            }
        }
        return metrics;
    }

    private NodeState ensureNodeState(String nodeId) {
        return nodeStates.computeIfAbsent(nodeId, id -> {
            Node node = loadOrCreateNode(id);
            return new NodeState(node);
        });
    }

    private Node loadOrCreateNode(String nodeId) {
        try {
            Node node = nodeDAO.findById(nodeId);
            if (node != null) return node;
            // Nœud découvert dynamiquement
            node = new Node(nodeId, nodeId, NodeType.WORKER, NodeStatus.ACTIVE);
            nodeDAO.save(node);
            LOGGER.info("New node registered: " + nodeId);
            return node;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "DB error for node " + nodeId + ", using in-memory fallback", e);
            return new Node(nodeId, nodeId, NodeType.WORKER, NodeStatus.ACTIVE);
        }
    }

    private boolean isValid(Metric metric) {
        return metric != null
                && metric.getNodeId() != null
                && !metric.getNodeId().isBlank()
                && metric.getCpu() >= 0 && metric.getCpu() <= 100
                && metric.getRam() >= 0 && metric.getRam() <= 100
                && metric.getDisk() >= 0 && metric.getDisk() <= 100;
    }
}
