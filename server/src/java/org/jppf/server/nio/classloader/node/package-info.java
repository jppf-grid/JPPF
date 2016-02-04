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

/**
 * Support for NIO-based communication with the nodes, for the class loader channel.
 * The possible states for these channels, and the possible transitions from these states are as follows:
 * <ul>
 * <li>WAITING_INITIAL_NODE_REQUEST<br/>
 * --> WAITING_INITIAL_NODE_REQUEST<br/>
 * --> SENDING_INITIAL_NODE_RESPONSE<br/>
 * <br/></li>
 * <li>SENDING_INITIAL_NODE_RESPONSE<br/>
 * --> SENDING_INITIAL_NODE_RESPONSE<br/>
 * --> WAITING_NODE_REQUEST<br/>
 * <br/></li>
 * <li>WAITING_NODE_REQUEST<br/>
 * --> WAITING_NODE_REQUEST<br/>
 * --> IDLE_NODE<br/>
 * --> SENDING_NODE_RESPONSE<br/>
 * <br/></li>
 * <li>IDLE_NODE<br/>
 * --> NODE_WAITING_PROVIDER_RESPONSE (by client channel)<br/>
 * <br/></li>
 * <li>NODE_WAITING_PROVIDER_RESPONSE<br/>
 * --> IDLE_NODE<br/>
 * --> SENDING_NODE_RESPONSE<br/>
 * <br/></li>
 * <li>SENDING_NODE_RESPONSE<br/>
 * --> SENDING_NODE_RESPONSE<br/>
 * --> WAITING_NODE_REQUEST
 * </li>
 * </ul>
 * @exclude
 */
package org.jppf.server.nio.classloader.node;
