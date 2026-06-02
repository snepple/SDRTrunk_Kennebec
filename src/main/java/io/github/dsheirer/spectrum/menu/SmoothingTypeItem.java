package io.github.dsheirer.spectrum.menu;

import io.github.dsheirer.dsp.filter.smoothing.SmoothingFilter.SmoothingType;
import io.github.dsheirer.spectrum.SpectralDisplayAdjuster;

import javafx.scene.control.CheckMenuItem;

public class SmoothingTypeItem extends CheckMenuItem
{
	private SpectralDisplayAdjuster mAdjuster;
	private SmoothingType mSmoothingType;
	
	public SmoothingTypeItem( SpectralDisplayAdjuster adjuster, SmoothingType type )
	{
		super( type.name() );
		
		mAdjuster = adjuster;
		mSmoothingType = type;
	
		setSelected( mAdjuster.getSmoothingType() == type );

		setOnAction( event -> {
			mAdjuster.setSmoothingType( mSmoothingType );
		} );
	}
}
