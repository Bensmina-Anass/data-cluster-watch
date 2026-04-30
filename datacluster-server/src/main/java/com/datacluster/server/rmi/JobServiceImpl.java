package com.datacluster.server.rmi;

import com.datacluster.common.enums.JobType;
import com.datacluster.common.model.Job;
import com.datacluster.common.rmi.IJobService;
import com.datacluster.server.dao.JobDAO;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implémentation RMI du service de jobs Big Data.
 */
public class JobServiceImpl extends UnicastRemoteObject implements IJobService {

    private static final Logger LOGGER = Logger.getLogger(JobServiceImpl.class.getName());

    private final JobDAO jobDAO;

    /**
     * @param jobDAO DAO des jobs
     * @throws RemoteException si l'export RMI échoue
     */
    public JobServiceImpl(JobDAO jobDAO) throws RemoteException {
        super(1100);
        this.jobDAO = jobDAO;
    }

    @Override
    public List<Job> getAllJobs() throws RemoteException {
        try {
            return jobDAO.findAll();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "getAllJobs failed", e);
            throw new RemoteException("Database error", e);
        }
    }

    @Override
    public List<Job> getJobsByNode(String nodeId) throws RemoteException {
        try {
            return jobDAO.findByNodeId(nodeId);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "getJobsByNode failed: " + nodeId, e);
            throw new RemoteException("Database error", e);
        }
    }

    @Override
    public List<Job> getRunningJobs() throws RemoteException {
        try {
            return jobDAO.findRunning();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "getRunningJobs failed", e);
            throw new RemoteException("Database error", e);
        }
    }

    @Override
    public List<Job> getJobsByType(JobType type) throws RemoteException {
        try {
            return jobDAO.findByType(type);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "getJobsByType failed: " + type, e);
            throw new RemoteException("Database error", e);
        }
    }
}
