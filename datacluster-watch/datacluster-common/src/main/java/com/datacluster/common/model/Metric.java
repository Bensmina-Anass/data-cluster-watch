package com.datacluster.common.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Instantané de métriques système collecté sur un nœud du cluster.
 */
public class Metric implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String nodeId;
    private long   timestamp;
    private double cpu;
    private double ram;
    private double disk;
    private int    activeJobs;
    private int    failedJobs;

    public Metric() {}

    /**
     * @param nodeId     identifiant du nœud source
     * @param timestamp  epoch ms de la collecte
     * @param cpu        utilisation CPU en pourcentage (0–100)
     * @param ram        utilisation RAM en pourcentage (0–100)
     * @param disk       utilisation disque en pourcentage (0–100)
     * @param activeJobs nombre de jobs actifs au moment de la collecte
     * @param failedJobs nombre de jobs en échec depuis le dernier reset
     */
    public Metric(String nodeId, long timestamp,
                  double cpu, double ram, double disk,
                  int activeJobs, int failedJobs) {
        this.nodeId     = nodeId;
        this.timestamp  = timestamp;
        this.cpu        = cpu;
        this.ram        = ram;
        this.disk       = disk;
        this.activeJobs = activeJobs;
        this.failedJobs = failedJobs;
    }

    // ─── Getters / Setters ─────────────────────────────────────────────────────

    public String getNodeId()               { return nodeId; }
    public void   setNodeId(String nodeId)  { this.nodeId = nodeId; }

    public long   getTimestamp()                { return timestamp; }
    public void   setTimestamp(long timestamp)  { this.timestamp = timestamp; }

    public double getCpu()              { return cpu; }
    public void   setCpu(double cpu)    { this.cpu = cpu; }

    public double getRam()              { return ram; }
    public void   setRam(double ram)    { this.ram = ram; }

    public double getDisk()             { return disk; }
    public void   setDisk(double disk)  { this.disk = disk; }

    public int  getActiveJobs()                 { return activeJobs; }
    public void setActiveJobs(int activeJobs)   { this.activeJobs = activeJobs; }

    public int  getFailedJobs()                 { return failedJobs; }
    public void setFailedJobs(int failedJobs)   { this.failedJobs = failedJobs; }

    @Override
    public String toString() {
        return "Metric{nodeId='" + nodeId + "', ts=" + timestamp
                + ", cpu=" + String.format("%.1f", cpu)
                + ", ram=" + String.format("%.1f", ram)
                + ", disk=" + String.format("%.1f", disk)
                + ", activeJobs=" + activeJobs
                + ", failedJobs=" + failedJobs + '}';
    }
}
