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

package org.jppf.classloader;

/**
 * An enumeration of the possible keys in a {@link JPPFResourceWrapper} data map.
 * @author Laurent Cohen
 * @exclude
 */
public enum ResourceIdentifier
{
  /**
   * A resource definition, normally file or class bytes.
   */
  DEFINITION,
  /**
   * Flag indicating that all definitions that can be found for a given resource should be returned.
   */
  MULTIPLE,
  /**
   * The names of multiple resources for which to find the defintion.
   */
  MULTIPLE_NAMES,
  /**
   * The name of a single resource for which to lookup the defintion.
   */
  NAME,
  /**
   * The definition of the result of a callable sent by a node to a client.
   */
  CALLABLE,
  /**
   * The identifier of a callable sent by a node to a client.
   */
  CALLABLE_ID,
  /**
   * The identifier of a callable within a driver instance.
   */
  DRIVER_CALLABLE_ID,
  /**
   * The resources held by a {@link CompositeResourceWrapper}.
   */
  RESOURCES_KEY,
  /**
   * Mapping of a list of available definitions for each resource name when looking up multiple resources. 
   */
  RESOURCE_MAP,
  /**
   * A list of available definitions for a single resource name. 
   */
  RESOURCE_LIST,
  /**
   * A node uuid.
   */
  NODE_UUID,
  /**
   * Flag indicating whether the remote peer is a peer server.
   */
  PEER,
  /**
   * Unique id of a client connection.
   */
  CONNECTION_UUID,
  /**
   * Whether lookups of resource in the file system are allowed.
   */
  FILE_LOOKUP_ALLOWED,
}
