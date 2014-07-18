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

import java.io.*;
import java.nio.charset.Charset;

import org.jppf.utils.streams.StreamUtils;

/**
 * This class is used to optimize the performance of reading sequences in a sequence database.
 * It generates a file that contains the length of each sequence to read, in the same order
 * as the sequences in the database.
 * @author Laurent Cohen
 */
public class DatabaseHandler
{
  /**
   * Data Input Stream from which to read the length of the sequences.
   */
  private DataInputStream dis = null;
  /**
   * Reader from which to read the sequences.
   */
  private Reader reader = null;
  /**
   * Initial read buffer size.
   */
  private int buffSize = 8192;
  /**
   * Sequence read buffer.
   */
  private char[] buffer = new char[buffSize];
  /**
   * name of the charset to use when reading the sequence database.
   */
  private String charset = null;
  /**
   * Determines whether EOF was reached on the sequence database.
   */
  private boolean eof = false;

  /**
   * Generate an index file for the specified database.
   * @param path the path to the sequence database.
   * @param indexPath the path to the index file to generate.
   * @param charset name of the charset to use when reading the sequence database.
   * If null, the default charset will be used.
   * @throws Exception if an IO error occurs.
   */
  public DatabaseHandler(final String path, final String indexPath, final String charset) throws Exception
  {
    this.charset = (charset == null) ? Charset.defaultCharset().name() : charset;
    dis = new DataInputStream(new BufferedInputStream(new FileInputStream(indexPath)));
    reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), this.charset));
  }

  /**
   * Read the next sequence from the sequences database.
   * @return the sequence as a string or null if end of file was reached.
   * @throws Exception if an IO error occurs.
   */
  public String nextSequence() throws Exception
  {
    if (eof) return null;
    int length = 0;
    try
    {
      length = dis.readInt();
    }
    catch(EOFException e)
    {
      eof = true;
      StreamUtils.closeSilent(dis);
      StreamUtils.closeSilent(reader);
      return null;
    }
    if (length > buffSize)
    {
      buffSize = length;
      buffer = new char[buffSize];
    }
    int count = 0;
    while (count < length)
    {
      int n = reader.read(buffer, 0, length);
      if (n < 0) break;
      count += n;
    }
    return new String(buffer, 0, length);
  }

  /**
   * Generate an index file for the specified database.
   * @param path the path to the sequence database.
   * @param indexPath the path to the index file to generate.
   * @param charset name of the charset to use when reading the sequence database.
   * If null, the default charset will be used.
   * @return the number of sequences in the database.
   * @throws Exception if an IO error occurs.
   */
  public static int generateIndex(final String path, final String indexPath, final String charset) throws Exception
  {
    String cs = charset;
    if (cs == null) cs = Charset.defaultCharset().name();
    Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), cs));
    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexPath)));
    int nbSequences = 0;
    try
    {
      boolean end = false;
      int c = reader.read();
      while (!end)
      {
        int length = 0;
        if (c == '>')
        {
          length++;
          boolean lineEnd = false;
          while (!lineEnd)
          {
            c = reader.read();
            if (c == -1)
            {
              lineEnd = true;
              end = true;
            }
            else
            {
              length++;
              if (c == '\n') lineEnd = true;
            }
          }
          boolean sequenceEnd = false;
          while (!sequenceEnd)
          {
            c = reader.read();
            if ((c == '>') || (c == -1))
            {
              sequenceEnd = true;
              dos.writeInt(length);
              nbSequences++;
              //if (nbSequences % 100 == 0) System.out.println("indexed "+nbSequences+" sequences");
              if (c == -1)
              {
                end = true;
              }
            }
            else length++;
          }
        }
      }
      dos.flush();
    }
    finally
    {
      dos.close();
      reader.close();
    }
    return nbSequences;
  }
}
