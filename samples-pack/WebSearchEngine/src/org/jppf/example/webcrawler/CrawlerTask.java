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

import java.net.URL;
import java.util.*;

import org.apache.commons.httpclient.params.DefaultHttpParams;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.jppf.server.protocol.JPPFTask;

import com.torunski.crawler.Crawler;
import com.torunski.crawler.events.*;
import com.torunski.crawler.filter.*;
import com.torunski.crawler.link.Link;
import com.torunski.crawler.model.MaxDepthModel;
import com.torunski.crawler.parser.PageData;
import com.torunski.crawler.parser.httpclient.SimpleHttpClientParser;

/**
 * Instances of this class implement the 2 phases of the web search process.<br>
 * The first phase is made of one or several JPPF invocations (one for each search depth level) and
 * consists in following the links up to the specified search depth.<br>
 * In the second phase, all gathered links are downloaded, indexed and matched with the user-specified query.
 * @author Laurent Cohen
 */
public class CrawlerTask extends JPPFTask
{
  static
  {
    /*
     * Set the default http and socket parameters for the commons-http-client API.
     */
    DefaultHttpParams.setHttpParamsFactory(new JPPFHttpDefaultParamsFactory());
  }

  /**
   * The start url.
   */
  private String url = null;
  /**
   * The search query.
   */
  private String query = null;
  /**
   * The list of links visited during the crawl.
   */
  private List<LinkMatch> matchedLinks = new ArrayList<>();
  /**
   * URLs left to visit.
   */
  private Set<String> toVisit = new HashSet<>();
  /**
   * Determines whether the search should also be done, in addition to crawling.
   */
  private boolean doSearch = false;

  /**
   * Initialize this task with the specified sequence to align with the target sequence.
   * @param url the start url to crawl on.
   * @param query the search query.
   * @param number uniquely identifies this task.
   * @param doSearch determines whether the search should also be done.
   */
  public CrawlerTask(final String url, final String query, final int number, final boolean doSearch)
  {
    this.url = url;
    this.query = query;
    this.doSearch = doSearch;
    setId("" + number);
  }

  /**
   * Perform the web crawling, or the indexed search, depending on the value of <code>doSearch</code>.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    try
    {
      if (doSearch) search();
      else crawl();
    }
    catch(Exception e)
    {
      setThrowable(e);
    }
  }

  /**
   * Crawl to find all links in the current page.
   * @throws Exception if an error occurs.
   */
  private void crawl() throws Exception
  {
    URL u = new URL(url);
    String server = u.getProtocol() + "://" + u.getHost();
    while (server.endsWith("/")) server = server.substring(0, server.length() -1);
    if (u.getPort() >= 0) server += ":" + u.getPort();
    String start = u.getPath() == null ? "" : u.getPath();
    while (start.startsWith("/")) start = start.substring(1);
    start = "/" + start;
    if (u.getQuery() != null) start += "?" + u.getQuery();

    int depth = 1;

    Crawler crawler = new Crawler();
    ILinkFilter filter = new ServerFilter(server);
    ILinkFilter filter2 = new FileExtensionFilter(
        new String[] {".png", ".jpg", ".gif", ".pdf", ".mpg", ".avi", ".wmv", ".swf", });
    filter2 = LinkFilterUtil.not(filter2);
    filter = LinkFilterUtil.and(filter, filter2);
    crawler.setLinkFilter(filter);
    crawler.setModel(new MaxDepthModel(depth));
    crawler.addParserListener(new IParserEventListener()
    {
      @Override
      public void parse(final ParserEvent event)
      {
        String url = event.getLink().getURI();
        if (!toVisit.contains(url)) toVisit.add(url);
      }
    });

    crawler.start(server, start);
  }

  /**
   * Search for the user-specified query expression in the current page.
   * @throws Exception if an error occurs.
   */
  private void search() throws Exception
  {
    QueryParser parser = new QueryParser("contents", new StandardAnalyzer());
    Query q = parser.parse(query);

    MemoryIndex index = new MemoryIndex();
    Link link = new Link(url);
    PageData pageData = new SimpleHttpClientParser().load(link);
    index.addField("contents", pageData.getData().toString(), new StandardAnalyzer());
    IndexSearcher searcher = index.createSearcher();
    Hits hits = searcher.search(q);
    Iterator it = hits.iterator();
    float relevance = 0f;
    if (it.hasNext())
    {
      while (it.hasNext())
      {
        Hit hit = (Hit) it.next();
        relevance += ((float) Math.round(hit.getScore() * 1000)) / 10;
      }
      matchedLinks.add(new LinkMatch(url, relevance));
    }
  }

  /**
   * Get the sequence number of this task.
   * @return the sequence number as an int.
   */
  public int getNumber()
  {
    return Integer.valueOf(getId());
  }

  /**
   * Get the list of links matching the search query.
   * @return a list of <code>LinkMatch</code> instances.
   */
  public Collection<LinkMatch> getMatchedLinks()
  {
    return matchedLinks;
  }

  /**
   * Get the start url.
   * @return the url as a string.
   */
  public String getUrl()
  {
    return url;
  }

  /**
   * Get the URLs left to visit.
   * @return a list of url strings.
   */
  public Collection<String> getToVisit()
  {
    return toVisit;
  }
}
