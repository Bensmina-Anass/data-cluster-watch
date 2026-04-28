package com.datacluster.common.model;

import com.datacluster.common.enums.JobStatus;
import com.datacluster.common.enums.JobType;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Représente un job Big Data en cours ou terminé sur un nœud.
 */
public class Job implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String    id;
    private String    nodeId;
    private JobType   type;
    private long      startTime;
    private long      endTime;
    private JobStatus status;
    private double    progress;

    public Job() {}

    /**
     * @param nodeId    identifiant du nœud exécutant le job
     * @param type      type de traitement Big Data
     * @param startTime epoch ms de démarrage
     */
    public Job(String nodeId, JobType type, long startTime) {
        this.id        = UUID.randomUUID().toString();
        this.nodeId    = nodeId;
        this.type      = type;
        this.startTime = startTime;
        this.endTime   = 0L;
        this.status    = JobStatus.RUNNING;
        this.progress  = 0.0;
    }

    // ─── Getters / Setters ─────────────────────────────────────────────────────

    public String    getId()             { return id; }
    public void      setId(String id)   { this.id = id; }

    public String    getNodeId()               { return nodeId; }
    public void      setNodeId(String nodeId)  { this.nodeId = nodeId; }

    public JobType   getType()               { return type; }
    public void      setType(JobType type)   { this.type = type; }

    public long      getStartTime()                { return startTime; }
    public void      setStartTime(long startTime)  { this.startTime = startTime; }

    public long      getEndTime()                { return endTime; }
    public void      setEndTime(long endTime)    { this.endTime = endTime; }

    public JobStatus getStatus()                   { return status; }
    public void      setStatus(JobStatus status)   { this.status = status; }

    public double    getProgress()                 { return progress; }
    public void      setProgress(double progress)  { this.progress = Math.min(100.0, Math.max(0.0, progress)); }

    @Override
    public String toString() {
        return "Job{id='" + id + "', nodeId='" + nodeId
                + "', type=" + type + ", status=" + status
                + ", progress=" + String.format("%.1f", progress) + "%" + '}';
    }
}
