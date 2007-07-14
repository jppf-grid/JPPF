/*
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
package org.jppf.client.event;

import java.util.EventListener;

/**
 * Listener interface for receiving notifications of task results received from the server.
 * @author Laurent Cohen
 */
public interface TaskResultListener extends EventListener
{
	/**
	 * Called to notify that that results of number of tasks have been received from the server.
	 * @param event the event that encapsulates the tasks that were received and related information.
	 */
	void resultsReceived(TaskResultEvent event);
}
