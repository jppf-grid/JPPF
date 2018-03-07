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

package org.jppf.utils.configuration;

import java.lang.reflect.*;
import java.util.*;

import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ConfigurationUtils {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ConfigurationUtils.class);

  /**
   * Get the list of all predefined configuration properties.
   * @param c the class containing the proeprties constants.
   * @return A list of {@link JPPFProperty} instances.
  */
  public synchronized static List<JPPFProperty<?>> allProperties(final Class<?> c) {
    final List<JPPFProperty<?>> properties = new ArrayList<>();
    try {
      final String i18nBase = c.getPackage().getName() + ".i18n." + c.getSimpleName();
      final Field[] fields = c.getDeclaredFields();
      for (Field field: fields) {
        final int mod = field.getModifiers();
        if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && Modifier.isFinal(mod) && (JPPFProperty.class == field.getType())) {
          if (!field.isAccessible()) field.setAccessible(true);
          final JPPFProperty<?> prop = (JPPFProperty<?>) field.get(null);
          if (prop instanceof AbstractJPPFProperty) ((AbstractJPPFProperty<?>) prop).setI18nBase(i18nBase);
          properties.add(prop);
        }
      }
    } catch (final Exception e) {
      log.error("error listing the properties in class {}:\n{}", c, ExceptionUtils.getStackTrace(e));
    }
    return properties;
  }
}
