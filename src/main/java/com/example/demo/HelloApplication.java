package com.example.demo;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Лабораторная работа по методам оптимизации");
        stage.setScene(scene);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.jpg")));
        stage.show();
        HelloController controller = fxmlLoader.getController();


    }

    public static void main(String[] args) {
        launch();
    }
}