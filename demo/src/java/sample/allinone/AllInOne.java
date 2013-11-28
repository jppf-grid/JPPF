/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package sample.allinone;

import java.io.File;
import java.util.List;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;


/**
 * Sample code demonstrating how to run a driver and node in the same JVM.
 * @author Laurent Cohen
 */
public class AllInOne {
  /**
   * 
   */
  private static Process process = null;

  /**
   * Entry point for this program.
   * @param args no used.
   */
  public static void main(final String... args) {
    JPPFClient client = null;
    try {
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          if (process != null) {
            process.destroy();
            process = null;
          }
        }
      });
      startDriverAndNode();
      client = new JPPFClient();
      JPPFJob job = new JPPFJob();
      job.addTask(new MyTask());
      List<JPPFTask> results = client.submit(job);
      MyTask task = (MyTask) results.get(0);
      if (task.getException() != null) throw task.getException();
      else System.out.println("task result: " + task.getResult());
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      System.out.println("closing jppf client");
      if (client != null) client.close();
      System.out.println("terminating process");
      if (process != null) {
        process.destroy();
        process = null;
      }
      System.out.println("exiting");
      System.exit(0);
    }
  }

  /**
   * Starts a driver and node in a seperate process.
   * @throws Exception if any error occurs.
   */
  public static void startDriverAndNode() throws Exception {
    ProcessBuilder builder = new ProcessBuilder();
    //String libDir = userDir + "/lib";
    String driverDir = "C:/Workspaces/temp/JPPF-3.3.5-driver";
    builder.directory(new File(driverDir));
    String libDir = driverDir + "/lib";
    String configDir = driverDir + "/config";
    String cp = configDir + ";$lib/jppf-common-node.jar;$lib/jppf-common.jar;$lib/jppf-server.jar" +
        ";$lib/jmxremote_optional-1.0_01-ea.jar;$lib/log4j-1.2.15.jar;$lib/slf4j-api-1.6.1.jar;$lib/slf4j-log4j12-1.6.1.jar";
    cp = cp.replace("$lib", libDir);
    builder.command(
        "java", "-cp", cp, "-Xmx512m", "-server",
        "-Dlog4j.configuration=log4j-driver.properties",
        "-Djppf.config=jppf-driver.properties",
        "-Djava.util.logging.config.file=" + configDir + "/logging-driver.properties",
        "org.jppf.server.JPPFDriver",
        "noLauncher");
    System.out.println("command = " + builder.command());
    builder.redirectError(new File("err.log"));
    builder.redirectOutput(new File("out.log"));
    process = builder.start();
  }

  /**
   * Simple task.
   */
  public static class MyTask extends JPPFTask {
    @Override
    public void run() {
      try {
        Thread.sleep(3000L);
        setResult("execution sucessful");
      } catch (Exception e) {
        setException(e);
      }
    }
  }
}
