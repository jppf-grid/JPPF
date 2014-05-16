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

package org.jppf.example.extendedclassloading;

import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.util.regex.Pattern;

import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;

/**
 * A collection of helper methods to manage a repository of Java libraries
 * that are added dynamically to the node's classpath and downloaded from
 * the client when necessary.
 * @author Laurent Cohen
 */
public class ClassPathHelper {
  /**
   * Name of the job metadata property that holds the {@link ClassPath} definition for a job.
   */
  public static final String JOB_CLASSPATH = "job.class.path";
  /**
   * Name of the job metadata property that holds the {@link RepositoryFilter} for the libraries ot delete.
   */
  public static final String REPOSITORY_DELETE_FILTER = "repository.delete.filter";
  /**
   * The name of the algorithm used to generate the file signatures, such as MD5 or SHA-256.
   */
  public static final String SIGNATURE_ALGORITHM = "MD5";

  /**
   * Compute a signature for the specified file.
   * @param filename the file path.
   * @return a hexadecimal representation of the signature.
   */
  public static String computeSignature(final String filename) {
    try {
      return computeSignature(FileUtils.getFileInputStream(filename));
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Compute a signature for the specified file.
   * @param file the file path.
   * @return a hexadecimal representation of the signature.
   */
  public static String computeSignature(final File file) {
    try {
      return computeSignature(new BufferedInputStream( new FileInputStream(file)));
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Compute a signature for the file pointed to by the specified url.
   * @param url the url path to where the file is.
   * @return a hexadecimal representation of the signature.
   */
  public static String computeSignature(final URL url) {
    try {
      return computeSignature(url.openStream());
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Compute a signature for the data in the specified stream.
   * @param is the stream from which to compute the signature.
   * @return a hexadecimal representation of the signature.
   */
  public static String computeSignature(final InputStream is) {
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
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      StreamUtils.closeSilent(is);
    }
    return null;
  }

  /**
   * Compute the actual file name for a library, based on the original name and its signature.
   * The resulting file name shoud be in the form <code>folder/<i>name</i>-<i>signature</i>.jar</code>
   * @param rootDir the root folder for the file location.
   * @param libName the original name of the library.
   * @param signature the file's signature.
   * @return a normalized fiie path.
   */
  public static File getLibFilePath(final String rootDir, final String libName, final String signature) {
    StringBuilder sb = new StringBuilder();
    sb.append(rootDir).append('/');
    int n = libName.lastIndexOf('.');
    String ext = libName.substring(n);
    String s = libName.substring(0, n);
    sb.append(s).append('-').append(signature).append(ext);
    return new File(sb.toString());
  }

  /**
   * Create a new ClassPath by scanning the specefied folder for jar files.
   * @param rootFolder the folder to scan.
   * @return a {@link ClassPath} object loaded with the files found in the folder.
   */
  public static ClassPath createClassPathFromRootFolder(final String rootFolder) {
    ClassPathImpl cp = new ClassPathImpl(rootFolder);
    try {
      return cp.loadFromFileSystem();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return cp;
  }

  /**
   * Create a new ClassPath from a specified array of jar file paths.
   * @param files the paths of the files to add to the classpath.
   * @return a {@link ClassPath} object containig the files paths and their associated signatures.
   */
  public static ClassPath createClassPath(final String...files) {
    ClassPathImpl cp = null;
    if ((files != null) && (files.length > 0)) {
      cp = new ClassPathImpl();
      for (String filename: files) {
        String signature = computeSignature(filename);
        cp.addElement(filename, signature);
      }
    }
    return cp;
  }

  /**
   * Compute a {@link ClassPath} from a file pattern expression within a list of arguments.<br>
   * Example argument: <code>-c "*1.jar|*2.jar"</code> (note the quotes enclosing the pattern,
   * to avoid the OS shell interpreting the string).<br>
   * This pattern will match all the jar files ending with 1 or 2.
   * @param rootDir the directory in which the files are located.
   * @param args a list of arguments provided on the command line.
   * @return a new ClassPath object, or null if no matching file could be found or no classpath argument was specified.
   */
  public static ClassPath createClassPathFromArguments(final String rootDir, final String[] args) {
    if ((args == null) || (args.length <= 0)) return null;
    ClassPath cp = null;
    for (int i=0; i<args.length; i++) {
      if ("-c".equalsIgnoreCase(args[i])) {
        // convert the file pattern to a java regex
        String regex = wildcardToRegex(args[i + 1]);
        final Pattern pattern = Pattern.compile(regex);

        // get the files in the root dir that match the pattern
        File dir = new File(rootDir);
        File[] files = dir.listFiles(new FileFilter() {
          @Override
          public boolean accept(final File pathname) {
            return pattern.matcher(pathname.getName()).matches();
          }
        });

        // build the classpath from the matching files
        if ((files != null) && (files.length > 0)) {
          cp = new ClassPathImpl();
          for (File file: files) {
            String name = file.getName();
            String signature = computeSignature(file);
            if (signature != null) cp.addElement(name, signature);
          }
        }
        break;
      }
    }
    return cp;
  }

  /**
   * Compute a {@link RepositoryFilter} from a file pattern expression within a list of arguments.<br>
   * Example argument: <code>-d "*1.jar|*2.jar"</code> (note the quotes enclosing the pattern,
   * to avoid the OS shell interpreting the string).<br>
   * This pattern will match all the jar files ending with 1 or 2.
   * @param args a list of arguments provided on the command line.
   * @return a new ClassPath object, or null if no matching file could be found.
   */
  public static RepositoryFilter getFilterFromArguments(final String[] args) {
    if ((args == null) || (args.length <= 0)) return null;
    RepositoryFilter filter = null;
    for (int i=0; i<args.length; i++) {
      if ("-d".equalsIgnoreCase(args[i])) {
        String regex = args[i + 1];
        filter = new RepositoryFilter.RegExFilter(regex);
        break;
      }
    }
    return filter;
  }

  /**
   * Convert a 'wildcard' pattern pattern into a java regex pattern.
   * @param patternString the wildcard pattern to convert.
   * @return a <code>java.util.regex.Pattern</code> instance.
   */
  public static String wildcardToRegex(final String patternString) {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<patternString.length(); i++) {
      char c = patternString.charAt(i);
      switch(c) {
        case '*':
        case '?':
          sb.append(".").append(c);
          break;
        case '.':
          sb.append("\\.");
          break;
        default:
          sb.append(Character.toLowerCase(c));
          break;
      }
    }
    return sb.toString();
  }
}
