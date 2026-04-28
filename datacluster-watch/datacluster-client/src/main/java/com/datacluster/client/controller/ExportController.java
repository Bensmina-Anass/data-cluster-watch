package com.datacluster.client.controller;

import com.datacluster.client.service.RmiClientService;
import com.datacluster.common.model.Alert;
import com.datacluster.common.model.Metric;
import com.datacluster.common.model.Node;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur de la vue d'export — génère des fichiers CSV et PDF.
 */
public class ExportController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ExportController.class.getName());
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @FXML private ComboBox<String>  cmbExportType;
    @FXML private ComboBox<String>  cmbFormat;
    @FXML private ComboBox<String>  cmbNode;
    @FXML private Button            btnExport;
    @FXML private Label             lblStatus;
    @FXML private TextArea          txtPreview;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbExportType.getItems().addAll("Métriques", "Alertes", "Nœuds");
        cmbExportType.setValue("Métriques");
        cmbFormat.getItems().addAll("CSV", "PDF");
        cmbFormat.setValue("CSV");

        loadNodes();
        btnExport.setOnAction(e -> export());
    }

    private void loadNodes() {
        cmbNode.getItems().add("TOUS");
        RmiClientService rmi = RmiClientService.getInstance();
        if (!rmi.isConnected()) return;
        try {
            rmi.getClusterService().getAllNodes()
               .forEach(n -> cmbNode.getItems().add(n.getId()));
        } catch (RemoteException e) {
            LOGGER.log(Level.WARNING, "Cannot load nodes for export", e);
        }
        cmbNode.setValue("TOUS");
    }

    private void export() {
        String type   = cmbExportType.getValue();
        String format = cmbFormat.getValue();
        String nodeId = cmbNode.getValue();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter vers…");
        if ("CSV".equals(format)) {
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV", "*.csv"));
        } else {
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        }
        File file = chooser.showSaveDialog(btnExport.getScene().getWindow());
        if (file == null) return;

        try {
            switch (type) {
                case "Métriques" -> exportMetrics(file, format, nodeId);
                case "Alertes"   -> exportAlerts(file, format, nodeId);
                case "Nœuds"     -> exportNodes(file, format);
            }
            lblStatus.setText("Export réussi → " + file.getName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Export failed", e);
            lblStatus.setText("Erreur : " + e.getMessage());
        }
    }

    private void exportMetrics(File file, String format, String nodeId) throws Exception {
        RmiClientService rmi = RmiClientService.getInstance();
        List<Metric> metrics = "TOUS".equals(nodeId)
                ? rmi.getClusterService().getLatestMetrics()
                : rmi.getStatsService().getMetricHistory(nodeId, 60);

        if ("CSV".equals(format)) {
            try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8)) {
                pw.println("node_id,timestamp,cpu,ram,disk,active_jobs,failed_jobs");
                for (Metric m : metrics) {
                    pw.printf("%s,%s,%.2f,%.2f,%.2f,%d,%d%n",
                            m.getNodeId(),
                            FMT.format(Instant.ofEpochMilli(m.getTimestamp())),
                            m.getCpu(), m.getRam(), m.getDisk(),
                            m.getActiveJobs(), m.getFailedJobs());
                }
            }
        } else {
            exportMetricsPdf(file, metrics);
        }
        txtPreview.setText("Export " + format + " — " + metrics.size() + " ligne(s)");
    }

    private void exportAlerts(File file, String format, String nodeId) throws Exception {
        RmiClientService rmi = RmiClientService.getInstance();
        List<Alert> alerts = "TOUS".equals(nodeId)
                ? rmi.getAlertService().getAllAlerts()
                : rmi.getAlertService().getAlertsByNode(nodeId);

        if ("CSV".equals(format)) {
            try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8)) {
                pw.println("id,node_id,timestamp,type,level,message,acknowledged");
                for (Alert a : alerts) {
                    pw.printf("%s,%s,%s,%s,%s,\"%s\",%b%n",
                            a.getId(), a.getNodeId(),
                            FMT.format(Instant.ofEpochMilli(a.getTimestamp())),
                            a.getType(), a.getLevel(),
                            a.getMessage().replace("\"", "'"),
                            a.isAcknowledged());
                }
            }
        } else {
            exportAlertsPdf(file, alerts);
        }
        txtPreview.setText("Export " + format + " — " + alerts.size() + " alerte(s)");
    }

    private void exportNodes(File file, String format) throws Exception {
        RmiClientService rmi = RmiClientService.getInstance();
        List<Node> nodes = rmi.getClusterService().getAllNodes();

        if ("CSV".equals(format)) {
            try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8)) {
                pw.println("id,name,type,status");
                for (Node n : nodes) {
                    pw.printf("%s,%s,%s,%s%n",
                            n.getId(), n.getName(), n.getType(), n.getStatus());
                }
            }
        }
        txtPreview.setText("Export " + format + " — " + nodes.size() + " nœud(s)");
    }

    private void exportMetricsPdf(File file, List<Metric> metrics) throws Exception {
        try (PdfWriter writer = new PdfWriter(file);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            doc.add(new Paragraph("DataCluster Watch — Export Métriques")
                    .setBold().setFontSize(16));
            doc.add(new Paragraph("Généré le : " + FMT.format(Instant.now())));

            float[] widths = {80, 120, 50, 50, 50, 60, 60};
            Table table = new Table(widths);
            for (String h : List.of("Nœud", "Timestamp", "CPU%", "RAM%", "Disk%", "Jobs Act.", "Jobs Éch.")) {
                table.addHeaderCell(new Cell().add(new Paragraph(h).setBold()));
            }
            for (Metric m : metrics) {
                table.addCell(m.getNodeId());
                table.addCell(FMT.format(Instant.ofEpochMilli(m.getTimestamp())));
                table.addCell(String.format("%.1f", m.getCpu()));
                table.addCell(String.format("%.1f", m.getRam()));
                table.addCell(String.format("%.1f", m.getDisk()));
                table.addCell(String.valueOf(m.getActiveJobs()));
                table.addCell(String.valueOf(m.getFailedJobs()));
            }
            doc.add(table);
        }
    }

    private void exportAlertsPdf(File file, List<Alert> alerts) throws Exception {
        try (PdfWriter writer = new PdfWriter(file);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            doc.add(new Paragraph("DataCluster Watch — Export Alertes")
                    .setBold().setFontSize(16));
            doc.add(new Paragraph("Généré le : " + FMT.format(Instant.now())));

            float[] widths = {80, 120, 70, 70, 200, 50};
            Table table = new Table(widths);
            for (String h : List.of("Nœud", "Timestamp", "Type", "Niveau", "Message", "ACK")) {
                table.addHeaderCell(new Cell().add(new Paragraph(h).setBold()));
            }
            for (Alert a : alerts) {
                table.addCell(a.getNodeId());
                table.addCell(FMT.format(Instant.ofEpochMilli(a.getTimestamp())));
                table.addCell(a.getType().name());
                table.addCell(a.getLevel().name());
                table.addCell(a.getMessage());
                table.addCell(a.isAcknowledged() ? "Oui" : "Non");
            }
            doc.add(table);
        }
    }
}
