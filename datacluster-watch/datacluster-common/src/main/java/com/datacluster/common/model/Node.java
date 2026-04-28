package com.datacluster.common.model;

import com.datacluster.common.enums.NodeStatus;
import com.datacluster.common.enums.NodeType;

import java.io.Serial;
import java.io.Serializable;

/**
 * Nœud physique ou virtuel appartenant au cluster Big Data.
 */
public class Node implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String     id;
    private String     name;
    private NodeType   type;
    private NodeStatus status;

    public Node() {}

    /**
     * @param id     identifiant unique du nœud
     * @param name   nom lisible (ex. worker-01)
     * @param type   rôle dans le cluster
     * @param status état opérationnel initial
     */
    public Node(String id, String name, NodeType type, NodeStatus status) {
        this.id     = id;
        this.name   = name;
        this.type   = type;
        this.status = status;
    }

    // ─── Getters / Setters ─────────────────────────────────────────────────────

    public String     getId()            { return id; }
    public void       setId(String id)  { this.id = id; }

    public String     getName()              { return name; }
    public void       setName(String name)   { this.name = name; }

    public NodeType   getType()               { return type; }
    public void       setType(NodeType type)  { this.type = type; }

    public NodeStatus getStatus()                   { return status; }
    public void       setStatus(NodeStatus status)  { this.status = status; }

    @Override
    public String toString() {
        return "Node{id='" + id + "', name='" + name
                + "', type=" + type + ", status=" + status + '}';
    }
}
