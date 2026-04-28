package com.datacluster.server.model;

import com.datacluster.common.model.Metric;
import com.datacluster.common.model.Node;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * État courant d'un nœud maintenu en mémoire par le serveur.
 * Contient la dernière métrique, le timestamp du dernier heartbeat,
 * et un historique glissant utilisé pour les calculs statistiques.
 */
public class NodeState {

    private static final int MAX_HISTORY = 120; // ~6 minutes à 3s/métrique

    private final Node         node;
    private volatile Metric    lastMetric;
    private volatile long      lastHeartbeat;
    private final Deque<Metric> metricHistory;

    /**
     * @param node nœud associé à cet état
     */
    public NodeState(Node node) {
        this.node          = node;
        this.lastHeartbeat = System.currentTimeMillis();
        this.metricHistory = new ArrayDeque<>(MAX_HISTORY);
    }

    /**
     * Met à jour la dernière métrique et l'ajoute à l'historique glissant.
     *
     * @param metric nouvelle métrique collectée
     */
    public synchronized void updateMetric(Metric metric) {
        this.lastMetric = metric;
        if (metricHistory.size() >= MAX_HISTORY) {
            metricHistory.pollFirst();
        }
        metricHistory.addLast(metric);
    }

    /** Met à jour le timestamp du dernier heartbeat reçu. */
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }

    /**
     * Retourne une copie de l'historique des métriques (ordre chronologique).
     *
     * @return tableau de métriques
     */
    public synchronized Metric[] getMetricHistorySnapshot() {
        return metricHistory.toArray(new Metric[0]);
    }

    public Node    getNode()          { return node; }
    public Metric  getLastMetric()    { return lastMetric; }
    public long    getLastHeartbeat() { return lastHeartbeat; }
}
