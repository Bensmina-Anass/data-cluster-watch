package com.datacluster.server.dao;

import com.datacluster.common.enums.JobStatus;
import com.datacluster.common.enums.JobType;
import com.datacluster.common.model.Job;
import com.datacluster.server.persistence.PersistenceModule;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO d'accès aux jobs Big Data persistés en base.
 */
public class JobDAO {

    private static final Logger LOGGER = Logger.getLogger(JobDAO.class.getName());
    private final PersistenceModule pm;

    /**
     * @param pm module de persistance
     */
    public JobDAO(PersistenceModule pm) {
        this.pm = pm;
    }

    /**
     * Insère un job en base.
     *
     * @param job job à persister
     * @throws SQLException en cas d'erreur SQL
     */
    public void save(Job job) throws SQLException {
        String sql = "INSERT INTO jobs (id, node_id, type, start_time, end_time, status, progress) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE end_time=VALUES(end_time), "
                   + "status=VALUES(status), progress=VALUES(progress)";
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, job.getId());
            ps.setString(2, job.getNodeId());
            ps.setString(3, job.getType().name());
            ps.setTimestamp(4, new Timestamp(job.getStartTime()));
            ps.setTimestamp(5, job.getEndTime() > 0 ? new Timestamp(job.getEndTime()) : null);
            ps.setString(6, job.getStatus().name());
            ps.setDouble(7, job.getProgress());
            ps.executeUpdate();
        }
    }

    /**
     * Retourne tous les jobs.
     *
     * @return liste de {@link Job}
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Job> findAll() throws SQLException {
        return query("SELECT * FROM jobs ORDER BY start_time DESC", ps -> {});
    }

    /**
     * Retourne les jobs d'un nœud donné.
     *
     * @param nodeId identifiant du nœud
     * @return liste filtrée
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Job> findByNodeId(String nodeId) throws SQLException {
        return query("SELECT * FROM jobs WHERE node_id = ? ORDER BY start_time DESC",
                ps -> ps.setString(1, nodeId));
    }

    /**
     * Retourne les jobs en cours d'exécution.
     *
     * @return liste des jobs RUNNING
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Job> findRunning() throws SQLException {
        return query("SELECT * FROM jobs WHERE status = 'RUNNING' ORDER BY start_time DESC",
                ps -> {});
    }

    /**
     * Retourne les jobs d'un type donné.
     *
     * @param type type de job
     * @return liste filtrée
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Job> findByType(JobType type) throws SQLException {
        return query("SELECT * FROM jobs WHERE type = ? ORDER BY start_time DESC",
                ps -> ps.setString(1, type.name()));
    }

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<Job> query(String sql, ParamSetter setter) throws SQLException {
        List<Job> jobs = new ArrayList<>();
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) jobs.add(mapRow(rs));
            }
        }
        return jobs;
    }

    private Job mapRow(ResultSet rs) throws SQLException {
        Job j = new Job();
        j.setId(rs.getString("id"));
        j.setNodeId(rs.getString("node_id"));
        j.setType(JobType.valueOf(rs.getString("type")));
        j.setStartTime(rs.getTimestamp("start_time").getTime());
        Timestamp end = rs.getTimestamp("end_time");
        j.setEndTime(end != null ? end.getTime() : 0L);
        j.setStatus(JobStatus.valueOf(rs.getString("status")));
        j.setProgress(rs.getDouble("progress"));
        return j;
    }
}
