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

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.*;

import javax.sql.DataSource;

import org.jppf.JPPFException;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class represents a registry and factory for data sources defined via JPPF configuration properties.
 * @author Laurent Cohen
 */
public final class JPPFDatasourceFactory {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFDatasourceFactory.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Start prefix for the name of all properties that define datasources.
   */
  private static final String DS_PROP_PREFIX = "jppf.datasource.";
  /**
   * Regex pattern for a property that assings a datasource name.
   */
  private static final Pattern DS_NAME_PATTERN = Pattern.compile("^" + DS_PROP_PREFIX.replace(".", "\\.") + "(.*)\\.name$");
  //private static final Pattern DS_NAME_PATTERN = Pattern.compile("^jppf\\.datasource\\.(.*)\\.name$");
  /**
   * Singleton instance of this class.
   */
  private static JPPFDatasourceFactory INSTANCE;
  /**
   * Mapping of datasources to their assigned name.
   */
  private final Map<String, DataSource> dsMap = new HashMap<>();
  /**
   * Performs the actual creation of the datasources and connection pools.
   */
  private final DatasourceInitializer initializer;
  /**
   * Possible scopes.
   * @exclude
   */
  public enum Scope {
    /**
     * Local JVM only.
     */
    LOCAL,
    /**
     * Remote nodes only.
     */
    REMOTE,
    /**
     * Local JVM and remote nodes.
     */
    ANY
  }

  /**
   * Can't be instantiated from another class.
   * @param initializer the initializer to use.
   */
  private JPPFDatasourceFactory(final DatasourceInitializer initializer) {
    this.initializer = initializer;
  }

  /**
   * Get a data source factory. This method always returns the same instance.
   * @return a {@link JPPFDatasourceFactory} instance.
   */
  public synchronized static JPPFDatasourceFactory getInstance() {
    if (INSTANCE == null) INSTANCE = new JPPFDatasourceFactory(new DatasourceInitializerImpl());
    return INSTANCE;
  }

  /**
   * Get the data source with the specified name.
   * @param name the name assigned to the data source.
   * @return a DataSource instance, or {@code null} if no such data source could be retrieved.
   */
  public DataSource getDataSource(final String name) {
    synchronized(dsMap) {
      return dsMap.get(name);
    }
  }

  /**
   * Get the names of all currently defined data sources.
   * @return a {@link List} of datasource names.
   */
  public List<String> getDataSourceNames() {
    synchronized(dsMap) {
      return new ArrayList<>(dsMap.keySet());
    }
  }

  /**
   * Create a data source from the specified configuration properties. This method assumes the property names have no prefix.
   * @param name the name of the data source to create.
   * @param props the data source configuration properties.
   * @return a {@link DataSource} instance.
   */
  public DataSource createDataSource(final String name, final Properties props) {
    if (name != null) props.setProperty("name", name);
    final Pair<String, DataSource> p = createDataSourceInternal(props, null, null);
    return (p == null) ? null : p.second();
  }

  /**
   * Create one or more data sources from the specified configuration properties. This method assumes the property names have
   * the same format as for static datasources configurations, that is:<br>
   * {@code jppf.datasource.<configId>.<property_name> = <property_value>}.
   * @param props the data sources configuration properties.
   * @return a {@link Map} whose keys are the names of the created data sources and whose values are the corresponding {@link DataSource} objects.
   */
  public Map<String, DataSource> createDataSources(final Properties props) {
    final TypedProperties config = (props instanceof TypedProperties) ? (TypedProperties) props : new TypedProperties(props);
    return configure(extractDefinitions(config, Scope.LOCAL), null);
  }

  /**
   * Remove the data source with the specified name. This method also closes the data source and releases the resources it is using.
   * @param name the name assigned to the datasource.
   * @return {@code true} if the removal was succcessful, {@code false} if it fails for any reason, including if no data source with the specified name exists.
   */
  public boolean removeDataSource(final String name) {
    synchronized(dsMap) {
      final DataSource ds = dsMap.remove(name);
      if (ds != null) initializer.close(ds);
      return ds != null;
    }
  }

  /**
   * Close and remove all the data sources in this registry.
   */
  public void clear() {
    synchronized(dsMap) {
      for (DataSource ds: dsMap.values()) initializer.close(ds);
      dsMap.clear();
    }
  }

  /**
   * Create and configure the datasources defined in the specified configuration.
   * @param config the configuration properties that define the datasources.
   * @param scope determines which scope to create data sources for.
   * @exclude
   */
  public void configure(final TypedProperties config, final Scope scope) {
    configure(extractDefinitions(config, scope), null);
  }

  /**
   * Create and configure the datasources defined in the specified configuration.
   * @param config the configuration properties that define the datasources.
   * @param scope determines which scope to create datasources for.
   * @param info the system information match an eventual execution policy.
   * @exclude
   */
  public void configure(final TypedProperties config, final Scope scope, final JPPFSystemInformation info) {
    configure(extractDefinitions(config, scope), info);
  }

  /**
   * Create and configure the datasources defined in the specified configurations.
   * @param definitionsMap a mapping of config ids and corresponding map of properties.
   * @exclude
   */
  public void configure(final Map<String, TypedProperties> definitionsMap) {
    configure(definitionsMap, null);
  }

  /**
   * Create and configure the datasources defined in the specified configurations.
   * The definitions may contain an execution policy which is then matched against
   * the specified {@link JPPFSystemInformation} to determine whteher the datasource should b created.
   * @param definitionsMap a mapping of config ids and corresponding map of properties.
   * @param info the system information match an eventual execution policy.
   * @return a {@link Map} whose keys are the names of the created datasources and whose values are the corresponding {@link DataSource} objects.
   * @exclude
   */
  public Map<String, DataSource> configure(final Map<String, TypedProperties> definitionsMap, final JPPFSystemInformation info) {
    final Map<String, DataSource> result = new HashMap<>();
    for (final Map.Entry<String, TypedProperties> entry: definitionsMap.entrySet()) {
      final Pair<String, DataSource> p = createDataSourceInternal(entry.getValue(), entry.getKey(), info);
      if (p != null) result.put(p.first(), p.second());
    }
    return result;
  }

  /**
   * Create and configure the datasources defined in the specified configuration.
   * @param config the configuration properties that define the datasources.
   * @param requestedScope in a driver, determines whether the definitions are intended for the nodes ({@code true}) or for the local JVM ({@code false}).
   * @return a mapping of config ids to the corresponding properties.
   * @exclude
   */
  public static Map<String, TypedProperties> extractDefinitions(final TypedProperties config, final Scope requestedScope) {
    final Scope reqScope = (requestedScope == null) ? Scope.LOCAL : requestedScope;
    final TypedProperties allDSProps = config.filter(new TypedProperties.Filter() {
      @Override
      public boolean accepts(final String name, final String value) {
        return (name != null) && name.startsWith(DS_PROP_PREFIX);
      }
    });
    final List<String> ids = getConfigIds(allDSProps, reqScope);
    if (debugEnabled) log.debug("found datasource configuration ids with scope={}: {}", reqScope, ids);
    final Map<String, TypedProperties> result = new HashMap<>(ids.size());
    for (final String id: ids) {
      final String prefix = DS_PROP_PREFIX + id + ".";
      final TypedProperties dsProps = allDSProps.filter(new TypedProperties.Filter() {
        @Override
        public boolean accepts(final String name, final String value) {
          return (name != null) && name.startsWith(prefix);
        }
      });
      resolvePolicy(dsProps, id);
      result.put(id, dsProps);
    }
    return result;
  }

  /**
   * 
   * @param allDSProps the configuration properties that define the datasources.
   * @param reqScope in a driver, determines whether the definitions are intended for the nodes ({@code true}) or for the local JVM ({@code false}).
   * @return a list of datasource configuration ids, possibly empty but never null.
   */
  private static List<String> getConfigIds(final TypedProperties allDSProps, final Scope reqScope) {
    final List<String> ids = new ArrayList<>();
    for (final String name: allDSProps.stringPropertyNames()) {
      final Matcher matcher = DS_NAME_PATTERN.matcher(name);
      if (matcher.matches()) {
        final String id = matcher.group(1);
        final String s = allDSProps.getString(DS_PROP_PREFIX + id + ".scope", Scope.LOCAL.name());
        Scope actualScope = Scope.LOCAL;
        for (final Scope sc: Scope.values()) {
          if (sc.name().equalsIgnoreCase(s)) {
            actualScope = sc;
            break;
          }
        }
        if ((actualScope == Scope.ANY) || (actualScope == reqScope)) ids.add(id);
      }
    }
    return ids;
  }

  /**
   * Create a datasource from the specified configuration properties. The {@code configId} is used to build a prefix for
   * the relevant property names, in the format {@code jppf.datasource.<configId>.<property_name> = <value>}.
   * If configId is {@code null}, then no prefix is applied.
   * @param props the datasource properties.
   * @param configId the identifier of the datasource in the configuration.
   * @param info the system information match an eventual execution policy.
   * @return a {@link Pair} made of the datasource name and its corresponding {@link DataSource} instance.
   */
  private Pair<String, DataSource> createDataSourceInternal(final Properties props, final String configId, final JPPFSystemInformation info) {
    final String prefix = (configId == null) ? null : DS_PROP_PREFIX + configId + ".";
    final String start = (prefix == null) ? "" : prefix;
    final String dsName = props.getProperty(start + "name");
    if (dsName == null) return null;
    if (getDataSource(dsName) != null) {
      log.warn(String.format("ignoring duplicate definition for datasource '%s' : %s", dsName, props));
      return null;
    }
    if (info != null) {
      final String policyDef = props.getProperty(start + "policy");
      final String policyText = props.getProperty(start + "policy.text");
      if ((policyDef != null) && (policyText != null)) {
        try {
          final ExecutionPolicy policy = PolicyParser.parsePolicy(policyText);
          if (policy != null) {
            if (!policy.evaluate(info)) {
              if (debugEnabled) log.debug("datasource '{}' execution policy does not macth for this node, it will be ignored", dsName);
              return null;
            }
          }
        } catch (final Exception e) {
          log.error(String.format("failed to parse execution policy for datasource '%'%npolicy defintion: %s%npolicy text: %s%nException: %s",
            dsName, policyDef, policyText, ExceptionUtils.getStackTrace(e)));
        }
      }
    }
    final TypedProperties cleanProps = new TypedProperties();
    for (final String key: props.stringPropertyNames()) {
      final String newKey = (prefix != null) && key.startsWith(prefix) ? key.substring(prefix.length()) : key;
      cleanProps.put(newKey, props.getProperty(key));
    }
    synchronized(dsMap) {
      final DataSource ds = initializer.createDataSource(cleanProps, dsName);
      dsMap.put(dsName, ds);
      if (debugEnabled) log.debug(String.format("defined datasource with configId=%s, name=%s, properties=%s", configId, dsName, cleanProps));
      return new Pair<>(dsName, ds);
    }
  }

  /**
   * Create the execution policy form the defintiion.
   * @param props the datasource properties.
   * @param configId the identifier of the datasource in the configuration.
   * @return an {@link ExecutionPolicy} instance, or {@code null} if none could be created.
   */
  private static  String resolvePolicy(final TypedProperties props, final String configId) {
    String policy = null;
    try {
      final String prefix = (configId == null) ? "" : "jppf.datasource." + configId + ".";
      final String s = props.getString(prefix + "policy");
      if (s == null) return null;
      final String[] tokens = s.split("\\|");
      for (int i=0; i<tokens.length; i++) tokens[i] = tokens[i].trim();
      final String type;
      final String source;
      if (tokens.length >= 2) {
        type = tokens[0].toLowerCase();
        source = tokens[1];
      } else {
        type = "inline";
        source = tokens[0];
      }
      try (final Reader reader = getPolicyReader(type, source)) {
        policy = FileUtils.readTextFile(reader);
        if (policy != null) props.setString(prefix + "policy.text", policy);
      }
    } catch (final Exception e) {
      log.error("error resolving the execution policy for datasource definition with configId=" + configId, e);
    }
    return policy;
  }

  /**
   * Get a reader for the execution policy based on the type and source.
   * @param type the type of source: one of "inline", "file", "url".
   * @param source the source for the policy, its smeaning depends on the type.
   * @return an execution policy parsed from the source.
   * @throws Exception if any error occurs.
   */
  private static Reader getPolicyReader(final String type, final String source) throws Exception {
    Reader reader = null;
    switch(type) {
      case "inline":
        reader = new StringReader(source);
        break;

      case "file":
        reader = FileUtils.getFileReader(source);
        break;

      case "url":
        final URL url = new URL(source);
        reader = new InputStreamReader(url.openConnection().getInputStream(), "utf-8");
        break;

      default:
        throw new JPPFException("unknown soure type '" + type + "' for execution policy '" + source + "'");
    }
    return reader;
  }
}
