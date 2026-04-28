package com.datacluster.server.dao;

import com.datacluster.common.model.Metric;
import com.datacluster.server.persistence.PersistenceModule;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO d'accès aux métriques persistées en base.
 */
public class MetricDAO {

    private static final Logger LOGGER = Logger.getLogger(MetricDAO.class.getName());
    private final PersistenceModule pm;

    /**
     * @param pm module de persistance
     */
    public MetricDAO(PersistenceModule pm) {
        this.pm = pm;
    }

    /**
     * Insère une métrique en base.
     *
     * @param metric métrique à persister
     * @throws SQLException en cas d'erreur SQL
     */
    public void save(Metric metric) throws SQLException {
        String sql = "INSERT INTO metrics (node_id, timestamp, cpu, ram, disk, active_jobs, failed_jobs) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, metric.getNodeId());
            ps.setTimestamp(2, new Timestamp(metric.getTimestamp()));
            ps.setDouble(3, metric.getCpu());
            ps.setDouble(4, metric.getRam());
            ps.setDouble(5, metric.getDisk());
            ps.setInt(6, metric.getActiveJobs());
            ps.setInt(7, metric.getFailedJobs());
            ps.executeUpdate();
        }
    }

    /**
     * Retourne la dernière métrique connue pour un nœud.
     *
     * @param nodeId identifiant du nœud
     * @return dernière {@link Metric}, ou {@code null}
     * @throws SQLException en cas d'erreur SQL
     */
    public Metric findLatestByNodeId(String nodeId) throws SQLException {
        String sql = "SELECT node_id, UNIX_TIMESTAMP(timestamp)*1000 AS ts, cpu, ram, disk, "
                   + "active_jobs, failed_jobs FROM metrics "
                   + "WHERE node_id = ? ORDER BY timestamp DESC LIMIT 1";
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nodeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /**
     * Retourne l'historique des métriques d'un nœud sur une fenêtre temporelle.
     *
     * @param nodeId  identifiant du nœud
     * @param fromMs  borne inférieure (epoch ms)
     * @param toMs    borne supérieure (epoch ms)
     * @return liste ordonnée par timestamp croissant
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Metric> findByNodeIdAndTimeRange(String nodeId, long fromMs, long toMs) throws SQLException {
        List<Metric> metrics = new ArrayList<>();
        String sql = "SELECT node_id, UNIX_TIMESTAMP(timestamp)*1000 AS ts, cpu, ram, disk, "
                   + "active_jobs, failed_jobs FROM metrics "
                   + "WHERE node_id = ? AND timestamp BETWEEN ? AND ? "
                   + "ORDER BY timestamp ASC";
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nodeId);
            ps.setTimestamp(2, new Timestamp(fromMs));
            ps.setTimestamp(3, new Timestamp(toMs));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) metrics.add(mapRow(rs));
            }
        }
        return metrics;
    }

    /**
     * Retourne les N dernières métriques d'un nœud pour le calcul de statistiques.
     *
     * @param nodeId  identifiant du nœud
     * @param limit   nombre de métriques à récupérer
     * @return liste ordonnée par timestamp décroissant
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Metric> findRecentByNodeId(String nodeId, int limit) throws SQLException {
        List<Metric> metrics = new ArrayList<>();
        String sql = "SELECT node_id, UNIX_TIMESTAMP(timestamp)*1000 AS ts, cpu, ram, disk, "
                   + "active_jobs, failed_jobs FROM metrics "
                   + "WHERE node_id = ? ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nodeId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) metrics.add(mapRow(rs));
            }
        }
        return metrics;
    }

    private Metric mapRow(ResultSet rs) throws SQLException {
        return new Metric(
                rs.getString("node_id"),
                rs.getLong("ts"),
                rs.getDouble("cpu"),
                rs.getDouble("ram"),
                rs.getDouble("disk"),
                rs.getInt("active_jobs"),
                rs.getInt("failed_jobs")
        );
    }
}
