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

package org.jppf.example.wordcount;

import org.jppf.utils.*;

/**
 * Enapsulation of the parameters included in the JPPF configuration file.
 * @author Laurent Cohen
 */
public class Parameters {
  /**
   * Path to the file containing the Wikipedia articles.
   */
  public String dataFile;
  /**
   * Number of articles per task.
   */
  public int nbArticles;
  /**
   * Number of tasks per job.
   */
  public int nbTasks;
  /**
   * Size of the JPPF client connection pool.
   */
  public int nbChannels;
  /**
   * Maximum number of jobs executed concurrently.
   */
  public int jobCapacity;

  /**
   * Initialize the application parameters.
   */
  public Parameters() {
    TypedProperties config = JPPFConfiguration.getProperties();
    dataFile = config.getString("wordcount.file", "data/wikipedia_en_small.xml");
    nbArticles = config.getInt("wordcount.articles.per.task", 100);
    nbTasks = config.getInt("wordcount.tasks.per.job", 100);
    nbChannels = config.getInt("wordcount.channels", 1);
    if (nbChannels < 1) nbChannels = 1;
    jobCapacity = config.getInt("wordcount.job.capacity", 1);
  }
}
