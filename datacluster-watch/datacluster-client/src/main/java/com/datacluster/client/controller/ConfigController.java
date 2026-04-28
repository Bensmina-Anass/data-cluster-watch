package com.datacluster.client.controller;

import com.datacluster.client.service.RmiClientService;
import com.datacluster.common.model.ThresholdConfig;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur du formulaire de configuration des seuils d'alerte.
 */
public class ConfigController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ConfigController.class.getName());

    @FXML private TextField  txtCpuWarning;
    @FXML private TextField  txtCpuCritical;
    @FXML private TextField  txtRamWarning;
    @FXML private TextField  txtRamCritical;
    @FXML private TextField  txtDiskWarning;
    @FXML private TextField  txtDiskCritical;
    @FXML private Button     btnSave;
    @FXML private Button     btnLoad;
    @FXML private Label      lblStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        btnLoad.setOnAction(e -> loadThresholds());
        btnSave.setOnAction(e -> saveThresholds());
        loadThresholds();
    }

    private void loadThresholds() {
        RmiClientService rmi = RmiClientService.getInstance();
        if (!rmi.isConnected()) { lblStatus.setText("Non connecté"); return; }
        try {
            List<ThresholdConfig> configs = rmi.getConfigService().getThresholds();
            for (ThresholdConfig c : configs) {
                switch (c.getMetric()) {
                    case "cpu"  -> { txtCpuWarning.setText(String.valueOf(c.getWarningValue()));
                                     txtCpuCritical.setText(String.valueOf(c.getCriticalValue())); }
                    case "ram"  -> { txtRamWarning.setText(String.valueOf(c.getWarningValue()));
                                     txtRamCritical.setText(String.valueOf(c.getCriticalValue())); }
                    case "disk" -> { txtDiskWarning.setText(String.valueOf(c.getWarningValue()));
                                     txtDiskCritical.setText(String.valueOf(c.getCriticalValue())); }
                }
            }
            lblStatus.setText("Seuils chargés");
        } catch (RemoteException e) {
            LOGGER.log(Level.WARNING, "Cannot load thresholds", e);
            lblStatus.setText("Erreur de chargement");
        }
    }

    private void saveThresholds() {
        RmiClientService rmi = RmiClientService.getInstance();
        if (!rmi.isConnected()) { lblStatus.setText("Non connecté"); return; }
        try {
            rmi.getConfigService().setThreshold("cpu",
                    parseDouble(txtCpuWarning), parseDouble(txtCpuCritical));
            rmi.getConfigService().setThreshold("ram",
                    parseDouble(txtRamWarning), parseDouble(txtRamCritical));
            rmi.getConfigService().setThreshold("disk",
                    parseDouble(txtDiskWarning), parseDouble(txtDiskCritical));
            lblStatus.setText("Seuils enregistrés avec succès");
        } catch (RemoteException | IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Cannot save thresholds", e);
            lblStatus.setText("Erreur : " + e.getMessage());
        }
    }

    private double parseDouble(TextField field) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valeur invalide : " + field.getText());
        }
    }
}
