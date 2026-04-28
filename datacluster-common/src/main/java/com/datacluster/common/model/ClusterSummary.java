package com.datacluster.common.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Résumé agrégé de l'état du cluster, retourné via RMI.
 */
public class ClusterSummary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int    totalNodes;
    private int    activeNodes;
    private int    idleNodes;
    private int    downNodes;
    private int    totalAlerts;
    private int    criticalAlerts;
    private int    activeJobs;
    private double avgCpu;
    private double avgRam;
    private double avgDisk;
    private double clusterHealth;

    public ClusterSummary() {}

    // ─── Getters / Setters ─────────────────────────────────────────────────────

    public int    getTotalNodes()                   { return totalNodes; }
    public void   setTotalNodes(int totalNodes)     { this.totalNodes = totalNodes; }

    public int    getActiveNodes()                  { return activeNodes; }
    public void   setActiveNodes(int activeNodes)   { this.activeNodes = activeNodes; }

    public int    getIdleNodes()                    { return idleNodes; }
    public void   setIdleNodes(int idleNodes)       { this.idleNodes = idleNodes; }

    public int    getDownNodes()                    { return downNodes; }
    public void   setDownNodes(int downNodes)       { this.downNodes = downNodes; }

    public int    getTotalAlerts()                  { return totalAlerts; }
    public void   setTotalAlerts(int totalAlerts)   { this.totalAlerts = totalAlerts; }

    public int    getCriticalAlerts()                     { return criticalAlerts; }
    public void   setCriticalAlerts(int criticalAlerts)   { this.criticalAlerts = criticalAlerts; }

    public int    getActiveJobs()                   { return activeJobs; }
    public void   setActiveJobs(int activeJobs)     { this.activeJobs = activeJobs; }

    public double getAvgCpu()               { return avgCpu; }
    public void   setAvgCpu(double avgCpu)  { this.avgCpu = avgCpu; }

    public double getAvgRam()               { return avgRam; }
    public void   setAvgRam(double avgRam)  { this.avgRam = avgRam; }

    public double getAvgDisk()                { return avgDisk; }
    public void   setAvgDisk(double avgDisk)  { this.avgDisk = avgDisk; }

    /**
     * Score de santé global du cluster (0–100).
     * 100 = tous les nœuds actifs, aucune alerte critique.
     */
    public double getClusterHealth()                      { return clusterHealth; }
    public void   setClusterHealth(double clusterHealth)  { this.clusterHealth = clusterHealth; }
}
