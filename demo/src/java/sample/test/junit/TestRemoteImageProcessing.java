/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package sample.test.junit;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.*;

/**
 * This class tests the remote task management features of JPPF,
 * such as cancelling or restarting a task and receiving notifications.
 * This test assumes a driver is started with the default ports, and a
 * node is started with jmx port = 12001.
 * @author Laurent Cohen
 */
public class TestRemoteImageProcessing extends TestCase implements Serializable
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Execute a single JPPF task and return the results.
	 * @throws Exception if the execution failed.
	 */
	public void testImageTask() throws Exception
	{
		JPPFTask result = null;
		JPPFClient client = new JPPFClient();
		try
		{
			BufferedImage image = ImageIO.read(new File("../samples-pack/Fractals/data/mandelbrot.jpg"));
			int w = image.getWidth();
			int h = image.getHeight();
			int[] rgb = image.getRGB(0, 0, w, h, null, 0, w);
			DataProvider dataProvider = new MemoryMapDataProvider();
			dataProvider.setValue("image.rgb", rgb);
			dataProvider.setValue("image.width", w);
			dataProvider.setValue("image.height", h);
			JPPFJob job = new JPPFJob(dataProvider);
			job.addTask(new ImageTask());
			List<JPPFTask> results = client.submit(job);
			assertNotNull(results);
			assertFalse(results.isEmpty());
			result = results.get(0);
			assertNull(result.getException());
		}
		finally
		{
			client.close();
		}
	}

	/**
	 * Simple task implementation that waits for the time specified in its constructor.
	 */
	public static class ImageTask extends JPPFTask
	{
		/**
		 * Initialize this task.
		 */
		public ImageTask()
		{
			setId("ImageTask");
		}

		/**
		 * Execute this task.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			fireNotification("Task [id="+getId()+"] started");
			try
			{
				int[] rgb = (int[]) getDataProvider().getValue("image.rgb");
				int w = (Integer) getDataProvider().getValue("image.width");
				int h = (Integer) getDataProvider().getValue("image.height");
				BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
				img.setRGB(0, 0, w, h, rgb, 0, w);
			}
			catch(Exception e)
			{
				setException(e);
			}
			fireNotification("Task [id="+getId()+"] completed");
		}

		/**
		 * Called when the task is cancelled.
		 * @see org.jppf.server.protocol.JPPFTask#onCancel()
		 */
		public void onCancel()
		{
			setResult("cancelled");
		}

		/**
		 * Called when the task is restarted.
		 * @see org.jppf.server.protocol.JPPFTask#onRestart()
		 */
		public void onRestart()
		{
			setResult("restarted");
		}
	}
}
