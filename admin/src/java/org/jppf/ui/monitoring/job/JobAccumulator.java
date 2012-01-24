package org.jppf.ui.monitoring.job;

/**
 * Created by IntelliJ IDEA.
 * User: jandam
 * Date: 1/23/12
 * Time: 9:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class JobAccumulator<T> {

  public static enum Type {
    ADD,
    KEEP,
    UPDATE,
    REMOVE
  }

  private T value;
  private Type    type;

  public JobAccumulator(final Type type, final T value)
  {
    if(type == null) throw new IllegalArgumentException("changeType is null");

    this.type = type;
    this.value = value;
  }

  public Type getType()
  {
    return type;
  }

  public T getValue()
  {
    return value;
  }

  public boolean mergeChange(final Type type) {
    return mergeChange(type, value);
  }

  public boolean mergeChange(final Type type, final T value) {
    if(this.type == type && this.type != Type.UPDATE) throw new IllegalStateException("Can't merge type: " + type);
    if(this.type.compareTo(type) > 0) throw new IllegalStateException("Can't merge type from " + this.type + " to " + type);

    this.value = value;
    if(this.type == Type.ADD && (type == Type.KEEP || type == Type.UPDATE)) return false;

    Type oldValue = this.type;
    this.type = type;
    return oldValue == Type.ADD && this.type == Type.REMOVE;
  }

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

  @Override
  public int hashCode()
  {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + type.hashCode();
    return result;
  }

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
