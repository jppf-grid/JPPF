/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.example.webcrawler;

import java.util.*;

import org.apache.commons.httpclient.params.DefaultHttpParams;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.slf4j.*;

import com.torunski.crawler.Crawler;
import com.torunski.crawler.events.*;
import com.torunski.crawler.filter.ServerFilter;
import com.torunski.crawler.lucene.LuceneParserEventListener;
import com.torunski.crawler.model.MaxDepthModel;

/**
 * Test of the crawler API.
 * @author Laurent Cohen
 */
public class CrawlerTest
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CrawlerTest.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Server to crawl.
   */
  //private static String server = "http://www.jppf.org";
  private static String server = "http://localhost:8880";
  /**
   * Starting server page.
   */
  private static String startPage = "/index.php";
  /**
   * Location of the temporary files folder.
   */
  //private static String index = System.getProperty("java.io.tmpdir") + File.separator + "crawler";
  private static String index = "crawler";

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String... args)
  {
    try
    {
      init();
      //simpleCrawl();
      //luceneIndex();
      luceneSearch("+client +driver", 50);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Simple crawler test.
   * @throws Exception if an error is thrown while executing.
   */
  public static void simpleCrawl() throws Exception
  {
    Crawler crawler = new Crawler();
    crawler.setLinkFilter(new ServerFilter(server));

    crawler.addParserListener(new IParserEventListener()
    {
      @Override
      public void parse(final ParserEvent event)
      {
        print("Parsing link: "+event.getLink());
      }
    });
    crawler.start(server, startPage);

    // show visited links
    Collection visitedLinks = crawler.getModel().getVisitedURIs();
    print("Links visited=" + visitedLinks.size());

    Iterator list = visitedLinks.iterator();
    while (list.hasNext()) print(""+list.next());

    // show visited links
    Collection notVisitedLinks = crawler.getModel().getToVisitURIs();

    print("Links NOT visited=" + notVisitedLinks.size());
    Iterator listNot = notVisitedLinks.iterator();
    while (listNot.hasNext()) print(""+listNot.next());
  }

  /**
   * Test of indexing with Lucene.
   * @throws Exception if an error is thrown while executing.
   */
  public static void luceneIndex() throws Exception
  {
    // setting default parameters
    int depth = 3;

    // create Lucene index writer
    IndexWriter writer = new IndexWriter(index, new StandardAnalyzer(), true);
    writer.setUseCompoundFile(true);
    writer.setMaxFieldLength(1000000);

    // common crawler settings
    Crawler crawler = new Crawler();
    crawler.setLinkFilter(new ServerFilter(server));
    crawler.setModel(new MaxDepthModel(depth));
    crawler.addParserListener(new IParserEventListener()
    {
      @Override
      public void parse(final ParserEvent event)
      {
        print("Parsing link: "+event.getLink());
      }
    });

    // create Lucene parsing listener and add it
    LuceneParserEventListener listener = new LuceneParserEventListener(writer);
    crawler.addParserListener(listener);

    // start crawler
    crawler.start(server, startPage);

    // Optimizing Lucene index
    writer.optimize();
    writer.close();
  }

  /**
   * Test searching with Lucene.
   * @param search the Lucene query text.
   * @param max the maximum number of results to show.
   * @throws Exception if an error is thrown while executing.
   */
  public static void luceneSearch(final String search, final int max) throws Exception
  {
    print("Searching for: " + search);
    print("  max results: " + max);

    IndexSearcher is = new IndexSearcher(index);
    QueryParser parser = new QueryParser("contents", new StandardAnalyzer());

    Query query = parser.parse(search);
    Hits hits = is.search(query);

    print("    results: " + hits.length());

    for (int i = 0; i < Math.min(hits.length(), max); i++)
    {
      float relevance = ((float) Math.round(hits.score(i) * 1000)) / 10;
      String url = hits.doc(i).getField("url").stringValue();
      print("No " + (i + 1) + " with relevance " + relevance + "% : " + url);
    }

    is.close();
  }

  /**
   * Print a string to the standard output and the log.
   * @param s the string to print.
   */
  private static void print(final String s)
  {
    System.out.println(s);
    log.info(s);
  }

  /**
   * Initializations.
   */
  private static void init()
  {
    DefaultHttpParams.setHttpParamsFactory(new JPPFHttpDefaultParamsFactory());
  }
}
