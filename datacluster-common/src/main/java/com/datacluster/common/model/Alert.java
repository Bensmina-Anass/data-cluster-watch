package com.datacluster.common.model;

import com.datacluster.common.enums.AlertLevel;
import com.datacluster.common.enums.AlertType;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Alerte générée par le moteur de détection côté serveur.
 */
public class Alert implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String     id;
    private String     nodeId;
    private long       timestamp;
    private AlertType  type;
    private AlertLevel level;
    private String     message;
    private boolean    acknowledged;

    public Alert() {}

    /**
     * @param nodeId    identifiant du nœud concerné
     * @param timestamp epoch ms de déclenchement
     * @param type      type d'alerte
     * @param level     niveau de sévérité
     * @param message   description lisible
     */
    public Alert(String nodeId, long timestamp,
                 AlertType type, AlertLevel level, String message) {
        this.id           = UUID.randomUUID().toString();
        this.nodeId       = nodeId;
        this.timestamp    = timestamp;
        this.type         = type;
        this.level        = level;
        this.message      = message;
        this.acknowledged = false;
    }

    // ─── Getters / Setters ─────────────────────────────────────────────────────

    public String     getId()                { return id; }
    public void       setId(String id)       { this.id = id; }

    public String     getNodeId()               { return nodeId; }
    public void       setNodeId(String nodeId)  { this.nodeId = nodeId; }

    public long       getTimestamp()                { return timestamp; }
    public void       setTimestamp(long timestamp)  { this.timestamp = timestamp; }

    public AlertType  getType()                 { return type; }
    public void       setType(AlertType type)   { this.type = type; }

    public AlertLevel getLevel()                  { return level; }
    public void       setLevel(AlertLevel level)  { this.level = level; }

    public String     getMessage()                 { return message; }
    public void       setMessage(String message)   { this.message = message; }

    public boolean    isAcknowledged()                      { return acknowledged; }
    public void       setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }

    @Override
    public String toString() {
        return "Alert{id='" + id + "', nodeId='" + nodeId
                + "', level=" + level + ", type=" + type
                + ", ack=" + acknowledged + '}';
    }
}
