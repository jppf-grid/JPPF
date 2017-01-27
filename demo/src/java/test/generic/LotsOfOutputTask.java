/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package test.generic;

import org.jppf.node.protocol.AbstractTask;

/**
 * This task generates a lot of output, to test if this use case is handled correctly by the NodeLauncher.
 * See bug report <a href="http://sourceforge.net/tracker/?func=detail&aid=2713542&group_id=135654&atid=733518">2713542 - OOM when node/driver generates too much console output</a>
 * @author Laurent Cohen
 */
public class LotsOfOutputTask extends AbstractTask<String>
{
  /**
   * Number of output lines to print.
   */
  private int nbLines = 0;
  /**
   * Length in chars of each output line.
   */
  private int lineLength = 0;

  /**
   * Initialize this task with the specified number of lines and line length.
   * @param nbLines - the number of output lines to print.
   * @param lineLength - the length in chars of each output line.
   */
  public LotsOfOutputTask(final int nbLines, final int lineLength)
  {
    this.nbLines = nbLines;
    this.lineLength = lineLength;
  }

  /**
   * Output <i>nbLines</i> of length <i>lineLength</i> to the console.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<lineLength; i++) sb.append('X');
    String s = sb.toString();
    for (int i=0; i<nbLines; i++) System.out.println(s);
  }
}
