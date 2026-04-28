package com.datacluster.server.rmi;

import com.datacluster.common.enums.AlertLevel;
import com.datacluster.common.model.Alert;
import com.datacluster.common.rmi.IAlertService;
import com.datacluster.server.dao.AlertDAO;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implémentation RMI du service de gestion des alertes.
 */
public class AlertServiceImpl extends UnicastRemoteObject implements IAlertService {

    private static final Logger LOGGER = Logger.getLogger(AlertServiceImpl.class.getName());

    private final AlertDAO alertDAO;

    /**
     * @param alertDAO DAO des alertes
     * @throws RemoteException si l'export RMI échoue
     */
    public AlertServiceImpl(AlertDAO alertDAO) throws RemoteException {
        super();
        this.alertDAO = alertDAO;
    }

    @Override
    public List<Alert> getAllAlerts() throws RemoteException {
        try {
            return alertDAO.findAll();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "getAllAlerts failed", e);
            throw new RemoteException("Database error", e);
        }
    }

    @Override
    public List<Alert> getAlertsByNode(String nodeId) throws RemoteException {
        try {
            return alertDAO.findByNodeId(nodeId);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "getAlertsByNode failed: " + nodeId, e);
            throw new RemoteException("Database error", e);
        }
    }

    @Override
    public List<Alert> getAlertsByLevel(AlertLevel level) throws RemoteException {
        try {
            return alertDAO.findByLevel(level);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "getAlertsByLevel failed: " + level, e);
            throw new RemoteException("Database error", e);
        }
    }

    @Override
    public List<Alert> getAlertsBetween(long fromEpochMs, long toEpochMs) throws RemoteException {
        try {
            return alertDAO.findByTimeRange(fromEpochMs, toEpochMs);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "getAlertsBetween failed", e);
            throw new RemoteException("Database error", e);
        }
    }

    @Override
    public List<Alert> getUnacknowledgedAlerts() throws RemoteException {
        try {
            return alertDAO.findUnacknowledged();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "getUnacknowledgedAlerts failed", e);
            throw new RemoteException("Database error", e);
        }
    }

    @Override
    public void acknowledgeAlert(String alertId) throws RemoteException {
        try {
            alertDAO.acknowledge(alertId);
            LOGGER.info("Alert acknowledged: " + alertId);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "acknowledgeAlert failed: " + alertId, e);
            throw new RemoteException("Database error", e);
        }
    }
}
