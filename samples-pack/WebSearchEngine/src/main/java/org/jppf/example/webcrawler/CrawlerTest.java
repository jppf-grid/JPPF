/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.httpclient.params.DefaultHttpParams;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.torunski.crawler.Crawler;
import com.torunski.crawler.events.IParserEventListener;
import com.torunski.crawler.events.ParserEvent;
import com.torunski.crawler.filter.ServerFilter;
import com.torunski.crawler.lucene.LuceneParserEventListener;
import com.torunski.crawler.model.MaxDepthModel;

/**
 * Test of the crawler API.
 * @author Laurent Cohen
 */
public class CrawlerTest {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CrawlerTest.class);
  /**
   * Server to crawl.
   */
  private static String server = "http://localhost:8880";
  /**
   * Starting server page.
   */
  private static String startPage = "/index.php";
  /**
   * Location of the temporary files folder.
   */
  private static String index = "crawler";

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String... args) {
    try {
      init();
      //simpleCrawl();
      //luceneIndex();
      luceneSearch("+client +driver", 50);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Simple crawler test.
   * @throws Exception if an error is thrown while executing.
   */
  @SuppressWarnings("rawtypes")
  public static void simpleCrawl() throws Exception {
    final Crawler crawler = new Crawler();
    crawler.setLinkFilter(new ServerFilter(server));

    crawler.addParserListener(new IParserEventListener() {
      @Override
      public void parse(final ParserEvent event) {
        print("Parsing link: " + event.getLink());
      }
    });
    crawler.start(server, startPage);

    // show visited links
    final Collection visitedLinks = crawler.getModel().getVisitedURIs();
    print("Links visited=" + visitedLinks.size());

    final Iterator list = visitedLinks.iterator();
    while (list.hasNext())
      print("" + list.next());

    // show visited links
    final Collection notVisitedLinks = crawler.getModel().getToVisitURIs();

    print("Links NOT visited=" + notVisitedLinks.size());
    final Iterator listNot = notVisitedLinks.iterator();
    while (listNot.hasNext())
      print("" + listNot.next());
  }

  /**
   * Test of indexing with Lucene.
   * @throws Exception if an error is thrown while executing.
   */
  public static void luceneIndex() throws Exception {
    // setting default parameters
    final int depth = 3;

    // create Lucene index writer
    final IndexWriter writer = new IndexWriter(index, new StandardAnalyzer(), true);
    writer.setUseCompoundFile(true);
    writer.setMaxFieldLength(1000000);

    // common crawler settings
    final Crawler crawler = new Crawler();
    crawler.setLinkFilter(new ServerFilter(server));
    crawler.setModel(new MaxDepthModel(depth));
    crawler.addParserListener(new IParserEventListener() {
      @Override
      public void parse(final ParserEvent event) {
        print("Parsing link: " + event.getLink());
      }
    });

    // create Lucene parsing listener and add it
    final LuceneParserEventListener listener = new LuceneParserEventListener(writer);
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
  public static void luceneSearch(final String search, final int max) throws Exception {
    print("Searching for: " + search);
    print("  max results: " + max);

    final IndexSearcher is = new IndexSearcher(index);
    final QueryParser parser = new QueryParser("contents", new StandardAnalyzer());

    final Query query = parser.parse(search);
    final Hits hits = is.search(query);

    print("    results: " + hits.length());

    for (int i = 0; i < Math.min(hits.length(), max); i++) {
      final float relevance = ((float) Math.round(hits.score(i) * 1000)) / 10;
      final String url = hits.doc(i).getField("url").stringValue();
      print("No " + (i + 1) + " with relevance " + relevance + "% : " + url);
    }

    is.close();
  }

  /**
   * Print a string to the standard output and the log.
   * @param s the string to print.
   */
  private static void print(final String s) {
    System.out.println(s);
    log.info(s);
  }

  /**
   * Initializations.
   */
  private static void init() {
    DefaultHttpParams.setHttpParamsFactory(new JPPFHttpDefaultParamsFactory());
  }
}
