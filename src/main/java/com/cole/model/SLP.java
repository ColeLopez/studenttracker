package com.cole.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Represents a Student Learning Pathway (SLP).
 * Used for JavaFX TableView bindings and SLP management.
 */
public class SLP {
    /** SLP unique identifier */
    private final SimpleIntegerProperty id;
    /** SLP code (unique, e.g., for display and lookup) */
    private final SimpleStringProperty slpCode;
    /** SLP name (human-readable) */
    private final SimpleStringProperty name;

    /**
     * Constructs an SLP instance.
     * @param id SLP unique identifier
     * @param slpCode SLP code
     * @param name SLP name
     */
    public SLP(int id, String slpCode, String name) {
        this.id = new SimpleIntegerProperty(id);
        this.slpCode = new SimpleStringProperty(slpCode);
        this.name = new SimpleStringProperty(name);
    }

    /**
     * Gets the SLP ID.
     * @return SLP ID
     */
    public int getId() {
        return id.get();
    }

    /**
     * Gets the SLP code.
     * @return SLP code
     */
    public String getSlpCode() {
        return slpCode.get();
    }

    /**
     * Gets the SLP name.
     * @return SLP name
     */
    public String getName() {
        return name.get();
    }

    /**
     * Property for SLP code (JavaFX binding).
     * @return SimpleStringProperty for SLP code
     */
    public SimpleStringProperty slpCodeProperty() {
        return slpCode;
    }

    /**
     * Property for SLP name (JavaFX binding).
     * @return SimpleStringProperty for SLP name
     */
    public SimpleStringProperty nameProperty() {
        return name;
    }

    /**
     * Returns a human-readable label for the SLP.
     * @return SLP code and name concatenated
     */
    @Override
    public String toString() {
        return slpCode.get() + " - " + name.get();
    }
}
