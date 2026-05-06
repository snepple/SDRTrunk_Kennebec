package io.github.dsheirer.spectrum;

import java.awt.image.IndexColorModel;

public class WaterfallColorModel
{
    public static int[] getARGBColorMap()
    {
        int indexSize = 256;
        int[] argb = new int[indexSize];

        //Background noise
        for( int x = 0; x < 16; x++ ) //Blue
        {
            argb[x] = (255 << 24) | (0 << 16) | (0 << 8) | 127;
        }

        for( int x = 16; x < 32; x++ ) //Blue
        {
            argb[x] = (255 << 24) | (0 << 16) | (0 << 8) | 191;
        }

        int r = 0;
        int g = 0;
        int b = 191;

        for( int x = 32; x < 60; x++ )
        {
            argb[x] = (255 << 24) | (r << 16) | (g << 8) | b;
            r += 9;
            g += 9;
            b -= 6;
        }

        r = 255;
        g = 255;
        b = 0;

        for( int x = 60; x < 188; x++ ) //Yellow
        {
            argb[x] = (255 << 24) | (r << 16) | (g << 8) | b;
            g -= 2;
        }

        for( int x = 188; x < 256; x++ ) //Red
        {
            argb[x] = (255 << 24) | (255 << 16) | (0 << 8) | 0;
        }

        return argb;
    }

	public static IndexColorModel getDefaultColorModel()
	{
		int bitDepth = 8;
		int indexSize = 256;

		byte[] red = new byte[ indexSize ];
		byte[] green = new byte[ indexSize ];
		byte[] blue = new byte[ indexSize ];

		//Background noise
		for( int x = 0; x < 16; x++ ) //Blue
		{
			red[ x ] = (byte)0;

			green[ x ] = (byte)0;

			blue[ x ] = (byte)127;
		}

		for( int x = 16; x < 32; x++ ) //Blue
		{
			red[ x ] = (byte)0;

			green[ x ] = (byte)0;

			blue[ x ] = (byte)191;
		}

		int r = 0;
		int g = 0;
		int b = 191;

		for( int x = 32; x < 60; x++ )
		{
			red[ x ] = (byte)r;
			r += 9;

			green[ x ] = (byte)g;
			g += 9;

			blue[ x ] = (byte)b;
			b -= 6;
		}

		r = 255;
		g = 255;
		b = 0;

		for( int x = 60; x < 188; x++ ) //Yellow
		{
			red[ x ] = (byte)r;

			green[ x ] = (byte)g;
			g -= 2;

			blue[ x ] = (byte)b;
		}

		for( int x = 188; x < 256; x++ ) //Red
		{
			red[ x ] = (byte)255;

			green[ x ] = (byte)0;

			blue[ x ] = (byte)0;
		}

		return new IndexColorModel( bitDepth, indexSize, red, green, blue );
	}
}
