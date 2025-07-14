package com.cole.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class SLP {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty slpCode;
    private final SimpleStringProperty name;

    public SLP(int id, String slpCode, String name) {
        this.id = new SimpleIntegerProperty(id);
        this.slpCode = new SimpleStringProperty(slpCode);
        this.name = new SimpleStringProperty(name);
    }

    public int getId() {
        return id.get();
    }

    public String getSlpCode() {
        return slpCode.get();
    }

    public String getName() {
        return name.get();
    }

    public SimpleStringProperty slpCodeProperty() {
        return slpCode;
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    @Override
    public String toString() {
        return slpCode.get() + " - " + name.get();  // ✅ Human-readable label
    }
}
