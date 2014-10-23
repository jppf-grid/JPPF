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

package test.filecopy;

import java.io.*;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.*;

/**
 *
 * @author Laurent Cohen
 */
public class FileCopyTask extends AbstractTask<String> {

  @Override
  public void run() {
    try {
      String dest = "node.txt";
      TimeMarker marker = new TimeMarker().start();
      Pair<Long, Integer> result = copyFile("client.txt", dest, 1024*1024);
      setResult(String.format("successfully written %,d bytes to '%s' in %s, %,d chunks", result.first(), dest, marker.stop().getLastElapsedAsString(), result.second()));
    } catch(Exception e) {
      e.printStackTrace();
      setThrowable(e);
    }
  }

  /**
   * Copy a file from the client ot the node.
   * @param clientLocation the location to copy the file from in the client file system.
   * @param nodeLocation the location to copy the file to in the node file system.
   * @param maxChunkSize the maximum size of each chunk read fom the file.
   * @return the number of bytes read.
   * @throws Exception if any error occurs.
   */
  private Pair<Long, Integer> copyFile(final String clientLocation, final String nodeLocation, final int maxChunkSize) throws Exception {
    boolean done = false;
    long size = 0L;
    int count = 0;
    FileCopyAction action = new FileCopyAction(clientLocation, maxChunkSize);
    try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(nodeLocation))) {
      while (!done) {
        byte[] bytes = null;
        try {
          CopyResult result = compute(action);
          count++;
          if (result.isDone()) done = true;
          if (result.getBytes() != null) { 
            bytes = result.getBytes();
            size += bytes.length;
            os.write(bytes);
            FileCopyAction tmp = new FileCopyAction(action);
            tmp.setPos(size);
            action = tmp;
          }
        } catch (Exception e) {
          done = true;
          throw e;
        }
      }
    }
    return new Pair<>(size, count);
  }
}
