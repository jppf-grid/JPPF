package test.nathalie;

import java.util.*;

import org.jppf.client.*;
import org.jppf.node.policy.*;
import org.jppf.server.protocol.JPPFTask;

public class Beginning
{
  JPPFClient client = new JPPFClient();

  public Beginning()
  {
  }

  public void begin() throws Exception
  {
    JPPFJob job  = new JPPFJob();
    for(int i = 0; i < 10; i++)
    {
      job.addTask(new Island(i));
    }
    // we execute these tasks on node with id = 1;
    ExecutionPolicy policy = new Equal("id", 1);
    job.getSLA().setExecutionPolicy(policy);
    List<JPPFTask> results = client.submit(job);
    List<String> auxs = new Vector<String>();
    int count = 0;
    for(JPPFTask task: results)
    {
      auxs.add((String) task.getResult());
      System.out.println("result #" + (count++) + " = " + task.getResult());
    }
    client.close();
  }
}
