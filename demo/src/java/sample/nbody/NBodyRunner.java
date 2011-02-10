/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
package sample.nbody;

import java.awt.Dimension;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Runner class for the &quot;N-Body demo&quot; demo.
 * @author Laurent Cohen
 */
public class NBodyRunner
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(NBodyRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * The panel displaying the animation.
	 */
	private static NBodyPanel panel;
	/**
	 * Random number generator.
	 */
	private static Random rand = new Random(System.nanoTime());
	/**
	 * The radius for the simulation.
	 */
	private static double radius = 0d;

	/**
	 * Entry point for this class, submits the tasks with a set duration to the server.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			perform();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			jppfClient.close();
		}
	}
	
	/**
	 * Perform the multiplication of 2 matrices with the specified size, for a specified number of times.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void perform() throws Exception
	{
		TypedProperties config = JPPFConfiguration.getProperties();
		double qp = config.getDouble("nbody.qp");
		double b = config.getDouble("nbody.b");
		double dt = config.getDouble("nbody.dt");
		radius = config.getDouble("nbody.radius");
		int nbBodies = config.getInt("nbody.n");
		int bodiesPerTask = config.getInt("nbody.bodies.per.task", 1);
		int iterations = config.getInt("nbody.time.steps");

		int nbTasks = nbBodies / bodiesPerTask + (nbBodies % bodiesPerTask == 0 ? 0 : 1);
		print("Running N-Body demo with "+nbBodies+" bodies (protons), dt = "+dt+", for "+iterations+" time steps");
		// perform "iteration" times
		long totalTime = 0L;
		Vector2d[] positions = new Vector2d[nbBodies];
		for (int i=0; i<nbBodies; i++)
		{
			positions[i] = new Vector2d(rand.nextDouble()*radius/2d+radius/4d, rand.nextDouble()*radius/2d+radius/4d);
		}
		createUI();
		int count = 0;
		DataProvider dp = new MemoryMapDataProvider();
		dp.setValue("qp_qp", Double.valueOf(qp*qp));
		dp.setValue("qp_b", Double.valueOf(qp*b));
		dp.setValue("dt", Double.valueOf(dt));
		List<JPPFTask> tasks = new ArrayList<JPPFTask>(nbTasks);
		for (int i=0; i<nbTasks; i++)
		{
			NBody[] bodies = new NBody[count + bodiesPerTask < nbBodies ? bodiesPerTask : nbBodies - count];
			for (int j=0; j<bodies.length; j++)
			{
				bodies[j] = new NBody(count, positions[count]);
				count++;
			}
			tasks.add(new NBodyTask(bodies));
		}
		for (int iter=0; iter<iterations; iter++)
		{
			JPPFJob job = new JPPFJob(dp);
			for (JPPFTask task: tasks) job.addTask(task);
			panel.updatePositions(positions);
			dp.setValue("positions", positions);
			long start = System.currentTimeMillis();
			// submit the tasks for execution
			List<JPPFTask> results = jppfClient.submit(job);
			for (JPPFTask task: results)
			{
				Exception e = task.getException();
				if (e != null) throw e;
			}
			tasks = results;
			positions = new Vector2d[nbBodies]; 
			for (int i=0; i<nbTasks; i++)
			{
				NBody[] bodies = ((NBodyTask) tasks.get(i)).getBodies();
				for (NBody body: bodies) positions[body.number] = body.pos;
			}
			long elapsed = System.currentTimeMillis() - start;
			totalTime += elapsed;
			//print("Iteration #"+(iter+1)+" performed in "+StringUtils.toStringDuration(elapsed));
			if (iter % 100 == 0) System.out.println(""+iter);
		}
		print("Total time:  " + StringUtils.toStringDuration(totalTime) + " (" + (totalTime/1000) + " seconds)" +
			", Average iteration time: " + StringUtils.toStringDuration(totalTime/iterations));
	}

	/**
	 * Create the UI.
	 * @throws Exception if any error occurs.
	 */
	private static void createUI() throws Exception
	{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		JFrame frame = new JFrame("N-Body demo");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		panel = new NBodyPanel();
		Dimension dim = new Dimension((int) radius, (int) radius);
		panel.setMinimumSize(dim);
		panel.setMaximumSize(dim);
		panel.setPreferredSize(dim);
		frame.getContentPane().add(panel);
		frame.setSize((int) radius+50, (int) radius+50);
		frame.setVisible(true);
	}

	/**
	 * Print a message tot he log and to the console.
	 * @param msg the message to print.
	 */
	private static void print(String msg)
	{
		log.info(msg);
		System.out.println(msg);
	}
}
