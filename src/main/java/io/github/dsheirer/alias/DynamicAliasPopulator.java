package io.github.dsheirer.alias;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.radio.Radio;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicAliasPopulator {
    private static final Logger mLog = LoggerFactory.getLogger(DynamicAliasPopulator.class);
    private static AliasModel mAliasModel;

    public static void init(AliasModel aliasModel) {
        mAliasModel = aliasModel;
    }

    public static void populate(IdentifierCollection identifierCollection) {
        if (mAliasModel == null || identifierCollection == null) return;
        
        for (Identifier id : identifierCollection.getIdentifiers()) {
            io.github.dsheirer.alias.id.AliasID targetId = null;
            if (id.getForm() == io.github.dsheirer.identifier.Form.TALKGROUP && id.getValue() instanceof Number) {
                targetId = new Talkgroup(id.getProtocol(), ((Number) id.getValue()).intValue());
            } else if (id.getForm() == io.github.dsheirer.identifier.Form.RADIO && id.getValue() instanceof Number) {
                targetId = new Radio(id.getProtocol(), ((Number) id.getValue()).intValue());
            }
            
            if (targetId != null) {
                checkAndCreate(targetId);
            }
        }
    }

    private static void checkAndCreate(io.github.dsheirer.alias.id.AliasID targetId) {
        if (!(targetId instanceof Talkgroup) && !(targetId instanceof Radio)) {
            return;
        }

        boolean exists = false;
        for (Alias alias : mAliasModel.getAliases()) {
            for (io.github.dsheirer.alias.id.AliasID aliasId : alias.getAliasIdentifiers()) {
                if (aliasId.equals(targetId)) {
                    exists = true;
                    break;
                }
            }
            if (exists) break;
        }

        if (!exists) {
            String name = "";
            if (targetId instanceof Talkgroup) {
                name = String.valueOf(((Talkgroup) targetId).getValue());
            } else if (targetId instanceof Radio) {
                name = String.valueOf(((Radio) targetId).getValue());
            }
            
            final Alias newAlias = new Alias("[" + name + "]");
            newAlias.setGroup("Auto-Populated");
            // Set random color or default
            
            // To be safe against concurrent modification, run on JavaFX thread
            Platform.runLater(() -> {
                // Double check it wasn't added in the meantime
                boolean stillExists = false;
                for (Alias alias : mAliasModel.getAliases()) {
                    for (io.github.dsheirer.alias.id.AliasID aliasId : alias.getAliasIdentifiers()) {
                        if (aliasId.equals(targetId)) {
                            stillExists = true;
                            break;
                        }
                    }
                    if (stillExists) break;
                }
                
                if (!stillExists) {
                    newAlias.addAliasID(targetId);
                    mAliasModel.addAlias(newAlias);
                    mLog.info("Auto-populated new alias: " + newAlias.getName());
                }
            });
        }
    }
}
