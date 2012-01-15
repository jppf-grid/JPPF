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

package org.jppf.example.extendedclassloading;

import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;

/**
 * A collection of helper methods to manage a repository of Java libraries
 * that are added dynamically to the node's classpath, and downloaded from
 * the client when necessary.
 * @author Laurent Cohen
 */
public class LibraryManager
{
  /**
   * Name of the job metadata that holds the libraries to update.
   */
  public static final String LIBS_TO_UPDATE = "libs.to.update";
  /**
   * Name of the job metadata that holds the libraries to delete.
   */
  public static final String LIBS_TO_DELETE = "libs.to.delete";
  /**
   * This <code>Boolean</code> flag determines whether the node should be restarted
   * after the updates (and before processing a job).
   */
  public static final String RESTART_NODE_FLAG = "restart.node";
  /**
   * The name of the algorithm used to generate the file signatures, such as MD5 or SHA-256.
   */
  public static final String SIGNATURE_ALGORITHM = "MD5";
  /**
   * The managed libraries index. It maps file names to their signatures.
   */
  private Map<String, String> index = null;
  /**
   * Location where the libraries are stored on the local file system.
   */
  private final String libDir;
  /**
   * Location listing the libraries to add or update on the local file system.
   */
  private final String indexFile;
  /**
   * Location of the file listing libraries to delete on the local file system.
   */
  private final String toDeleteFile;

  /**
   * Create a library manager with the specified root folder.
   * @param libDir the folder where the libraries are stored on the local file system.
   */
  public LibraryManager(final String libDir)
  {
    this.libDir = libDir;
    indexFile = libDir + "/index.txt";
    toDeleteFile = libDir + "/toDelete.txt";
  }

  /**
   * Read the index of downloaded libraries.
   * @return the index as a map of library file name to associated signature.
   */
  public synchronized Map<String, String> getIndex()
  {
    if (index == null) index = loadIndex();
    return index;
  }

  /**
   * Read the index of downloaded libraries.
   * @return the index as a map of library file names to associated signatures.
   */
  private Map<String, String> loadIndex()
  {
    Map<String, String> map = new TreeMap<String, String>();
    try
    {
      checkOrCreateStore();
      List<String> lines = new ArrayList<String>();
      Reader reader = FileUtils.getFileReader(indexFile);
      lines = FileUtils.textFileAsLines(reader);
      for (String line: lines)
      {
        line = line.trim();
        if (!line.isEmpty())
        {
          String[] association = line.split("=");
          map.put(association[0], association[1]);
        }
      }
    }
    catch(IOException e)
    {
      e.printStackTrace();
    }
    return map;
  }

  /**
   * Store the index to a file.
   */
  public synchronized void storeIndex()
  {
    try
    {
      checkOrCreateStore();
      Writer writer = null;
      try
      {
        writer = FileUtils.getFileWriter(indexFile);
        for (Map.Entry<String, String> entry: index.entrySet())
        {
          writer.write(entry.getKey() + '=' + entry.getValue() + '\n');
        }
      }
      finally
      {
        if (writer != null) writer.close();
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Check if the library store exists, and create it if it doesn't.
   * @throws IOException if the store could not be created.
   */
  private void checkOrCreateStore() throws IOException
  {
    File idxFile = new File(indexFile);
    if (!idxFile.exists())
    {
      File dir = idxFile.getParentFile();
      // if the folder does not exist, attemp to create it
      if (!dir.exists())
      {
        if (!dir.mkdirs()) throw new IOException("could not create the library store at '" + dir.getCanonicalPath() + "'");
      }
      // create an empty index file
      FileWriter writer = null;
      try
      {
        writer = new FileWriter(indexFile);
      }
      finally
      {
        if (writer != null) writer.close();
      }
    }
  }

  /**
   * Compute a signature for the specified file.
   * @param file the file path.
   * @return a hexadecimal representation of the signature.
   */
  public String computeSignature(final String file)
  {
    try
    {
      return computeSignature(FileUtils.getFileInputStream(file));
    }
    catch (Exception e)
    {
      return null;
    }
  }

  /**
   * Compute a signature for the data pointed to by the specified url.
   * @param url the url path where the data is.
   * @return a hexadecimal representation of the signature.
   */
  public String computeSignature(final URL url)
  {
    try
    {
      return computeSignature(url.openStream());
    }
    catch (Exception e)
    {
      return null;
    }
  }

  /**
   * Compute a signature for the data in the specified stream.
   * @param is the stream from which to compute the signature.
   * @return a hexadecimal representation of the signature.
   */
  public String computeSignature(final InputStream is)
  {
    try
    {
      // compute the signature
      MessageDigest digest = MessageDigest.getInstance(SIGNATURE_ALGORITHM);
      byte[] buffer = new byte[2048];
      int numBytes;
      while ((numBytes = is.read(buffer)) != -1) digest.update(buffer, 0, numBytes);
      byte[] sig = digest.digest();
      // convert the signature to a hexadecimal string
      return StringUtils.toHexString(sig);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      StreamUtils.closeSilent(is);
    }
    return null;
  }

  /**
   * Compare the current index with a map of libraries associated to their signature.
   * This method attempts to match each entry in the index with a corresponding entry
   * in the updates, and compares their respective signatures.
   * @param updates a map of library names ot their MD5 signature.
   * @return a list of library names whose signatures are found to differ.
   */
  public synchronized List<String> computeUpdatesList(final Map<String, String> updates)
  {
    Map<String, String> index = getIndex();
    List<String> results = new ArrayList<String>();
    for (Map.Entry<String, String> entry: updates.entrySet())
    {
      String oldSig = index.get(entry.getKey());
      // if the lib is not yet in the index or the signature has changed
      if ((oldSig == null) || !oldSig.equals(entry.getValue())) results.add(entry.getKey());
    }
    return results;
  }

  /**
   * Download a list of libraries using the specified class loader,
   * add them to the node's classpath when relevant, and update the index accordingly.
   * @param updates the names of the libraries to download and update.
   * @param cl the class loader used to download the libraries.
   */
  public synchronized void processUpdates(final List<String> updates, final AbstractJPPFClassLoader cl)
  {
    try
    {
      // compute the path for each lib on the client side
      // we assume the folder where the libs are located is in the client's classpath
      // this means the libs should be in the client's classpath root
      String[] resourceNames = updates.toArray(new String[updates.size()]);
      // download the libraries all at once, in a single network transaction
      // they will be saved to the temporary resource cache
      URL[] urls = cl.getMultipleResources(resourceNames);
      // determine the class loader to which to add the new libraries
      AbstractJPPFClassLoader parentCL = (cl.getParent() instanceof AbstractJPPFClassLoader) ? (AbstractJPPFClassLoader) cl.getParent() : cl;
      Map<String, String> index = getIndex();
      for (int i=0; i<updates.size(); i++)
      {
        if (urls[i] != null)
        {
          String name = updates.get(i);
          String sig = computeSignature(urls[i]);
          // save the lib to a permanenent location
          URL permanentURL = saveLibToFile(name, sig, urls[i]);
          // add a new library to the classpath of the server class loader
          // there is no point in adding a new version of an existing library
          // it will only be taken into account at the next node restart
          if (!index.containsKey(name)) parentCL.addURL(permanentURL);
          // update the index entry with the corresponding signature
          index.put(name, sig);
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Save a resource obtained from the resource cache of the node.
   * @param fileName the name of the resource.
   * @param signature the library file MD5 signature.
   * @param url URl provided by the JPPF class loader which points to a location in the temporary cache.
   * @return a URL pointing to the location where the resource is saved.
   * @throws Exception if any error occurs.
   */
  private URL saveLibToFile(final String fileName, final String signature, final URL url) throws Exception
  {
    // save the file to the local directory
    File file = new File(getLibFileName(fileName, signature));
    OutputStream os = FileUtils.getFileOutputStream(file);
    InputStream is = url.openStream();
    // copy the input stream into the output stream
    StreamUtils.copyStream(is, os);
    return file.toURI().toURL();
  }

  /**
   * Compute the actual file name for a library, based on the original name and its signature.
   * The resulting file name shoud be in the form <code>folder/<i>name</i>-<i>signature</i>.jar</code>
   * @param libName the original name of the library.
   * @param signature the signature of the library.
   * @return a normalized fie path.
   */
  public String getLibFileName(final String libName, final String signature)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(libDir).append('/');
    int n = libName.lastIndexOf('.');
    String ext = libName.substring(n);
    String s = libName.substring(0, n);
    sb.append(s).append('-').append(signature).append(ext);
    return sb.toString();
  }

  /**
   * Get the location of the file listing libraries to delete on the local file system.
   * @return the file location as a string.
   */
  public String getToDeleteFile()
  {
    return toDeleteFile;
  }

  /**
   * Location where the libraries are stored on the local file system.
   * @return the location as a string. 
   */
  public String getLibDir()
  {
    return libDir;
  }
}
