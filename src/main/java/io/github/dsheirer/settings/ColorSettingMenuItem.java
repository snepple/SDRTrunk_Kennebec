/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.settings;

import io.github.dsheirer.settings.ColorSetting.ColorSettingName;

import javafx.scene.control.MenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * MenuItem for selecting a color and automatically setting (saving) the
 * color selection in the settings manager
 */
public class ColorSettingMenuItem extends MenuItem
{
    private ColorSettingName mColorSettingName;
    private SettingsManager mSettingsManager;
    private javafx.scene.paint.Color mCurrentColor;
	
	public ColorSettingMenuItem( SettingsManager settingsManager,
								 ColorSettingName colorSettingName )
	{
		super( colorSettingName.getLabel() );

		mSettingsManager = settingsManager;
		mColorSettingName = colorSettingName;

		mCurrentColor = mSettingsManager.getSettingsModel()
				.getColorSetting( mColorSettingName ).getColor();

		Color fxColor = Color.rgb((int)(mCurrentColor.getRed() * 255), (int)(mCurrentColor.getGreen() * 255), (int)(mCurrentColor.getBlue() * 255), mCurrentColor.getOpacity());
		
		Rectangle icon = new Rectangle(16, 16);
		icon.setFill(fxColor);
		icon.setStroke(Color.BLACK);
		this.setGraphic(icon);
		
		setOnAction( e -> {
			Dialog<Color> dialog = new Dialog<>();
			dialog.setTitle(mColorSettingName.getDialogTitle());
			ColorPicker colorPicker = new ColorPicker(fxColor);
			dialog.getDialogPane().setContent(colorPicker);
			dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
			dialog.setResultConverter(button -> button == ButtonType.OK ? colorPicker.getValue() : null);
			
			dialog.showAndWait().ifPresent(newColor -> {
				javafx.scene.paint.Color awtNewColor = new javafx.scene.paint.Color(
					(float)newColor.getRed(), 
					(float)newColor.getGreen(), 
					(float)newColor.getBlue(), 
					(float)newColor.getOpacity()
				);
				mSettingsManager.getSettingsModel().setColorSetting( mColorSettingName, awtNewColor );
			});
		} );
	}
}
