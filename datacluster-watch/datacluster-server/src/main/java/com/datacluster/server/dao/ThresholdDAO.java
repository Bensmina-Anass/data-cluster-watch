package com.datacluster.server.dao;

import com.datacluster.common.model.ThresholdConfig;
import com.datacluster.server.persistence.PersistenceModule;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO d'accès aux seuils d'alertes configurés en base.
 */
public class ThresholdDAO {

    private static final Logger LOGGER = Logger.getLogger(ThresholdDAO.class.getName());
    private final PersistenceModule pm;

    /**
     * @param pm module de persistance
     */
    public ThresholdDAO(PersistenceModule pm) {
        this.pm = pm;
    }

    /**
     * Retourne tous les seuils configurés.
     *
     * @return liste de {@link ThresholdConfig}
     * @throws SQLException en cas d'erreur SQL
     */
    public List<ThresholdConfig> findAll() throws SQLException {
        List<ThresholdConfig> list = new ArrayList<>();
        String sql = "SELECT metric, warning_value, critical_value FROM thresholds";
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new ThresholdConfig(
                        rs.getString("metric"),
                        rs.getDouble("warning_value"),
                        rs.getDouble("critical_value")
                ));
            }
        }
        return list;
    }

    /**
     * Insère ou met à jour le seuil d'une métrique.
     *
     * @param config configuration à persister
     * @throws SQLException en cas d'erreur SQL
     */
    public void upsert(ThresholdConfig config) throws SQLException {
        String sql = "INSERT INTO thresholds (metric, warning_value, critical_value) VALUES (?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE warning_value=VALUES(warning_value), "
                   + "critical_value=VALUES(critical_value)";
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, config.getMetric());
            ps.setDouble(2, config.getWarningValue());
            ps.setDouble(3, config.getCriticalValue());
            ps.executeUpdate();
        }
    }
}
