package org.jppf.ui.monitoring.job;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 1/24/12
 * Time: 6:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class JobAccumulatorBranch<T, K, V> extends JobAccumulator<T>
{

  private final Map<K, V> map = new HashMap<K, V>();

  public JobAccumulatorBranch(final Type type, final T value)
  {
    super(type, value);
  }

  public Map<K, V> getMap()
  {
    return map;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) return true;
    if (!(o instanceof JobAccumulatorBranch)) return false;
    if (!super.equals(o)) return false;

    JobAccumulatorBranch that = (JobAccumulatorBranch) o;

    return map.equals(that.map);
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + map.hashCode();
    return result;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("JobAccumulatorBranch");
    sb.append("{type=").append(getType());
    sb.append(", value=").append(getValue());
    sb.append(", map=").append(getMap());
    sb.append('}');
    return sb.toString();
  }
}
