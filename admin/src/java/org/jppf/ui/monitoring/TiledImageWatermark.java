/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.ui.monitoring;

import java.awt.*;
import java.net.URL;
import javax.imageio.ImageIO;
import org.jvnet.substance.SubstanceImageCreator;
import org.jvnet.substance.watermark.SubstanceWatermark;

/**
 * This class enables using a watermark image throughout the GUI.
 * @author Laurent Cohen
 */
public class TiledImageWatermark implements SubstanceWatermark
{
	/**
	 * Watermark image (screen-sized).
	 */
	private static Image watermarkImage = null;
	/**
	 * .
	 */
	private Image brushedMetalTile = null;

	/**
	 * Initialize this watermark with a specified image file.
	 * @param imagePath location of the image.
	 */
	public TiledImageWatermark(String imagePath)
	{
		try
		{
			ClassLoader cl = TiledImageWatermark.class.getClassLoader();
			URL url = cl.getResource(imagePath);
			this.brushedMetalTile = ImageIO.read(url);
		}
		catch(Exception exc)
		{
			// ignore - probably specified incorrect file
			// or file is not image
		}
	}

	/**
	 * Draws the watermark on the specified graphics context in the specified region.
	 * @param graphics Graphics context.
	 * @param c Component that is painted.
	 * @param x Left X of the region.
	 * @param y Top Y of the region.
	 * @param width Region width.
	 * @param height Region height.
	 * @see org.jvnet.substance.watermark.SubstanceWatermark#drawWatermarkImage(java.awt.Graphics, java.awt.Component, int, int, int, int)
	 */
	public void drawWatermarkImage(Graphics graphics, Component c, int x, int y, int width, int height)
	{
		int dx = c.getLocationOnScreen().x;
		int dy = c.getLocationOnScreen().y;
		graphics.drawImage(watermarkImage, x, y, x + width, y + height, x + dx, y + dy, x + dx + width, y + dy + height, null);
	}

	/**
	 * Updates the current watermark image.
	 * @return .
	 * @see org.jvnet.substance.watermark.SubstanceWatermark#updateWatermarkImage()
	 */
	public boolean updateWatermarkImage()
	{
		// fix by Chris for bug 67 - support for multiple screens
		Rectangle virtualBounds = new Rectangle();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gds = ge.getScreenDevices();
		for (GraphicsDevice gd : gds)
		{
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			virtualBounds = virtualBounds.union(gc.getBounds());
		}
		int screenWidth = virtualBounds.width;
		int screenHeight = virtualBounds.height;
		watermarkImage = SubstanceImageCreator.getBlankImage(screenWidth, screenHeight);
		Graphics2D graphics = (Graphics2D) watermarkImage.getGraphics().create();
		Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f);
		graphics.setComposite(comp);
		boolean status = this.drawWatermarkImage(graphics, 0, 0, screenWidth, screenHeight, false);
		graphics.dispose();
		return status;
	}

	/**
	 * Preview the watermark image.
	 * @param g Graphics context.
	 * @param x Left X of the region.
	 * @param y Top Y of the region.
	 * @param width Region width.
	 * @param height Region height.
	 * @see org.jvnet.substance.watermark.SubstanceWatermark#previewWatermark(java.awt.Graphics, int, int, int, int)
	 */
	public void previewWatermark(Graphics g, int x, int y, int width, int height)
	{
		this.drawWatermarkImage((Graphics2D) g, x - 60, y - 120, 60 + width, 120 + height, true);
	}

	/**
	 * Draw the watermark image.
	 * @param graphics Graphics context.
	 * @param x Left X of the region.
	 * @param y Top Y of the region.
	 * @param width Region width.
	 * @param height Region height.
	 * @param isPreview determines whether the drawing is for preview or not.
	 * @return true.
	 */
	private boolean drawWatermarkImage(Graphics2D graphics, int x, int y, int width, int height, boolean isPreview)
	{
		for (int row = 0; row < height; row += this.brushedMetalTile.getWidth(null))
		{
			for (int col = 0; col < width; col += this.brushedMetalTile.getHeight(null))
			{
				graphics.drawImage(this.brushedMetalTile, x + col, y + row, null);
			}
		}
		return true;
	}

	/**
	 * Returns the display name of <code>this</code> watermark.
	 * @return Display name of <code>this</code> watermark.
	 * @see org.jvnet.substance.watermark.SubstanceWatermark#getDisplayName()
	 */
	public String getDisplayName()
	{
		return getName();
	}

	/**
	 * Get the name of this watermark.
	 * @return the name as a string.
	 */
	public static String getName()
	{
		return "Tile Image";
	}

	/**
	 * Returns indication whether <code>this</code> watermark depends on the current {@link org.jvnet.substance.theme.SubstanceTheme}.
	 * @return <code>true</code> if <code>this</code> watermark depends on the current {@link org.jvnet.substance.theme.SubstanceTheme},
	 * <code>false</code> otherwise.
	 * @see org.jvnet.substance.watermark.SubstanceWatermark#isDependingOnTheme()
	 */
	public boolean isDependingOnTheme()
	{
		return true;
	}
}
