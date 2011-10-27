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

package org.jppf.logging.jdk;

import java.util.logging.*;

import org.jppf.logging.jmx.JmxMessageNotifier;

/**
 * A handler that prints log messages as JMX notifications.
 * @author Laurent Cohen
 */
public class JmxHandler extends Handler
{
	/**
	 * The notifier that sends formatted log messages as JMX notifications.
	 */
	private JmxMessageNotifier notifier = null;
	/**
	 * The name of the mbean that sends messages as JMX notifications.
	 */
	private String mbeanName = null;

	/**
	 * Initialize this appender from its configuration.
	 */
	private void init()
	{
		LogManager lm = LogManager.getLogManager();
		mbeanName = lm.getProperty(getClass().getName() + ".mbeanName");
		notifier = new JmxMessageNotifier(mbeanName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void publish(final LogRecord record)
	{
		if (notifier == null) init();
		Formatter f = getFormatter();
		if (f == null) f = new JPPFLogFormatter();
		String s = f.format(record);
		notifier.sendMessage(s);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flush()
	{
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws SecurityException
	{
	}
}
