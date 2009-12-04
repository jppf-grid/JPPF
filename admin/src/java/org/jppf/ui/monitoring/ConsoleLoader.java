/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jppf.libmanagement.Downloader;
import org.jppf.server.protocol.LocationEvent;
import org.jppf.server.protocol.LocationEventListener;
import org.jppf.ui.options.OptionElement;
import org.jppf.ui.options.TextAreaOption;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.FileUtils;

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
	static Log log = LogFactory.getLog(ConsoleLoader.class);
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
	 * This method checks whether the JFreeChart libraries are present in the classpath.
	 * If not, we propose the user to either download and install the jars automatically, or use the console without charts. 
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			ClassLoader cl = ConsoleLoader.class.getClassLoader();
			String[] names = new String[] { "jcommon-1.0.15.jar", "jfreechart-1.0.12.jar" };
			File folder = new File("lib");
			File[] files = FileUtils.toFiles(folder, names);
			Downloader downloader = new Downloader();
			boolean present = downloader.checkFilesPresent(folder, names);
			if (!present)
			{
				frame = new JFrame(TITLE);
				frame.setUndecorated(true);
				frame.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/jppf-icon.gif").getImage());
				if (showDownloadDialog())
				{
					downloader.setListener(new DownloadListener());
					downloader.extractFiles("http://downloads.sourceforge.net/jfreechart/jfreechart-1.0.12.zip", "lib", names);
					present = downloader.checkFilesPresent(folder, names);
				}
				frame.setVisible(false);
				frame.dispose();
			}
			String xmlPath = "org/jppf/ui/options/xml/JPPFAdminTool" + (present ? "" : "NoCharts") + ".xml";
			if (present)
			{
				URL[] urls = FileUtils.toURLs(files);
				//URLClassLoader consoleClassLoader = new URLClassLoader(urls, cl);
				ConsoleClassLoader consoleClassLoader = new ConsoleClassLoader(null, cl);
				for (URL url: urls) consoleClassLoader.addURL(url);
				Thread.currentThread().setContextClassLoader(consoleClassLoader);
			}
			UILauncher.main(xmlPath, "file");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			log.error(e.getMessage(), e);
			System.exit(1);
		}
	}

	/**
	 * Shows a dialog asking the user whether to download the charting libs or not.
	 * @return true if th euser accepted automatic dialog.
	 * @throws Exception if any error occurs.
	 */
	private static boolean showDownloadDialog() throws Exception
	{
		OptionElement panel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/ChartsCheckPanel.xml");
		JButton yesBtn = (JButton) panel.findFirstWithName("/YesBtn").getUIComponent();
		JButton noBtn = (JButton) panel.findFirstWithName("/NoBtn").getUIComponent();
		final JDialog dialog = new JDialog(frame, TITLE, true);
		//final JDialog dialog = null;
		doDownload = false;
		yesBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				doDownload = true;
				closeDialog(dialog);
			}
		});
		noBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				closeDialog(dialog);
			}
		});
		TextAreaOption textArea = (TextAreaOption) panel.findFirstWithName("/msgText");
		textArea.setValue("\nDo you want do download the JFreeChart libraries?\n");
		dialog.add(panel.getUIComponent());
		frame.setVisible(true);
		SwingUtilities.invokeAndWait(new Runnable()
		{
			public void run()
			{
				dialog.pack();
				dialog.setVisible(true);
			}
		});
		return doDownload;
	}

	/**
	 * Close the opend dialog.
	 * @param d - the dialog to close.
	 */
	private static void closeDialog(final JDialog d)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
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
		 * Contains the displayed progess text.
		 */
		private JLabel label = null;
		/**
		 * 
		 */
		private double max = 7542745;

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
			JLabel l = new JLabel("Download in progress:");
			l.setFont(font);
			window.add(l);
			window.add(label, "grow, push, gap rel");
			window.setLocation(400, 400);
			window.pack();
			window.setVisible(true);
		}

		/**
		 * Notification that some data was transferred from a source to a destination.
		 * @param event - the event that encapsulates the transfer informaiton.
		 * @see org.jppf.server.protocol.LocationEventListener#dataTransferred(org.jppf.server.protocol.LocationEvent)
		 */
		public void dataTransferred(LocationEvent event)
		{
			count += event.bytesTransferred();
			label.setText(nf.format((double) count / max));
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
		public ConsoleClassLoader(URL[] urls, ClassLoader cl)
		{
			super(urls == null ? new URL[0] : urls, cl);
		}

		/**
		 * Add a url to the existing class path.
		 * @param url - the url to add.
		 * @see java.net.URLClassLoader#addURL(java.net.URL)
		 */
		public void addURL(URL url)
		{
			super.addURL(url);
		}
	}
}
