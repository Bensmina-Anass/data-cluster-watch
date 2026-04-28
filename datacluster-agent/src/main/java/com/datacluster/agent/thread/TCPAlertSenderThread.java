package com.datacluster.agent.thread;

import com.datacluster.common.model.Alert;
import com.datacluster.common.util.JsonSerializer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread dédié à l'envoi d'alertes critiques via une connexion TCP.
 * Attend un ACK du serveur après chaque envoi.
 */
public class TCPAlertSenderThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger(TCPAlertSenderThread.class.getName());

    private final String serverHost;
    private final int    serverPort;
    private final BlockingQueue<Alert> queue;

    /**
     * @param serverHost hôte du serveur
     * @param serverPort port TCP du serveur
     */
    public TCPAlertSenderThread(String serverHost, int serverPort) {
        super("tcp-alert-sender");
        setDaemon(true);
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.queue      = new LinkedBlockingQueue<>(128);
    }

    @Override
    public void run() {
        LOGGER.info("TCPAlertSenderThread ready → " + serverHost + ":" + serverPort);
        while (!isInterrupted()) {
            try {
                Alert alert = queue.take();
                sendAlert(alert);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("TCPAlertSenderThread stopped");
    }

    private void sendAlert(Alert alert) {
        try (Socket socket = new Socket(serverHost, serverPort);
             PrintWriter  out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String json = JsonSerializer.toJson(alert);
            out.println(json);
            LOGGER.info("TCP alert sent: " + alert.getId() + " level=" + alert.getLevel());

            String ack = in.readLine();
            if (!"ACK".equals(ack)) {
                LOGGER.warning("Unexpected ACK from server: " + ack);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send alert via TCP: " + alert.getId(), e);
        }
    }

    /**
     * Enfile une alerte critique pour envoi asynchrone.
     *
     * @param alert alerte à envoyer
     */
    public void enqueue(Alert alert) {
        if (!queue.offer(alert)) {
            LOGGER.warning("TCP alert queue full, dropping alert: " + alert.getId());
        }
    }
}
