/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.management.diagnostics;

import java.io.*;
import java.util.Map;

/**
 * Abstract implementation of a {@link ThreadDumpWriter}.
 * @author Laurent Cohen
 */
public abstract class AbstractThreadDumpWriter implements ThreadDumpWriter
{
  /**
   * The underlying print writer.
   */
  protected final PrintWriter out;
  /**
   * The character sequence used for the indentation, for instance spaces or tabs.
   */
  protected final String indentString;
  /**
   * The current identation level of the output.
   */
  protected int indentLevel = 0;
  /**
   * The current identation to use before writing each line of the output.
   */
  protected StringBuilder indent = new StringBuilder();

  /**
   * Intiialize this printer with the specified writer.
   * @param writer the writer to print to.
   * @param indentString the string to use as indentation.
   */
  public AbstractThreadDumpWriter(final Writer writer, final String indentString)
  {
    if (writer == null) throw new IllegalArgumentException("writer cannot be null");
    this.out = (writer instanceof PrintWriter) ? (PrintWriter) writer : new PrintWriter(writer);
    this.indentString = indentString;
  }

  @Override
  public void printThreadDump(final ThreadDump threadDump)
  {
    printDeadlocks(threadDump);
    for (Map.Entry<Long, ThreadInformation> entry: threadDump.getThreads().entrySet())
    {
      printThread(entry.getValue());
    }
  }

  /**
   * Get a simple representation fort he specified thread.
   * @param ti the thread information to use.
   * @return a string in format <code>thread id thread_id "thread_name"</code>.
   */
  protected String simpleName(final ThreadInformation ti)
  {
    return new StringBuilder().append("thread id ").append(ti.getId()).append(" \"").append(ti.getName()).append('"').toString();
  }

  /**
   * Get a simple representation for the specified lock.
   * @param li the lock information to use.
   * @return a string in format <code>lock_class_name@lock_hashcode</code>.
   */
  protected String simpleName(final LockInformation li)
  {
    return new StringBuilder().append(li.getClassName()).append('@').append(Integer.toHexString(li.getIdentityHashcode())).toString();
  }

  @Override
  public void printString(final String message)
  {
    out.print(message);
  }

  /**
   * Increment the current indentation level.
   */
  protected void incIndent()
  {
    indentLevel++;
    indent = new StringBuilder();
    for (int i=0; i<indentLevel; i++) indent.append(indentString);
  }

  /**
   * Decrement the current indentation level.
   */
  protected void decIndent()
  {
    indentLevel--;
    indent = new StringBuilder();
    for (int i=0; i<indentLevel; i++) indent.append(indentString);
  }

  /**
   * Get the current indentation.
   * @return the indentation as a string.
   */
  protected String getIndent()
  {
    return indent.toString();
  }
}
