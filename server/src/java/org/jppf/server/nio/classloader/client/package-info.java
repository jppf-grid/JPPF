/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
 * Support for NIO-based communication with the clients, for the class loader channel.
 * The allowed state transitions for these channels are:
 * <ul>
 * <li>WAITING_INITIAL_PROVIDER_REQUEST<br/>
 * --> WAITING_INITIAL_PROVIDER_REQUEST<br/>
 * --> SENDING_INITIAL_PROVIDER_RESPONSE<br/>
 * <br/></li>
 * 
 * <li>SENDING_INITIAL_PROVIDER_RESPONSE<br/>
 * --> SENDING_INITIAL_PROVIDER_RESPONSE<br/>
 * --> IDLE_PROVIDER<br/>
 * <br/></li>
 * 
 * <li>SENDING_PROVIDER_REQUEST<br/>
 * --> SENDING_PROVIDER_REQUEST<br/>
 * --> WAITING_PROVIDER_RESPONSE<br/>
 * --> IDLE_PROVIDER<br/>
 * <br/></li>
 * 
 * <li>WAITING_PROVIDER_RESPONSE<br/>
 * --> WAITING_PROVIDER_RESPONSE<br/>
 * --> SENDING_PROVIDER_REQUEST<br/>
 * <br/></li>
 * 
 * <li>IDLE_PROVIDER<br/>
 * --> IDLE_PROVIDER<br/>
 * --> SENDING_PROVIDER_REQUEST (including from node channel)<br/>
 * <br/></li>
 * 
 * <li>SENDING_PEER_CHANNEL_IDENTIFIER<br/>
 * --> SENDING_PEER_CHANNEL_IDENTIFIER<br/>
 * --> SENDING_PEER_INITIATION_REQUEST<br/>
 * <br/></li>
 * 
 * <li>SENDING_PEER_INITIATION_REQUEST<br/>
 * --> SENDING_PEER_INITIATION_REQUEST<br/>
 * --> WAITING_PEER_INITIATION_RESPONSE<br/>
 * <br/></li>
 * 
 * <li>WAITING_PEER_INITIATION_RESPONSE<br/>
 * --> WAITING_PEER_INITIATION_RESPONSE<br/>
 * --> IDLE_PROVIDER<br/>
 * <br/></li>
 * </ul>
 * @exclude
 */
package org.jppf.server.nio.classloader.client;
