package com.datacluster.server.network;

import com.datacluster.common.model.Alert;
import com.datacluster.common.util.JsonSerializer;
import com.datacluster.server.dao.AlertDAO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Module de réception TCP des alertes critiques envoyées par les agents.
 * Utilise un pool de threads pour traiter les connexions concurrentes.
 */
public class TCPReceiverModule extends Thread {

    private static final Logger LOGGER = Logger.getLogger(TCPReceiverModule.class.getName());

    private final int            port;
    private final AlertDAO       alertDAO;
    private final ExecutorService pool;
    private ServerSocket         serverSocket;

    /**
     * @param port       port d'écoute TCP
     * @param alertDAO   DAO pour persister les alertes reçues
     * @param poolSize   taille du pool de threads de traitement
     */
    public TCPReceiverModule(int port, AlertDAO alertDAO, int poolSize) {
        super("tcp-receiver");
        setDaemon(true);
        this.port     = port;
        this.alertDAO = alertDAO;
        this.pool     = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "tcp-handler-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            LOGGER.info("TCPReceiverModule listening on port " + port);

            while (!isInterrupted()) {
                Socket client = serverSocket.accept();
                pool.submit(() -> handleClient(client));
            }
        } catch (Exception e) {
            if (!isInterrupted()) {
                LOGGER.log(Level.SEVERE, "TCPReceiverModule error", e);
            }
        } finally {
            shutdown();
            LOGGER.info("TCPReceiverModule stopped");
        }
    }

    private void handleClient(Socket client) {
        try (client;
             BufferedReader in  = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter    out = new PrintWriter(client.getOutputStream(), true)) {

            String line = in.readLine();
            if (line == null || line.isBlank()) return;

            Alert alert = JsonSerializer.fromJson(line, Alert.class);
            alertDAO.save(alert);
            LOGGER.info("Alert received via TCP: " + alert.getId()
                    + " [" + alert.getLevel() + "] from " + alert.getNodeId());
            out.println("ACK");

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to persist TCP alert", e);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "TCP handler error", e);
        }
    }

    /** Arrête le module proprement. */
    public void shutdown() {
        interrupt();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
            pool.shutdown();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during TCP shutdown", e);
        }
    }
}
