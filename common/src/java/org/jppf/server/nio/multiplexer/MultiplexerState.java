/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.server.nio.multiplexer;

/**
 * Enumeration of the possible states for a class server channel.
 * @author Laurent Cohen
 */
public enum MultiplexerState
{
	/**
	 * IDLE state, when a channel has nothing to do.
	 */
	IDLE,
	/**
	 * State of sending or receiving data.
	 */
	SENDING_OR_RECEIVING,
	/**
	 * State of sending data.
	 */
	SENDING,
	/**
	 * State of receiving data.
	 */
	RECEIVING,
	/**
	 * State of identifying an inbound connection from a remote multiplexer.
	 */
	IDENTIFYING_INBOUND_CHANNEL,
	/**
	 * State of sending outbound port information to a remote multiplexer.
	 */
	SENDING_MULTIPLEXING_INFO
}
