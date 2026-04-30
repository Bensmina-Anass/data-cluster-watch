package com.datacluster.server.rmi;

import com.datacluster.common.model.ClusterSummary;
import com.datacluster.common.model.Metric;
import com.datacluster.common.model.Node;
import com.datacluster.common.enums.NodeStatus;
import com.datacluster.common.rmi.IClusterService;
import com.datacluster.server.dao.AlertDAO;
import com.datacluster.server.dao.NodeDAO;
import com.datacluster.server.model.NodeState;
import com.datacluster.server.processor.MetricsProcessor;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implémentation RMI du service principal de cluster.
 */
public class ClusterServiceImpl extends UnicastRemoteObject implements IClusterService {

    private static final Logger LOGGER = Logger.getLogger(ClusterServiceImpl.class.getName());

    private final NodeDAO          nodeDAO;
    private final MetricsProcessor processor;
    private final AlertDAO         alertDAO;

    /**
     * @param nodeDAO   DAO des nœuds
     * @param processor processeur de métriques (cache)
     * @param alertDAO  DAO des alertes
     * @throws RemoteException si l'export RMI échoue
     */
    public ClusterServiceImpl(NodeDAO nodeDAO, MetricsProcessor processor, AlertDAO alertDAO)
            throws RemoteException {
        super(1100);
        this.nodeDAO   = nodeDAO;
        this.processor = processor;
        this.alertDAO  = alertDAO;
    }

    @Override
    public List<Node> getAllNodes() throws RemoteException {
        try {
            return nodeDAO.findAll();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "getAllNodes failed", e);
            throw new RemoteException("Database error", e);
        }
    }

    @Override
    public Node getNode(String nodeId) throws RemoteException {
        try {
            return nodeDAO.findById(nodeId);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "getNode failed: " + nodeId, e);
            throw new RemoteException("Database error", e);
        }
    }

    @Override
    public List<Metric> getLatestMetrics() throws RemoteException {
        return processor.getLatestMetrics();
    }

    @Override
    public Metric getLatestMetric(String nodeId) throws RemoteException {
        NodeState state = processor.getNodeState(nodeId);
        return state != null ? state.getLastMetric() : null;
    }

    @Override
    public ClusterSummary getClusterSummary() throws RemoteException {
        ClusterSummary summary = new ClusterSummary();
        Collection<NodeState> states = processor.getAllNodeStates();

        int active = 0, idle = 0, down = 0, jobs = 0;
        double sumCpu = 0, sumRam = 0, sumDisk = 0;
        int count = 0;

        for (NodeState state : states) {
            NodeStatus status = state.getNode().getStatus();
            switch (status) {
                case ACTIVE -> active++;
                case IDLE   -> idle++;
                case DOWN   -> down++;
            }
            Metric m = state.getLastMetric();
            if (m != null) {
                sumCpu  += m.getCpu();
                sumRam  += m.getRam();
                sumDisk += m.getDisk();
                jobs    += m.getActiveJobs();
                count++;
            }
        }

        int total = active + idle + down;
        summary.setTotalNodes(total);
        summary.setActiveNodes(active);
        summary.setIdleNodes(idle);
        summary.setDownNodes(down);
        summary.setActiveJobs(jobs);

        if (count > 0) {
            summary.setAvgCpu(Math.round(sumCpu / count * 10.0) / 10.0);
            summary.setAvgRam(Math.round(sumRam / count * 10.0) / 10.0);
            summary.setAvgDisk(Math.round(sumDisk / count * 10.0) / 10.0);
        }

        try {
            int totalAlerts    = alertDAO.findAll().size();
            int criticalAlerts = alertDAO.findUnacknowledged().stream()
                    .filter(a -> a.getLevel().name().equals("CRITICAL")).toList().size();
            summary.setTotalAlerts(totalAlerts);
            summary.setCriticalAlerts(criticalAlerts);
            // Health = 100 - (down/total*50) - (critical/total*50)
            double health = 100.0;
            if (total > 0) health -= ((double) down / total) * 50.0;
            if (total > 0) health -= ((double) criticalAlerts / Math.max(total, 1)) * 50.0;
            summary.setClusterHealth(Math.max(0, Math.round(health * 10.0) / 10.0));
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to compute alert stats", e);
        }

        return summary;
    }
}
