package com.datacluster.client.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur du conteneur principal (navigation entre vues).
 */
public class MainController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    @FXML private StackPane contentPane;

    private final Map<String, Node> viewCache = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showDashboard();
    }

    @FXML public void showDashboard()  { loadView("dashboard");   }
    @FXML public void showNodeList()   { loadView("node-list");   }
    @FXML public void showNodeDetail() { loadView("node-detail"); }
    @FXML public void showAlerts()     { loadView("alert-view");  }
    @FXML public void showJobs()       { loadView("job-view");    }
    @FXML public void showStats()      { loadView("stats-view");  }
    @FXML public void showConfig()     { loadView("config-view"); }
    @FXML public void showExport()     { loadView("export-view"); }

    private void loadView(String name) {
        Node view = viewCache.computeIfAbsent(name, k -> {
            try {
                return FXMLLoader.load(
                        getClass().getResource("/fxml/" + k + ".fxml"));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Cannot load view: " + k, e);
                return null;
            }
        });
        if (view != null) {
            contentPane.getChildren().setAll(view);
        }
    }
}
