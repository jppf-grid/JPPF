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
package org.jppf.ui.monitoring.job;

/**
 * Instances of this class represent changes made to the tree table.
 * @param <T> the type of the values that are changed.
 * @author Martin Janda
 */
public class JobAccumulator<T>
{

  /**
   * The types of changes.
   */
  public static enum Type
  {
    /**
     * A value was added.
     */
    ADD,
    /**
     * A value was kept.
     */
    KEEP,
    /**
     * A value was updated.
     */
    UPDATE,
    /**
     * A value was removed.
     */
    REMOVE
  }

  /**
   * The value to change.
   */
  private T value;
  /**
   * The type of change performed.
   */
  private Type type;

  /**
   * Initialize this job accumulator with the specified value and type of change.
   * @param type  the type of change performed.
   * @param value the initial value to change.
   */
  public JobAccumulator(final Type type, final T value)
  {
    if (type == null) throw new IllegalArgumentException("changeType is null");

    this.type = type;
    this.value = value;
  }

  /**
   * Get the type of change performed.
   * @return and instance of {@link Type}.
   */
  public Type getType()
  {
    return type;
  }

  /**
   * Get the value to change.
   * @return an instance of the valyes type.
   */
  public T getValue()
  {
    return value;
  }

  /**
   * Merge a change of a different type for the same value.
   * @param type the type of change to merge.
   * @return <code>true</code> if the previous change type is <code>ADD</code> and the new one is <code>REMOVE</code>, <code>false</code> otherwise.
   */
  public boolean mergeChange(final Type type)
  {
    return mergeChange(type, value);
  }

  /**
   * Merge a change of a different type for a new value.
   * @param type  the type of change to merge.
   * @param value the new value to merge.
   * @return <code>true</code> if the previous change type is <code>ADD</code> and the new one is <code>REMOVE</code>, <code>false</code> otherwise.
   */
  public boolean mergeChange(final Type type, final T value)
  {
    Type oldType = this.type;

    if (this.type == type && this.type != Type.UPDATE) throw new IllegalStateException("Can't merge type: " + type);

    if (this.type == Type.REMOVE && type == Type.ADD)
    {
      this.type = Type.UPDATE;
      this.value = value;

      return false;
    }
    else
    {
      if (this.type.compareTo(type) > 0)
      {
        throw new IllegalStateException("Can't merge type from " + this.type + " to " + type);
      }

      this.value = value;
      if (this.type == Type.ADD && (type == Type.KEEP || type == Type.UPDATE)) return false;

      this.type = type;
      return oldType == Type.ADD && this.type == Type.REMOVE;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(final Object o)
  {
    if (this == o) return true;
    if (!(o instanceof JobAccumulator)) return false;

    JobAccumulator that = (JobAccumulator) o;

    if (type != that.type) return false;
    if (value != null ? !value.equals(that.value) : that.value != null) return false;

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + type.hashCode();
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("JobAccumulator");
    sb.append("{type=").append(type);
    sb.append(", value=").append(value);
    sb.append('}');
    return sb.toString();
  }
}
