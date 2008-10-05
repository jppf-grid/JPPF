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
import java.util.*;

import javax.imageio.ImageIO;

/**
 * 
 * @author Laurent Cohen
 */
public class ThumbnailGenerator
{
	/**
	 * The name prefix for the generated thumbnails.
	 */
	private  static final String TH_PREFIX = "_th_";
	/**
	 * The name prefix for the generated thumbnails.
	 */
	private  static final String DEFAULT_INCLUDE_PATH = "C:/Workspaces/SourceForge/JPPF/docs/home/templates";
	/**
	 * Default number of thumbnails per row.
	 */
	private  static final int DEFAULT_ROW_LENGTH = 10;
	/**
	 * Number of thumbnails per row.
	 */
	private  int rowLength = 10;
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
	 * Generated file to include in the screenshots php page.
	 */
	private String includePath = null;
	/**
	 * Mapping of image files to their corresponding thumbnail.
	 */
	private Map<File, File> fileMap = new TreeMap<File, File>();

	/**
	 * Initialize this thumbnail generator with the psecified root dir, width and height.
	 * @param path the path to the directory in which to find the images.
	 * @param width the width of the generated thumbnails.
	 * @param height the height of the generated thumbnails.
	 */
	public ThumbnailGenerator(String path, int width, int height)
	{
		this(path, width, height, DEFAULT_INCLUDE_PATH, DEFAULT_ROW_LENGTH);
	}

	/**
	 * Initialize this thumbnail generator with the psecified root dir, width, height
	 * and default number of thumbnails per row.
	 * @param path the path to the directory in which to find the images.
	 * @param width the width of the generated thumbnails.
	 * @param height the height of the generated thumbnails.
	 * @param rowLength the number of thumbnails per row.
	 */
	public ThumbnailGenerator(String path, int width, int height, int rowLength)
	{
		this(path, width, height, DEFAULT_INCLUDE_PATH, rowLength);
	}

	/**
	 * Initialize this thumbnail generator with the psecified root dir, width, height
	 * and default number of thumbnails per row.
	 * @param path the path to the directory in which to find the images.
	 * @param width the width of the generated thumbnails.
	 * @param height the height of the generated thumbnails.
	 * @param includePath the generated file to include in the screenshots php page.
	 * @param rowLength the number of thumbnails per row..
	 */
	public ThumbnailGenerator(String path, int width, int height, String includePath, int rowLength)
	{
		this.path = path;
		this.width = width;
		this.height = height;
		this.includePath = includePath;
		this.rowLength = rowLength;
	}

	/**
	 * Generate all thumbnails.
	 * @throws Exception if an IO error is raised.
	 */
	public void generate() throws Exception
	{
		generateFileMap();
		generateThumbnails();
		generateIncludeFile();
	}

	/**
	 * Generate all thumbnails.
	 * @throws Exception if an error is raised while generating the thumbnails.
	 */
	private void generateThumbnails() throws Exception
	{
		for (Map.Entry<File, File> entry: fileMap.entrySet())
		{
			BufferedImage img = ImageIO.read(entry.getKey());
			BufferedImage thumbnail = scale(img);
			ImageIO.write(thumbnail, "jpeg", entry.getValue());
		}
	}

	/**
	 * Henerate the file to include in the screenshots php page.
	 * @throws Exception if an IO error is raised.
	 */
	private void generateIncludeFile() throws Exception
	{
		StringBuilder sb = new StringBuilder();
		int count = 0;
		String indent = "\t\t\t\t\t";
		sb.append(indent).append("<table align=\"center\" border=\"0\" cellspacing=\"0\" cellpadding=\"5\">\n");
		for (Map.Entry<File, File> entry: fileMap.entrySet())
		{
			if (count % rowLength == 0)
			{
				if (count > 0) sb.append(indent).append("\t</tr>\n");
				sb.append(indent).append("\t<tr>\n");
			}
			String name1 = entry.getKey().getName();
			String name2 = entry.getValue().getName();
			// $template{name="shots-row" image="popup1.gif" thumbnail="popup1.jpg"}$
			sb.append(indent).append("\t\t$template{name=\"shots_cell\" image=\"");
			sb.append(name1);
			sb.append("\" thumbnail=\"");
			sb.append(name2);
			sb.append("\"}$\n");
			count++;
		}
		sb.append(indent).append("\t</tr>\n");
		sb.append(indent).append("</table>\n");
		FileUtils.writeTextFile(includePath + "/shots.html", sb.toString());
		File file = fileMap.keySet().iterator().next();
		FileUtils.writeTextFile(includePath + "/first-shot.html", file.getName());
	}

	/**
	 * Generate the map of image files to their corresponding thumbnail file.
	 * @throws Exception if an IO error occurs.
	 */
	private void generateFileMap() throws Exception
	{
		File dir = new File(path);
		if (!dir.isDirectory()) throw new IOException("The specified path is not a directory");
		File[] list = dir.listFiles(new ImageFileFilter("gif", "jpg", "png"));
		for (File file: list)
		{
			String s = path;
			if (s.endsWith("/") || s.endsWith("\\")) s = s.substring(0, s.length() - 1);
			s += "/" + TH_PREFIX + file.getName();
			int idx = s.lastIndexOf(".");
			s = s.substring(0, idx+1) + "jpg";
			fileMap.put(file, new File(s));
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

	/**
	 * File filter that only accepts GIF, JPG and PNG files.
	 */
	public static class ImageFileFilter implements FileFilter
	{
		/**
		 * An array of the accepted extensions.
		 */
		private String[] extensions = null;
		
		/**
		 * Initialize this filter witht he specified extensions.
		 * @param extensions an array of the accepted extensions.
		 */
		public ImageFileFilter(String...extensions)
		{
			this.extensions = extensions;
		}

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
			for (String s: extensions)
			{
				if (s.equalsIgnoreCase(ext)) return true;
			}
			return false;
		}
	}

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
			String includePath = args[3]; 
			int rowLength = Integer.valueOf(args[4]);
			System.out.println("Using folder = " + path + ", max width = " + width + ", max height = " + height +
				", include file path = " + includePath + ", thumbnails per row = " + rowLength);
			ThumbnailGenerator tg = new ThumbnailGenerator(path, width, height, includePath, rowLength);
			tg.generate();
			System.out.println("finished");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
