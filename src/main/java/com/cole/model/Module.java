package com.cole.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Represents a module in the system.
 * Used for JavaFX TableView bindings and module management.
 */
public class Module {
    /** Module unique identifier */
    private final SimpleIntegerProperty id;
    /** Module code (unique, e.g., for display and lookup) */
    private final SimpleStringProperty moduleCode;
    /** Module name (human-readable) */
    private final SimpleStringProperty name;
    /** Pass rate for the module (must match "pass_rate" column in DB) */
    private final SimpleIntegerProperty passRate;

    /**
     * Constructs a Module instance.
     * @param id Module unique identifier
     * @param moduleCode Module code
     * @param name Module name
     * @param passRate Pass rate for the module
     */
    public Module(int id, String moduleCode, String name, int passRate) {
        this.id = new SimpleIntegerProperty(id);
        this.moduleCode = new SimpleStringProperty(moduleCode);
        this.name = new SimpleStringProperty(name);
        this.passRate = new SimpleIntegerProperty(passRate);
    }

    /**
     * Gets the module ID.
     * @return Module ID
     */
    public int getId() {
        return id.get();
    }

    /**
     * Gets the module code.
     * @return Module code
     */
    public String getModuleCode() {
        return moduleCode.get();
    }

    /**
     * Gets the module name.
     * @return Module name
     */
    public String getName() {
        return name.get();
    }

    /**
     * Gets the pass rate for the module.
     * @return Pass rate
     */
    public int getPassRate() {
        return passRate.get();
    }

    /**
     * Property for module code (JavaFX binding).
     * @return SimpleStringProperty for module code
     */
    public SimpleStringProperty moduleCodeProperty() {
        return moduleCode;
    }

    /**
     * Property for module name (JavaFX binding).
     * @return SimpleStringProperty for module name
     */
    public SimpleStringProperty nameProperty() {
        return name;
    }

    /**
     * Property for pass rate (JavaFX binding).
     * @return SimpleIntegerProperty for pass rate
     */
    public SimpleIntegerProperty passRateProperty() {
        return passRate;
    }
}
