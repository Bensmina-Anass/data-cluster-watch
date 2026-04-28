package com.datacluster.agent.thread;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread dédié à l'envoi de datagrammes UDP vers le serveur central.
 * Les messages sont mis en file par les producteurs et consommés en séquence.
 */
public class UDPSenderThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger(UDPSenderThread.class.getName());

    private final String serverHost;
    private final int    serverPort;
    private final BlockingQueue<String> queue;

    private DatagramSocket socket;

    /**
     * @param serverHost hôte du serveur (hostname ou IP)
     * @param serverPort port UDP du serveur
     */
    public UDPSenderThread(String serverHost, int serverPort) {
        super("udp-sender");
        setDaemon(true);
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.queue      = new LinkedBlockingQueue<>(512);
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(serverHost);
            LOGGER.info("UDPSenderThread ready → " + serverHost + ":" + serverPort);

            while (!isInterrupted()) {
                String payload = queue.take();
                byte[] data = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, serverPort);
                socket.send(packet);
                LOGGER.finest("UDP sent (" + data.length + " bytes)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (SocketException e) {
            LOGGER.log(Level.SEVERE, "UDP socket error", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "UDPSenderThread error", e);
        } finally {
            if (socket != null && !socket.isClosed()) socket.close();
            LOGGER.info("UDPSenderThread stopped");
        }
    }

    /**
     * Enfile un message JSON pour envoi asynchrone.
     * Si la file est pleine, le message est abandonné avec un avertissement.
     *
     * @param jsonPayload message à envoyer
     */
    public void enqueue(String jsonPayload) {
        if (!queue.offer(jsonPayload)) {
            LOGGER.warning("UDP send queue full, dropping message");
        }
    }
}
