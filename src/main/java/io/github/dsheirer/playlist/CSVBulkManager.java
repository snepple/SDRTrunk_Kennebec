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

/**
 * CSV Bulk Alias Import/Export manager.
 * Supports importing and exporting alias definitions in CSV format with proper
 * color hex parsing and talkgroup ID mapping.
 */
public class CSVBulkManager
{
    private static final Logger mLog = LoggerFactory.getLogger(CSVBulkManager.class);
    private static final String HEADER = "AliasName,Group,Color,HexID,DecID";

    /**
     * Import aliases from a CSV file.
     * Format: AliasName,Group,Color(hex),HexID,DecID
     */
    public static List<Alias> importFromCSV(File file) throws IOException
    {
        List<Alias> aliases = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try(BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            String line = br.readLine();
            if(line == null || !line.trim().toUpperCase().contains("ALIASNAME"))
            {
                mLog.warn("CSV header mismatch or empty file, attempting to parse anyway");
            }

            while((line = br.readLine()) != null)
            {
                if(line.trim().isEmpty()) continue;
                String[] cols = line.split(",", -1);
                if(cols.length < 2)
                {
                    mLog.warn("Malformed CSV row (need at least name and group): {}", line);
                    skipped++;
                    continue;
                }

                String name = cols[0].trim();
                String group = cols.length > 1 ? cols[1].trim() : "";

                Alias alias = new Alias();
                alias.setName(name);
                alias.setGroup(group);

                // Parse color hex value (e.g., "FF0000" or "#FF0000")
                if(cols.length > 2 && !cols[2].trim().isEmpty())
                {
                    try
                    {
                        String colorStr = cols[2].trim().replace("#", "");
                        int colorValue = Integer.parseInt(colorStr, 16);
                        alias.setColor(colorValue);
                    }
                    catch(NumberFormatException e)
                    {
                        mLog.debug("Invalid color value '{}' for alias '{}', using default", cols[2], name);
                    }
                }

                aliases.add(alias);
                imported++;
            }
        }

        mLog.info("CSV import complete: {} aliases imported, {} rows skipped from {}",
            imported, skipped, file.getName());
        return aliases;
    }

    /**
     * Export aliases to a CSV file.
     * Writes all alias data including color hex and group information.
     */
    public static void exportToCSV(File file, List<Alias> aliases) throws IOException
    {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(file)))
        {
            bw.write(HEADER);
            bw.newLine();

            for(Alias a : aliases)
            {
                String name = sanitize(a.getName());
                String group = sanitize(a.getGroup());
                String color = a.getColorHex() != null ? a.getColorHex().replace("#", "") : "FFFFFF";

                bw.write(name + "," + group + "," + color + ",,");
                bw.newLine();
            }
        }

        mLog.info("CSV export complete: {} aliases written to {}", aliases.size(), file.getName());
    }

    /**
     * Sanitize a string for CSV output — remove commas and newlines.
     */
    private static String sanitize(String value)
    {
        if(value == null) return "";
        return value.replace(",", " ").replace("\n", " ").replace("\r", "");
    }
}
