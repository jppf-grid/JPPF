/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.logging.log4j;

import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;
import org.jppf.logging.jmx.JmxMessageNotifier;

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
   * Platform-defined line separator.
   */
  private static String LINE_SEP = System.getProperty("line.separator");
  /**
   * The notifier that sends formatted log messages as JMX notifications.
   */
  private JmxMessageNotifier notifier = null;
  /**
   * The name of the mbean that sends messages as JMX notifications.
   */
  private String mbeanName = null;

  /**
   * Initialize this appender.
   */
  public JmxAppender()
  {
  }

  /**
   * Initialize this appender from its configuration.
   */
  private void init()
  {
    notifier = new JmxMessageNotifier(mbeanName);
  }

  /**
   * Append the specified event to the logger.
   * @param event the event to log.
   * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
   */
  @Override
  protected void append(final LoggingEvent event)
  {
    if (notifier == null) init();
    Layout layout = getLayout();
    if (layout == null) layout = DEFAULT_LAYOUT;
    StringBuilder sb = new StringBuilder(layout.format(event));
    if (layout.ignoresThrowable())
    {
      String[] strs = event.getThrowableStrRep();
      if (strs != null) for (String s: strs) sb.append(s).append(LINE_SEP);
    }
    notifier.sendMessage(sb.toString());
  }

  /**
   * Close this appender. This method does nothing.
   * @see org.apache.log4j.Appender#close()
   */
  @Override
  public void close()
  {
  }

  /**
   * Determines whether a layout is required.
   * @return true.
   * @see org.apache.log4j.Appender#requiresLayout()
   */
  @Override
  public boolean requiresLayout()
  {
    return true;
  }

  /**
   * Get the name of the mbean that sends messages as JMX notifications.
   * @return the mbean name as a string.
   */
  public String getMbeanName()
  {
    return mbeanName;
  }

  /**
   * Set the name of the mbean that sends messages as JMX notifications.
   * @param mbeanName the mbean name as a string.
   */
  public void setMbeanName(final String mbeanName)
  {
    this.mbeanName = mbeanName;
  }
}
