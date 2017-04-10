/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
 * Instances of this class generate thumbnails from a set of images stored in a file system folder.
 * @author Laurent Cohen
 * @exclude
 */
public class ThumbnailGenerator {
  /**
   * The name prefix for the generated thumbnails.
   */
  private  static final String TH_PREFIX = "_th_";
  /**
   * The name prefix for the generated thumbnails.
   */
  private  static final String DEFAULT_INCLUDE_PATH = "../JPPF/docs/home/templates";
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
  private Map<File, ImageAttributes> fileMap = new TreeMap<>();

  /**
   * Initialize this thumbnail generator with the specified root dir, width and height.
   * @param path the path to the directory in which to find the images.
   * @param width the width of the generated thumbnails.
   * @param height the height of the generated thumbnails.
   */
  public ThumbnailGenerator(final String path, final int width, final int height) {
    this(path, width, height, DEFAULT_INCLUDE_PATH, DEFAULT_ROW_LENGTH);
  }

  /**
   * Initialize this thumbnail generator with the specified root dir, width, height
   * and default number of thumbnails per row.
   * @param path the path to the directory in which to find the images.
   * @param width the width of the generated thumbnails.
   * @param height the height of the generated thumbnails.
   * @param rowLength the number of thumbnails per row.
   */
  public ThumbnailGenerator(final String path, final int width, final int height, final int rowLength) {
    this(path, width, height, DEFAULT_INCLUDE_PATH, rowLength);
  }

  /**
   * Initialize this thumbnail generator with the specified root dir, width, height
   * and default number of thumbnails per row.
   * @param path the path to the directory in which to find the images.
   * @param width the width of the generated thumbnails.
   * @param height the height of the generated thumbnails.
   * @param includePath the generated file to include in the screenshots php page.
   * @param rowLength the number of thumbnails per row..
   */
  public ThumbnailGenerator(final String path, final int width, final int height, final String includePath, final int rowLength) {
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
  public void generate() throws Exception {
    generateFileMap();
    generateThumbnails();
    generateIncludeFile();
  }

  /**
   * Generate all thumbnails.
   * @throws Exception if an error is raised while generating the thumbnails.
   */
  private void generateThumbnails() throws Exception {
    for (Map.Entry<File, ImageAttributes> entry: fileMap.entrySet()) {
      BufferedImage img = ImageIO.read(entry.getKey());
      ImageAttributes attrs = entry.getValue();
      attrs.height = img.getHeight();
      BufferedImage thumbnail = scale(img);
      ImageIO.write(thumbnail, "jpeg", attrs.thumbnailFile);
    }
  }

  /**
   * Generate the file to include in the screenshots php page.
   * @throws Exception if an IO error is raised.
   */
  private void generateIncludeFile() throws Exception {
    StringBuilder sb = new StringBuilder();
    int count = 0;
    String indent = "\t\t\t\t\t";
    sb.append(indent).append("<table align=\"center\" border=\"0\" cellspacing=\"0\" cellpadding=\"4\">\n");
    for (Map.Entry<File, ImageAttributes> entry: fileMap.entrySet()) {
      if (count % rowLength == 0) {
        if (count > 0) sb.append(indent).append("\t</tr>\n");
        sb.append(indent).append("\t<tr>\n");
      }
      ImageAttributes attrs = entry.getValue();
      String name1 = entry.getKey().getName();
      String name2 = attrs.thumbnailFile.getName();
      // $template{name="shots-row" image="popup1.gif" thumbnail="popup1.jpg"}$
      sb.append(indent).append("\t\t$template{name=\"shots_cell\" image=\"");
      sb.append(name1);
      sb.append("\" thumbnail=\"");
      sb.append(name2);
      sb.append("\" height=\"");
      sb.append(attrs.height + 60);
      sb.append("\" picnum=\"");
      sb.append(count);
      sb.append("\" shot_title=\"");
      sb.append(titleFromFilename(name1));
      sb.append("\"}$\n");
      count++;
    }
    sb.append(indent).append("\t</tr>\n");
    sb.append(indent).append("</table>\n");
    FileUtils.writeTextFile(includePath + "/shots.html", sb.toString());
    File file = fileMap.keySet().iterator().next();
    FileUtils.writeTextFile(includePath + "/first-shot.html", file.getName());
    FileUtils.writeTextFile(includePath + "/first-shot-title.html", titleFromFilename(file.getName()));
  }

  /**
   * Generate the map of image files to their corresponding thumbnail file.
   * @throws Exception if an IO error occurs.
   */
  private void generateFileMap() throws Exception {
    File dir = new File(path);
    if (!dir.isDirectory()) throw new IOException("The specified path is not a directory");
    File[] list = dir.listFiles(new ImageFileFilter("gif", "jpg", "png"));
    for (File file: list) {
      String s = path;
      if (s.endsWith("/") || s.endsWith("\\")) s = s.substring(0, s.length() - 1);
      s += '/' + TH_PREFIX + file.getName();
      int idx = s.lastIndexOf('.');
      s = s.substring(0, idx+1) + "jpg";
      ImageAttributes attrs = new ImageAttributes();
      attrs.thumbnailFile = new File(s);
      fileMap.put(file, attrs);
    }
  }

  /**
   * Scale the specified image.
   * @param img the image to scaled.
   * @return a scaled version of the input image.
   */
  private BufferedImage scale(final BufferedImage img) {
    int w = img.getWidth();
    int h = img.getHeight();
    double r = Math.max((double) w / width, (double) h / height);
    BufferedImage thumbnail = new BufferedImage((int) (w/r), (int) (h/r), BufferedImage.TYPE_INT_RGB);
    Graphics2D g = thumbnail.createGraphics();
    g.drawImage(img.getScaledInstance((int) (w/r), (int) (h/r), Image.SCALE_AREA_AVERAGING), 0, 0, null);
    return thumbnail;
  }

  /**
   * Generate a title from an image ffile name, assuming a cmael-case notiation.
   * @param source the source from which to geenrate a title.
   * @return the generated title as a string.
   */
  private String titleFromFilename(final String source) {
    String name = FileUtils.getFileName(source);
    int idx = name.lastIndexOf('.');
    if (idx >= 0) name = name.substring(0, idx);
    char[] chars = name.toCharArray();
    StringBuilder sb = new StringBuilder();
    int count = 0;
    for (char c: chars) {
      if ((c == '-') || (c == '-')) sb.append(' ');
      else if (Character.isUpperCase(c) && (count > 0)) sb.append(' ').append(c);
      else sb.append(c);
      count++;
    }
    return sb.toString();
  }

  /**
   * File filter that only accepts files with specified extensions.
   * @exclude
   */
  public static class ImageFileFilter implements FileFilter {
    /**
     * An array of the accepted extensions.
     */
    private String[] extensions = null;

    /**
     * Initialize this filter with the specified extensions.
     * @param extensions an array of the accepted extensions.
     */
    public ImageFileFilter(final String...extensions) {
      this.extensions = extensions;
    }

    @Override
    public boolean accept(final File file) {
      if (file.getName().startsWith(TH_PREFIX)) return false;
      String ext = FileUtils.getFileExtension(file);
      for (String s: extensions) {
        if (s.equalsIgnoreCase(ext)) return true;
      }
      return false;
    }
  }

  /**
   * Perform the thumbnail generation.
   * @param args contains in that order: root path, thumbnail width, thumbnail height.
   */
  public static void main(final String...args) {
    try {
      String path = args[0];
      int width = Integer.valueOf(args[1]);
      int height = Integer.valueOf(args[2]);
      String includePath = args[3];
      int rowLength = Integer.valueOf(args[4]);
      System.out.println("Using folder = " + path + ", max width = " + width + ", max height = " + height + ", include file path = " + includePath + ", thumbnails per row = " + rowLength);
      ThumbnailGenerator tg = new ThumbnailGenerator(path, width, height, includePath, rowLength);
      tg.generate();
      System.out.println("finished");
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 
   */
  public static class ImageAttributes {
    /**
     * 
     */
    public File thumbnailFile;
    /**
     * 
     */
    public int height;
  }
}
