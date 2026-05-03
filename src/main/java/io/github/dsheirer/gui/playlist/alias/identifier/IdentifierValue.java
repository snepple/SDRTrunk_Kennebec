package io.github.dsheirer.gui.playlist.alias.identifier;

public class IdentifierValue {
    private Integer value;
    private String label;

    public IdentifierValue(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public Integer getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        if (label != null && !label.isEmpty()) {
            return String.valueOf(value) + " - " + label;
        }
        return String.valueOf(value);
    }
}
