package com.datacluster.server.dao;

import com.datacluster.common.enums.AlertLevel;
import com.datacluster.common.enums.AlertType;
import com.datacluster.common.model.Alert;
import com.datacluster.server.persistence.PersistenceModule;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO d'accès aux alertes persistées en base.
 */
public class AlertDAO {

    private static final Logger LOGGER = Logger.getLogger(AlertDAO.class.getName());
    private final PersistenceModule pm;

    /**
     * @param pm module de persistance
     */
    public AlertDAO(PersistenceModule pm) {
        this.pm = pm;
    }

    /**
     * Insère une alerte en base.
     *
     * @param alert alerte à persister
     * @throws SQLException en cas d'erreur SQL
     */
    public void save(Alert alert) throws SQLException {
        String sql = "INSERT INTO alerts (id, node_id, timestamp, type, level, message, acknowledged) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alert.getId());
            ps.setString(2, alert.getNodeId());
            ps.setTimestamp(3, new Timestamp(alert.getTimestamp()));
            ps.setString(4, alert.getType().name());
            ps.setString(5, alert.getLevel().name());
            ps.setString(6, alert.getMessage());
            ps.setBoolean(7, alert.isAcknowledged());
            ps.executeUpdate();
        }
    }

    /**
     * Acquitte une alerte par son identifiant.
     *
     * @param alertId identifiant de l'alerte
     * @throws SQLException en cas d'erreur SQL
     */
    public void acknowledge(String alertId) throws SQLException {
        String sql = "UPDATE alerts SET acknowledged = TRUE WHERE id = ?";
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alertId);
            ps.executeUpdate();
        }
    }

    /**
     * Retourne toutes les alertes.
     *
     * @return liste de {@link Alert}
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Alert> findAll() throws SQLException {
        return query("SELECT * FROM alerts ORDER BY timestamp DESC", ps -> {});
    }

    /**
     * Retourne les alertes pour un nœud spécifique.
     *
     * @param nodeId identifiant du nœud
     * @return liste filtrée
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Alert> findByNodeId(String nodeId) throws SQLException {
        return query("SELECT * FROM alerts WHERE node_id = ? ORDER BY timestamp DESC",
                ps -> ps.setString(1, nodeId));
    }

    /**
     * Retourne les alertes d'un niveau donné.
     *
     * @param level niveau d'alerte
     * @return liste filtrée
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Alert> findByLevel(AlertLevel level) throws SQLException {
        return query("SELECT * FROM alerts WHERE level = ? ORDER BY timestamp DESC",
                ps -> ps.setString(1, level.name()));
    }

    /**
     * Retourne les alertes non acquittées.
     *
     * @return liste filtrée
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Alert> findUnacknowledged() throws SQLException {
        return query("SELECT * FROM alerts WHERE acknowledged = FALSE ORDER BY timestamp DESC",
                ps -> {});
    }

    /**
     * Retourne les alertes dans une fenêtre temporelle.
     *
     * @param fromMs borne inférieure (epoch ms)
     * @param toMs   borne supérieure (epoch ms)
     * @return liste filtrée
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Alert> findByTimeRange(long fromMs, long toMs) throws SQLException {
        return query("SELECT * FROM alerts WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC",
                ps -> {
                    ps.setTimestamp(1, new Timestamp(fromMs));
                    ps.setTimestamp(2, new Timestamp(toMs));
                });
    }

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<Alert> query(String sql, ParamSetter setter) throws SQLException {
        List<Alert> alerts = new ArrayList<>();
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) alerts.add(mapRow(rs));
            }
        }
        return alerts;
    }

    private Alert mapRow(ResultSet rs) throws SQLException {
        Alert a = new Alert();
        a.setId(rs.getString("id"));
        a.setNodeId(rs.getString("node_id"));
        a.setTimestamp(rs.getTimestamp("timestamp").getTime());
        a.setType(AlertType.valueOf(rs.getString("type")));
        a.setLevel(AlertLevel.valueOf(rs.getString("level")));
        a.setMessage(rs.getString("message"));
        a.setAcknowledged(rs.getBoolean("acknowledged"));
        return a;
    }
}
