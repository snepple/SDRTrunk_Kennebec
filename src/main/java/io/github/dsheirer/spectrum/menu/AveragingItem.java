package io.github.dsheirer.spectrum.menu;

import io.github.dsheirer.spectrum.SpectralDisplayAdjuster;

import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;

public class AveragingItem extends Slider
{
    private SpectralDisplayAdjuster mAdjuster;
    private int mDefaultValue;
    
    public AveragingItem( SpectralDisplayAdjuster adjuster, int defaultValue )
    {
    	super( 1, 20, adjuster.getAveraging() );
    	mDefaultValue = defaultValue;
    	
    	mAdjuster = adjuster;
    	
    	setMajorTickUnit( 5 );
    	setMinorTickCount( 4 ); // Minor ticks between major ticks
    	setShowTickMarks( true );
    	setShowTickLabels( true );
    	setSnapToTicks(true);
    	
    	valueProperty().addListener( (obs, oldVal, newVal) -> {
    		mAdjuster.setAveraging( newVal.intValue() );
    	} );

    	setOnMouseClicked( event -> {
			if( event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 )
			{
				setValue( mDefaultValue );
			}
		} );
    }
}
