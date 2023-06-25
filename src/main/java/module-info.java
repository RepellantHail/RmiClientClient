module com.example.rmiclientclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires java.rmi;

    opens com.example.rmiclientclient to javafx.fxml;
    exports com.example.rmiclientclient;
}