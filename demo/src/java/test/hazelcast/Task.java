package test.hazelcast;

import java.util.Map;

import org.jppf.server.protocol.JPPFTask;

import com.hazelcast.core.Hazelcast;

public class Task extends JPPFTask
{
  private static final long serialVersionUID = -6322352369831759279L;
  /**
   * task index
   */
  private int id;

  public Task(final int id)
  {
    this.id = id;
  }

  @Override
  public void run()
  {
    System.out.println("starting task " + id);
    Map<Object, Object> map = Hazelcast.getMap("myobjects");
    System.out.println("got distributed map for task " + id);
    for (Object o: map.values()) System.out.println("task " + id + ": object = " + o);
    System.out.println("task #" + id + " complete");
  }
}
