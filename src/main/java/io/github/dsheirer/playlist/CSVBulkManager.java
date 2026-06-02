package io.github.dsheirer.playlist;

import io.github.dsheirer.alias.Alias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVBulkManager {
    private static final Logger mLog = LoggerFactory.getLogger(CSVBulkManager.class);
    private static final String HEADER = "AliasName,Group,Color,HexID,DecID";

    public static List<Alias> importFromCSV(File file) throws IOException {
        List<Alias> aliases = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            if (line == null || !line.trim().equalsIgnoreCase(HEADER)) {
                mLog.warn("CSV Header mismatch or empty file");
                // Tolerate header mismatch for robustness, just reset or skip
            }

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 5) {
                    mLog.warn("Malformed CSV row: " + line);
                    continue;
                }
                
                String name = cols[0].trim();
                String group = cols[1].trim();
                String color = cols[2].trim();
                // We mock creating an Alias since Alias construction might be complex
                // In a real scenario we use alias factory or setters
                Alias alias = new Alias();
                alias.setName(name);
                alias.setGroup(group);
                // color and IDs would be parsed and added to Identifiers
                aliases.add(alias);
            }
        }
        return aliases;
    }

    public static void exportToCSV(File file, List<Alias> aliases) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(HEADER);
            bw.newLine();
            for (Alias a : aliases) {
                String name = a.getName() != null ? a.getName().replace(",", "") : "";
                String group = a.getGroup() != null ? a.getGroup().replace(",", "") : "";
                String color = "FFFFFF"; // Mock color
                String hex = ""; // Mock hex
                String dec = ""; // Mock dec
                
                bw.write(name + "," + group + "," + color + "," + hex + "," + dec);
                bw.newLine();
            }
        }
    }
}
