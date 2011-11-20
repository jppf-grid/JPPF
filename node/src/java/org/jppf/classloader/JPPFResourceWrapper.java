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
package org.jppf.classloader;

import java.io.Serializable;
import java.util.*;

import org.jppf.utils.TraversalList;

/**
 * Instances of this class encapsulate the necessary information used by the network classloader,
 * for sending class definition requests as well as receiving the class definitions.
 * @author Laurent Cohen
 */
public class JPPFResourceWrapper implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Enumeration of the possible states for this resource wrapper.
   */
  public enum State
  {
    /**
     * State for a node first contacting the class server.
     */
    NODE_INITIATION,
    /**
     * State for a node requesting a resource from the class server.
     */
    NODE_REQUEST,
    /**
     * State for a node receiving a resource from the class server.
     */
    NODE_RESPONSE,
    /**
     * State for a resource provider first contacting the class server.
     */
    PROVIDER_INITIATION,
    /**
     * State for the class server requesting a resource from a resource provider.
     */
    PROVIDER_REQUEST,
    /**
     * State for the class server receiving a resource from a resource provider.
     */
    PROVIDER_RESPONSE
  }

  /**
   * Keeps and manages the uuid path list and the current position in it.
   */
  private TraversalList<String> uuidPath = new TraversalList<String>();
  /**
   * Determines whether the class should be loaded through the network classloader.
   */
  private boolean dynamic = false;
  /**
   * The state associated with this resource wrapper.
   */
  private State state = null;
  /**
   * The uuid sent by a node when it first contacts a resource provider.
   */
  private String providerUuid = null;
  /**
   * Determines whether the resource is to be loaded using <code>ClassLoader.getResource()</code>.
   */
  private boolean asResource = false;
  /**
   * The identifier for the driver's management (JMX) server.
   */
  private String managementId = null;
  /**
   * Uuid of the original task bundle that triggered this resource request.
   */
  private String requestUuid = null;
  /**
   * Contains data about th kind of lookup that is to be done.
   */
  private Map<String, Object> data = new HashMap<String, Object>();

  /**
   * Add a uuid to the uuid path of this resource wrapper.
   * @param uuid - the identifier as a string.
   */
  public void addUuid(final String uuid)
  {
    uuidPath.add(uuid);
  }

  /**
   * Get the name of the class whose definition is requested.
   * @return the class name as a string.
   */
  public String getName()
  {
    return (String) getData("name");
  }

  /**
   * Set the name of the class whose definition is requested.
   * @param name - the class name as a string.
   */
  public void setName(final String name)
  {
    setData("name", name);
  }

  /**
   * Get the actual definition of the requested class.
   * @return the class definition as an array of bytes.
   */
  public byte[] getDefinition()
  {
    return (byte[]) getData("definition");
  }

  /**
   * Set the actual definition of the requested class.
   * @param definition - the class definition as an array of bytes.
   */
  public void setDefinition(final byte[] definition)
  {
    setData("definition", definition);
  }

  /**
   * Determine whether the class should be loaded through the network classloader.
   * @return true if the class should be loaded via the network classloader, false otherwise.
   */
  public boolean isDynamic()
  {
    return dynamic;
  }

  /**
   * Set whether the class should be loaded through the network classloader.
   * @param dynamic - true if the class should be loaded via the network classloader, false otherwise.
   */
  public void setDynamic(final boolean dynamic)
  {
    this.dynamic = dynamic;
  }

  /**
   * Get the state associated with this resource wrapper.
   * @return a <code>State</code> typesafe enumerated value.
   */
  public State getState()
  {
    return state;
  }

  /**
   * Set the state associated with this resource wrapper.
   * @param state - a <code>State</code> typesafe enumerated value.
   */
  public void setState(final State state)
  {
    this.state = state;
  }

  /**
   * Get a reference to the traversal list that Keeps and manages the uuid path list
   * and the current position in it.
   * @return a traversal list of string elements.
   */
  public TraversalList<String> getUuidPath()
  {
    return uuidPath;
  }

  /**
   * Set the reference to the traversal list that Keeps and manages the uuid path list
   * and the current position in it.
   * @param uuidPath - a traversal list of string elements.
   */
  public void setUuidPath(final TraversalList<String> uuidPath)
  {
    this.uuidPath = uuidPath;
  }

  /**
   * Get the uuid sent by a node when it first contacts a resource provider.
   * @return the uuid as a string.
   */
  public String getProviderUuid()
  {
    return providerUuid;
  }

  /**
   * Set the uuid sent by a node when it first contacts a resource provider.
   * @param providerUuid - the uuid as a string.
   */
  public void setProviderUuid(final String providerUuid)
  {
    this.providerUuid = providerUuid;
  }

  /**
   * Determine whether the resource is to be loaded using <code>ClassLoader.getResource()</code>.
   * @return true if the resource is loaded using getResource(), false otherwise.
   */
  public boolean isAsResource()
  {
    return asResource;
  }

  /**
   * Set whether the resource is to be loaded using <code>ClassLoader.getResource()</code>.
   * @param asResource - true if the resource is loaded using getResource(), false otherwise.
   */
  public void setAsResource(final boolean asResource)
  {
    this.asResource = asResource;
  }

  /**
   * Get the identifier for the driver's management (JMX) server.
   * @return the identifier as a string.
   */
  public String getManagementId()
  {
    return managementId;
  }

  /**
   * Set the identifier for the driver's management (JMX) server.
   * @param managementId - the identifier as a string.
   */
  public void setManagementId(final String managementId)
  {
    this.managementId = managementId;
  }

  /**
   * Get the uuid for the original task bundle that triggered this resource request.
   * @return the uuid as a string.
   */
  public String getRequestUuid()
  {
    return requestUuid;
  }

  /**
   * Set the uuid for the original task bundle that triggered this resource request.
   * @param requestUuid the uuid as a string.
   */
  public void setRequestUuid(final String requestUuid)
  {
    this.requestUuid = requestUuid;
  }

  /**
   * Get the serialized callback to execute code on the client side.
   * @return a <code>byte[]</code> instance.
   */
  public byte[] getCallable()
  {
    return (byte[]) getData("callable");
  }

  /**
   * Set the serialized callback to execute code on the client side.
   * @param callable - a <code>byte[]</code> instance.
   */
  public void setCallable(final byte[] callable)
  {
    setData("callable", callable);
  }

  /**
   * Get the metadata corresponding to the specified key.
   * @param key - the string identifying the metadata.
   * @return an object value or null if the metadata could not be found.
   */
  public Object getData(final String key)
  {
    return data.get(key);
  }

  /**
   * Get the metadata corresponding to the specified key.
   * @param key - the string identifying the metadata.
   * @param value - the value of the metadata.
   */
  public void setData(final String key, final Object value)
  {
    data.put(key, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("JPPFResourceWrapper[");
    sb.append("dynamic=").append(dynamic);
    sb.append(", asResource=").append(asResource);
    sb.append(", state=").append(state);
    sb.append(", name=").append(data.get("name"));
    sb.append(']');
    return sb.toString();
  }
}
