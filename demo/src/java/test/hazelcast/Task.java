package test.hazelcast;

import java.util.Map;

import org.jppf.node.NodeRunner;
import org.jppf.server.protocol.JPPFTask;

import com.hazelcast.core.Hazelcast;

public class Task extends JPPFTask {

    private static final long serialVersionUID = -6322352369831759279L;
    
    /**
     * task index
     */
    private int id;
    
    /**
     * shared map
     */
    private transient Map<Integer, Double> sharedMap;
    
    public Task(int id) {
        this.id = id;
    }
    
    private Map<Integer, Double> getMap()
    {
      String key = "sharedArray";
      Map<Integer,Double> map = (Map<Integer, Double>) NodeRunner.getPersistentData(key);
      if (map == null)
      {
        map = Hazelcast.getMap(key);
        NodeRunner.setPersistentData(key, map);
      }
      return map;
    }
    
    public void run() {
        // recover data
        sharedMap = getMap();
        double data = sharedMap.get(id);
        System.out.println("task # " + id +  "data = " + data);
        sharedMap.put(id, data * 2);
        System.out.println("task # " + id +  " changed " + sharedMap.get(id));
    }

}

