/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
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

package org.jppf.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;

/**
 * 
 * @author Laurent Cohen
 */
public class ImageUtils
{
	/**
	 * The name prefix for the genrated thumbnails.
	 */
	private  static final String TH_PREFIX = "_th_";

	/**
	 * Perform the thumbnail generation.
	 * @param args contains in that order: root path, thumbnail width, thumbnail height.
	 */
	public static void main(String...args)
	{
		try
		{
			String path = args[0];
			int width = Integer.valueOf(args[1]);
			int height = Integer.valueOf(args[2]);
			System.out.println("Using folder = " + path + ", max width = " + width + ", max height = " + height);
			ThumbnailGenerator tg = new ThumbnailGenerator(path, width, height);
			tg.generateThumbnails();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Thumbnails generator.
	 */
	public static class ThumbnailGenerator
	{
		/**
		 * The path to the directory in which to find the images.
		 */
		private String path = null;
		/**
		 * The width of the generated thumbnails.
		 */
		private int width = 0;
		/**
		 * The height of the generated thumbnails.
		 */
		private int height = 0;

		/**
		 * Initialize this thumbnail generator with the psecified root dir, width and height.
		 * @param path the path to the directory in which to find the images.
		 * @param width the width of the generated thumbnails.
		 * @param height the height of the generated thumbnails.
		 */
		public ThumbnailGenerator(String path, int width, int height)
		{
			this.path = path;
			this.width = width;
			this.height = height;
		}
	
		/**
		 * Generate all thumbnails.
		 * @throws Exception if an error is raised while generating the thumbnails.
		 */
		public void generateThumbnails() throws Exception
		{
			File dir = new File(path);
			if (!dir.isDirectory()) throw new IOException("The specified path is not a directory");
			File[] list = dir.listFiles(new ImageFileFilter());
			for (File file: list)
			{
				BufferedImage img = ImageIO.read(file);
				BufferedImage thumbnail = scale(img);
				String s = path;
				if (s.endsWith("/") || s.endsWith("\\")) s = s.substring(0, s.length() - 1);
				s += "/" + TH_PREFIX + file.getName();
				int idx = s.lastIndexOf(".");
				s = s.substring(0, idx+1) + "jpg";
				ImageIO.write(thumbnail, "jpeg", new File(s));
			}
		}
	
		/**
		 * Scale the specified image.
		 * @param img the image to scaled.
		 * @return a scaled version of the input image.
		 */
		private BufferedImage scale(BufferedImage img)
		{
			int w = img.getWidth();
			int h = img.getHeight();
			double r = Math.max((double) w / width, (double) h / height);
			BufferedImage thumbnail = new BufferedImage((int) (w/r), (int) (h/r), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = thumbnail.createGraphics();
			g.drawImage(img.getScaledInstance((int) (w/r), (int) (h/r), Image.SCALE_AREA_AVERAGING), 0, 0, null);
			return thumbnail;
		}
	}

	/**
	 * File filter that only accepts GIF, JPG and PNG files.
	 */
	public static class ImageFileFilter implements FileFilter
	{
		/**
		 * Tests whether or not the specified abstract pathname should be included in a pathname list.
		 * @param file the abstract pathname to be tested.
		 * @return true if and only if pathname  should be included.
		 * @see java.io.FileFilter#accept(java.io.File)
		 */
		public boolean accept(File file)
		{
			String ext = FileUtils.getFileExtension(file);
			if (file.getName().startsWith(TH_PREFIX)) return false;
			return "gif".equalsIgnoreCase(ext) || "jpg".equalsIgnoreCase(ext) || "png".equalsIgnoreCase(ext);
		}
	}
}
