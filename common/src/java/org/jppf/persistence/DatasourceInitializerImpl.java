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

package org.jppf.persistence;

import java.lang.reflect.*;
import java.util.*;

import javax.sql.DataSource;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 * @exclude
 */
public class DatasourceInitializerImpl implements DatasourceInitializer {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DatasourceInitializerImpl.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * HikariDataSource class name.
   */
  private static final String HIKARI_DS_CLASS = "com.zaxxer.hikari.HikariDataSource";
  /**
   * HikariConfig class name.
   */
  private static final String HIKARI_CONFIG_CLASS = "com.zaxxer.hikari.HikariConfig";
  /**
   * PropertyElf class name.
   */
  private static final String PROPERTY_ELF_CLASS = "com.zaxxer.hikari.util.PropertyElf";
  /**
   * The datasource constructor.
   */
  private static Constructor<?> hikariDatasourceConstructor;
  /**
   * The datasource close() method.
   */
  private static Method hikariDatasourceCloseMethod;
  /**
   * The datasource config constructor.
   */
  private static Constructor<?> hikariConfigConstructor;
  /**
   * Whether initialization succeeded.
   */
  private static boolean initSuccess = false;
  /**
   * The names of allowed config properties.
   */
  private static Set<String> allowedProperties;
  static {
    init();
  }

  @Override
  public DataSource createDataSource(final TypedProperties props, final String dsName) {
    if (initSuccess) {
      try {
        props.setString("poolName", dsName);
        final TypedProperties cleanProps = props.filter(new TypedProperties.Filter() {
          @Override
          public boolean accepts(final String name, final String value) {
            final boolean b = (name != null) && allowedProperties.contains(name);
            if (!b && log.isWarnEnabled() && !StringUtils.isOneOf(name, false, "scope", "name", "policy", "policy.text"))
              log.warn(String.format("property '%s' not supported in definition of datasource with name=%s. Will be ignored", name, dsName));
            return b;
          }
        });
        final Object cfg = hikariConfigConstructor.newInstance(cleanProps);
        final DataSource ds = (DataSource) hikariDatasourceConstructor.newInstance(cfg);
        if (debugEnabled) log.debug(String.format("defined datasource with name=%s, properties=%s", dsName, cleanProps));
        return ds;
      } catch (final Exception e) {
        log.error(String.format("defined datasource with name=%s, properties=%s:%n%s", dsName, props, ExceptionUtils.getStackTrace(e)));
      }
    }
    return null;
  }

  @Override
  public void close(final DataSource datasource) {
    try {
      if (hikariDatasourceCloseMethod != null) hikariDatasourceCloseMethod.invoke(datasource);
    } catch (final Exception e) {
      log.error("error closing datasource {}: {}", datasource, ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * Initialize the variables that will allow creating datasources via reflection.
   */
  @SuppressWarnings("unchecked")
  private static void init() {
    try {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) cl = DatasourceInitializerImpl.class.getClassLoader();
      final Class<?> hikariConfigClass;
      try {
        hikariConfigClass = Class.forName(HIKARI_CONFIG_CLASS, true, cl);
      } catch(final ClassNotFoundException e) {
        final String msg = "HikariCP libraries are not in the classpath, no datasource will be defined";
        if (debugEnabled) log.debug(msg, e);
        else log.warn(msg);
        return;
      }
      hikariConfigConstructor = ReflectionHelper.findConstructor(hikariConfigClass, Properties.class);
      final Class<?> hikariDatasourceClass = cl.loadClass(HIKARI_DS_CLASS);
      hikariDatasourceConstructor = ReflectionHelper.findConstructor(hikariDatasourceClass, hikariConfigClass);
      hikariDatasourceCloseMethod = ReflectionHelper.findMethod(hikariDatasourceClass, "close");
      final Class<?> elfPropertyClass = cl.loadClass(PROPERTY_ELF_CLASS);
      final Method getAllowedPropertiesMethod = ReflectionHelper.findMethod(elfPropertyClass, "getPropertyNames", Class.class);
      allowedProperties = (Set<String>) getAllowedPropertiesMethod.invoke(null, hikariConfigClass);
      initSuccess = true;
    } catch (final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }      
  }
}
