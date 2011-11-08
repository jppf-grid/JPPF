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

/**
 * Classes for the task communication sample.<br>
 * <p>This sample illustrates a way for tasks to communicate with each other using the <a href="http://www.hazelcast.com">Hazelcast</a> framework.<br>
 * In this sample a task MyTask1 sends a message to another task MyTask2, using a distributed Map, and waits for a response from MyTask2, sent thorough the same distributed Map.
 */
package sample.dist.taskcommunication;
