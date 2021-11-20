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
package org.jppf.ui.options.factory;

import java.awt.Frame;
import java.io.*;
import java.util.*;
import java.util.prefs.*;

import org.jppf.ui.monitoring.ConsoleLauncher;
import org.jppf.ui.monitoring.charts.config.JPPFChartBuilder;
import org.jppf.ui.monitoring.diagnostics.JVMHealthPanel;
import org.jppf.ui.options.*;
import org.jppf.ui.options.docking.DockingManager;
import org.jppf.ui.options.event.ValueChangeEvent;
import org.jppf.ui.options.xml.OptionsPageBuilder;
import org.jppf.ui.plugin.PluggableViewHandler;
import org.jppf.ui.treetable.AbstractTreeTableOption;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * This class handles the persistence of the dynamic UI com.
 * @author Laurent Cohen
 */
public final class OptionsHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(OptionsHandler.class);
  /**
   * The root of the preferences subtree in which the chart configurations are saved.
   */
  private static final Preferences JPPF_PREFERENCES = Preferences.userRoot().node("jppf");
  /**
   * The root of the preferences subtree in which the chart configurations are saved.
   */
  private static Preferences preferences = JPPF_PREFERENCES;
  /**
   * The list of option pages managed by this handler.
   */
  private static List<OptionElement> pageList = new Vector<>();
  /**
   * A mapping of option pages to their name.
   */
  private static Map<String, OptionElement> pageMap = new Hashtable<>();
  /**
   * The page builder used to instantiate pages from XML descriptors.
   */
  private static OptionsPageBuilder builder = new OptionsPageBuilder();
  /**
   * The main window of the application.
   */
  private static Frame mainWindow = null;
  /**
   * Handles user-defined pluggable views.
   */
  private static final PluggableViewHandler pluggableViewHandler = new PluggableViewHandler();

  /**
   * Get the list of option pages managed by this handler.
   * @return a list of <code>OptionsPage</code> instances.
   */
  public static List<OptionElement> getPageList() {
    return pageList;
  }

  /**
   * Retrieve a page from its name.
   * @param name the name of the page to retrieve.
   * @return an <code>OptionsPage</code> instance.
   */
  public static synchronized OptionElement getPage(final String name) {
    return pageMap.get(name);
  }

  /**
   * Retrieve the first loaded page.
   * @return an <code>OptionsPage</code> instance.
   */
  public static synchronized OptionElement getTopPage() {
    return pageList.isEmpty() ? null : pageList.get(0);
  }

  /**
   * Add a page to the list of pages managed by this handler.
   * @param page an <code>OptionsPage</code> instance.
   * @return the page that was added.
   */
  public static synchronized OptionElement addPage(final OptionElement page) {
    if (page != null) {
      if (pageList.isEmpty()) setPreferences(JPPF_PREFERENCES.node(page.getName()));
      pageList.add(page);
      try {
        pageMap.put(page.getName(), page);
        if (log.isDebugEnabled()) log.debug("adding page '{}' = {}", page.getName(), page);
      } catch(final RuntimeException e) {
        log.error("Exception for page = {} : {}", page, ExceptionUtils.getMessage(e));
        throw e;
      }
    }
    return page;
  }

  /**
   * Remove a page from the list of pages managed by this handler.
   * @param page an <code>OptionsPage</code> instance.
   */
  public static synchronized void removePage(final OptionContainer page) {
    if (log.isDebugEnabled()) log.debug("removing page '{}'", page.getName());
    pageList.remove(page);
    pageMap.remove(page.getName());
  }

  /**
   * Load a page built from an xml document.
   * @param xmlPath the path to the xml document.
   * @return the page that was added.
   */
  public static synchronized OptionElement loadPageFromXml(final String xmlPath) {
    try {
      if (log.isDebugEnabled()) log.debug("loading page '{}'", xmlPath);
      return builder.buildPage(xmlPath, null);
    } catch(final Exception e) {
      log.error("error loading page '{}'", xmlPath, e);
    }
    return null;
  }

  /**
   * Add a page built from an xml document.
   * @param xmlPath the path to the xml document.
   * @return the page that was added.
   */
  public static synchronized OptionElement addPageFromXml(final String xmlPath) {
    return addPage(loadPageFromXml(xmlPath));
  }

  /**
   * Load a page built from an xml document.
   * @param xmlPath the path to the xml document.
   * @param baseName base name for resource bundle lookup.
   * @return the page that was added.
   */
  public static synchronized OptionElement loadPageFromURL(final String xmlPath, final String baseName) {
    try {
      if (log.isDebugEnabled()) log.debug("loading page '{}', baseName={}", xmlPath, baseName);
      return builder.buildPageFromURL(xmlPath, baseName);
    } catch(final Exception e) {
      log.error("error loading page '{}'", xmlPath, e);
    }
    return null;
  }

  /**
   * Add a page built from an xml document.
   * @param xmlPath the path to the xml document.
   * @param baseName base name for resource bundle lookup.
   * @return the page that was added.
   */
  public static synchronized OptionElement addPageFromURL(final String xmlPath, final String baseName) {
    return addPage(loadPageFromURL(xmlPath, baseName));
  }

  /**
   * Save the value of all persistent options in the preferences store.
   */
  public static void savePreferences() {
    log.trace("saving preferences");
    try {
      for (final OptionElement elt: pageList) {
        final OptionNode node = buildPersistenceGraph(elt);
        savePreferences(node, JPPF_PREFERENCES);
      }
      getPreferences().flush();
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Save the value of all persistent options in the preferences store.
   * @param node the root of the options subtree to save.
   * @param prefs the preferences node in which to save the values.
   */
  public static void savePreferences(final OptionNode node, final Preferences prefs) {
    if (!node.children.isEmpty()) {
      log.trace("persisting node {}", node.elt.getName());
      final Preferences p = prefs.node(node.elt.getName());
      for (final OptionNode child: node.children) savePreferences(child, p);
    } else if (node.elt instanceof Option) {
      final Option option = (Option) node.elt;
      if (option.isPersistent()) {
        final Object value = option.getValue();
        log.trace("persisting option {} = {}", option.getStringPath(), value);
        final String s = String.valueOf(value);
        prefs.put(option.getName(), s);
      }
    } else log.trace("persisting node {}", node.elt.getName());
  }

  /**
   * Load the value of all persistent options in the preferences store.
   */
  public static void loadPreferences() {
    for (final OptionElement elt: pageList) {
      final OptionNode node = buildPersistenceGraph(elt);
      loadPreferences(node, JPPF_PREFERENCES);
    }
  }

  /**
   * Save the value of all persistent options in the preferences store.
   * @param node the root of the options subtree to save.
   * @param prefs the preferences node in which to save the values.
   */
  public static void loadPreferences(final OptionNode node, final Preferences prefs) {
    if (!node.children.isEmpty()) {
      log.trace("loading node {}", node.elt.getName());
      final Preferences p = prefs.node(node.elt.getName());
      for (final OptionNode child: node.children) loadPreferences(child, p);
    } else if (node.elt instanceof AbstractOption) {
      final AbstractOption option = (AbstractOption) node.elt;
      final Object def = option.getValue();
      log.trace("loading option {} = {}", option.getStringPath(), def);
      final String val = prefs.get(option.getName(), def == null ? null : def.toString());
      option.setValue(val);
    }
  }

  /**
   * Get the page builder used to instantiate pages from XML descriptors.
   * @return an <code>OptionsPageBuilder</code> instance.
   */
  public static OptionsPageBuilder getBuilder() {
    return builder;
  }

  /**
   * Build a graph of the persistent elements.
   * @param elt the root of the current subgraph.
   * @return an <code>OptionNode</code> instance.
   */
  public static OptionNode buildPersistenceGraph(final OptionElement elt) {
    OptionNode node = null;
    if (elt instanceof OptionContainer) {
      log.trace("processing container {}", elt.getStringPath());
      node = new OptionNode(elt);
      final OptionContainer page = (OptionContainer) elt;
      for (final OptionElement child: page.getChildren()) {
        final OptionNode childNode = buildPersistenceGraph(child);
        if (childNode != null) node.children.add(childNode);
      }
    } else if (elt instanceof AbstractOption) {
      log.trace("processing option {}", elt.getStringPath());
      if (((AbstractOption) elt).isPersistent()) node = new OptionNode(elt);
    }
    return node;
  }

  /**
   * Get the root of the preferences subtree in which the chart configurations are saved.
   * @return a {@link Preferences} instance.
   */
  public static synchronized Preferences getPreferences() {
    return preferences;
  }

  /**
   * Set the root of the preferences subtree in which the chart configurations are saved.
   * @param preferences a {@link Preferences} instance.
   */
  public static synchronized void setPreferences(final Preferences preferences) {
    OptionsHandler.preferences = preferences;
  }

  /**
   * Load the application's main window state from the preferences store.
   * @param pref the preferences node from where the attributes are loaded.
   */
  public static void loadMainWindowAttributes(final Preferences pref) {
    loadFrameAttributes(mainWindow, pref);
  }

  /**
   * Save the application's main window state to the preferences store.
   * @param pref the preferences node where the attributes are saved.
   */
  public static void saveMainWindowAttributes(final Preferences pref) {
    saveFrameAttributes(mainWindow, pref);
  }

  /**
   * Load the specified frame state from the preferences store.
   * @param frame the frame for which the attributes are retrieved.
   * @param pref the preferences node from where the attributes are loaded.
   */
  public static void loadFrameAttributes(final Frame frame, final Preferences pref) {
    final int x = pref.getInt("locationx", 0);
    final int y = pref.getInt("locationy", 0);
    final int width = pref.getInt("width", 600);
    final int height = pref.getInt("height", 768);
    frame.setSize(width, height);
    frame.setLocation(x, y);
    final boolean maximized = pref.getBoolean("maximized", false);
    if (maximized) frame.setExtendedState(Frame.MAXIMIZED_BOTH);
  }

  /**
   * Save the specified frame state to the preferences store.
   * @param frame the frame for which the attributes are saved.
   * @param pref the preferences node where the attributes are saved.
   */
  public static void saveFrameAttributes(final Frame frame, final Preferences pref) {
    final int state = frame.getExtendedState();
    final boolean maximized = (state & Frame.MAXIMIZED_BOTH) > 0;
    if (maximized) frame.setExtendedState(Frame.NORMAL);
    final java.awt.Point p = frame.getLocation();
    pref.putInt("locationx", p.x);
    pref.putInt("locationy", p.y);
    final java.awt.Dimension d = frame.getSize();
    pref.putInt("width", d.width);
    pref.putInt("height", d.height);
    pref.putBoolean("maximized", maximized);
    try {
      pref.flush();
    } catch(final BackingStoreException e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * A graph of the persistent options.
   */
  public static class OptionNode {
    /**
     * The corresponding option element.
     */
    public OptionElement elt = null;
    /**
     * The children of the corresponding option element.
     */
    public List<OptionNode> children = new ArrayList<>();

    /**
     * Initialize this node.
     * @param elt the corresponding option element.
     */
    public OptionNode(final OptionElement elt) {
      this.elt = elt;
    }
  }

  /**
   * Get the main window of the application.
   * @return a {@link Frame} instance.
   */
  public static Frame getMainWindow() {
    return mainWindow;
  }

  /**
   * Set the main window of the application.
   * @param mainWindow a {@link Frame} instance.
   */
  public static void setMainWindow(final Frame mainWindow) {
    OptionsHandler.mainWindow = mainWindow;
  }

  /**
   * 
   * @param searchRoot the root option to search from.
   * @param name the name of the option to search.
   * @return the first option found with the specified name.
   */
  public static OptionElement findOptionWithName(final OptionElement searchRoot, final String name) {
    OptionElement result = null;
    if (searchRoot != null) {
      result = searchRoot.findFirstWithName(name);
    }
    if (result == null) result = DockingManager.getInstance().findFirstElementWithName(name);
    return result;
  }

  /**
   * Get the object which handles user-defined pluggable views.
   * @return a {@link PluggableViewHandler} instance.
   */
  public static PluggableViewHandler getPluggableViewHandler() {
    return pluggableViewHandler;
  }

  /**
   * Export the UI settings to an XML file.
   * @param path the path of the XML file to save to.
   */
  public static void exportSettings(final String path) {
    try {
      savePreferences();
      if (ConsoleLauncher.isEmbedded()) saveMainWindowAttributes(getPreferences());
      final OptionElement root = pageList.get(0);
      final OptionElement elt = findOptionWithName(root, "/ChartsBuilder");
      if (elt != null) {
        final JPPFChartBuilder chartBuilder = (JPPFChartBuilder) elt.getUIComponent();
        if (chartBuilder != null) chartBuilder.getStorage().saveAll();
      }
      final String[] names = { "/health.treetable", "/NodeTreeTable", "/JobTreetable" };
      JVMHealthPanel panel = null;
      for (final String name: names) {
        final AbstractTreeTableOption option = (AbstractTreeTableOption) findOptionWithName(root, name);
        if (option != null) option.saveTableColumnsSettings();
        if ("/health.treetable".equals(name)) panel = (JVMHealthPanel) option;
      }
      if (panel != null) panel.saveThresholds();
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
    try (final OutputStream os = new BufferedOutputStream(new FileOutputStream(path))) {
      getPreferences().exportSubtree(os); 
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Export the UI settings to an XML file.
   * @param path the path of the XML file to save to.
   */
  public static void importSettings(final String path) {
    try (InputStream is = new BufferedInputStream(new FileInputStream(path))) {
      Preferences.importPreferences(is); 
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
    try {
      loadPreferences();
      if (!ConsoleLauncher.isEmbedded()) loadMainWindowAttributes(getPreferences());
      final OptionElement root = pageList.get(0);
      final OptionElement elt = findOptionWithName(root, "/ChartsBuilder");
      if (elt != null) {
        final JPPFChartBuilder chartBuilder = (JPPFChartBuilder) elt.getUIComponent();
        if (chartBuilder != null) chartBuilder.reset();
        final OptionElement chartConfigPage = findOptionWithName(root, "/ChartsConfiguration");
        chartConfigPage.getInitializer().valueChanged(new ValueChangeEvent(chartConfigPage));
      }
      final String[] names = { "/health.treetable", "/NodeTreeTable", "/JobTreetable" };
      JVMHealthPanel panel = null;
      for (final String name: names) {
        final AbstractTreeTableOption option = (AbstractTreeTableOption) findOptionWithName(root, name);
        if (option != null) option.setupTableColumns();
        if ("/health.treetable".equals(name)) panel = (JVMHealthPanel) option;
      }
      if (panel != null) panel.loadThresholds();
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
