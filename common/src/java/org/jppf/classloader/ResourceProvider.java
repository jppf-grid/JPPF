/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.classloader;

import java.util.*;

import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Instances of this interface are dedicated to reading resource files form the JVM's classpath and converting them into arrays of bytes.
 * @author Laurent Cohen
 * @since 5.0
 * @exclude
 */
public interface ResourceProvider {
  /**
   * Get a resource as an array of byte using a call to <b>ClassLoader.getResource()</b>.
   * This method simply calls {@link #getResource(java.lang.String, java.lang.ClassLoader) getResource(String, ClassLoader)}
   * with a null class loader.
   * @param resName the name of the resource to find.
   * @param lookupInFileSystem whether resources should be looked up in the file system if not found in the classpath.
   * @return the content of the resource as an array of bytes.
   */
  byte[] getResource(String resName, boolean lookupInFileSystem);

  /**
   * Get a resource as an array of byte using a call to <b>ClassLoader.getResource()</b>.
   * @param resName the name of the resource to find.
   * @param classloader the class loader to use to load the request resource.
   * @param lookupInFileSystem whether resources should be looked up in the file system if not found in the classpath.
   * @return the content of the resource as an array of bytes.
   */
  byte[] getResource(String resName, ClassLoader classloader, boolean lookupInFileSystem);

  /**
   * Get a resource as an array of byte using a call to <b>ClassLoader.getResource()</b>.
   * @param resName the name of the resource to find.
   * @param classLoaders the set of class loaders available to use to load the requested resource.
   * @param lookupInFileSystem whether resources should be looked up in the file system if not found in the classpath.
   * @return the content of the resource as an array of bytes.
   */
  byte[] getResource(String resName, Collection<ClassLoader> classLoaders, boolean lookupInFileSystem);

  /**
   * Compute a callable sent through the JPPF class loader.
   * @param serializedCallable the callable to execute in serialized form.
   * @return the serialized result of the callable's execution, or of an eventually resulting exception.
   */
  byte[] computeCallable(byte[] serializedCallable);

  /**
   * Get all resources associated with the specified resource name.
   * @param name the name of the resources to look for.
   * @param classloader the class loader used to load the resources.
   * @param lookupInFileSystem whether resources should be looked up in the file system if not found in the classpath.
   * @return the content of all found resources as a list of byte arrays.
   */
  List<byte[]> getMultipleResourcesAsBytes(String name, ClassLoader classloader, boolean lookupInFileSystem);

  /**
   * Get all resources associated with the specified resource name.
   * @param name the name of the resources to look for.
   * @param classLoaders the set of class loaders available to load the resources.
   * @param lookupInFileSystem whether resources should be looked up in the file system if not found in the classpath.
   * @return the content of all found resources as a list of byte arrays.
   */
  List<byte[]> getMultipleResourcesAsBytes(String name, Collection<ClassLoader> classLoaders, boolean lookupInFileSystem);

  /**
   * Get all resources associated with each specified resource name.
   * @param classloader the class loader used to load the resources.
   * @param lookupInFileSystem whether resources should be looked up in the file system if not found in the classpath.
   * @param names the names of all the resources to look for.
   * @return A mapping of each resource names with a list of the byte content of corresponding resources in the classpath.
   */
  Map<String, List<byte[]>> getMultipleResourcesAsBytes(ClassLoader classloader, boolean lookupInFileSystem, String... names);

  /**
   * Get all resources associated with each specified resource name.
   * @param classLoaders the set of class loader available to load the resources.
   * @param lookupInFileSystem whether resources should be looked up in the file system if not found in the classpath.
   * @param names the names of all the resources to look for.
   * @return A mapping of each resource names with a list of the byte content of corresponding resources in the classpath.
   */
  Map<String, List<byte[]>> getMultipleResourcesAsBytes(Collection<ClassLoader> classLoaders, boolean lookupInFileSystem, String...names);

  /**
   * Factory class for {@link ResourceProvider} implementations.
   * The implementation must have a public no-arg constructor and is specified
   * with the configuration property {@code "jppf.resource.provider.class"}.
   * @since 5.0
   * @exclude
   */
  public static class Factory {
    /**
     * Logger for this class.
     */
    private static Logger log = LoggerFactory.getLogger(Factory.class);
    /**
     * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
     */
    private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
    /**
     * Construct a resource provider based on the JPPF configuration.
     * @return an {@link AbstractResourceProvider} implementation.
     */
    public static ResourceProvider initResourceProvider() {
      String name = JPPFConfiguration.get(JPPFProperties.RESOURCE_PROVIDER_CLASS);
      if (debugEnabled) log.debug("jppf.resource.provider.class = {}", name);
      try {
        Class<?> clazz = Class.forName(name);
        return (ResourceProvider) clazz.newInstance();
      } catch (Exception e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
      }
      return new ResourceProviderImpl();
    }
  }
}
