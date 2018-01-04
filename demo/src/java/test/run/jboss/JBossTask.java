/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package test.run.jboss;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.jar.*;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.node.protocol.CommandLineTask;
import org.jppf.process.ProcessWrapperEvent;
import org.jppf.utils.ExceptionUtils;

/**
 * This task simply prints a message.
 * @author Laurent Cohen
 */
public class JBossTask extends CommandLineTask<Object> {
  /**
   * The location of the JBoss root installation.
   */
  private String jbossHome = "C:/Tools/jboss-5.1.0.GA";
  /**
   * The JBoss server configuration to use.
   */
  private String serverConfig = "jppf";

  /**
   * Initialize this task.
   * @param jbossHome the location of the JBoss root installation.
   * @param serverConfig the JBoss server configuration to use.
   */
  public JBossTask(final String jbossHome, final String serverConfig) {
    this.jbossHome = jbossHome;
    this.serverConfig = serverConfig;
  }

  /**
   * Execute this task.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    jbossHome = jbossHome.replace("\\", "/");
    while (jbossHome.endsWith("/")) jbossHome = jbossHome.substring(0, jbossHome.length() - 1);
    final String[] args = { "-server", "-Xms128M", "-Xmx1024M", "-XX:MaxPermSize=256M", "-Dprogram.name=run.bat", "-Dsun.rmi.dgc.client.gcInterval=3600000", "-Dsun.rmi.dgc.server.gcInterval=3600000",
      "-Dorg.jboss.resolver.warning=true", "-Djava.endorsed.dirs=" + jbossHome + "/lib/endorsed", "-classpath", jbossHome + "/bin/run.jar", "org.jboss.Main", "-c", serverConfig };
    // build invocation of java executable
    final StringBuilder javaCmd = new StringBuilder(System.getProperty("java.home").replace("\\", "/"));
    if (javaCmd.charAt(javaCmd.length() - 1) != '/') javaCmd.append('/');
    javaCmd.append("bin/java");
    final List<String> command = new ArrayList<>();
    command.add(javaCmd.toString());
    command.addAll(Arrays.asList(args));
    setCommandList(command);

    final Map<String, String> rawEnv = new HashMap<>();
    rawEnv.put("JBOSS_CLASSPATH", jbossHome + "/bin/run.jar");
    rawEnv.put("JBOSS_ENDORSED_DIRS", jbossHome + "/lib/endorsed");
    rawEnv.put("JBOSS_HOME", jbossHome);
    rawEnv.put("RUN_CLASSPATH", jbossHome + "/bin/run.jar");
    rawEnv.put("RUN_CONF", jbossHome + "/bin/run.conf.bat");
    rawEnv.put("RUNJAR", jbossHome + "/bin/run.jar");
    final Map<String, String> env = new HashMap<>();
    final String sep = System.getProperty("file.separator");
    for (final Map.Entry<String, String> entry: rawEnv.entrySet()) env.put(entry.getKey(), entry.getValue().replace("/", sep));
    rawEnv.clear();
    setEnv(env);

    setStartDir(jbossHome + "/bin/");
    setCaptureOutput(true);
    try {
      final int n = launchProcess();
      final String s = "process ended with exit code " + n;
      System.out.println(s);
      setResult(s);
    } catch (@SuppressWarnings("unused") final InterruptedException e) {
      // this excception is normally raised when the task
      // is cancelled from a separate thread so it's considered "normal"
      System.out.println("this task has been cancelled");
      //setResult("this task has been cancelled");
    } catch (final Exception e) {
      setThrowable(e);
      e.printStackTrace();
    }
  }

  /**
   * Overriden to redirect the JBoss standard output to the node's oconsole.
   * @param event a chunk of standard output wrapped as a {@link ProcessWrapperEvent}.
   */
  @Override
  public void outputStreamAltered(final ProcessWrapperEvent event) {
    final String s = event.getContent();
    if (s == null) return;
    System.out.print(s);
  }

  /**
   * Overriden to redirect the JBoss error output to the node's oconsole.
   * @param event a chunk of error output wrapped as a {@link ProcessWrapperEvent}.
   */
  @Override
  public void errorStreamAltered(final ProcessWrapperEvent event) {
    final String s = event.getContent();
    if (s == null) return;
    System.err.print(s);
  }

  /**
   * Shutdown the JBoss server when the job is cancelled.
   * <p>An attempt is made to shutdown via the JBoss API, which implies adding the
   * required jars to the classpath. If that fails, we simply <code>destroy()</code>
   * the JBoss process.
   */
  @Override
  public void onCancel() {
    try {
      final AbstractJPPFClassLoader cl = (AbstractJPPFClassLoader) getClass().getClassLoader();
      final URL[] urls = cl.getURLs();
      boolean found = false;
      // is shutdown.jar already in the classpath ?
      for (URL url: urls) {
        if (url.toString().indexOf("shutdown.jar") >= 0) {
          found = true;
          break;
        }
      }
      // if not let's add it dynamically
      if (!found) {
        File file = new File(jbossHome + "/bin/shutdown.jar");
        cl.addURL(file.toURI().toURL());
        file = new File(jbossHome + "/client/jbossall-client.jar");
        cl.addURL(file.toURI().toURL());
        final JarFile jar = new JarFile(file);
        final Manifest manifest = jar.getManifest();
        final String classPath = manifest.getMainAttributes().getValue("Class-Path");
        final String[] libs = classPath.split("\\s");
        final File dir = file.getParentFile();
        for (String s: libs) cl.addURL(new File(dir, s).toURI().toURL());
        jar.close();
      }
      // with shutdown.jar in the classpath, we can now invoke the ocmmand
      // org.jboss.Shutdown.main("-S") to shtudown the server
      System.setProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
      System.setProperty("jboss.boot.loader.name", "shutdown.bat");
      final Class<?> clazz = cl.loadClass("org.jboss.Shutdown");
      final Method method = clazz.getDeclaredMethod("main", String[].class);
      System.out.println("shutting down by invoking " + method);
      method.invoke((Object) null, (Object) new String[] { "-S" });
    } catch (final Exception e) {
      System.out.println("Could not shutdown properly, destroying the process: " + ExceptionUtils.getStackTrace(e));
      final Process p = getProcess();
      if (p != null) p.destroy();
    }
  }
}
