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
package io.github.dsheirer.playlist;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Kennebec-only sidecar document that stores two-tone detector identifiers separately from the main playlist file.
 *
 * The two-tone detector is a fork-specific {@code AliasID} subtype that does not exist in the upstream sdrtrunk
 * application.  Persisting it inside the standard playlist's polymorphic {@code <id>} list causes upstream to abort
 * the entire playlist load on an unknown type id and subsequently overwrite the file with an empty playlist,
 * destroying every alias.  To keep the standard playlist byte-for-byte compatible with upstream, two-tone detector
 * data is written to this companion file instead and merged back into the in-memory aliases on load.
 *
 * Entries are matched back to their owning alias by the (alias list, group, name) tuple, which is the alias's
 * effective display identity within a playlist.
 */
@JacksonXmlRootElement(localName = "kennebec_two_tone")
public class TwoToneSidecar
{
    private List<Entry> mEntries = new ArrayList<>();

    public TwoToneSidecar()
    {
    }

    @JacksonXmlProperty(isAttribute = false, localName = "alias")
    public List<Entry> getEntries()
    {
        return mEntries;
    }

    public void setEntries(List<Entry> entries)
    {
        mEntries = (entries != null) ? entries : new ArrayList<>();
    }

    /**
     * A single alias's two-tone detector assignments, keyed by the owning alias's identity.
     */
    public static class Entry
    {
        private String mList;
        private String mGroup;
        private String mName;
        private List<String> mDetectorNames = new ArrayList<>();

        public Entry()
        {
        }

        public Entry(String list, String group, String name, List<String> detectorNames)
        {
            mList = list;
            mGroup = group;
            mName = name;
            mDetectorNames = (detectorNames != null) ? detectorNames : new ArrayList<>();
        }

        @JacksonXmlProperty(isAttribute = true, localName = "list")
        public String getList()
        {
            return mList;
        }

        public void setList(String list)
        {
            mList = list;
        }

        @JacksonXmlProperty(isAttribute = true, localName = "group")
        public String getGroup()
        {
            return mGroup;
        }

        public void setGroup(String group)
        {
            mGroup = group;
        }

        @JacksonXmlProperty(isAttribute = true, localName = "name")
        public String getName()
        {
            return mName;
        }

        public void setName(String name)
        {
            mName = name;
        }

        @JacksonXmlProperty(isAttribute = false, localName = "detector")
        public List<String> getDetectorNames()
        {
            return mDetectorNames;
        }

        public void setDetectorNames(List<String> detectorNames)
        {
            mDetectorNames = (detectorNames != null) ? detectorNames : new ArrayList<>();
        }
    }
}
