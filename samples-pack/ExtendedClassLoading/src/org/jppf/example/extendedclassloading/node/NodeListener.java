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

package org.jppf.example.extendedclassloading.node;

import java.net.URL;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.example.extendedclassloading.*;
import org.jppf.node.event.*;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.node.protocol.JobMetadata;
import org.slf4j.*;

/**
 * A {@link NodeLifeCycleListener} implementation that performs the dynamic management of Java libraries,
 * based on the metadata provided by the jobs.
 * @author Laurent Cohen
 */
public class NodeListener implements NodeLifeCycleListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeListener.class);
  /**
   * Location where the downloaded libraries are stored on the node's file system.
   */
  public static final String REPOSITORY_DIR = "repository";
  /**
   * Reference to the repository of jar files, of which subsets can be added to a client class loader's classpath.
   */
  private Repository repository = null;

  /**
   * Default no-arg constructor, used by the servide provide interface (SPI)
   * to create and register this node listener.
   */
  public NodeListener() {
  }

  /**
   * Upon connection to the server, delete the libraries listed
   * in the "toDelete" file, and update the file accordingly.
   * {@inheritDoc}
   */
  @Override
  public void nodeStarting(final NodeLifeCycleEvent event) {
    repository = new RepositoryImpl(REPOSITORY_DIR);
    output("node starting: using repository at '" + REPOSITORY_DIR + "'");
  }

  /**
   * Upon disconnection from the server, perform repository cleanup operations.
   * {@inheritDoc}
   */
  @Override
  public void nodeEnding(final NodeLifeCycleEvent event) {
    repository.cleanup();
  }

  /**
   * Before the execution of a job, and before its tasks are loaded, check its 
   * metadata and update or download the managed libraries accordingly.
   * <p>if a delete filter is provided, then the the repository attemps to delete them
   * immediately, in particular before adding new libaries to the repository.
   * {@inheritDoc}
   */
  @Override
  public void jobHeaderLoaded(final NodeLifeCycleEvent event) {
    JPPFDistributedJob job = event.getJob();
    output("*** processing metadata for job '" + job.getName() + "' ***");
    JobMetadata metadata = job.getMetadata();

    // get the optional delete filter and delete files in repository accordingly
    RepositoryFilter filter = (RepositoryFilter) metadata.getParameter(ClassPathHelper.REPOSITORY_DELETE_FILTER);
    if (filter != null) {
      output("attempting to delete the files matching " + filter);
      repository.delete(filter);
    }

    // if the client class loader cannot be obtained, no point in pursuing further
    AbstractJPPFClassLoader cl = event.getTaskClassLoader();
    if (cl == null) {
      output("  could not get the client class loader, aborting the update");
      return;
    }

    // the set of urls held by the current class loader.
    URL[] currentURLs = cl.getURLs();

    // fetch the requested classpath for the job
    ClassPath classpath = (ClassPath) metadata.getParameter(ClassPathHelper.JOB_CLASSPATH);
    if (classpath != null) {
      output("requested libraries: " + classpath);
      // get the jar files already present in the repository
      // and download the missing ones from the client
      URL[] urls = repository.download(classpath, cl);
      if ((urls != null) && (urls.length > 0)) {
        // if a classpath is already set onto the current class loader, then create a new class loader for
        // the same client, which will now be used by the node; this will cause the old class loader to be discarded
        boolean isNewCl = false;
        if ((currentURLs != null) && (currentURLs.length > 0)) {
          cl = event.getNode().resetTaskClassLoader();
          isNewCl = true;
        }
        // add all requested jar files to the class loader's classpath
        for (int i=0; i<urls.length; i++) {
          if (urls[i] != null) cl.addURL(urls[i]);
        }
        // display the class loader with its classpath
        output((isNewCl ? "created new" : "updated current") + " task class loader " + cl);
      }
    }
  }

  /**
   * This implementation does nothing.
   * {@inheritDoc}
   */
  @Override
  public void jobStarting(final NodeLifeCycleEvent event) {
  }

  /**
   * This implementation does nothing.
   * {@inheritDoc}
   */
  @Override
  public void jobEnding(final NodeLifeCycleEvent event) {
  }

  /**
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  public static void output(final String message) {
    // comment out this line to remove messages from the console
    System.out.println(message);
    // comment out this line to remove messages from the log file
    log.info(message);
  }
}
