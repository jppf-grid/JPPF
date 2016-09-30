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
package org.jppf.ui.options.xml;

import java.awt.event.MouseListener;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.JComponent;

import org.jppf.ui.options.*;
import org.jppf.ui.options.event.*;
import org.jppf.ui.options.xml.OptionDescriptor.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class build options pages from XML descriptors.
 * @author Laurent Cohen
 */
public class OptionsPageBuilder {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(OptionsPageBuilder.class);
  /**
   * Base name used to localize labels and tooltips.
   */
  public static final String BASE_NAME = "org.jppf.ui.i18n.";
  /**
   * Base name used to localize labels and tooltips.
   */
  private String baseName = null;
  /**
   * Determines whether events should be triggered after the component is built.
   */
  private boolean eventEnabled = true;
  /**
   * Element factory used by this builder.
   */
  private OptionElementFactory factory = null;

  /**
   * Default constructor.
   */
  public OptionsPageBuilder() {
  }

  /**
   * Initialize this page builder.
   * @param enableEvents determines if events triggering should be performed once the page is built.
   */
  public OptionsPageBuilder(final boolean enableEvents) {
    this.eventEnabled = enableEvents;
  }

  /**
   * Build an option page from the specified XML descriptor.
   * @param content the text of the XML document to parse.
   * @param baseName the base path where the localization resources are located.
   * @return an <code>OptionElement</code> instance, or null if the page could not be build.
   * @throws Exception if an error was raised while parsing the xml document or building the page.
   */
  public OptionElement buildPageFromContent(final String content, final String baseName) throws Exception {
    this.baseName = baseName;
    OptionDescriptor desc = new OptionDescriptorParser().parse(new StringReader(content));
    if (desc == null) return null;
    OptionElement page = build(desc).get(0);
    if (eventEnabled) triggerInitialEvents(page);
    return page;
  }

  /**
   * Build an option page from an XML descriptor specified as a URL.
   * @param urlString the URL of the XML descriptor file.
   * @param baseName the base path where the localization resources are located.
   * @return an <code>OptionsPage</code> instance, or null if the page could not be build.
   * @throws Exception if an error was raised while parsing the xml document or building the page.
   */
  public OptionElement buildPageFromURL(final String urlString, final String baseName) throws Exception {
    if (urlString == null) return null;
    URL url = null;
    try {
      url = new URL(urlString);
    } catch(MalformedURLException e) {
      log.error(e.getMessage(), e);
      return null;
    }
    Reader reader = new InputStreamReader(url.openStream());
    try {
      return buildPageFromContent(FileUtils.readTextFile(reader), baseName);
    } finally {
      reader.close();
    }
  }

  /**
   * Build an option page from the specified XML descriptor.
   * @param xmlPath the path to the XML descriptor file.
   * @param baseName the base path where the localization resources are located.
   * @return an <code>OptionElement</code> instance, or null if the page could not be build.
   * @throws Exception if an error was raised while parsing the xml document or building the page.
   */
  public OptionElement buildPage(final String xmlPath, final String baseName) throws Exception {
    if (baseName == null) {
      String path = xmlPath.replace("\\", "/");
      int idx = path.lastIndexOf('/');
      this.baseName = BASE_NAME + ((idx < 0) ? path : path.substring(idx + 1));
      idx = this.baseName.lastIndexOf('.');
      if (idx >= 0) this.baseName = this.baseName.substring(0, idx);
    }
    else this.baseName = baseName;
    OptionDescriptor desc = new OptionDescriptorParser().parse(xmlPath);
    if (desc == null) return null;

    //if (eventEnabled) triggerInitialEvents(page);
    return build(desc).get(0);
  }

  /**
   * Trigger all initializers for all options, immediately after the page has been built.
   * This ensures the consistence of the UI's initial state.
   * @param elt the root element of the options on which to trigger the events.
   */
  public void triggerInitialEvents(final OptionElement elt) {
    triggerLifeCycleEvents(elt, true);
  }

  /**
   * Trigger all finalizers for all options, before they are disposed.
   * This enables saving some state that can be reloaded upon the next startup.
   * @param elt the root element of the options on which to trigger the events.
   */
  public void triggerFinalEvents(final OptionElement elt) {
    triggerLifeCycleEvents(elt, false);
  }

  /**
   * Trigger all events listeners for all options, immediately after the page has been built.
   * This ensures the consistence of the UI's initial state.
   * @param elt the root element of the options on which to trigger the events.
   * @param initial true to trigger the initializers, false to trigger the finalizers.
   */
  private static void triggerLifeCycleEvents(final OptionElement elt, final boolean initial) {
    if (elt == null) return;
    if ((elt instanceof OptionProperties) && initial) {
      OptionProperties op = (OptionProperties) elt;
      JComponent comp = op.getUIComponent();
      if (comp != null) {
        MouseListener listener = op.getMouseListener();
        if (listener != null) comp.addMouseListener(listener);
      }
    }
    final ValueChangeListener listener = initial ? elt.getInitializer() : elt.getFinalizer();
    if (listener != null) listener.valueChanged(new ValueChangeEvent(elt));
    if (elt instanceof OptionContainer) {
      for (OptionElement child: ((OptionContainer) elt).getChildren()) {
        triggerLifeCycleEvents(child, initial);
      }
    }
    if ((elt instanceof AbstractOptionElement) && initial) {
      OptionProperties op = (OptionProperties) elt;
      JComponent comp = op.getUIComponent();
      if (comp != null) comp.setName(elt.getStringPath());
    }
  }

  /**
   * Initialize the attributes common to all option elements from an option descriptor.
   * @param elt the element whose attributes are to be initialized.
   * @param desc the descriptor to get the attribute values from.
   * @throws Exception if an error was raised while building the page.
   */
  public void initCommonAttributes(final AbstractOptionElement elt, final OptionDescriptor desc) throws Exception {
    elt.setName(desc.name);
    elt.setLabel(LocalizationUtils.getLocalized(baseName, desc.name+".label", desc.getProperty("label")));
    elt.setToolTipText(LocalizationUtils.getLocalized(baseName, desc.name+".tooltip", desc.getProperty("tooltip")));
    elt.setScrollable(desc.getBoolean("scrollable", false));
    elt.setHorizontalScrollbar(desc.getBoolean("horizontalScrollbar", true));
    elt.setVerticalScrollbar(desc.getBoolean("verticalScrollbar", true));
    elt.setBordered(desc.getBoolean("bordered", false));
    elt.setLayoutConstraints(desc.getString("layoutConstraints", "fill, gapy 2!, insets 0 0 0 0"));
    elt.setComponentConstraints(desc.getString("componentConstraints", "growx"));
    elt.setDetachable(desc.getBoolean("detachable", true));
    elt.setIconPath(desc.getString("icon"));
    for (ScriptDescriptor script: desc.scripts) elt.getScripts().add(script);
    if (desc.initializer != null) elt.setInitializer(createListener(desc.initializer));
    if (desc.finalizer != null) elt.setFinalizer(createListener(desc.finalizer));
  }

  /**
   * Initialize the attributes common to all options from an option descriptor.
   * @param option the option whose attributes are to be initialized.
   * @param desc the descriptor to get the attribute values from.
   * @throws Exception if an error was raised while building the page.
   */
  public void initCommonOptionAttributes(final AbstractOption option, final OptionDescriptor desc) throws Exception {
    initCommonAttributes(option, desc);
    if (desc.mouseListener != null) {
      MouseListener listener = createMouseListener(option, desc.mouseListener);
      option.setMouseListener(listener);
    }
    //option.setEditable(desc.getBoolean("editable", false));
    option.setPersistent(desc.getBoolean("persistent", false));
    for (ListenerDescriptor listenerDesc: desc.listeners) {
      ValueChangeListener listener = createListener(listenerDesc);
      if (listener != null) option.addValueChangeListener(listener);
    }
  }

  /**
   * Create a value change listener from a listener descriptor.
   * @param listenerDesc the listener descriptor to get the listener properties from.
   * @return a ValueChangeListener instance.
   * @throws Exception if an error was raised while building the page.
   */
  public ValueChangeListener createListener(final ListenerDescriptor listenerDesc) throws Exception {
    ValueChangeListener listener = null;
    if (listenerDesc != null) {
      if ("java".equals(listenerDesc.type)) {
        Class clazz = Class.forName(listenerDesc.className);
        listener = (ValueChangeListener) clazz.newInstance();
      } else {
        ScriptDescriptor script = listenerDesc.script;
        listener = new ScriptedValueChangeListener(script.language, script.content);
      }
    }
    return listener;
  }

  /**
   * Create a value change listener from a listener descriptor.
   * @param option the option whose attributes are to be initialized.
   * @param listenerDesc the listener descriptor to get the listener properties from.
   * @return a ValueChangeListener instance.
   * @throws Exception if an error was raised while building the page.
   */
  public MouseListener createMouseListener(final AbstractOption option, final ListenerDescriptor listenerDesc) throws Exception {
    MouseListener listener = null;
    if (listenerDesc != null) {
      if ("java".equals(listenerDesc.type)) {
        Class clazz = Class.forName(listenerDesc.className);
        listener = (MouseListener) clazz.newInstance();
      } else {
        ScriptDescriptor script = listenerDesc.script;
        listener = new ScriptedMouseListener(option, script.language, script.content);
      }
    }
    return listener;
  }

  /**
   * Add all the children elements in a page.
   * @param desc the descriptor for the page.
   * @return an OptionElement instance.
   * @throws Exception if an error was raised while building the page.
   */
  public List<OptionElement> build(final OptionDescriptor desc) throws Exception {
    OptionElementFactory f = getFactory();
    List<OptionElement> list = new ArrayList<>();
    String type = desc.type;
    if ("page".equalsIgnoreCase(type)) list.add(f.buildPage(desc));
    else if ("SplitPane".equalsIgnoreCase(desc.type)) list.add(f.buildSplitPane(desc));
    else if ("TabbedPane".equalsIgnoreCase(desc.type)) list.add(f.buildTabbedPane(desc));
    else if ("Toolbar".equalsIgnoreCase(desc.type)) list.add(f.buildToolbar(desc));
    else if ("ToolbarSeparator".equalsIgnoreCase(desc.type)) list.add(f.buildToolbarSeparator(desc));
    else if ("Button".equalsIgnoreCase(desc.type)) list.add(f.buildButton(desc));
    else if ("TextArea".equalsIgnoreCase(desc.type)) list.add(f.buildTextArea(desc));
    else if ("CodeEditor".equalsIgnoreCase(desc.type)) list.add(f.buildCodeEditor(desc));
    else if ("Password".equalsIgnoreCase(desc.type)) list.add(f.buildPassword(desc));
    else if ("PlainText".equalsIgnoreCase(desc.type)) list.add(f.buildPlainText(desc));
    else if ("FormattedNumber".equalsIgnoreCase(desc.type)) list.add(f.buildFormattedNumber(desc));
    else if ("SpinnerNumber".equalsIgnoreCase(desc.type)) list.add(f.buildSpinnerNumber(desc));
    else if (StringUtils.isOneOf(desc.type, true, "Boolean", "Checkbox")) list.add(f.buildBoolean(desc));
    else if ("Radio".equalsIgnoreCase(desc.type)) list.add(f.buildRadio(desc));
    else if ("ComboBox".equalsIgnoreCase(desc.type)) list.add(f.buildComboBox(desc));
    else if ("Filler".equalsIgnoreCase(desc.type)) list.add(f.buildFiller(desc));
    else if ("List".equalsIgnoreCase(desc.type)) list.add(f.buildList(desc));
    else if ("FileChooser".equalsIgnoreCase(desc.type)) list.add(f.buildFileChooser(desc));
    else if ("Label".equalsIgnoreCase(desc.type)) list.add(f.buildLabel(desc));
    else if ("PickList".equalsIgnoreCase(desc.type)) list.add(f.buildPickList(desc));
    else if ("import".equalsIgnoreCase(desc.type)) list.addAll(f.loadImport(desc));
    else if ("Java".equalsIgnoreCase(desc.type)) list.add(f.buildJavaOption(desc));
    else if ("Custom".equalsIgnoreCase(desc.type)) list.add(f.buildCustomOption(desc));
    return list;
  }

  /**
   * Get the element factory used by this builder.
   * @return an <code>OptionElementFactory</code> instance.
   */
  public OptionElementFactory getFactory() {
    if (factory == null) factory = new OptionElementFactory(this);
    return factory;
  }

  /**
   * Get the base name used to localize labels and tooltips.
   * @return the base name as a string value.
   */
  public String getBaseName() {
    return baseName;
  }

  /**
   * Set the base name used to localize labels and tooltips.
   * @param baseName the base name as a string value.
   */
  public void setBaseName(final String baseName) {
    this.baseName = baseName;
  }

  /**
   * Determine whether events should be triggered after the component is built.
   * @return true if events should be triggered, false otherwise.
   */
  public boolean isEventEnabled() {
    return eventEnabled;
  }
}
