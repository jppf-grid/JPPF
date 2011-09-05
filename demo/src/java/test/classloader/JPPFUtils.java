package test.classloader;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.task.storage.ClientDataProvider;

import java.io.Closeable;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jandam
 * Date: Aug 30, 2011
 * Time: 9:00:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class JPPFUtils {
    protected static final int BUFFER_SIZE = 2048;

    /**
     *
     * @param client
     * @param baseURL - JAR base directory
     * @throws Exception
     */
    public static void preInit(final JPPFClient client, final URL baseURL) throws Exception {
        if(client == null) throw new IllegalArgumentException("client is null");
        if(baseURL == null) throw new IllegalArgumentException("baseURL is null");
        
        URI baseURI = baseURL.toURI().normalize();

        long durInit = System.nanoTime();

        try {
            URL[] urls = getURLs();
            if(urls != null && urls.length > 0) {
                Map<ByteKey, URL> urlMap = new LinkedHashMap<ByteKey, URL>();
                System.out.println("URLs: " + urls.length);
                for (URL url : urls) {
                    URI rel = baseURI.relativize(url.toURI());

                    if(rel.isAbsolute()) continue;
                    if(rel.toASCIIString().contains("/jppf/jppf-")) continue;

                    byte[] key = makeKey(url);
                    if(key == null || key.length == 0) {
                        System.out.println("  SKIPPED");
                    } else {
                        ByteKey byteKey = new ByteKey(key);
                        urlMap.put(byteKey, url);
                    }
                }

                System.out.printf("Found %d URLs%n", urlMap.size());

                JPPFJob job = new JPPFJob(new ClientDataProvider());
                job.addTask(new JPPFTaskPreInit(urlMap));
                job.setBlocking(true);
                job.getJobSLA().setBroadcastJob(true);
                client.submit(job);

                durInit = System.nanoTime() - durInit;
                System.out.println("Init: " + (durInit / 1000000.0));
            } else
                System.out.println("No URLClassLoader");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static byte[] makeKey(final URL url) {
        if(url != null) {
            InputStream is = null;
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                is = url.openStream();
                byte[] bytes = new byte[BUFFER_SIZE];
                int numBytes;
                while ((numBytes = is.read(bytes)) != -1) {
                    digest.update(bytes, 0, numBytes);
                }
                return digest.digest();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeSilent(is);
            }
        }
        return null;
    }

    public static URL[] getURLs() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if(loader instanceof URLClassLoader) {
            return ((URLClassLoader)loader).getURLs();
        } else if(loader.getParent() instanceof URLClassLoader) {
            return ((URLClassLoader)loader.getParent()).getURLs();
        } else
            return new URL[0];
    }

    public static void closeSilent(final Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (Throwable e) {
            // ignore - silent close
        }
    }
}
