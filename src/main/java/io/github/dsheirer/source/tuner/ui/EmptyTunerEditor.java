/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Empty tuner editor panel
 */
public class EmptyTunerEditor extends StackPane
{
    private static final Logger mLog = LoggerFactory.getLogger(EmptyTunerEditor.class);

    /**
     * Constructs an instance
     */
    public EmptyTunerEditor()
    {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EmptyTunerView.fxml"));
            loader.setRoot(this);
            loader.load();
        } catch (IOException e) {
            mLog.error("Error loading EmptyTunerView.fxml", e);
        }
    }
}
