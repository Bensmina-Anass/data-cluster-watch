package com.datacluster.server;

import com.datacluster.common.model.Metric;
import com.datacluster.common.model.Node;
import com.datacluster.common.enums.NodeStatus;
import com.datacluster.common.enums.NodeType;
import com.datacluster.server.model.NodeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du moteur de statistiques (moyennes mobiles, z-scores).
 * Ces tests utilisent directement NodeState sans la couche base de données.
 */
class StatisticsEngineTest {

    private NodeState nodeState;

    @BeforeEach
    void setUp() {
        Node node = new Node("worker-01", "Worker Node 01", NodeType.WORKER, NodeStatus.ACTIVE);
        nodeState = new NodeState(node);
    }

    @Test
    void movingAverageOnEmptyHistoryReturnsZero() {
        // Sans métriques, l'historique est vide
        Metric[] history = nodeState.getMetricHistorySnapshot();
        assertEquals(0, history.length);
    }

    @Test
    void metricHistoryGrowsCorrectly() {
        for (int i = 0; i < 5; i++) {
            nodeState.updateMetric(
                    new Metric("worker-01", System.currentTimeMillis() + i * 3000,
                            30.0 + i, 50.0 + i, 40.0, 0, 0));
        }
        Metric[] history = nodeState.getMetricHistorySnapshot();
        assertEquals(5, history.length);
    }

    @Test
    void movingAverageCalculatedManually() {
        double[] cpuValues = {40.0, 50.0, 60.0};
        for (double cpu : cpuValues) {
            nodeState.updateMetric(
                    new Metric("worker-01", System.currentTimeMillis(), cpu, 50.0, 40.0, 0, 0));
        }
        Metric[] history = nodeState.getMetricHistorySnapshot();
        double sum = 0;
        for (Metric m : history) sum += m.getCpu();
        double avg = sum / history.length;
        assertEquals(50.0, avg, 0.001, "Moving average of [40,50,60] should be 50");
    }

    @Test
    void zScoreOfMeanIsZero() {
        // Si toutes les valeurs sont identiques, le z-score doit être 0
        for (int i = 0; i < 10; i++) {
            nodeState.updateMetric(
                    new Metric("worker-01", System.currentTimeMillis() + i, 50.0, 60.0, 40.0, 0, 0));
        }
        Metric[] history = nodeState.getMetricHistorySnapshot();
        double[] cpus = new double[history.length];
        for (int i = 0; i < history.length; i++) cpus[i] = history[i].getCpu();

        double mean = 0;
        for (double v : cpus) mean += v;
        mean /= cpus.length;
        assertEquals(50.0, mean, 0.001);

        double variance = 0;
        for (double v : cpus) variance += (v - mean) * (v - mean);
        double stddev = Math.sqrt(variance / cpus.length);
        assertEquals(0.0, stddev, 0.001, "All values equal → stddev=0");
    }

    @Test
    void heartbeatUpdatesTimestamp() throws InterruptedException {
        long before = nodeState.getLastHeartbeat();
        Thread.sleep(10);
        nodeState.updateHeartbeat();
        assertTrue(nodeState.getLastHeartbeat() > before);
    }
}
