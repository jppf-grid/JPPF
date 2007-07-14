/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.ui.monitoring;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import org.jvnet.substance.utils.SubstanceCoreUtilities;
import org.jvnet.substance.utils.SubstanceConstants.ImageWatermarkKind;
import org.jvnet.substance.watermark.*;

/**
 * This class enables using a watermark image throughout the GUI.
 * @author Laurent Cohen
 */
public class JPPFTiledWatermark implements SubstanceWatermark
{
	/**
	 * Watermark image (screen-sized).
	 */
	private static Image watermarkImage = null;
	/**
	 * 
	 */
	private static ImageWatermarkKind kind = ImageWatermarkKind.SCREEN_TILE;
	/**
	 * 
	 */
	private static float opacity = 0.2f;
	/**
	 * The original image (as read from the disk / HTTP connection).
	 */
	protected BufferedImage origImage;
	/**
	 * 
	 */
	protected String origImageLocation;

	/**
	 * Creates an instance.
	 */
	public JPPFTiledWatermark()
	{
		String imageLocation = "org/jppf/ui/resources/GridWatermark.gif";
		try
		{
			ClassLoader cl = JPPFTiledWatermark.class.getClassLoader();
			URL url = cl.getResource(imageLocation);
			origImage = ImageIO.read(url);
		}
		catch(Exception exc)
		{
			exc.printStackTrace();
		}
		origImageLocation = imageLocation;
	}

	/**
	 * Creates an instance with specified image.
	 * @param imageLocation .
	 */
	public JPPFTiledWatermark(String imageLocation)
	{
		try
		{
			ClassLoader cl = JPPFTiledWatermark.class.getClassLoader();
			URL url = cl.getResource(imageLocation);
			origImage = ImageIO.read(url);
		}
		catch(Exception exc)
		{
			exc.printStackTrace();
		}
		origImageLocation = imageLocation;
	}

	/**
	 * 
	 * @param graphics .
	 * @param c .
	 * @param x .
	 * @param y .
	 * @param width .
	 * @param height .
	 * @see org.jvnet.substance.watermark.SubstanceWatermark#drawWatermarkImage(java.awt.Graphics, java.awt.Component, int, int, int, int)
	 */
	public void drawWatermarkImage(Graphics graphics, Component c, int x, int y, int width, int height)
	{
		int dx = 0;
		int dy = 0;
		Component topParent = null;
		switch (getKind())
		{
			case SCREEN_CENTER_SCALE:
			case SCREEN_TILE:
				dx = c.getLocationOnScreen().x;
				dy = c.getLocationOnScreen().y;
				break;
			case APP_ANCHOR:
			case APP_TILE:
				if (c instanceof JComponent)
				{
					topParent = ((JComponent) c).getTopLevelAncestor();
				}
				else
				{
					Component comp = c;
					while (comp.getParent() != null)
					{
						comp = comp.getParent();
					}
					topParent = comp;
				}
				dx = c.getLocationOnScreen().x - topParent.getLocationOnScreen().x;
				dy = c.getLocationOnScreen().y - topParent.getLocationOnScreen().y;
				break;
			case APP_CENTER:
				if (c instanceof JComponent)
				{
					topParent = ((JComponent) c).getTopLevelAncestor();
				}
				else
				{
					Component comp = c;
					while (comp.getParent() != null)
					{
						comp = comp.getParent();
					}
					topParent = comp;
				}
				dx = c.getLocationOnScreen().x - topParent.getLocationOnScreen().x;
				dy = c.getLocationOnScreen().y - topParent.getLocationOnScreen().y;
				dx -= (topParent.getWidth() / 2 - this.origImage.getWidth() / 2);
				dy -= (topParent.getHeight() / 2 - this.origImage.getHeight() / 2);
		}
		graphics.drawImage(watermarkImage, x, y, x + width, y + height, x + dx, y + dy, x + dx + width, y + dy + height, null);
	}

	/**
	 * 
	 * @param g .
	 * @param x .
	 * @param y .
	 * @param width .
	 * @param height .
	 * @see org.jvnet.substance.watermark.SubstanceWatermark#previewWatermark(java.awt.Graphics, int, int, int, int)
	 */
	public void previewWatermark(Graphics g, int x, int y, int width, int height)
	{
	}

	/**
	 * @return .
	 * @see org.jvnet.substance.watermark.SubstanceWatermark#updateWatermarkImage()
	 */
	public boolean updateWatermarkImage()
	{
		if (origImage == null) return false;
		Rectangle virtualBounds = new Rectangle();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gds = ge.getScreenDevices();
		for (GraphicsDevice gd : gds)
			virtualBounds = virtualBounds.union(gd.getDefaultConfiguration().getBounds());
		int screenWidth = virtualBounds.width;
		int screenHeight = virtualBounds.height;
		int origImageWidth = this.origImage.getWidth();
		int origImageHeight = this.origImage.getHeight();
		if (getKind() == ImageWatermarkKind.SCREEN_CENTER_SCALE)
		{
			watermarkImage = SubstanceCoreUtilities.getBlankImage(screenWidth, screenHeight);
			Graphics2D graphics = (Graphics2D) watermarkImage.getGraphics().create();
			Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
			graphics.setComposite(comp);
			boolean isWidthFits = (origImageWidth <= screenWidth);
			boolean isHeightFits = (origImageHeight <= screenHeight);
			if (isWidthFits && isHeightFits)
			{
				graphics.drawImage(this.origImage, (screenWidth - origImageWidth) / 2, (screenHeight - origImageHeight) / 2, null);
				graphics.dispose();
				return true;
			}
			if (isWidthFits)
			{
				double scaleFact = (double) screenHeight / (double) origImageHeight;
				int dx = (int) (screenWidth - scaleFact * origImageWidth) / 2;
				graphics.drawImage(this.origImage, dx, 0, screenWidth - dx, screenHeight, 0, 0, origImageWidth, origImageHeight, null);
				graphics.dispose();
				return true;
			}
			if (isHeightFits)
			{
				double scaleFact = (double) screenWidth / (double) origImageWidth;
				int dy = (int) (screenHeight - scaleFact * origImageHeight) / 2;
				graphics.drawImage(this.origImage, 0, dy, screenWidth, screenHeight - dy, 0, 0, origImageWidth, origImageHeight, null);
				graphics.dispose();
				return true;
			}
			double scaleFactY = (double) screenHeight / (double) origImageHeight;
			double scaleFactX = (double) screenWidth / (double) origImageWidth;
			double scaleFact = Math.min(scaleFactX, scaleFactY);
			int dx = Math.max(0, (int) (screenWidth - scaleFact * origImageWidth) / 2);
			int dy = Math.max(0, (int) (screenHeight - scaleFact * origImageHeight) / 2);
			graphics.drawImage(this.origImage, dx, dy, screenWidth - dx, screenHeight - dy, 0, 0, origImageWidth, origImageHeight, null);
			graphics.dispose();
			return true;
		}
		if ((getKind() == ImageWatermarkKind.SCREEN_TILE) || (getKind() == ImageWatermarkKind.APP_TILE))
		{
			watermarkImage = SubstanceCoreUtilities.getBlankImage(screenWidth, screenHeight);
			Graphics2D graphics = (Graphics2D) watermarkImage.getGraphics().create();
			Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
			graphics.setComposite(comp);
			int replicateX = 1 + screenWidth / origImageWidth;
			int replicateY = 1 + screenHeight / origImageHeight;
			for (int i = 0; i < replicateX; i++)
			{
				for (int j = 0; j < replicateY; j++)
					graphics.drawImage(this.origImage, i * origImageWidth, j * origImageHeight, null);
			}
			graphics.dispose();
			return true;
		}
		if ((getKind() == ImageWatermarkKind.APP_ANCHOR) || (getKind() == ImageWatermarkKind.APP_CENTER))
		{
			watermarkImage = SubstanceCoreUtilities.getBlankImage(origImageWidth, origImageHeight);
			Graphics2D graphics = (Graphics2D) watermarkImage.getGraphics().create();
			Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
			graphics.setComposite(comp);
			graphics.drawImage(this.origImage, 0, 0, null);
			graphics.dispose();
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @return .
	 * @see org.jvnet.substance.watermark.SubstanceWatermark#getDisplayName()
	 */
	public String getDisplayName()
	{
		return SubstanceImageWatermark.getName();
	}

	/**
	 * 
	 * @return .
	 */
	public static String getName()
	{
		return "Image";
	}

	/**
	 * @return .
	 * @see org.jvnet.substance.watermark.SubstanceWatermark#isDependingOnTheme()
	 */
	public boolean isDependingOnTheme()
	{
		return true;
	}

	/**
	 * @see org.jvnet.substance.watermark.SubstanceWatermark#dispose()
	 */
	public void dispose()
	{
		watermarkImage = null;
	}

	/**
	 * 
	 * @return .
	 */
	public String getOrigImageLocation()
	{
		return this.origImageLocation;
	}

	/**
	 * .
	 * @param aKind .
	 */
	public static void setKind(ImageWatermarkKind aKind)
	{
		if (aKind == null) { throw new IllegalArgumentException("Can't pass null to SubstanceImageWatermark.setKind()"); }
		kind = aKind;
	}

	/**
	 * 
	 * @return .
	 */
	public static ImageWatermarkKind getKind()
	{
		return kind;
	}

	/**
	 * 
	 * @return .
	 */
	public static float getOpacity()
	{
		return opacity;
	}

	/**
	 * 
	 * @param aOpacity .
	 */
	public static void setOpacity(float aOpacity)
	{
		if ((opacity < 0.0f) || (opacity > 1.0f)) { throw new IllegalArgumentException(
				"SubstanceImageWatermark.setOpacity() can get value in 0.0-1.0 range, was passed value " + opacity); }
		opacity = aOpacity;
	}
}
