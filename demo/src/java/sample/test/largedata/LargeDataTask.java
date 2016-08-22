/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
package sample.test.largedata;

import java.util.*;

import org.jppf.node.protocol.AbstractTask;

/**
 * 
 * @author Laurent Cohen
 */
public class LargeDataTask extends AbstractTask<Map<String, Long>>
{
  /**
   * The data in this task.
   */
  private List<String> articles = null;

  /**
   * Initialize with the specified data size.
   * @param articles the articles to process.
   */
  public LargeDataTask(final List<String> articles)
  {
    this.articles = articles;
  }

  /**
   * Perform the execution of this task.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    Map<String, Long> countMap = new HashMap<>();
    try
    {
      for (String article: articles)
      {
        String text = tagValue(article, "text");
        int limit = text.length() - 1;
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<text.length(); i++)
        {
          char c = Character.toLowerCase(text.charAt(i));
          boolean b = isWordChar(c);
          if (b) sb.append(c);
          if ((i == limit) || !b)
          {
            
            if (isWord(sb))
            {
              String key = sb.toString();
              Long n = countMap.get(key);
              if (n == null) n = 1L;
              else n++;
              countMap.put(key, n);
            }
            if (sb.length() > 0) sb = new StringBuilder();
          }
        }
      }
      setResult(countMap);
      //System.out.println("counted " + countMap.size() + " words");
    }
    finally
    {
      articles = null;
    }
  }

  /**
   * Find the content of the specified tag in the specified text.
   * @param text the text to look into.
   * @param tag the tag to look for.
   * @return the tag content as a string.
   */
  private String tagValue(final String text, final String tag)
  {
    String startTag = "<" + tag;
    int idx = text.indexOf(startTag);
    if (idx < 0) return null;
    idx = text.indexOf('>', idx + startTag.length());
    if (idx < 0) return null;
    String endTag = "</" + tag + ">";
    int idx2 = text.indexOf(endTag, idx);
    if (idx2 < 0) return null;
    return text.substring(idx + 1, idx2);
  }

  /**
   * Determine whether the specified character is part of a word or to be discarded.
   * @param c the character to examine.
   * @return <code>true</code> if the character is part of a word, <code>false</code> otherwise.
   */
  private boolean isWordChar(final char c)
  {
    return ((c >= 'a') && (c <='z')) || ((c >= 'A') && (c <='Z'));
    //return Character.isLetter(c);
    //return Character.isLetterOrDigit(c) || (c == '_');
  }

  /**
   * Determine whether the specified character sequence is a word or to be discarded.
   * @param sb the character sequence to examine.
   * @return <code>true</code> if the character is part of a word, <code>false</code> otherwise.
   */
  private boolean isWord(final StringBuilder sb)
  {
    return (sb.length() > 0) && (sb.length() < 25);
  }
}
