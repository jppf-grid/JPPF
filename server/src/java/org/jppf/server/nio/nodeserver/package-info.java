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

/**
 * Classes supporting the server node-side of the JPPF asynchronous communication model.
 * <p><b>Current flow for standard nodes:</b><br>
 * upon connection:<br>
 *   SEND_INITIAL_BUNDLE<br>
 *   --> WAIT_INITIAL_BUNDLE<br>
 *   --> IDLE<br>
 * when a job is disptached:<br>
 *   SENDING_BUNDLE<br> 
 *   --> WAITING_RESULTS<br> 
 *   --> IDLE<br>
 *    
 * <p><b>Current flow for offline nodes:</b><br>
 * upon connection:<br>
 *   SEND_INITIAL_BUNDLE<br>
 *   --> WAIT_INITIAL_BUNDLE<br>
 *   --> IDLE<br>
 * when a job is disptached:<br>
 *   SENDING_BUNDLE<br> 
 *   --> channel is closed<br>
 * <p>Note that offline nodes never transition to WAITING_RESULTS.
 * Getting the results of a previously dispatched job is done in WAIT_INITIAL_BUNDLE. 
 * @exclude
 */
package org.jppf.server.nio.nodeserver;
