/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package org.jppf.ui.monitoring;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.text.NumberFormat;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import org.jppf.libmanagement.Downloader;
import org.jppf.server.protocol.*;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.FileUtils;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * This class provides a graphical interface for monitoring the status and health of the JPPF servers and nodes.<br>
 * It also provides a few customization options, such as setting the interval between 2 server refreshes.
 * @author Laurent Cohen
 */
public class ConsoleLoader
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ConsoleLoader.class);
  /**
   * Determines whether to proceed with the download or not.
   */
  private static boolean doDownload = false;
  /**
   * The title of the frame.
   */
  private static final String TITLE = "Charting Libraries are Missing";
  /**
   * The frame.
   */
  private static JFrame frame = null;

  /**
   * Start the console UI, optionally with the charting components.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      System.out.println("Default charset: " + Charset.defaultCharset());
      startWithCheckNoDownload();
      log.info("terminating");
    }
    catch(Exception e)
    {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }

  /**
   * Check if the charting library classes are avaialble from the classpath.
   * @return <code>true</code> if the classes are available, <code>false</code> otherwise.
   */
  private static boolean checkChartClassesAvailable()
  {
    try
    {
      Class clazz = Class.forName("org.jfree.chart.ChartFactory");
      return true;
    }
    catch(ClassNotFoundException e)
    {
      return false;
    }
  }

  /**
   * Start the console UI, optionally with the charting components.
   * This method checks whether the JFreeChart libraries are present in the classpath.
   * If not, the charting functionalities are discarded from the application.
   * @throws Exception if any error occurs.
   */
  private static void startWithCheckNoDownload() throws Exception
  {
    boolean present = checkChartClassesAvailable();
    String xmlPath = "org/jppf/ui/options/xml/JPPFAdminTool" + (present ? "" : "NoCharts") + ".xml";
    UILauncher.main(xmlPath, "file");
  }

  /**
   * Start the console UI, optionally with the charting components.
   * This method checks whether the JFreeChart libraries are present in the classpath.
   * If not, we propose the user to either download and install the jars automatically, or use the console without charts.
   * @throws Exception if any error occurs.
   */
  private static void startWithCheckAndDownload() throws Exception
  {
    String[] names = { "jcommon-1.0.15.jar", "jfreechart-1.0.12.jar" };
    File folder = new File("lib");
    File[] files = FileUtils.toFiles(folder, names);
    Downloader downloader = new Downloader();
    boolean available = checkChartClassesAvailable();
    boolean present = downloader.checkFilesPresent(folder, names);
    if (!present && !available)
    {
      File dontAskAgain = new File(".dontAskAgain");
      if (!dontAskAgain.exists())
      {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        frame = new JFrame(TITLE);
        frame.setUndecorated(true);
        frame.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/jppf-icon.gif").getImage());
        if (showDownloadDialog())
        {
          downloader.setListener(new DownloadListener());
          downloader.extractFiles("http://sourceforge.net/projects/jfreechart/files/1.%20JFreeChart/1.0.12/jfreechart-1.0.12.zip/download", "lib/", names);
          present = downloader.checkFilesPresent(folder, names);
        }
        frame.setVisible(false);
        frame.dispose();
      }
    }
    if (present && !available)
    {
      ClassLoader cl = ConsoleLoader.class.getClassLoader();
      URL[] urls = FileUtils.toURLs(files);
      ConsoleClassLoader consoleClassLoader = new ConsoleClassLoader(null, cl);
      for (URL url: urls) consoleClassLoader.addURL(url);
      Thread.currentThread().setContextClassLoader(consoleClassLoader);
    }
    String xmlPath = "org/jppf/ui/options/xml/JPPFAdminTool" + (present || available ? "" : "NoCharts") + ".xml";
    UILauncher.main(xmlPath, "file");
  }

  /**
   * Shows a dialog asking the user whether to download the charting libs or not.
   * @return true if the user accepted automatic dialog.
   * @throws Exception if any error occurs.
   */
  private static boolean showDownloadDialog() throws Exception
  {
    final OptionElement panel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/ChartsCheckPanel.xml");
    JButton yesBtn = (JButton) panel.findFirstWithName("/YesBtn").getUIComponent();
    JButton noBtn = (JButton) panel.findFirstWithName("/NoBtn").getUIComponent();
    final JDialog dialog = new JDialog(frame, TITLE, true);
    doDownload = false;
    yesBtn.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(final ActionEvent event)
      {
        doDownload = true;
        closeDialog(dialog, panel);
      }
    });
    noBtn.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(final ActionEvent event)
      {
        closeDialog(dialog, panel);
      }
    });
    TextAreaOption textArea = (TextAreaOption) panel.findFirstWithName("/msgText");
    StringBuilder sb = new StringBuilder();
    sb.append("\nJPPF Admin console has detected the JFreeChart charting libraries are missing");
    sb.append("\nIf you choose not to download them, charts will not be available in the console");
    sb.append("\nDo you want do download the JFreeChart libraries?\n");
    textArea.getTextArea().setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
    textArea.setValue(sb.toString());
    dialog.add(panel.getUIComponent());
    frame.setVisible(true);
    SwingUtilities.invokeAndWait(new Runnable()
    {
      @Override
      public void run()
      {
        dialog.pack();
        dialog.setVisible(true);
      }
    });
    return doDownload;
  }

  /**
   * Close the opened dialog.
   * @param d the dialog to close.
   * @param panel the panel managed by the dialog.
   */
  private static void closeDialog(final JDialog d, final OptionElement panel)
  {
    BooleanOption opt = (BooleanOption) panel.findFirstWithName("/dontAskAgain");
    Boolean dontAskAgain = (Boolean) opt.getValue();
    if (dontAskAgain == null) dontAskAgain = Boolean.TRUE;
    if (dontAskAgain)
    {
      FileWriter writer = null;
      try
      {
        writer = new FileWriter(".dontAskAgain");
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
      finally
      {
        StreamUtils.closeSilent(writer);
      }
    }
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        if (d != null)
        {
          d.setVisible(false);
          d.dispose();
        }
      }
    });
  }

  /**
   * 
   */
  private static class DownloadListener implements LocationEventListener
  {
    /**
     * Total count of bytes transferred.
     */
    private int count = 0;
    /**
     * Used to format the number of bytes downloaded.
     */
    private NumberFormat nf = null;
    /**
     * Window used to display the progress.
     */
    private Window window = null;
    /**
     * Contains the displayed progress text.
     */
    private JLabel label = null;
    /**
     * 
     */
    private double max = 7542745.0;

    /**
     * Default constructor.
     */
    public DownloadListener()
    {
      nf = NumberFormat.getPercentInstance();
      nf.setMinimumFractionDigits(1);
      nf.setMaximumFractionDigits(1);
      nf.setGroupingUsed(true);
      Font font = new Font("Arial", Font.BOLD, 24);
      label = new JLabel("");
      label.setFont(font);
      label.setHorizontalAlignment(SwingConstants.RIGHT);
      label.setPreferredSize(new Dimension(80, 20));
      window = new Window(frame);
      window.setLayout(new MigLayout("fill"));
      JLabel l = new JLabel("Download in progress, please wait ...");
      l.setFont(font);
      window.add(l);
      //window.add(label, "grow, push, gap rel");
      window.setLocation(400, 400);
      window.pack();
      window.setVisible(true);
    }

    /**
     * Notification that some data was transferred from a source to a destination.
     * @param event - the event that encapsulates the transfer information.
     * @see org.jppf.server.protocol.LocationEventListener#dataTransferred(org.jppf.server.protocol.LocationEvent)
     */
    @Override
    public void dataTransferred(final LocationEvent event)
    {
      count += event.bytesTransferred();
      /*
      SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          label.setText(nf.format(count / max));
        }
      });
      */
    }
  }

  /**
   * Custom class loader.
   */
  private static class ConsoleClassLoader extends URLClassLoader
  {
    /**
     * Initialize with the specified urls and parent class loader.
     * @param urls - the initial URLs.
     * @param cl - the parent class loader.
     */
    public ConsoleClassLoader(final URL[] urls, final ClassLoader cl)
    {
      super(urls == null ? new URL[0] : urls, cl);
    }

    /**
     * Add a url to the existing class path.
     * @param url - the url to add.
     * @see java.net.URLClassLoader#addURL(java.net.URL)
     */
    @Override
    public void addURL(final URL url)
    {
      super.addURL(url);
    }
  }
}
