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

package test.job.broadcast;

import java.util.*;

import org.jppf.node.protocol.AbstractTask;

/**
 * 
 * @author Laurent Cohen
 */
public class BroadcastTask extends AbstractTask<Object> {
  @Override
  public void run() {
    // the execution result sent as a notification
    Map<String, Object> result = new HashMap<>();
    try {
      // do whatever the task is supposed to do here
      // ...

      // it could be a more complex data structure instead, such as a Map
      result.put("success", true);
    } catch (Exception e) {
      result.put("success", false);
      // add the exception so the notification listener knows what happened
      result.put("exception", e);
    }
    // first param is a user object, here our result Map
    fireNotification(result, true);    
  }
}
