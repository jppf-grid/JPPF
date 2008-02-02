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
package org.jppf.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Instances of this class represent the state of a socket channel connection.
 * @author Domingos Creado
 */
public interface ChannelState
{
	/**
	 * Perform the action associated with this state.
	 * @param key the selector key this state is associated with.
	 * @throws IOException if an error occurred while executing the action.
	 */
	void exec(SelectionKey key) throws IOException;
	/**
	 * Perform the action associated with this state.
	 * @param key the selector key this state is associated with.
	 * @param context the context associated with this state.
	 * @throws IOException if an error occurred while executing the action.
	 */
	//void exec(SelectionKey key, ChannelContext context) throws IOException;
}
