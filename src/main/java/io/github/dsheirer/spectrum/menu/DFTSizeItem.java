package io.github.dsheirer.spectrum.menu;

import io.github.dsheirer.spectrum.DFTSize;
import io.github.dsheirer.spectrum.IDFTWidthChangeProcessor;

import javafx.scene.control.CheckMenuItem;

public class DFTSizeItem extends CheckMenuItem
{
    private IDFTWidthChangeProcessor mDFTProcessor;
    private DFTSize mDFTSize;
    
    public DFTSizeItem( IDFTWidthChangeProcessor processor, DFTSize size )
    {
    	super( size.getLabel() );
    	
    	mDFTProcessor = processor;
    	mDFTSize = size;

    	if( processor.getDFTSize() == mDFTSize )
    	{
    		setSelected( true );
    	}
    	
    	setOnAction( event -> {
			mDFTProcessor.setDFTSize( mDFTSize );
		} );
    }
}
