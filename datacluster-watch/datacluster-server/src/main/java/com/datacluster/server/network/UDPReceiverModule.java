package com.datacluster.server.network;

import com.datacluster.common.model.Metric;
import com.datacluster.common.util.JsonSerializer;
import com.datacluster.server.processor.MetricsProcessor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Module de réception UDP.
 * Écoute sur le port configuré, parse les datagrammes JSON et dispatche
 * vers le {@link MetricsProcessor}.
 */
public class UDPReceiverModule extends Thread {

    private static final Logger LOGGER = Logger.getLogger(UDPReceiverModule.class.getName());
    private static final int BUFFER_SIZE = 65_535;

    private final int              port;
    private final MetricsProcessor processor;
    private DatagramSocket         socket;

    /**
     * @param port      port d'écoute UDP
     * @param processor processeur de métriques destination
     */
    public UDPReceiverModule(int port, MetricsProcessor processor) {
        super("udp-receiver");
        setDaemon(true);
        this.port      = port;
        this.processor = processor;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port);
            LOGGER.info("UDPReceiverModule listening on port " + port);
            byte[] buffer = new byte[BUFFER_SIZE];

            while (!isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String payload = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                dispatch(payload);
            }
        } catch (SocketException e) {
            if (!isInterrupted()) {
                LOGGER.log(Level.SEVERE, "UDP socket error", e);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "UDPReceiverModule error", e);
        } finally {
            if (socket != null && !socket.isClosed()) socket.close();
            LOGGER.info("UDPReceiverModule stopped");
        }
    }

    private void dispatch(String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            String type = json.has("type") ? json.get("type").getAsString() : "METRIC";

            switch (type) {
                case "HEARTBEAT" -> {
                    String nodeId = json.has("nodeId") ? json.get("nodeId").getAsString() : null;
                    if (nodeId != null) processor.processHeartbeat(nodeId);
                }
                case "METRIC" -> {
                    Metric metric = JsonSerializer.getGson().fromJson(json, Metric.class);
                    processor.processMetric(metric);
                }
                default -> LOGGER.warning("Unknown UDP message type: " + type);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse UDP payload: " + payload, e);
        }
    }

    /** Arrête le module de réception. */
    public void shutdown() {
        interrupt();
        if (socket != null) socket.close();
    }
}
