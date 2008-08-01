/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.client;

//MatrixSVDTester.java:
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

import org.jppf.client.JPPFClient;
import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;

import Jama.*;

/*  This example does singular value decomposition of N random MxM matrices in parallel, on pelican.
 *    The code that submits the tasks to pelican does not block.
 */

public class MatrixSVDTester {

  // parameters of experiment.
  static private int N = 50;
  static private int M = 300;
  final static private int[] N_VALUES = { 5, 20, 35 };
  final static private int[] M_VALUES = { 100, 300, 500 };
  
  // initialise a connection to pelican.
  static JPPFClient client = null;
  private static PrintWriter log = null;

  // subclass for specifying SVD tasks:
  private static class MatrixSVD extends JPPFTask
  {
    private Matrix M;

    MatrixSVD(Matrix M_)
    {
      setMatrix(M_);
    }
    public void setMatrix(Matrix M_)
    {
      M = M_.copy();
    }
    public Matrix getMatrix()
    {
      return M;
    }
    public void run()
    {
    	//System.out.println("starting SVD");
      setResult(M.svd());
    	//System.out.println("SVD end");
    }
  }

  // this subclass' methods will be called when tasks complete.  we'll use this to unlock the main program after pelican finishes all the tasks.
  private static class BlockingListener implements TaskResultListener
  {
    // the number of results left.
    private int pending_results;

    // the time listening started.
    private Date start_time;

    // list of results returned from the server
    private JPPFTask[] results;

    public JPPFTask[] getResults()
    {
      return results;
    }

    // constructor
    BlockingListener(int N)
    {
      pending_results = N;
      start_time = new Date();
      results = new JPPFTask[N];
      //lock = new Object();
    }
   
    // this method is inherited from the TaskResultListener interface.  it is called when results are returned from pelican:
    public void resultsReceived(TaskResultEvent event)
    {
      int start_index = event.getStartIndex();
      List<JPPFTask> tasks = event.getTaskList();

      System.out.print("Received results: ");
      for(int i=0; i<tasks.size(); i++)
      {
        System.out.print(""+(i+start_index)+",");
        results[i+start_index] = tasks.get(i);
        pending_results--;
      }
      System.out.println(" (took "+(((new Date()).getTime() - start_time.getTime())/1000.0) + " seconds)");
      if(pending_results<=0)
      {
        // gain ownership of this monitor, and notify the main thread.
        synchronized(this)
        {
          notify();
        }
      }
    }
  }

  // these will be the results of applying SVD to the above N matrices
  private static SingularValueDecomposition[] SVDsCluster;
  private static SingularValueDecomposition[] SVDsLocal;
  private static Matrix[] matrices = null;

  // compare two arrays of doubles
  private static boolean compareDoubleArrays(double[] A, double []B, double precision)
  {
    boolean result=true;
    if(A.length!=B.length)
    {
      result=false;
    }
    for(int i=0;i<A.length&&result;i++)
    {
      if((A[i]-B[i])>precision||(B[i]-A[i])>precision)
      {
        result=false;
      }
    }
    return result;
  }

  public static void main(String[] args)
  {
  	try
  	{
			client = new JPPFClient();
    	log = new PrintWriter(new BufferedWriter(new FileWriter("performance.csv")));
    	//perform("Sequential");
    	perform("Parallel");
  	}
  	catch(Exception e)
  	{
  		e.printStackTrace();
  	}
    finally
    {
    	if (client != null) client.close();
    	if (log != null) log.close();
    }
  }

  public static void perform(String name) throws Exception
  {
  	Method method = MatrixSVDTester.class.getMethod("perform" + name, (Class[]) null);
  	log.println(name + " test");
  	for (int i=0; i<N_VALUES.length; i++)
  	{
  		N = N_VALUES[i];
    	log.println("N = " + N);
    	log.flush();
	  	for (int j=0; j<M_VALUES.length; j++)
	  	{
	  		M = M_VALUES[j];
	      matrices = new Matrix[N];
	      for(int k=0; k<N; k++) matrices[k] = Matrix.random(M,M);
	      method.invoke(null, (Object[]) null);
    	}
  	}
  }

  public static void performSequential()
  {
    long start_time;
    long elapsed;
    try
    {
      // solve the matrices on the laptop:
      SVDsLocal = new SingularValueDecomposition[N];

      start_time = System.currentTimeMillis();
      System.out.println("Performing SVD of "+N+" "+M+"x"+M+" matrices on the laptop ... ");
      for(int i=0; i<N; i++)
      {
        System.out.print("Solving matrix "+i+" ... ");
        System.out.flush();
        SVDsLocal[i] = matrices[i].svd();
        System.out.println("DONE! (took "+((System.currentTimeMillis() - start_time)/1000.0)+" seconds)");
      }
      elapsed = System.currentTimeMillis() - start_time;
      System.out.println("DONE! (total time was " + (elapsed/1000.0) + " seconds)");
    	log.println("" + N + ", " + elapsed);
    	log.flush();

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public static void performParallel()
  {
    long start_time;
    long elapsed;

    try
    {
      // solve the matrices on pelican:

      // objects added to this list will be executed in parallel on pelican.  (the object must extend JPPFTask)
      List<JPPFTask> SVDTasks = new ArrayList<JPPFTask>();

      // this list will contain the results of the tasks.
      List<JPPFTask> results;
      System.out.println("Performing SVD of "+N+" "+M+"x"+M+" matrices on pelican ... ");
      System.out.flush();

      // add the N SVD tasks as new tasks to the list of things to be executed in parallel on pelican.
      for(int i=0;i<N;i++) SVDTasks.add(new MatrixSVD(matrices[i]));
      // this object will collect the results, and unblock the main thread.
      TaskResultListener listener = new BlockingListener(N);

      start_time = System.currentTimeMillis();

      // this method submits the tasks to pelican without blocking.
      client.submitNonBlocking(
        SVDTasks, //list of tasks.
        null, //data shared by the tasks.
        listener //this will let us check on the status of the tasks.
      );
      // this will block until all the tasks are completed.
      synchronized(listener)
      {
        listener.wait();
      }
      // cast the results to SingularValueDecomposition class
      JPPFTask[] ClusterResults = ((BlockingListener)listener).getResults();

      /*
      JPPFTask[] ClusterResults = client.submit(SVDTasks, null).toArray(new JPPFTask[0]);
      */
      SVDsCluster = new SingularValueDecomposition[N];
      for(int i=0;i<N;i++)
      {
        SVDsCluster[i] = (SingularValueDecomposition)ClusterResults[i].getResult();
      }
      elapsed = System.currentTimeMillis() - start_time;
      System.out.println("DONE! (total time was " + (elapsed/1000.0) + " seconds)");
    	log.println("" + N + ", " + elapsed);
    	log.flush();

      /*
    	System.out.print("Comparing results ... ");
      System.out.flush();
      boolean same=true;
      for(int i=0;i<SVDTasks.size()&&same;i++)
      {
        if(
          !compareDoubleArrays(
            SVDsLocal[i].getSingularValues(),
            SVDsCluster[i].getSingularValues(),0.000005))
        {
          same=false;
        }
      }
      if(same)
      {
        System.out.println("RESULTS WERE THE SAME!");
      }
      else
      {
        System.out.println("RESULTS WERE _NOT_ THE SAME!");
      }
      */
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
