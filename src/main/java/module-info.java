module com.duplicatefilefinder.duplicatefilefinder {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.fontawesome;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;


    opens com.duplicatefinder to javafx.fxml;
    exports com.duplicatefinder;
    exports com.duplicatefinder.utils;
    opens com.duplicatefinder.utils to javafx.fxml;
    exports com.duplicatefinder.models;
    opens com.duplicatefinder.models to javafx.fxml;
    exports com.duplicatefinder.controllers;
    opens com.duplicatefinder.controllers to javafx.fxml;
    exports com.duplicatefinder.services;
    opens com.duplicatefinder.services to javafx.fxml;
}