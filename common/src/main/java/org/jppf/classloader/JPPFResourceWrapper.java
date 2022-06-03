/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import static org.jppf.classloader.ResourceIdentifier.*;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.utils.TraversalList;

/**
 * Instances of this class encapsulate the necessary information used by the network classloader,
 * for sending class definition requests as well as receiving the class definitions.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
public class JPPFResourceWrapper implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Used to generate locally unique ids for the remote-computed callables.
   */
  private static final AtomicLong CALLABLE_ID_SEQUENCE = new AtomicLong(0L);
  /**
   * Used to generate locally unique ids for the remote-computed callables.
   */
  private static final long NO_CALLABLE_ID = -1L;
  /**
   * Constant for an empty <code>JPPFResourceWrapper</code> array.
   */
  public static final JPPFResourceWrapper[] EMPTY_RESOURCE_WRAPPER_ARRAY = new JPPFResourceWrapper[0];

  /**
   * Enumeration of the possible states for this resource wrapper.
   * @exclude
   */
  public enum State {
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
     * State for a node receiving a resource from the class server, when the resonse could not be obtained from the client.
     */
    NODE_RESPONSE_ERROR,
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
    PROVIDER_RESPONSE,
    /**
     * Used for sending a close channel command to the server
     */
    CLOSE_CHANNEL
  }

  /**
   * Keeps and manages the uuid path list and the current position in it.
   */
  private TraversalList<String> uuidPath = new TraversalList<>();
  /**
   * Determines whether the class should be loaded through the network classloader.
   */
  private boolean dynamic;
  /**
   * The state associated with this resource wrapper.
   */
  private State state;
  /**
   * The uuid sent by a node when it first contacts a resource provider.
   */
  private String providerUuid;
  /**
   * Uuid of the original task bundle that triggered this resource request.
   */
  private String requestUuid;
  /**
   * Contains data about the kind of lookup that is to be done.
   */
  private final Map<ResourceIdentifier, Object> dataMap = new EnumMap<>(ResourceIdentifier.class);
  /**
   * Performance optimization.
   */
  protected transient JPPFResourceWrapper[] resources;
  /**
   * Uniquely identifies this resource request in the server.
   */
  private Map<String, Long> resourceIds;
  /**
   * The time at which the request is received by the server.
   */
  private transient long requestStartTime;
  /**
   * Whether this is a handshake request.
   */
  private transient boolean handshaking;

  /**
   * Default constructor.
   */
  public JPPFResourceWrapper() {
  }
  /**
   * Add a uuid to the uuid path of this resource wrapper.
   * @param uuid the identifier as a string.
   */
  public void addUuid(final String uuid) {
    uuidPath.add(uuid);
  }

  /**
   * Get the name of the class whose definition is requested.
   * @return the class name as a string.
   */
  public String getName() {
    return (String) getData(NAME);
  }

  /**
   * Set the name of the class whose definition is requested.
   * @param name the class name as a string.
   */
  public void setName(final String name) {
    setData(NAME, name);
  }

  /**
   * Get the actual definition of the requested class.
   * @return the class definition as an array of bytes.
   */
  public byte[] getDefinition() {
    return (byte[]) getData(DEFINITION);
  }

  /**
   * Set the actual definition of the requested class.
   * @param definition the class definition as an array of bytes.
   */
  public void setDefinition(final byte[] definition) {
    setData(DEFINITION, definition);
  }

  /**
   * Determine whether the class should be loaded through the network classloader.
   * @return true if the class should be loaded via the network classloader, false otherwise.
   */
  public boolean isDynamic() {
    return dynamic;
  }

  /**
   * Set whether the class should be loaded through the network classloader.
   * @param dynamic true if the class should be loaded via the network classloader, false otherwise.
   */
  public void setDynamic(final boolean dynamic) {
    this.dynamic = dynamic;
  }

  /**
   * Get the state associated with this resource wrapper.
   * @return a <code>State</code> typesafe enumerated value.
   */
  public State getState() {
    synchronized (dataMap) {
      return state;
    }
  }

  /**
   * Set the state associated with this resource wrapper.
   * @param state a <code>State</code> typesafe enumerated value.
   */
  public void setState(final State state) {
    synchronized (dataMap) {
      this.state = state;
    }
  }

  /**
   * Get a reference to the traversal list that Keeps and manages the uuid path list
   * and the current position in it.
   * @return a traversal list of string elements.
   */
  public TraversalList<String> getUuidPath() {
    return uuidPath;
  }

  /**
   * Set the reference to the traversal list that Keeps and manages the uuid path list
   * and the current position in it.
   * @param uuidPath a traversal list of string elements.
   */
  public void setUuidPath(final TraversalList<String> uuidPath) {
    this.uuidPath = uuidPath;
  }

  /**
   * Get the uuid sent by a node when it first contacts a resource provider.
   * @return the uuid as a string.
   */
  public String getProviderUuid() {
    return providerUuid;
  }

  /**
   * Set the uuid sent by a node when it first contacts a resource provider.
   * @param providerUuid the uuid as a string.
   */
  public void setProviderUuid(final String providerUuid) {
    this.providerUuid = providerUuid;
  }

  /**
   * Get the uuid for the original task bundle that triggered this resource request.
   * @return the uuid as a string.
   */
  public String getRequestUuid() {
    return requestUuid;
  }

  /**
   * Set the uuid for the original task bundle that triggered this resource request.
   * @param requestUuid the uuid as a string.
   */
  public void setRequestUuid(final String requestUuid) {
    this.requestUuid = requestUuid;
  }

  /**
   * Get the serialized callback to execute code on the client side.
   * @return a <code>byte[]</code> instance.
   */
  public byte[] getCallable() {
    return (byte[]) getData(CALLABLE);
  }

  /**
   * Set the serialized callback to execute code on the client side.
   * @param callable a <code>byte[]</code> instance.
   */
  public void setCallable(final byte[] callable) {
    setData(CALLABLE, callable);
  }

  /**
   * Get the ID of an eventual remote-computed callable.
   * @return the id as a long.
   */
  public long getCallableID() {
    final Long id = (Long) getData(CALLABLE_ID);
    return id == null ? NO_CALLABLE_ID : id;
  }

  /**
   * Get the metadata corresponding to the specified key.
   * @param key the string identifying the metadata.
   * @return an object value or null if the metadata could not be found.
   */
  public Object getData(final ResourceIdentifier key) {
    synchronized (dataMap) {
      return dataMap.get(key);
    }
  }

  /**
   * Get the metadata corresponding to the specified key.
   * @param key the string identifying the metadata.
   * @param def a default value to return if the key is not found.
   * @return an object value or the specified default if the metadata could not be found.
   */
  public Object getData(final ResourceIdentifier key, final Object def) {
    synchronized (dataMap) {
      final Object o = dataMap.get(key);
      return o == null ? def : o;
    }
  }

  /**
   * Get the metadata corresponding to the specified key.
   * @param key the string identifying the metadata.
   * @param value the value of the metadata.
   */
  public void setData(final ResourceIdentifier key, final Object value) {
    synchronized (dataMap) {
      dataMap.put(key, value);
    }
  }

  /**
   * Get the array of requests held by this request. For simple request returns arrays containing <code>this</code>.
   * @return a array of {@link JPPFResourceWrapper} instances.
   */
  public JPPFResourceWrapper[] getResources() {
    if (resources == null) resources = new JPPFResourceWrapper[] { this };
    return resources;
  }

  /**
   * Get the monitor used for synchronized access to data.
   * @return a <code>Object</code> instance.
   */
  protected Object getMonitor() {
    return dataMap;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("dynamic=").append(dynamic);
    sb.append(", name=").append(getName());
    sb.append(", state=").append(state);
    sb.append(", uuidPath=").append(uuidPath);
    sb.append(", callableID=").append(getCallableID());
    sb.append(']');
    return sb.toString();
  }

  /**
   * Ensure there is a callableId if needed.
   */
  void preProcess() {
    if ((getCallable() != null) && (getCallableID() == NO_CALLABLE_ID)) setData(CALLABLE_ID, CALLABLE_ID_SEQUENCE.incrementAndGet());
  }

  @Override
  public boolean equals(final Object obj) {
    if ((obj == null) || (obj.getClass() != this.getClass())) return false;
    final JPPFResourceWrapper other = (JPPFResourceWrapper) obj;
    if (dynamic != other.dynamic) return false;
    if (getCallableID() != other.getCallableID()) return false;
    if (uuidPath == null) {
      if (other.uuidPath != null) return false;
    } else {
      if (!uuidPath.equals(other.uuidPath)) return false;
    }
    final String name = getName();
    final String otherName = other.getName();
    if (name == null) {
      if (otherName != null) return false;
    }
    return name.equals(otherName);
  }

  @Override
  public int hashCode() {
    final long id = getCallableID();
    final String name = getName();
    return 31 + (dynamic ? 1 : 0) + (uuidPath == null ? 0 : uuidPath.hashCode()) + (int) id + (name == null ? 0 : name.hashCode());
  }

  /**
   * Determine whether this resource has any of the specified data.
   * @param ids the ids of the data to check.
   * @return <code>true</code> if this resource has any of the specified data, <code>false</code> otherwise.
   */
  public boolean hasAny(final ResourceIdentifier... ids) {
    if (ids != null) {
      synchronized (dataMap) {
        for (ResourceIdentifier id : ids) {
          if (dataMap.get(id) != null) return true;
        }
      }
    }
    return false;
  }

  /**
   * Determine whether this is a request for a single resource definition.
   * @return <code>true</code> if this is a request for a single resource definition, <code>false</code> otherwise.
   */
  public boolean isSingleResource() {
    return !hasAny(MULTIPLE, MULTIPLE_NAMES, CALLABLE);
  }

  /**
   * @param uuid the uuid of the driver for which to get a resource id.
   * @return the unique identifier this resource request in the server.
   */
  public long getResourceId(final String uuid) {
    if (resourceIds == null) return -1L;
    return resourceIds.get(uuid);
  }

  /**
   * Set the unique identifier for this resource request in the specified driver.
   * @param uuid the uuid of the driver for which to set a resource id.
   * @param resourceId the identifier as a {@code long}.
   */
  public void setResourceId(final String uuid, final long resourceId) {
    if (resourceIds == null) resourceIds = new HashMap<>();
    resourceIds.put(uuid, resourceId);
  }

  /**
   * @return the map of driver uuids to resource id.
   */
  public Map<String, Long> getResourceIds() {
    return resourceIds;
  }

  /**
   * @param resourceIds the map of driver uuids to resource id.
   */
  public void setResourceIds(final Map<String, Long> resourceIds) {
    this.resourceIds = resourceIds;
  }

  /**
   * @return the time at which the request is received by the server.
   */
  public long getRequestStartTime() {
    return requestStartTime;
  }

  /**
   * @param requestStartTime the time at which the request is received by the server.
   */
  public void setRequestStartTime(final long requestStartTime) {
    this.requestStartTime = requestStartTime;
  }

  /**
   * @return whether this is a handshake request.
   */
  public boolean isHandshaking() {
    return handshaking;
  }

  /**
   * @param handshaking whether this is a handshake request.
   */
  public void setHandshaking(final boolean handshaking) {
    this.handshaking = handshaking;
  }
}
