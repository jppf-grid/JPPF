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

package test.org.jppf.test.runner;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractTestResultRenderer implements TestResultRenderer
{
  /**
   * The report header.
   */
  protected StringBuilder header = new StringBuilder();
  /**
   * The report footer.
   */
  protected StringBuilder footer = new StringBuilder();
  /**
   * The report body.
   */
  protected StringBuilder body = new StringBuilder();
  /**
   * The indent.
   */
  protected String indent = "  ";
  /**
   * The results to render.
   */
  protected final ResultHolder result;
  /**
   * the level of indentation.
   */
  protected int indentLevel = 0;
  /**
   * The indent.
   */
  protected String currentIndentation = "";

  /**
   * Initialize this renderer witht he specified results.
   * @param result the results to render.
   */
  protected AbstractTestResultRenderer(final ResultHolder result)
  {
    this.result = result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getHeader()
  {
    return header.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getFooter()
  {
    return footer.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getBody()
  {
    return body.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getIndent()
  {
    return indent;
  }

  /**
   * Increment the indentation level
   */
  protected void incIndentation()
  {
    indentLevel++;
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<indentLevel; i++) sb.append(indent);
    currentIndentation = sb.toString();
  }
  
  /**
   * Increment the indentation level
   */
  protected void decIndentation()
  {
    indentLevel--;
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<indentLevel; i++) sb.append(indent);
    currentIndentation = sb.toString();
  }
  
  /**
   * Get the current indentation.
   * @return the indentation as a string.
   */
  protected String getIndentation()
  {
    return currentIndentation;
  }
}
