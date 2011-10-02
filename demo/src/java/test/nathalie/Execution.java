
package test.nathalie;

import java.util.*;

import org.jppf.client.*;
import org.jppf.node.policy.*;
import org.jppf.server.protocol.JPPFTask;

public class Execution
{
	public void execute(int c)
	{
		try
		{
			System.out.println("in execute(" + c + ')');
			JPPFClient client = new JPPFClient();
			JPPFJob jobIsland  = new JPPFJob();
	    //jobIsland.setBlocking(false);
	    jobIsland.addTask(new Final(c));
	    // we execute these tasks on node with id = 2,
	    // so as not to cause a deadlock with execution on node 1
	    ExecutionPolicy policy = new Equal("id", 2);
	    jobIsland.getJobSLA().setExecutionPolicy(policy);
	    List<JPPFTask> results = client.submit(jobIsland);
	    List<String> auxs = new Vector<String>();
	    for(JPPFTask task: results) auxs.add((String) task.getResult());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
