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

package io.github.dsheirer.gui.preference.layout;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * A composite layout component that groups a section header, a SettingsCard, and an optional
 * footer description into a single VBox. This encapsulates the repeating pattern used throughout
 * the Apple HIG-compliant settings panels:
 *
 *   [Section Header]         <-- 12px bold uppercase-ish caption
 *   ┌─────────────────────┐
 *   │ SettingsRow          │
 *   │─────────────────────│
 *   │ SettingsRow          │  <-- SettingsCard with auto-separators
 *   │─────────────────────│
 *   │ SettingsRow          │
 *   └─────────────────────┘
 *   [Footer description]    <-- 11px secondary text (optional)
 */
public class SettingsSection extends VBox
{
    private final Label mHeaderLabel;
    private final SettingsCard mCard;
    private Label mFooterLabel;

    /**
     * Constructs a settings section with a header title.
     * @param headerText the section header text
     */
    public SettingsSection(String headerText)
    {
        setSpacing(0);

        mHeaderLabel = new Label(headerText.toUpperCase());
        mHeaderLabel.getStyleClass().add("hig-section-header");

        mCard = new SettingsCard();

        getChildren().addAll(mHeaderLabel, mCard);
    }

    /**
     * Returns the SettingsCard contained in this section.
     * Add SettingsRow instances to this card.
     */
    public SettingsCard getCard()
    {
        return mCard;
    }

    /**
     * Adds a SettingsRow directly to the card.
     * Convenience method to avoid calling getCard().getChildren().add()
     */
    public void addRow(SettingsRow row)
    {
        mCard.getChildren().add(row);
    }

    /**
     * Sets a footer description text below the card.
     * @param footerText the description text
     */
    public void setFooter(String footerText)
    {
        if(mFooterLabel == null)
        {
            mFooterLabel = new Label();
            mFooterLabel.getStyleClass().add("hig-section-footer");
            mFooterLabel.setWrapText(true);
            getChildren().add(mFooterLabel);
        }
        mFooterLabel.setText(footerText);
    }
}
