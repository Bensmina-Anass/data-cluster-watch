package com.datacluster.server.dao;

import com.datacluster.common.enums.NodeStatus;
import com.datacluster.common.enums.NodeType;
import com.datacluster.common.model.Node;
import com.datacluster.server.persistence.PersistenceModule;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO d'accès aux nœuds persistés en base.
 */
public class NodeDAO {

    private static final Logger LOGGER = Logger.getLogger(NodeDAO.class.getName());
    private final PersistenceModule pm;

    /**
     * @param pm module de persistance fournissant les connexions
     */
    public NodeDAO(PersistenceModule pm) {
        this.pm = pm;
    }

    /**
     * Insère un nouveau nœud en base.
     *
     * @param node nœud à persister
     * @throws SQLException en cas d'erreur SQL
     */
    public void save(Node node) throws SQLException {
        String sql = "INSERT INTO nodes (id, name, type, status) VALUES (?, ?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE name=VALUES(name), type=VALUES(type), status=VALUES(status)";
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, node.getId());
            ps.setString(2, node.getName());
            ps.setString(3, node.getType().name());
            ps.setString(4, node.getStatus().name());
            ps.executeUpdate();
        }
    }

    /**
     * Met à jour le statut d'un nœud existant.
     *
     * @param nodeId identifiant du nœud
     * @param status nouveau statut
     * @throws SQLException en cas d'erreur SQL
     */
    public void updateStatus(String nodeId, NodeStatus status) throws SQLException {
        String sql = "UPDATE nodes SET status = ? WHERE id = ?";
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, nodeId);
            ps.executeUpdate();
        }
    }

    /**
     * Recherche un nœud par son identifiant.
     *
     * @param id identifiant du nœud
     * @return nœud trouvé, ou {@code null}
     * @throws SQLException en cas d'erreur SQL
     */
    public Node findById(String id) throws SQLException {
        String sql = "SELECT id, name, type, status FROM nodes WHERE id = ?";
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /**
     * Retourne tous les nœuds enregistrés.
     *
     * @return liste de {@link Node}
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Node> findAll() throws SQLException {
        List<Node> nodes = new ArrayList<>();
        String sql = "SELECT id, name, type, status FROM nodes ORDER BY name";
        try (Connection conn = pm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                nodes.add(mapRow(rs));
            }
        }
        return nodes;
    }

    private Node mapRow(ResultSet rs) throws SQLException {
        return new Node(
                rs.getString("id"),
                rs.getString("name"),
                NodeType.valueOf(rs.getString("type")),
                NodeStatus.valueOf(rs.getString("status"))
        );
    }
}
