package io.github.dsheirer.module.control;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.FileWriter;
import java.io.IOException;

public class ZelloConfigOrchestrator {

    public static void generateDocumentation(String outputPath) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        
        JsonArray modules = new JsonArray();
        
        JsonObject quickConnect = new JsonObject();
        quickConnect.addProperty("module", "QuickConnect");
        quickConnect.addProperty("description", "How to configure Zello QuickConnect for rapid PTT setup.");
        quickConnect.addProperty("steps", "1. Open Zello Management Console.\n2. Navigate to QuickConnect settings.\n3. Enable QuickConnect for active groups.");
        modules.add(quickConnect);
        
        JsonObject playbackBuffers = new JsonObject();
        playbackBuffers.addProperty("module", "Playback Buffers");
        playbackBuffers.addProperty("description", "Tuning audio playback buffers for simulcast.");
        playbackBuffers.addProperty("steps", "1. Access the streaming editor.\n2. Set stream guard to 500ms.\n3. Verify Opus settings.");
        modules.add(playbackBuffers);
        
        try (FileWriter writer = new FileWriter(outputPath)) {
            gson.toJson(modules, writer);
        }
    }
}
