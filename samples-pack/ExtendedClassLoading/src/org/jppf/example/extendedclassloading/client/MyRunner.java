/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.example.extendedclassloading.client;

import java.io.*;
import java.util.*;

import org.jppf.client.*;
import org.jppf.example.extendedclassloading.LibraryManager;
import org.jppf.node.protocol.JobMetadata;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * <p>This client application maintains a repository of Java libraries that are automatically
 * downloaded by the nodes. Each node also maintains its own local repository.
 * The updates are computed by scanning the folder where the libs are stored, and comparing
 * the scan results with the repository's index file to determine which libraries were added,
 * updated or removed. This information is then communicated to the node via the metadata in
 * a JPPF job.
 * <p>This enables the management of the nodes remote repositories by simply removing files from,
 * or dropping files into the folder where the libraries are located.
 * @author Laurent Cohen
 */
public class MyRunner
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(MyRunner.class);
  /**
   * The JPPF client.
   */
  private static JPPFClient client = null;
  /**
   * Location where the downloaded libraries are stored on the client's file system.
   */
  public static final String CLIENT_LIB_DIR = "dynamicLibs";
  /**
   * The library manager.
   */
  private static final LibraryManager libraryManager = new LibraryManager(CLIENT_LIB_DIR);
  /**
   * Represents the actual current content of the libs repository.
   */
  private static Map<String, String> currentIndex = null;

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String[] args)
  {
    try
    {
      // force the loading of the "index.txt" file
      libraryManager.getIndex();
      // this index represents the actual current content of the libs repository
      currentIndex = loadCurrentIndex();

      client = new JPPFClient();
      JPPFJob job = new JPPFJob();
      job.setName("Extended Class Loading");

      // compute the job metadata that specifiy which libraries are to be added, updated or removed
      computeJobMetadata(job);

      // add the tasks and submit the job
      job.addTask(new MyTask());
      List<JPPFTask> results = client.submit(job);

      // process the results
      for (JPPFTask task: results)
      {
        if (task.getException() != null)
        {
          System.out.println("Got exception: " + ExceptionUtils.getStackTrace(task.getException()));
        }
        else
        {
          System.out.println("Result: " + task.getResult());
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      updateRepositoryIndex();
      if (client != null) client.close();
    }
  }

  /**
   * Set the specified job metadata so the library management
   * can take place properly in the nodes.
   * @param job the job on which to set the metatdata.
   */
  public static void computeJobMetadata(final JPPFJob job)
  {
    JobMetadata metadata = job.getMetadata();
    if (currentIndex == null) currentIndex = loadCurrentIndex();
    Map<String, String> updates = computeNewAndUpdated(currentIndex);
    List<String> deletes = listDeleted(currentIndex);
    displayRepositoryChanges(updates, deletes);
    metadata.setParameter(LibraryManager.LIBS_TO_UPDATE, updates);
    metadata.setParameter(LibraryManager.LIBS_TO_DELETE, deletes);
    String s = System.getProperty("restart.node");
    if (s == null) s = JPPFConfiguration.getProperties().getString("restart.node");
    boolean restartNode = (s == null) ? false : Boolean.valueOf(s);
    metadata.setParameter(LibraryManager.RESTART_NODE_FLAG, restartNode);
    output("restart node flag set to " + restartNode);
  }

  /**
   * Fetch the list of libraries currently present and compute their signature.
   * This method performs a scan of the folder where the libraries are stored.
   * @return a map of jar file names associated with their signature.
   */
  private static synchronized Map<String, String> loadCurrentIndex()
  {
    Map<String, String> map = new HashMap<String, String>();
    File dir = new File(libraryManager.getLibDir() + '/');
    File[] jars = dir.listFiles(new FileFilter()
    {
      @Override
      public boolean accept(final File file)
      {
        return !file.isDirectory() && file.getName().toLowerCase().endsWith(".jar");
      }
    });
    if ((jars != null) && (jars.length > 0))
    {
      for (File jar: jars)
      {
        String name = jar.getName();
        String signature = libraryManager.computeSignature(jar.getPath());
        map.put(name, signature);
      }
    }
    return map;
  }

  /**
   * Update the repository index file to the most current state.
   */
  private static void updateRepositoryIndex()
  {
    libraryManager.getIndex().clear();
    libraryManager.getIndex().putAll(currentIndex);
    libraryManager.storeIndex();
  }

  /**
   * Compare the current content of the folder to the index and return a list of libraries that were deleted.
   * @param currentIndex the reproesentation of the repository content.
   * @return a list of file names representing the libraries deleted from the repository.
   */
  private static List<String> listDeleted(final Map<String, String> currentIndex)
  {
    List<String> result = new ArrayList<String>();
    Map<String, String> oldIndex = libraryManager.getIndex();
    for (Map.Entry<String, String> entry: oldIndex.entrySet())
    {
      if (!currentIndex.containsKey(entry.getKey())) result.add(entry.getKey());
    }
    return result;
  }

  /**
   * Compare the current content of the folder to the index and returns a map of newx and updated libraries.
   * @param currentIndex the reproesentation of the repository content.
   * @return a map of new and updated library names associated with their signature.
   */
  private static Map<String, String> computeNewAndUpdated(final Map<String, String> currentIndex)
  {
    Map<String, String> map = new HashMap<String, String>();
    List<String> list = libraryManager.computeUpdatesList(currentIndex);
    for (String name: list) map.put(name, currentIndex.get(name));
    return map;
  }

  /**
   * Display a summary of the changes detected in the repository.
   * @param updates the library updates that were detected.
   * @param deletes the deleted libraries.
   */
  private static void displayRepositoryChanges(final Map<String, String> updates, final List<String> deletes)
  {
    Map<String, String> index = libraryManager.getIndex();
    if ((updates == null) || updates.isEmpty()) output("there are no new or updated libraries");
    else
    {
      int size = updates.size();
      output("found " + size + " new or updated " + (size > 1 ? "libraries" : "library"));
      for (Map.Entry<String, String> entry: updates.entrySet())
      {
        String name = entry.getKey();
        String newSig = entry.getValue();
        // new library
        if (index.get(name) == null) output("  - NEW   : " + name + ", signature = " + newSig);
        // update of existing library
        else output("  - UPDATE: " + name + ", new signature = " + newSig + ", old signature = " + index.get(name));
      }
    }
    if ((deletes == null) || deletes.isEmpty()) output("there are no deleted libraries");
    else
    {
      int size = deletes.size();
      output("found " + size + " deleted " + (size > 1 ? "libraries" : "library"));
      for (String name: deletes) output("  - " + name);
    }
  }

  /**
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  public static void output(final String message)
  {
    // comment out this line to remove messages from the console
    System.out.println(message);
    // comment out this line to remove messages from the log file
    log.info(message);
  }
}
