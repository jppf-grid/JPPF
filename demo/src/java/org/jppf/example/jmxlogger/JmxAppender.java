/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.example.jmxlogger;

import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;

/**
 * An appender that delegates message appending to a JmxLogger.
 * @author Laurent Cohen
 */
public class JmxAppender extends AppenderSkeleton
{
	/**
	 * Default layout to use if none is specified.
	 */
	private static Layout DEFAULT_LAYOUT = new SimpleLayout();
	/**
	 * The logger to which appends are delegated.
	 */
	private JmxLoggerMBean logger = null;

	/**
	 * Initialize this appender with the specified {@link JmxLogger}.
	 * @param logger the logger to delegate logging to.
	 */
	public JmxAppender(JmxLoggerMBean logger)
	{
		this.logger = logger;
	}

	/**
	 * Append the specified event to the {@link JmxLogger}.
	 * @param event the event to log.
	 * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
	 */
	protected void append(LoggingEvent event)
	{
		Layout layout = getLayout();
		if (layout == null) layout = DEFAULT_LAYOUT;
		String s = (layout != null) ? layout.format(event) : event.getRenderedMessage();
		logger.log(s);
	}

	/**
	 * Close this appender. This method does nothing.
	 * @see org.apache.log4j.Appender#close()
	 */
	public void close()
	{
	}

	/**
	 * Determines whether a layout is required.
	 * @return true.
	 * @see org.apache.log4j.Appender#requiresLayout()
	 */
	public boolean requiresLayout()
	{
		return true;
	}
}
