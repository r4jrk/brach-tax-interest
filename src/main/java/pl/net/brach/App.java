package pl.net.brach;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class App extends Application {

    public static List<List<String>> records = new ArrayList<>();

    private static final String BRACHSOFT_TITLE = "BRACHSoft - Odsetki podatkowe v.1.1";
    private static final String ICON_PATH = "pl/net/brach/brachs.png";
    private static Scene scene;
    private static Parent root;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("MainWindow.fxml"));
        root = (Parent) fxmlLoader.load();
        scene = new Scene(root);
        stage.setTitle(BRACHSOFT_TITLE);
        stage.getIcons().add(new Image(ICON_PATH));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    protected static void displaySummary(String[] args) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("SummaryWindow.fxml"));
        Pane root = (Pane) fxmlLoader.load();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle(BRACHSOFT_TITLE + " - Podsumowanie");
        stage.getIcons().add(new Image(ICON_PATH));
        stage.setResizable(false);

        SummaryWindowController swc = fxmlLoader.getController();
        swc.populateSummaryFields(args);

        stage.show();
    }
}