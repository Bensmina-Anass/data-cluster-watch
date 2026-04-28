package com.datacluster.common;

import com.datacluster.common.model.Metric;
import com.datacluster.common.util.JsonSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du modèle {@link Metric} et de la sérialisation JSON.
 */
class MetricTest {

    @Test
    void constructorSetsAllFields() {
        long now = System.currentTimeMillis();
        Metric m = new Metric("worker-01", now, 55.5, 70.2, 40.0, 2, 0);

        assertEquals("worker-01", m.getNodeId());
        assertEquals(now,         m.getTimestamp());
        assertEquals(55.5,        m.getCpu(), 0.001);
        assertEquals(70.2,        m.getRam(), 0.001);
        assertEquals(40.0,        m.getDisk(), 0.001);
        assertEquals(2,           m.getActiveJobs());
        assertEquals(0,           m.getFailedJobs());
    }

    @Test
    void jsonRoundTripPreservesValues() {
        Metric original = new Metric("master-01", 1_700_000_000_000L, 30.0, 50.0, 60.0, 1, 0);
        String json = JsonSerializer.toJson(original);

        assertNotNull(json);
        assertTrue(json.contains("master-01"));

        Metric restored = JsonSerializer.fromJson(json, Metric.class);
        assertEquals(original.getNodeId(),    restored.getNodeId());
        assertEquals(original.getTimestamp(), restored.getTimestamp());
        assertEquals(original.getCpu(),       restored.getCpu(),  0.001);
        assertEquals(original.getRam(),       restored.getRam(),  0.001);
        assertEquals(original.getDisk(),      restored.getDisk(), 0.001);
    }

    @Test
    void settersWork() {
        Metric m = new Metric();
        m.setNodeId("storage-01");
        m.setCpu(95.0);
        m.setRam(85.0);
        m.setDisk(99.0);
        m.setActiveJobs(3);
        m.setFailedJobs(1);

        assertEquals("storage-01", m.getNodeId());
        assertEquals(95.0, m.getCpu(),  0.001);
        assertEquals(85.0, m.getRam(),  0.001);
        assertEquals(99.0, m.getDisk(), 0.001);
        assertEquals(3, m.getActiveJobs());
        assertEquals(1, m.getFailedJobs());
    }
}
