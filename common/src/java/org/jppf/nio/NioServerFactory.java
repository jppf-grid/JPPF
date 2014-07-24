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

package org.jppf.nio;

import java.nio.channels.SelectionKey;
import java.util.Map;

import org.jppf.utils.collections.CollectionMap;

/**
 * Instances of this class provide a mapping of enumerated values for states and
 * transitions to the actual corresponding objects.
 * @param <S> the type safe enumeration of the states.
 * @param <T> the type safe enumeration of the state transitions.
 * @author Laurent Cohen
 */
public abstract class NioServerFactory<S extends Enum<S>, T extends Enum<T>>
{
  /**
   * A short name for read and write channel operations.
   */
  public static final int RW = SelectionKey.OP_READ|SelectionKey.OP_WRITE;
  /**
   * A short name for read channel operations.
   */
  public static final int R = SelectionKey.OP_READ;
  /**
   * A short name for write channel operations.
   */
  public static final int W = SelectionKey.OP_WRITE;
  /**
   * Map of all states for a class server.
   */
  protected final Map<S, NioState<T>> stateMap;
  /**
   * Map of all states for a class server.
   */
  protected final Map<T, NioTransition<S>> transitionMap;
  /**
   * The server for which this factory is intended.
   */
  protected final NioServer<S, T> server;
  /**
   * A map of the allowed states to which each state can transition.
   */
  protected final CollectionMap<S, S> allowedTransitions;

  /**
   * Initialize this factory with the specified server.
   * @param server the server for which to initialize.
   */
  protected NioServerFactory(final NioServer<S, T> server)
  {
    this.server = server;
    stateMap = createStateMap();
    transitionMap = createTransitionMap();
    allowedTransitions = createAllowedTransitionsMap();
  }

  /**
   * Create the map of all possible states.
   * @return a mapping of the states enumeration to the corresponding NioStateInstances.
   */
  protected abstract Map<S, NioState<T>> createStateMap();

  /**
   * Create the map of all possible states.
   * @return a mapping of the states enumeration to the corresponding NioStateInstances.
   */
  protected abstract Map<T, NioTransition<S>> createTransitionMap();

  /**
   * 
   * @return a CollectionMap for the possible states.
   */
  protected CollectionMap<S, S> createAllowedTransitionsMap()
  {
    return null;
  }

  /**
   * Get a state given its name.
   * @param name the name of the state to lookup.
   * @return an <code>NioState</code> instance.
   */
  public NioState<T> getState(final S name)
  {
    return stateMap.get(name);
  }

  /**
   * Get a transition given its name.
   * @param name the name of the transition to lookup.
   * @return an <code>NioTransition</code> instance.
   */
  public NioTransition<S> getTransition(final T name)
  {
    return transitionMap.get(name);
  }

  /**
   * Get the server for which this factory is intended.
   * @return an <code>NioServer</code> instance.
   */
  public NioServer<S, T> getServer()
  {
    return server;
  }

  /**
   * Create a transition to the specified state for the specified IO operations.
   * @param state resulting state of the transition.
   * @param ops the operations allowed.
   * @return an <code>NioTransition&lt;ClassState&gt;</code> instance.
   */
  protected NioTransition<S> transition(final S state, final int ops)
  {
    return new NioTransition<>(state, ops);
  }

  /**
   * Determine whether the transion from the current state to a new state is allowed.
   * @param currentState the current state.
   * @param newState the new state.
   * @return <code>true</code> if the transition is allowed, <code>false</code> otherwise.
   */
  public boolean isTransitionAllowed(final S currentState, final S newState)
  {
    return (allowedTransitions == null) || allowedTransitions.containsValue(currentState, newState);
  }
}
