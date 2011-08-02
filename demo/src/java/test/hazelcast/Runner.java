package test.hazelcast;

import java.util.Map;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;

import com.hazelcast.core.Hazelcast;

public class Runner {

    /**
     * JPPF client used to submit execution requests.
     */
    private static JPPFClient jppfClient = null;
    
    /**
     * distributed map containing array
     */
    private static Map<Integer, Double> sharedMap;

    public static void main(String[] args) {
        try {
            jppfClient = new JPPFClient();
            int nTask = 2;

            // create and initialize array
            double[] data = new double[nTask];
            for (int i = 0; i < data.length; i++) {
                data[i] = i;
            } 
            
            // distribute array
            sharedMap = Hazelcast.getMap("sharedArray");
            for(int i = 0; i < nTask; i++) {
                    sharedMap.put(i, data[i]);
            }
            
            // perform tasks
            JPPFJob job = new JPPFJob();
            for (int i = 0; i < nTask; i++) {
                job.addTask(new Task(i));
            }            
            
            // submit and wait for all job completions
            jppfClient.submit(job);
            
            // print out new values
            System.out.println("print results");
            for (int i = 0; i < nTask; i++) {
                System.out.println("after task " + sharedMap.get(i));
            }   
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        finally {
            jppfClient.close();
        }
        System.exit(0);
    }
}

