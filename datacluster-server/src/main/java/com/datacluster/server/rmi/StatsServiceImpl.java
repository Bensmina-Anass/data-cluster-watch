package com.datacluster.server.rmi;

import com.datacluster.common.model.Metric;
import com.datacluster.common.rmi.IStatsService;
import com.datacluster.server.stats.StatisticsEngine;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implémentation RMI du service de statistiques avancées.
 */
public class StatsServiceImpl extends UnicastRemoteObject implements IStatsService {

    private static final Logger LOGGER = Logger.getLogger(StatsServiceImpl.class.getName());

    private final StatisticsEngine statsEngine;

    /**
     * @param statsEngine moteur de calcul statistique
     * @throws RemoteException si l'export RMI échoue
     */
    public StatsServiceImpl(StatisticsEngine statsEngine) throws RemoteException {
        super();
        this.statsEngine = statsEngine;
    }

    @Override
    public Map<String, Double> getMovingAverages(String nodeId, int windowSize) throws RemoteException {
        try {
            return statsEngine.getMovingAverages(nodeId, windowSize);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "getMovingAverages failed: " + nodeId, e);
            throw new RemoteException("Statistics error", e);
        }
    }

    @Override
    public Map<String, Double> getZScores(String nodeId) throws RemoteException {
        try {
            return statsEngine.getZScores(nodeId);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "getZScores failed: " + nodeId, e);
            throw new RemoteException("Statistics error", e);
        }
    }

    @Override
    public List<Metric> getMetricHistory(String nodeId, int minutes) throws RemoteException {
        try {
            return statsEngine.getMetricHistory(nodeId, minutes);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "getMetricHistory failed: " + nodeId, e);
            throw new RemoteException("Statistics error", e);
        }
    }

    @Override
    public Map<String, Map<String, Double>> getClusterComparison() throws RemoteException {
        try {
            return statsEngine.getClusterComparison();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "getClusterComparison failed", e);
            throw new RemoteException("Statistics error", e);
        }
    }
}
