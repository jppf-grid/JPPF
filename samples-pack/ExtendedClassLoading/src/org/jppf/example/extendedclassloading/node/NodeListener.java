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

package org.jppf.example.extendedclassloading.node;

import java.io.*;
import java.net.*;
import java.util.*;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.example.extendedclassloading.LibraryManager;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.node.event.*;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * A {@link NodeLifeCycleListener} implementation that performs the dynamic management of Java libraries,
 * based on the metadata provided by the jobs.
 * @author Laurent Cohen
 */
public class NodeListener implements NodeLifeCycleListener
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeListener.class);
  /**
   * Location where the downloaded libraries are stored on the node's file system.
   */
  public static final String NODE_LIB_DIR = "downloadedLibs";
  /**
   * A list of old library files to delete whenever possible.
   */
  private List<String> filesToDelete = new ArrayList<String>();
  /**
   * The library manager.
   */
  private LibraryManager libraryManager = new LibraryManager(NODE_LIB_DIR);
  /**
   * Determines whether the node should be restarted when the current job ends.
   */
  private boolean restartNode = false;
  /**
   * Wraps a local (same JVM) connection to the node's JMX server.
   */
  private JMXNodeConnectionWrapper jmx = null;

  /**
   * Perform library management upon connection to the server:
   * <ul>
   * <li> delete the libraries listed in the "toDelete" file, and update the file accordingly</li>
   * <li> add the libraries listed in the index file to the class path of the current class loader (server class laoder)</li>
   * </ul>
   * {@inheritDoc}
   */
  @Override
  public void nodeStarting(final NodeLifeCycleEvent event)
  {
    // do the cleanup of old libraries
    loadFilesToDelete();
    int n1 = filesToDelete.size();
    deleteFilesToDelete();
    if (filesToDelete.size() != n1) saveFilesToDelete();
    // add all libraies in the index to the classpath
    Map<String, String> index = libraryManager.getIndex();
    int size = index.size();
    if (size == 0) output("found no library in the store");
    else output("found " + size  + " librar" + (size > 1 ? "ies" : "y") + " in the store:");
    AbstractJPPFClassLoader cl = (AbstractJPPFClassLoader) getClass().getClassLoader();
    for (Map.Entry<String, String> entry: index.entrySet())
    {
      File file = new File(libraryManager.getLibFileName(entry.getKey(), entry.getValue()));
      try
      {
        URL url = file.toURI().toURL();
        cl.addURL(url);
        output("  added " + entry.getKey() + " to the classpath");
      }
      catch (MalformedURLException e)
      {
        output(ExceptionUtils.getMessage(e));
      }
    }
  }

  /**
   * Upon disconnection from the node, save the "toDelete" file.
   * {@inheritDoc}
   */
  @Override
  public void nodeEnding(final NodeLifeCycleEvent event)
  {
    saveFilesToDelete();
  }

  /**
   * Before the execution of a job, check its metadata and update, download or delete the managed libraries accordingly.
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public void jobStarting(final NodeLifeCycleEvent event)
  {
    JPPFDistributedJob job = event.getJob();
    output("processing metadata for job '" + job.getName() + "'");
    // if the client class loader cannot be obtained, no point in pursuing further
    List<Task> tasks = event.getTasks();
    if ((tasks == null) || tasks.isEmpty())
    {
      output("  could not get the client class loader, aborting the update");
      return;
    }
    JobMetadata metadata = job.getMetadata();

    // process the new and updated libraries
    int nbUpdates = 0;
    Map<String, String> updates = (Map<String, String>) metadata.getParameter(LibraryManager.LIBS_TO_UPDATE);
    if ((updates == null) || updates.isEmpty()) output("  no library updates found");
    else nbUpdates = doUpdates(updates, tasks);

    // now process the libraries to remove
    int nbDeletes = 0;
    List<String> deletes = (List<String>) metadata.getParameter(LibraryManager.LIBS_TO_DELETE);
    if ((deletes == null) || deletes.isEmpty())  output("  no library to remove");
    else nbDeletes = doDeletes(deletes);
 
    if (nbUpdates + nbDeletes > 0)
    {
      libraryManager.storeIndex();
      saveFilesToDelete();
      Boolean restart  = (Boolean) metadata.getParameter(LibraryManager.RESTART_NODE_FLAG, Boolean.FALSE);
      // restart the node to ensure the changes are taken into account
      if (restart) restartNode(job);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void jobEnding(final NodeLifeCycleEvent event)
  {
    if (restartNode)
    {
      // restart the node
      output("*** restarting this node ***");
      try
      {
        getJmx().restart();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  /**
   * Perform the specified library updates.
   * @param updates a mapping of libraries to update or add to the classpath to their corresponding MD5 signature.
   * @param tasks the tasks from which to obtain the client class loader.
   * @return the number of libraries that were actually updated or added.
   */
  private int doUpdates(final Map<String, String> updates, final List<Task> tasks)
  {
    // filter out libraries with matching signature: they don't need to be updated
    List<String> libsToUpdate = libraryManager.computeUpdatesList(updates);
    // process new and updated libraries
    if (libsToUpdate.isEmpty()) output("  no library updates found");
    else
    {
      // print the list of libs to update and whether they are new or simply updated
      output("  found " + libsToUpdate.size() + " libraries to update:");
      Map<String, String> index = libraryManager.getIndex();
      for (String name: libsToUpdate)
      {
        String oldSignature = index.get(name);
        String newSignature = updates.get(name);
        // this is a new library
        if (oldSignature == null)
        {
          output("  - NEW   : " + name + ", signature = " + newSignature);
        }
        // this is an existing library update
        else
        {
          output("  - UPDATE: " + name + ", old signature = " + oldSignature + ", new signature = " + newSignature);
          // we want to remove the old version of the library
          filesToDelete.add(libraryManager.getLibFileName(name, oldSignature));
        }
      }
      // get a reference to the client class loader, which is used to load the tasks classes
      AbstractJPPFClassLoader clientCL = (AbstractJPPFClassLoader) tasks.get(0).getTaskObject().getClass().getClassLoader();
      libraryManager.processUpdates(libsToUpdate, clientCL);
    }
    return libsToUpdate.size();
  }

  /**
   * Delete the specified libraries from the index and from the file system.
   * The libraries will be deleted the next time the node restarts.
   * @param deletes the list of libraries to delete.
   * @return the number of libraries that were actually deleted.
   */
  private int doDeletes(final List<String> deletes)
  {
    List<String> actual = new ArrayList<String>();
    Map<String, String> index = libraryManager.getIndex();
    for (String name: deletes)
    {
      if (index.containsKey(name)) actual.add(name);
    }
    int nbDeletes = actual.size();
    if (nbDeletes > 0)
    {
      output("  found " + nbDeletes + " libraries to delete:");
      for (String name: actual)
      {
        output ("  - " + name);
        // remove the library from the index
        String signature = index.remove(name);
        if (signature != null)
        {
          String fileName = libraryManager.getLibFileName(name, signature);
          File file = new File(fileName);
          // remove the lib from the file system
          // as the JVM is potentially holding a lock on the file, the delete will actually occur when the node is restarted
          if (file.exists()) filesToDelete.add(fileName);
        }
      }
    }
    return nbDeletes;
  }
 
  /**
   * Load the list of old libraries to delete from the file system.
   */
  private void loadFilesToDelete()
  {
    try
    {
      Reader reader = new BufferedReader(new FileReader(libraryManager.getToDeleteFile()));
      // transform the text file into a listof  strings and close the reader.
      filesToDelete = FileUtils.textFileAsLines(reader);
    }
    catch (IOException ignore)
    {
    }
  }
 
  /**
   * Save the list of old libraries to delete from the file system.
   */
  private void saveFilesToDelete()
  {
    BufferedWriter writer = null;
    try
    {
      writer = new BufferedWriter(new FileWriter(libraryManager.getToDeleteFile()));
      for (String s: filesToDelete) writer.write(s + '\n');
    }
    catch (Exception ignore)
    {
    }
    finally
    {
      StreamUtils.closeSilent(writer);
    }
  }
 
  /**
   * Delete the old libraries if possible.
   */
  private void deleteFilesToDelete()
  {
    Iterator<String> it = filesToDelete.iterator();
    while(it.hasNext())
    {
      File file = new File(it.next());
      // remove the file from the list if it doesn't exist or if deletion is successful
      if (!file.exists() || file.delete()) it.remove();
    }
  }
 
  /**
   * Restart the node and cancel the specified job.
   * @param job the job to cancel.
   */
  private void restartNode(final JPPFDistributedJob job)
  {
    try
    {
      // cancel the job and ensure it is requeued on the server
      output("canceling the job");
      getJmx().cancelJob(job.getUuid(), true);
      job.getSLA().setSuspended(false);
      // set the restart node flag for processing in the jobEnded() event
      this.restartNode = true;
    }
    catch (Exception e)
    {
      log.error("error while attempting to restart the node", e);
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

  /**
   * Get a connection ot the node's JMX server, creating it if needed.
   * @return a <code>JMXNodeConnectionWrapper</code> instance.
   */
  private JMXNodeConnectionWrapper getJmx()
  {
    if (jmx == null)
    {
      jmx = new JMXNodeConnectionWrapper();
      jmx.connect();
    }
    return jmx;
  }
}
