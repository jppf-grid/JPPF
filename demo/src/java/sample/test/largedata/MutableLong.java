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

package sample.test.largedata;

import java.io.Serializable;

/**
 * Instances of this class hold a single long value and provide methods to update it.
 * @author Laurent Cohen
 */
public class MutableLong implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The value of this mutable long.
   */
  private long value;

  /**
   * Initialize this object with a value of 0.
   */
  public MutableLong()
  {
    this.value = 0L;
  }

  /**
   * Initialize this object with the specified value.
   * @param value the value to set.
   */
  public MutableLong(final long value)
  {
    this.value = value;
  }

  /**
   * Get the value of this object.
   * @return a long value.
   */
  public long value()
  {
    return value;
  }

  /**
   * Set the value of this object and return the new value.
   * @param value the value to set.
   * @return the new value.
   */
  public long value(final long value)
  {
    return this.value = value;
  }

  /**
   * Increment the value of this object by 1 and return the new value.
   * @return a long value.
   */
  public long increment()
  {
    return ++value;
  }

  /**
   * Decrement the value of this object by 1 and return the new value.
   * @return a long value.
   */
  public long decrement()
  {
    return --value;
  }
}
