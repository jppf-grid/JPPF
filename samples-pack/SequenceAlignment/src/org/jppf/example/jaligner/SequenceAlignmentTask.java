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

package org.jppf.example.jaligner;

import jaligner.*;
import jaligner.matrix.Matrix;
import jaligner.util.SequenceParser;

import org.jppf.server.protocol.JPPFTask;

/**
 * This task performs the alignment of 2 DNA or protein sequences.
 * @author Laurent Cohen
 */
public class SequenceAlignmentTask extends JPPFTask
{
  /**
   * Data provider key for the target sequence.
   */
  public static final String TARGET_SEQUENCE = "targetSequence";
  /**
   * Data provider key for the scoring matrix to use..
   */
  public static final String SCORING_MATRIX = "scoringMatrix";
  /**
   * The sequence to align with the target sequence.
   */
  private String sequence = null;

  /**
   * Initialize this task with the specified sequence to align with the target sequence.
   * @param sequence the sequence as a string.
   * @param number uniquely identifies this task.
   */
  public SequenceAlignmentTask(final String sequence, final int number)
  {
    this.sequence = sequence;
    setId("" + number);
  }

  /**
   * Perform the sequence alignment.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    try
    {
      Sequence seq = SequenceParser.parse(sequence);
      Sequence target = getDataProvider().getParameter(TARGET_SEQUENCE);
      Matrix matrix = getDataProvider().getParameter(SCORING_MATRIX);
      Alignment a = SmithWatermanGotoh.align(seq, target, matrix, 10.0f, 0.5f);
      setResult(a.calculateScore());
    }
    catch(Exception e)
    {
      setThrowable(e);
    }
  }

  /**
   * Get the sequence number of this task.
   * @return the sequence number as an int.
   */
  public int getNumber()
  {
    return Integer.valueOf(getId());
  }

  /**
   * Get the sequence to align with the target sequence.
   * @return  the sequence as a string.
   */
  public String getSequence()
  {
    return sequence;
  }
}
