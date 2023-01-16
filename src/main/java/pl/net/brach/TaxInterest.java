package pl.net.brach;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class TaxInterest extends Application {

    static final String BRACHSOFT_TITLE = "BRACHSoft - Odsetki podatkowe v.1.2";
    static final String ICON_PATH = "pl/net/brach/brachicon.png";
    static final String STYLE_PATH = "pl/net/brach/style.css";

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("MainWindow.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(STYLE_PATH);

        stage.setTitle(BRACHSOFT_TITLE);
        stage.getIcons().add(new Image(ICON_PATH));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    protected static void displaySummary(String[] args) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(TaxInterest.class.getResource("Summary.fxml"));
        Pane root = fxmlLoader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(STYLE_PATH);

        Stage stage = new Stage();

        stage.setTitle(BRACHSOFT_TITLE + " - Podsumowanie");
        stage.getIcons().add(new Image(ICON_PATH));
        stage.setScene(scene);
        stage.setResizable(false);

        SummaryController summaryController = fxmlLoader.getController();
        summaryController.populateSummaryFields(args);

        stage.show();
    }
}