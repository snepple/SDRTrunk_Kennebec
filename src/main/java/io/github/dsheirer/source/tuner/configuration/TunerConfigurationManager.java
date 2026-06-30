/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.source.tuner.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.TunerFactory;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.IDiscoveredTunerStatusListener;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.util.ThreadPool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages settings and configurations for all tuner types.
 */
public class TunerConfigurationManager implements IDiscoveredTunerStatusListener
{
    private static final Logger mLog = LoggerFactory.getLogger(TunerConfigurationManager.class);
    private static final String SETTINGS_FILE_NAME = "tuner_configuration.json";
    private UserPreferences mUserPreferences;
    private List<DisabledTuner> mDisabledTunerList = new ArrayList<>();
    private List<TunerConfiguration> mTunerConfigurations = new ArrayList<>();
    private AtomicBoolean mSavePending = new AtomicBoolean();
    private Lock mLock = new ReentrantLock();
    //Tracks the IDs of all currently-discovered tuners so that getTunerConfiguration() can identify
    //"orphaned" configs whose bus:port changed between restarts.
    private java.util.Set<String> mDiscoveredTunerIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Constructs an instance and loads the save configuration state.
     *
     * @param userPreferences to determine directories for accessing files
     */
    public TunerConfigurationManager(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        load();
    }

    /**
     * Loads settings from the persisted tuner state file
     */
    private void load()
    {
        Path configPath = getConfigurationFilePath();

        if(Files.exists(configPath))
        {
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            try
            {
                TunerConfigurationState state = objectMapper.readValue(configPath.toFile(), TunerConfigurationState.class);
                mDisabledTunerList.addAll(state.getDisabledTuners());
                mTunerConfigurations.addAll(state.getTunerConfigurations());
                //Note: deduplicateFriendlyNames() removed from startup path.  The root cause of duplicate
                //names (orphaned configs from volatile bus:port IDs) is now fixed in getTunerConfiguration().
                //Running dedup every startup was itself destructive — it permanently renamed configs and the
                //suffixed names became new duplicates on subsequent restarts, causing progressive name inflation.
            }
            catch(IOException ioe)
            {
                mLog.error("Error loading tuner configuration file", ioe);
            }
        }
    }

    /**
     * Detects and resolves duplicate friendly names among tuner configurations loaded from disk.
     * When two or more configs share the same non-empty friendly name, a numeric suffix is appended
     * to the duplicates (e.g., "Airspy 2" becomes "Airspy 2 (2)") and the corrected state is saved.
     *
     * This repairs configurations affected by a prior bug where known-good profile cloning copied
     * the donor's friendly name to new tuners of the same type.
     */
    private void deduplicateFriendlyNames()
    {
        java.util.Map<String, List<TunerConfiguration>> nameGroups = new java.util.HashMap<>();
        boolean changed = false;

        for(TunerConfiguration config : mTunerConfigurations)
        {
            String name = config.getFriendlyName();

            if(name != null && !name.trim().isEmpty())
            {
                nameGroups.computeIfAbsent(name.trim(), k -> new ArrayList<>()).add(config);
            }
        }

        for(java.util.Map.Entry<String, List<TunerConfiguration>> entry : nameGroups.entrySet())
        {
            List<TunerConfiguration> group = entry.getValue();

            if(group.size() > 1)
            {
                mLog.warn("Found " + group.size() + " tuner configurations sharing friendly name [" +
                    entry.getKey() + "] - appending suffixes to resolve duplicates");

                // Keep the first one as-is, append suffix to the rest
                for(int i = 1; i < group.size(); i++)
                {
                    String newName = entry.getKey() + " (" + (i + 1) + ")";
                    mLog.info("Renaming tuner [" + group.get(i).getUniqueID() + "] from [" +
                        entry.getKey() + "] to [" + newName + "]");
                    group.get(i).setFriendlyName(newName);
                    changed = true;
                }
            }
        }

        if(changed)
        {
            saveConfigurations();
        }
    }

    /**
     * Tuner configuration state file (.json).
     */
    private Path getConfigurationFilePath()
    {
        return mUserPreferences.getDirectoryPreference().getDirectoryConfiguration().resolve(SETTINGS_FILE_NAME);
    }

    /**
     * Saves the current tuner configuration state to disk.
     */
    private void save()
    {
        TunerConfigurationState state = new TunerConfigurationState();

        mLock.lock();

        try
        {
            state.setDisabledTuners(new ArrayList<>(mDisabledTunerList));
            state.setTunerConfigurations(new ArrayList<>(mTunerConfigurations));
        }
        catch(Exception e)
        {
            mLog.error("Error", e);
        }
        finally
        {
            mLock.unlock();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try
        {
            objectMapper.writeValue(getConfigurationFilePath().toFile(), state);
        }
        catch(IOException ioe)
        {
            mLog.error("Error writing tuner configuration state to file [" + getConfigurationFilePath() + "]", ioe);
        }
    }

    /**
     * Schedules a configurations save task.  Subsequent calls to this method will be ignored until the
     * save event occurs, thus limiting repetitive saving to a minimum.
     */
    public void saveConfigurations()
    {
        if(mSavePending.compareAndSet(false, true))
        {
            ThreadPool.SCHEDULED.schedule(new ConfigurationSaveTask(), 2, TimeUnit.SECONDS);
        }
    }

    /**
     * Monitors discovered tuner enabled status and applies configurations or updates disable state of tuners.
     * @param discoveredTuner that has a status change.
     * @param previous tuner status
     * @param current tuner status
     */
    @Override
    public void tunerStatusUpdated(DiscoveredTuner discoveredTuner, TunerStatus previous, TunerStatus current)
    {
        //Always register the discovered ID so orphan detection in getTunerConfiguration() is accurate.
        registerDiscoveredTunerId(discoveredTuner.getId());

        if(current == TunerStatus.DISABLED)
        {
            addDisabledTuner(discoveredTuner);
        }
        else if(current == TunerStatus.ENABLED)
        {
            removeDisabledTuner(discoveredTuner);

            if(discoveredTuner.hasTuner())
            {
                TunerType tunerType = discoveredTuner.getTuner().getTunerType();

                if(tunerType != TunerType.RECORDING)
                {
                    TunerConfiguration tunerConfiguration = getTunerConfiguration(tunerType, discoveredTuner.getId());

                    if(tunerConfiguration != null)
                    {
                        discoveredTuner.setTunerConfiguration(tunerConfiguration);
                        saveConfigurations();
                    }
                }
            }
        }
    }

    /**
     * Registers a discovered tuner's ID so that {@link #getTunerConfiguration} can distinguish orphaned configs
     * (whose bus:port changed) from configs belonging to a different physical tuner of the same type.
     */
    public void registerDiscoveredTunerId(String id)
    {
        if(id != null)
        {
            mDiscoveredTunerIds.add(id);
        }
    }

    /**
     * Updates the tuner configuration with the current tuner PPM setting so that the value can be stored across
     * sessions.
     * @param discoveredTuner that has an updated PPM value.
     */
    public void updateTunerPPM(DiscoveredTuner discoveredTuner)
    {
        if(discoveredTuner != null)
        {
            TunerType tunerType = discoveredTuner.getTuner().getTunerType();

            if(tunerType != TunerType.RECORDING)
            {
                TunerConfiguration tunerConfiguration = getTunerConfiguration(tunerType, discoveredTuner.getId());

                if(tunerConfiguration != null)
                {
                    tunerConfiguration.setFrequencyCorrection(discoveredTuner.getTuner().getTunerController().getFrequencyCorrection());
                    saveConfigurations();
                }
            }
        }
    }

    /**
     * Adds the discovered tuner to the list of disabled tuners
     */
    private void addDisabledTuner(DiscoveredTuner discoveredTuner)
    {
        if(!isDisabled(discoveredTuner))
        {
            mLock.lock();

            try
            {
                mDisabledTunerList.add(new DisabledTuner(discoveredTuner.getTunerClass(), discoveredTuner.getId()));
            }
            finally
            {
                mLock.unlock();
            }

            saveConfigurations();
        }
    }

    /**
     * Removes the tuner from the disabled tuners list
     * @param discoveredTuner to remove
     */
    private void removeDisabledTuner(DiscoveredTuner discoveredTuner)
    {
        mLock.lock();

        try
        {
            mDisabledTunerList.removeIf(tuner -> tuner.matches(discoveredTuner));
        }
        finally
        {
            mLock.unlock();
        }

        saveConfigurations();
    }

    /**
     * Indicates if the discovered tuner is disabled.  This method should only be used to determine the disabled state
     * of a tuner when it is added to the system for use, such as tuner discovery at application startup, or for
     * USB tuner hotplug device add notifications.
     *
     * @param discoveredTuner to check for disabled status.
     * @return true if the tuner is supposed to be disabled.
     */
    public boolean isDisabled(DiscoveredTuner discoveredTuner)
    {
        return findDisabledTuner(discoveredTuner) != null;
    }

    /**
     * Finds the disabled tuner that matches the discovered tuner.
     * @param discoveredTuner to search for
     * @return disabled tuner instance or null if the discovered tuner is not currently disabled.
     */
    private DisabledTuner findDisabledTuner(DiscoveredTuner discoveredTuner)
    {
        DisabledTuner found = null;

        mLock.lock();

        try
        {
            for(DisabledTuner disabledTuner: mDisabledTunerList)
            {
                if(disabledTuner.matches(discoveredTuner))
                {
                    found = disabledTuner;
                    break;
                }
            }
        }
        finally
        {
            mLock.unlock();
        }

        return found;
    }

    /**
     * Adds the tuner configuration if one doesn't exist that matches the tuner type and unique id.
     * @param tunerConfiguration to add
     */
    public void addTunerConfiguration(TunerConfiguration tunerConfiguration)
    {
        if(!mTunerConfigurations.stream().filter(config -> config.getTunerType().equals(tunerConfiguration.getTunerType()) &&
                config.getUniqueID().equalsIgnoreCase(tunerConfiguration.getUniqueID())).findFirst().isPresent())
        {
            mTunerConfigurations.add(tunerConfiguration);
            saveConfigurations();
        }
    }

    /**
     * Removes the tuner configuration from this manager.
     * @param tunerConfiguration to remove
     */
    public void removeTunerConfiguration(TunerConfiguration tunerConfiguration)
    {
        mTunerConfigurations.remove(tunerConfiguration);
        saveConfigurations();
    }

    /**
     * Provides an existing or creates a new tuner configuration for the specified tuner type and unique ID value.
     *
     * USB bus:port addresses are volatile — they can change when the host enumerates devices in a different order,
     * or when a tuner is plugged into a different USB port.  The prior implementation treated every bus:port change
     * as a "new" tuner and cloned the donor config, creating an orphaned entry with the old bus:port ID.  Over many
     * restarts these orphans accumulated and caused {@link #deduplicateFriendlyNames()} to append ever-growing
     * numeric suffixes ("Airspy 2 (2)", "Airspy 2 (3)", ...).
     *
     * The fix:
     * <ol>
     *   <li>Try an exact uniqueID match first (fast path, no bus:port change).</li>
     *   <li>If that fails, find an "orphaned" config of the same TunerType whose uniqueID doesn't match any
     *       currently-discovered tuner.  Adopt it by updating its uniqueID to the new bus:port value.  This is
     *       almost certainly the same physical tuner on a different bus/port.</li>
     *   <li>Only clone from a donor (profile cloning) if no orphan of that type exists — i.e., it really is a
     *       brand-new tuner that has never been configured.</li>
     *   <li>Factory defaults as last resort.</li>
     * </ol>
     *
     * Note: this method is not thread-safe.
     */
    public TunerConfiguration getTunerConfiguration(TunerType type, String uniqueID )
    {
        //1. Exact match on TunerType + uniqueID (fast path, no bus:port change).
        Optional<TunerConfiguration> optional = mTunerConfigurations.stream().filter(config -> config.getTunerType().equals(type) &&
                config.getUniqueID().equalsIgnoreCase(uniqueID)).findFirst();

        if(optional.isPresent())
        {
            return optional.get();
        }

        //2. Adopt an orphaned config: same TunerType, but its uniqueID doesn't match any currently-discovered
        //   tuner.  This handles the common case where a tuner's USB bus:port changed between restarts.
        //   Collect all currently-discovered tuner IDs so we can tell which saved configs are "orphaned."
        java.util.Set<String> discoveredIds = new java.util.HashSet<>();
        for(String id : mDiscoveredTunerIds)
        {
            discoveredIds.add(id.toLowerCase());
        }

        //Find all same-type configs whose uniqueID is NOT in the set of discovered tuner IDs.
        List<TunerConfiguration> orphans = mTunerConfigurations.stream()
            .filter(config -> config.getTunerType().equals(type))
            .filter(config -> !discoveredIds.contains(config.getUniqueID().toLowerCase()))
            .toList();

        if(!orphans.isEmpty())
        {
            //Take the first orphan and update its uniqueID to match the new bus:port.
            TunerConfiguration orphan = orphans.get(0);
            mLog.info("Tuner [" + uniqueID + "] matched orphaned config [" + orphan.getUniqueID() +
                "] (same type: " + type + ") - adopting with updated ID (USB bus:port likely changed)");
            orphan.setUniqueID(uniqueID);
            saveConfigurations();
            return orphan;
        }

        //3. Known-good profile cloning: when a new tuner of a type we already have a configuration for
        //is plugged in, clone the existing (presumably tuned/working) configuration instead of
        //starting from factory defaults - gain, PPM and sample rate carry over automatically.
        TunerConfiguration donor = mTunerConfigurations.stream()
            .filter(existing -> existing.getTunerType().equals(type)).findFirst().orElse(null);

        if(donor != null)
        {
            try
            {
                ObjectMapper mapper = new ObjectMapper();
                TunerConfiguration clone = mapper.readValue(mapper.writeValueAsBytes(donor), donor.getClass());
                clone.setUniqueID(uniqueID);
                clone.setFriendlyName(null); // Don't inherit donor's display name — each tuner needs its own
                mLog.info("New tuner [" + uniqueID + "] - cloned known-good " + type +
                    " configuration from tuner [" + donor.getUniqueID() + "]");
                addTunerConfiguration(clone);
                return clone;
            }
            catch(Exception e)
            {
                mLog.warn("Unable to clone existing " + type + " configuration for new tuner [" + uniqueID +
                    "] - using factory defaults", e);
            }
        }

        TunerConfiguration config = TunerFactory.getTunerConfiguration(type, uniqueID);
        addTunerConfiguration(config);
        return config;
    }

    /**
     * Get all tuner configurations that match the specified tuner type.
     * @param tunerType to match
     * @return list of all configurations.
     */
    public List<TunerConfiguration> getTunerConfigurations(TunerType tunerType)
    {
        return mTunerConfigurations.stream().filter(tunerConfiguration -> tunerConfiguration.getTunerType()
                .equals(tunerType)).toList();
    }

    /**
     * Saves the current tuner configuration state and resets the save pending flag
     */
    public class ConfigurationSaveTask implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                save();
            }
            catch(Exception e)
            {
                mLog.error("Error saving tuner configurations", e);
            }

            mSavePending.set(false);
        }
    }
}
