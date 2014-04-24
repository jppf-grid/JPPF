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

package org.jppf.jca.cci;

import javax.resource.cci.Record;

/**
 * Implementation of the {@link javax.resource.cci.Record Record} interface for
 * the JPPF resource adapter.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFRecord implements Record
{
  /**
   * Get this record's name.
   * @return null.
   * @see javax.resource.cci.Record#getRecordName()
   */
  @Override
  public String getRecordName()
  {
    return null;
  }

  /**
   * Set this record's name.
   * @param name the name of the record.
   * @see javax.resource.cci.Record#setRecordName(java.lang.String)
   */
  @Override
  public void setRecordName(final String name)
  {
  }

  /**
   * Get a short description of this record.
   * @return null.
   * @see javax.resource.cci.Record#getRecordShortDescription()
   */
  @Override
  public String getRecordShortDescription()
  {
    return null;
  }

  /**
   * Set a short description of this record.
   * @param desc this record's short description.
   * @see javax.resource.cci.Record#setRecordShortDescription(java.lang.String)
   */
  @Override
  public void setRecordShortDescription(final String desc)
  {
  }

  /**
   * Not supported.
   * @return nothing.
   * @throws CloneNotSupportedException is always thrown.
   * @see java.lang.Object#clone()
   */
  @Override
  public Object clone() throws CloneNotSupportedException
  {
    throw new CloneNotSupportedException();
  }
}
