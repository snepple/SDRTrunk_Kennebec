package io.github.dsheirer.preference.ai;

import java.util.List;

public class GeminiModel {
    private String name;
    private String version;
    private String displayName;
    private String description;
    private List<String> supportedGenerationMethods;

    public GeminiModel(String name, String version, String displayName, String description, List<String> supportedGenerationMethods) {
        this.name = name;
        this.version = version;
        this.displayName = displayName;
        this.description = description;
        this.supportedGenerationMethods = supportedGenerationMethods;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getSupportedGenerationMethods() {
        return supportedGenerationMethods;
    }

    @Override
    public String toString() {
        return displayName != null ? displayName : name;
    }
}
