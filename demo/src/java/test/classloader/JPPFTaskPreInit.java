package test.classloader;

import java.io.*;
import java.net.URL;
import java.util.Map;

import org.jppf.JPPFException;
import org.jppf.classloader.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.ClientDataProvider;
import org.jppf.utils.JPPFCallable;

/**
 * Created by IntelliJ IDEA.
 * User: jandam
 * Date: Aug 30, 2011
 * Time: 8:59:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class JPPFTaskPreInit extends JPPFTask {

  private static final long serialVersionUID = 3255941091582311973L;

  private final Map<ByteKey, URL> map;

  public JPPFTaskPreInit(final Map<ByteKey, URL> map) {
    if(map == null) throw new IllegalArgumentException("map is null");

    this.map = map;
  }

  @Override
  public void run() {
    System.out.println("Task init");
    long totalDur = System.nanoTime();
    long size = 0L;
    int count = 0;
    int loadCount = 0;
    try {
      ClientDataProvider dataProvider = (ClientDataProvider) getDataProvider();

      ClassLoader classLoader = getClass().getClassLoader();
      if(classLoader instanceof AbstractJPPFClassLoader) {
        JPPFClassLoader jppfClassLoader = (JPPFClassLoader) classLoader;

        for (Map.Entry<ByteKey, URL> entry : map.entrySet()) {
          long dur = System.nanoTime();
          ByteKey key = entry.getKey();
          URL url = entry.getValue();

          File out = new File("jppf-cache-" + key.toHex() + ".bin");
          if(!out.canRead()) {
            byte[] data = safeComputeValue(dataProvider, url.toExternalForm(), new DownloadFile(url));

            size += data.length;

            FileOutputStream os = null;
            try {
              os = new FileOutputStream(out);
              os.write(data);
            } finally {
              if(os != null) os.close();
            }
            dur = System.nanoTime() - dur;
            loadCount++;
            System.out.printf("Loaded file: %s - source: %s - size: %d, dur: %f%n", out.getName(), url.toExternalForm(), data.length, dur / 1000000.0);
//                        out.deleteOnExit();
          }

          if(out.canRead()) {
            jppfClassLoader.addURL(out.toURI().toURL());
            count++;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      setException(e);
    } catch (Throwable t) {
      t.printStackTrace();
      setException(new JPPFException(t));
    }  finally {
      totalDur = System.nanoTime() - totalDur;
      System.out.printf("FINISHED in: %s\t downloaded: %d\t - loaded: %s, registered: %s%n", (totalDur / 1000000.0), size, loadCount, count);
    }
  }

  @SuppressWarnings("unchecked")
  public static <V> V safeComputeValue(final ClientDataProvider dataProvider, final Object key, final JPPFCallable<V> callable) {
    if (key == null) return null;
    Object result = dataProvider.computeValue(key, callable);
    if (result instanceof Throwable) {
//                System.out.println("Result: " + result);
      ((Throwable) result).printStackTrace();
    }
    return (V) result;
  }

  public static class DownloadFile implements JPPFCallable<byte[]> {
    private static final long serialVersionUID = 8363386268214414851L;

    private final URL file;

    public DownloadFile(final URL file) {
      this.file = file;
    }

    @Override
    public byte[] call() throws Exception {
      System.out.printf("Downloading: %s%n", file.toExternalForm());
      InputStream is = null;
      ByteArrayOutputStream os = new ByteArrayOutputStream();

      try {
        is = file.openStream();
        copy(is, os);
      } finally {
        if(is != null) is.close();
        os.close();
      }
      return os.toByteArray();
    }

    protected static void copy(final InputStream in, final ByteArrayOutputStream out) throws IOException {
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0){
        out.write(buf, 0, len);
      }
    }
  }
}