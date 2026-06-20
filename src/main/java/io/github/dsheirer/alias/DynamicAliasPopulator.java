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

        //Capture the channel's alias list name so auto-created aliases land in the correct list
        String aliasListName = identifierCollection.getAliasListConfiguration() != null ?
            identifierCollection.getAliasListConfiguration().getValue() : null;

        for (Identifier id : identifierCollection.getIdentifiers()) {
            io.github.dsheirer.alias.id.AliasID targetId = null;
            //Only auto-create aliases for RADIO (subscriber unit) ids - e.g. P25/DMR radio ids.  Talkgroups
            //(including NBFM) are intentionally NOT auto-created: those are user-managed, and auto-creating
            //them produced duplicate/unwanted entries.
            if (id.getForm() == io.github.dsheirer.identifier.Form.RADIO && id.getValue() instanceof Number) {
                targetId = new Radio(id.getProtocol(), ((Number) id.getValue()).intValue());
            }

            if (targetId != null) {
                checkAndCreate(targetId, aliasListName);
            }
        }
    }

    /**
     * Finds stream (broadcast channel) names that every existing talkgroup alias in the alias list is
     * assigned to.  A newly discovered talkgroup inherits those assignments, so a user who streamed an
     * entire system doesn't have to manually add each newly-discovered talkgroup to the stream.
     */
    private static java.util.Set<String> getInheritedStreamNames(String aliasListName) {
        java.util.Set<String> common = null;

        for (Alias alias : mAliasModel.getAliases()) {
            if (aliasListName.equals(alias.getAliasListName()) && hasTalkgroupIdentifier(alias)) {
                java.util.Set<String> streams = new java.util.HashSet<>();

                for (io.github.dsheirer.alias.id.broadcast.BroadcastChannel broadcastChannel : alias.getBroadcastChannels()) {
                    if (broadcastChannel.getChannelName() != null) {
                        streams.add(broadcastChannel.getChannelName());
                    }
                }

                if (common == null) {
                    common = streams;
                } else {
                    common.retainAll(streams);
                }

                if (common.isEmpty()) {
                    return common;
                }
            }
        }

        return common == null ? java.util.Collections.emptySet() : common;
    }

    /**
     * Indicates if the alias has at least one talkgroup identifier.
     */
    private static boolean hasTalkgroupIdentifier(Alias alias) {
        for (io.github.dsheirer.alias.id.AliasID aliasId : alias.getAliasIdentifiers()) {
            if (aliasId instanceof Talkgroup) {
                return true;
            }
        }

        return false;
    }

    private static void checkAndCreate(io.github.dsheirer.alias.id.AliasID targetId, String aliasListName) {
        if (!(targetId instanceof Talkgroup) && !(targetId instanceof Radio)) {
            return;
        }

        boolean exists = false;
        for (Alias alias : mAliasModel.getAliases()) {
            for (io.github.dsheirer.alias.id.AliasID aliasId : alias.getAliasIdentifiers()) {
                if (aliasId.matches(targetId)) {
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

            //Assign to the originating channel's alias list so the alias is active for that channel
            if (aliasListName != null && !aliasListName.isEmpty()) {
                newAlias.setAliasListName(aliasListName);

                //Newly discovered talkgroups inherit stream assignments common to every existing
                //talkgroup alias in the list (e.g. when the user streamed the entire system)
                if (targetId instanceof Talkgroup) {
                    for (String stream : getInheritedStreamNames(aliasListName)) {
                        newAlias.addAliasID(new io.github.dsheirer.alias.id.broadcast.BroadcastChannel(stream));
                    }
                }
            }

            // To be safe against concurrent modification, run on JavaFX thread
            Platform.runLater(() -> {
                // Double check it wasn't added in the meantime
                boolean stillExists = false;
                for (Alias alias : mAliasModel.getAliases()) {
                    for (io.github.dsheirer.alias.id.AliasID aliasId : alias.getAliasIdentifiers()) {
                        if (aliasId.matches(targetId)) {
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
