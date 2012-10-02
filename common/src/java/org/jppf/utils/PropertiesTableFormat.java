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

package org.jppf.utils;

import java.util.*;

/**
 * Abstract class for formatters that print the contents of a {@link java.util.Properties Properties} object as a string.
 * @author Laurent Cohen
 */
public abstract class PropertiesTableFormat
{
  /**
   * Contains the formatted text.
   */
  protected StringBuffer sb = new StringBuffer();
  /**
   * The title of the whole document.
   */
  protected String docTitle = null;

  /**
   * Initialize this formatter with the specified title.
   * @param docTitle the title of the whole document.
   */
  public PropertiesTableFormat(final String docTitle)
  {
    this.docTitle = docTitle;
  }

  /**
   * Get the formatted text.
   * @return the text as a string.
   */
  public String getText()
  {
    return sb.toString();
  }

  /**
   * Write the prologue for the formatted text.
   */
  public void start()
  {
  }

  /**
   * Write the epilogue for the formatted text.
   */
  public void end()
  {
  }

  /**
   * Generate the formatted text for a set of properties.
   * @param props the set of properties to format.
   * @param title the title of the formatted table.
   */
  public void formatTable(final Properties props, final String title)
  {
    Set<String> orderedProps = new TreeSet<String>();
    Enumeration en = props.propertyNames();
    while (en.hasMoreElements()) orderedProps.add((String) en.nextElement());
    tableStart(title);
    for (String name: orderedProps)
    {
      rowStart();
      cellStart();
      sb.append(name);
      cellEnd();
      cellSeparator();
      cellStart();
      sb.append(props.getProperty(name));
      cellEnd();
      rowEnd();
    }
    tableEnd();
  }

  /**
   * Write the prologue of a table.
   * @param title the title for the table.
   */
  public void tableStart(final String title)
  {
  }

  /**
   * Write the prologue of a table.
   */
  public void tableEnd()
  {
  }

  /**
   * Write the prologue of a table row.
   */
  public void rowStart()
  {
  }

  /**
   * Write the prologue of a table row.
   */
  public void rowEnd()
  {
  }

  /**
   * Write the prologue of a table cell.
   */
  public void cellStart()
  {
  }

  /**
   * Write the prologue of a table cell.
   */
  public void cellEnd()
  {
  }

  /**
   * Write the separator between 2 cells.
   */
  public void cellSeparator()
  {
  }

  /**
   * Print the specified string.
   * @param message the string to print.
   */
  public void print(final String message)
  {
    sb.append(message);
  }
}
