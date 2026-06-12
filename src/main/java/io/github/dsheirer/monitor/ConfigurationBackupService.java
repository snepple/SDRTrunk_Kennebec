/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.monitor;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Automatic daily backup of application configuration: the playlist XML and an export of all
 * java Preferences (user settings).  Backups are zipped into the application's
 * configuration_backups directory and the most recent backups are retained.
 *
 * The playlist already has a single .backup file, but the java Preferences store (tuner
 * configurations, streaming credentials, application settings) previously had no backup at all -
 * corruption of the platform preferences store was unrecoverable.
 *
 * To restore: unzip the backup, copy the playlist XML over the active playlist, and import the
 * preferences XML via java.util.prefs.Preferences.importPreferences().
 */
public class ConfigurationBackupService
{
    private static final Logger mLog = LoggerFactory.getLogger(ConfigurationBackupService.class);
    private static final String BACKUP_DIRECTORY = "configuration_backups";
    private static final int RETAINED_BACKUPS = 10;
    private static final long BACKUP_INTERVAL_HOURS = 24;
    private static final long INITIAL_DELAY_MINUTES = 5;

    private final UserPreferences mUserPreferences;
    private ScheduledFuture<?> mBackupFuture;

    public ConfigurationBackupService(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
    }

    public void start()
    {
        if(mBackupFuture == null)
        {
            mBackupFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::backup, INITIAL_DELAY_MINUTES,
                BACKUP_INTERVAL_HOURS * 60, TimeUnit.MINUTES);
            mLog.info("Configuration backup service started - daily backups retained [" + RETAINED_BACKUPS + "]");
        }
    }

    public void stop()
    {
        if(mBackupFuture != null)
        {
            mBackupFuture.cancel(false);
            mBackupFuture = null;
        }
    }

    private Path getBackupDirectory() throws IOException
    {
        Path directory = mUserPreferences.getDirectoryPreference().getDirectoryApplicationRoot()
            .resolve(BACKUP_DIRECTORY);
        Files.createDirectories(directory);
        return directory;
    }

    /**
     * Creates a timestamped zip containing the playlist and a full preferences export, then prunes
     * backups beyond the retention count.
     */
    public void backup()
    {
        try
        {
            Path backupDirectory = getBackupDirectory();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path backupFile = backupDirectory.resolve("config_backup_" + timestamp + ".zip");

            try(ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(backupFile)))
            {
                //Playlist XML
                Path playlist = mUserPreferences.getPlaylistPreference().getPlaylist();

                if(playlist != null && Files.exists(playlist))
                {
                    zip.putNextEntry(new ZipEntry(playlist.getFileName().toString()));
                    Files.copy(playlist, zip);
                    zip.closeEntry();
                }

                //Java preferences (tuner configs, application settings, credentials)
                zip.putNextEntry(new ZipEntry("preferences_export.xml"));
                exportPreferences(zip);
                zip.closeEntry();
            }

            mLog.info("Configuration backup created [" + backupFile.getFileName() + "]");
            pruneOldBackups(backupDirectory);
        }
        catch(Exception e)
        {
            mLog.error("Error creating configuration backup", e);
        }
    }

    /**
     * Exports the application's preferences subtree as XML to the supplied stream without closing it.
     */
    private void exportPreferences(OutputStream outputStream) throws Exception
    {
        Preferences node = Preferences.userRoot().node("/io/github/dsheirer");

        //exportSubtree closes the stream it is given - shield the zip stream from closure
        OutputStream nonClosing = new OutputStream()
        {
            @Override
            public void write(int b) throws IOException
            {
                outputStream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException
            {
                outputStream.write(b, off, len);
            }

            @Override
            public void flush() throws IOException
            {
                outputStream.flush();
            }

            @Override
            public void close() throws IOException
            {
                outputStream.flush();
            }
        };

        node.exportSubtree(nonClosing);
    }

    private void pruneOldBackups(Path backupDirectory) throws IOException
    {
        List<Path> backups;

        try(Stream<Path> paths = Files.list(backupDirectory))
        {
            backups = paths.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().startsWith("config_backup_") &&
                             p.getFileName().toString().endsWith(".zip"))
                .sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed())
                .toList();
        }

        for(int i = RETAINED_BACKUPS; i < backups.size(); i++)
        {
            try
            {
                Files.deleteIfExists(backups.get(i));
            }
            catch(IOException e)
            {
                mLog.warn("Unable to delete old configuration backup [" + backups.get(i) + "]");
            }
        }
    }
}
