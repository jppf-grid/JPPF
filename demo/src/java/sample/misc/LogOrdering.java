/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package sample.misc;

import java.io.*;
import java.util.*;

/**
 * 
 * @author Laurent Cohen
 */
public class LogOrdering
{
  /**
   * 
   * @param args .
   */
  public static void main(final String[] args)
  {
    try
    {
      TreeMap<Long, String> map = new TreeMap<Long, String>();
      BufferedReader reader = new BufferedReader(new FileReader(args[0]));
      long min = Long.MAX_VALUE;
      String s = "";
      while (s != null)
      {
        s = reader.readLine();
        if (s != null)
        {
          if ("".equals(s.trim())) continue;
          long l = getBundleNumber(s);
          if ((l != 0L) && (l < min)) min = l; 
          map.put(l, s);
        }
      }
      reader.close();
      long prev = min - 1L;
      BufferedWriter writer = new BufferedWriter(new FileWriter(args[0]));
      for (Map.Entry<Long, String> entry: map.entrySet())
      {
        long l = entry.getKey();
        if ((l != 0L) && (l > min) && (l - prev > 1)) System.out.println("missing " + (l - 1));
        prev = l;
        writer.write(entry.getValue() + '\n');
      }
      writer.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * 
   * @param source .
   * @return the bundle number.
   */
  private static long getBundleNumber(final String source)
  {
    if (source.indexOf("bundle.uuid=null") >= 0) return 0L;
    String search = "bundle.uuid=d1-";
    int idx = source.indexOf(search);
    if (idx < 0) throw new IllegalArgumentException("bad string: " + source);
    idx += search.length();
    int idx2 = source.indexOf(",", idx);
    String s = source.substring(idx, idx2);
    long l = Long.valueOf(s);
    return l;
  }
}
