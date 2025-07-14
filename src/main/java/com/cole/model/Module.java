package com.cole.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Module {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty moduleCode;
    private final SimpleStringProperty name;
    private final SimpleIntegerProperty passRate;  // Must match "pass_rate" column

    public Module(int id, String moduleCode, String name, int passRate) {
        this.id = new SimpleIntegerProperty(id);
        this.moduleCode = new SimpleStringProperty(moduleCode);
        this.name = new SimpleStringProperty(name);
        this.passRate = new SimpleIntegerProperty(passRate);
    }

    public int getId() {
        return id.get();
    }

    public String getModuleCode() {
        return moduleCode.get();
    }

    public String getName() {
        return name.get();
    }

    public int getPassRate() {
        return passRate.get();
    }

    // Properties for JavaFX TableView bindings
    public SimpleStringProperty moduleCodeProperty() {
        return moduleCode;
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public SimpleIntegerProperty passRateProperty() {
        return passRate;
    }
}
