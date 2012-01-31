/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.serialization;

import java.io.*;

import org.jppf.JPPFError;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class builds object streams based on JPPF configuration properties.
 * @author Laurent Cohen
 */
public class JPPFObjectStreamFactory
{
  /**
   * Property name for the object input stream class to use.
   */
  private static final String OIS_CLASS = "jppf.object.input.stream.class";
  /**
   * Property name for the object output stream class to use.
   */
  private static final String OOS_CLASS = "jppf.object.output.stream.class";
  /**
   * Property name for the object stream builder class to use.
   */
  private static final String BUILDER_CLASS = "jppf.object.stream.builder";
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFObjectStreamFactory.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The object stream builder used by this factory to instantiate object streams.
   */
  private static JPPFObjectStreamBuilder builder = initializeBuilder();

  /**
   * Initialize the object stream builder.<br>
   * The lookup for the builder to use is done in this order:
   * <ol>
   * <li>if both &quot;jppf.object.input.stream.class&quot; and &quot;jppf.object.output.stream.class&quot;
   * configuration properties are specified, use an instance of {@link org.jppf.serialization.JPPFConfigurationObjectStreamBuilder JPPFConfigurationObjectStreamBuilder}</li>
   * <li>otherwise, if the &quot;jppf.object.stream.builder&quot; property is specified, use an instance of the specified builder</li>
   * <li>otherwise use an instance of {@link org.jppf.serialization.JPPFObjectStreamBuilderImpl JPPFObjectStreamBuilderImpl}</li>
   * </ol>
   * @return a <code>JPPFObjectStreamBuilder</code> instance.
   */
  private static JPPFObjectStreamBuilder initializeBuilder()
  {
    JPPFObjectStreamBuilder result = null;
    TypedProperties props = JPPFConfiguration.getProperties();
    String oisName = props.getString(OIS_CLASS);
    String oosName = props.getString(OOS_CLASS);
    if ((oisName != null) && (oosName != null))
    {
      try
      {
        return new JPPFConfigurationObjectStreamBuilder(oisName, oosName);
      }
      catch(Exception e)
      {
        StringBuilder sb = new StringBuilder();
        sb.append("Could not instantiate object stream builder for [").append(OIS_CLASS).append(" = ").append(oisName);
        sb.append(", ").append(OOS_CLASS).append(" = ").append(oosName).append("]\nTerminating this application\n");
        log.error(sb + e.getMessage(), e);
        throw new JPPFError(sb + e.getMessage(), e);
      }
    }
    String builderName = props.getString(BUILDER_CLASS);
    if (builderName != null)
    {
      try
      {
        return (JPPFObjectStreamBuilder) Class.forName(builderName).newInstance();
      }
      catch(Exception e)
      {
        StringBuilder sb = new StringBuilder();
        sb.append("Could not instantiate object stream builder for [").append(BUILDER_CLASS).append(" = ");
        sb.append(builderName).append("]\nTerminating this application\n");
        log.error(sb + e.getMessage(), e);
        throw new JPPFError(sb + e.getMessage(), e);
      }
    }
    return new JPPFObjectStreamBuilderImpl();
  }

  /**
   * Obtain an input stream used for deserializing objects.
   * @param	in input stream to read from.
   * @return an <code>ObjectInputStream</code>
   * @throws Exception if an error is raised while creating the stream.
   */
  public static ObjectInputStream newObjectInputStream(final InputStream in) throws Exception
  {
    return builder.newObjectInputStream(in);
  }

  /**
   * Obtain an Output stream used for serializing objects.
   * @param	out output stream to write to.
   * @return an <code>ObjectOutputStream</code>
   * @throws Exception if an error is raised while creating the stream.
   */
  public static ObjectOutputStream newObjectOutputStream(final OutputStream out) throws Exception
  {
    return builder.newObjectOutputStream(out);
  }
}
