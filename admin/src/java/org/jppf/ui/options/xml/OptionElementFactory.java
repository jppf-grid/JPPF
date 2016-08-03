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

import java.awt.Color;
import java.util.*;

import javax.swing.*;

import org.jppf.scripting.*;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.options.xml.OptionDescriptor.*;
import org.jppf.ui.picklist.PickList;
import org.jppf.utils.*;

/**
 * Factory class used to build UI elements from XML descriptors.
 * @author Laurent Cohen
 */
public class OptionElementFactory {
  /**
   * The builder using this factory.
   */
  private OptionsPageBuilder builder = null;

  /**
   * Initialize this factory.
   * @param builder the builder using this factory.
   */
  public OptionElementFactory(final OptionsPageBuilder builder) {
    this.builder = builder;
  }

  /**
   * Get a page builder using the specified path for localized resource bundles.
   * @param i18n the path for localized resource bundles.
   * @return an {@link OptionsPageBuilder} instance.
   */
  private OptionsPageBuilder getOrCreateBuilder(final String i18n) {
    if ((i18n == null) || "".equals(i18n.trim()) || i18n.equals(builder.getBaseName())) return this.builder;
    OptionsPageBuilder newBuilder = new OptionsPageBuilder(builder.isEventEnabled());
    newBuilder.setBaseName(i18n);
    return newBuilder;
  }

  /**
   * Build an option page from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>OptionsPage</code> instance, or null if the page could not be build.
   * @throws Exception if an error was raised while building the page.
   */
  public OptionElement buildPage(final OptionDescriptor desc) throws Exception {
    OptionsPageBuilder tmpBuilder = getOrCreateBuilder(desc.i18n);
    OptionPanel page = new OptionPanel();
    page.setEventsEnabled(false);
    tmpBuilder.initCommonAttributes(page, desc);
    page.createUI();
    for (OptionDescriptor child: desc.children) {
      for (OptionElement elt: tmpBuilder.build(child)) page.add(elt);
    }
    page.setEventsEnabled(true);
    return page;
  }

  /**
   * Build a split pane option from the specified option descriptor.
   * @param desc the descriptor to get the properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public OptionElement buildSplitPane(final OptionDescriptor desc) throws Exception {
    OptionsPageBuilder tmpBuilder = getOrCreateBuilder(desc.i18n);
    SplitPaneOption option = new SplitPaneOption();
    tmpBuilder.initCommonAttributes(option, desc);
    option.setDividerWidth(desc.getInt("dividerWidth", 4));
    option.setResizeWeight(desc.getDouble("resizeWeight", 0.5d));
    String s = desc.getString("orientation", "horizontal");
    option.setOrientation("horizontal".equalsIgnoreCase(s) ? SplitPaneOption.HORIZONTAL : SplitPaneOption.VERTICAL);
    option.createUI();
    for (OptionDescriptor child: desc.children) {
      for (OptionElement elt: tmpBuilder.build(child)) option.add(elt);
    }
    return option;
  }

  /**
   * Build a toolbar option from the specified option descriptor.
   * @param desc the descriptor to get the properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public OptionElement buildToolbar(final OptionDescriptor desc) throws Exception {
    OptionsPageBuilder tmpBuilder = getOrCreateBuilder(desc.i18n);
    ToolbarOption option = new ToolbarOption();
    option.setEventsEnabled(false);
    tmpBuilder.initCommonAttributes(option, desc);
    option.createUI();
    for (OptionDescriptor child: desc.children) {
      for (OptionElement elt: tmpBuilder.build(child)) option.add(elt);
    }
    option.setEventsEnabled(true);
    return option;
  }

  /**
   * Build a toolbar option from the specified option descriptor.
   * @param desc the descriptor to get the properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public OptionElement buildTabbedPane(final OptionDescriptor desc) throws Exception {
    OptionsPageBuilder tmpBuilder = getOrCreateBuilder(desc.i18n);
    TabbedPaneOption option = new TabbedPaneOption();
    option.setEventsEnabled(false);
    tmpBuilder.initCommonAttributes(option, desc);
    option.createUI();
    for (OptionDescriptor child: desc.children) {
      for (OptionElement elt: tmpBuilder.build(child)) option.add(elt);
    }
    option.setEventsEnabled(true);
    return option;
  }

  /**
   * Build a button option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildButton(final OptionDescriptor desc) throws Exception {
    ButtonOption option = new ButtonOption();
    option.setEventsEnabled(false);
    builder.initCommonOptionAttributes(option, desc);
    option.setToggle(desc.getBoolean("toggle", false));
    option.createUI();
    option.setEventsEnabled(true);
    return option;
  }

  /**
   * Build a label option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildLabel(final OptionDescriptor desc) throws Exception {
    LabelOption option = new LabelOption();
    option.setEventsEnabled(false);
    builder.initCommonOptionAttributes(option, desc);
    option.setValue(desc.getProperty("value"));
    option.createUI();
    option.setEventsEnabled(true);
    return option;
  }

  /**
   * Build a text area option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildTextArea(final OptionDescriptor desc) throws Exception {
    TextAreaOption option = new TextAreaOption();
    option.setEventsEnabled(false);
    builder.initCommonOptionAttributes(option, desc);
    option.setEditable(desc.getBoolean("editable", false));
    option.setTimestampFormat(desc.getProperty("timestamp.format", null));
    option.createUI();
    option.setEventsEnabled(true);
    return option;
  }

  /**
   * Build a code editor option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildCodeEditor(final OptionDescriptor desc) throws Exception {
    CodeEditorOption option = new CodeEditorOption();
    option.setEventsEnabled(false);
    builder.initCommonOptionAttributes(option, desc);
    option.setEditable(desc.getBoolean("editable", false));
    option.setLanguage(desc.getString("language", "text/xml"));
    option.createUI();
    option.setEventsEnabled(true);
    return option;
  }

  /**
   * Build a password option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildPassword(final OptionDescriptor desc) throws Exception {
    PasswordOption option = new PasswordOption();
    option.setEventsEnabled(false);
    builder.initCommonOptionAttributes(option, desc);
    option.setValue(desc.getProperty("value"));
    option.createUI();
    option.setEventsEnabled(true);
    return option;
  }

  /**
   * Build a plain text option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildPlainText(final OptionDescriptor desc) throws Exception {
    PlainTextOption option = new PlainTextOption();
    option.setEventsEnabled(false);
    builder.initCommonOptionAttributes(option, desc);
    option.setValue(desc.getProperty("value"));
    option.createUI();
    option.setColumns(desc.getInt("columns", 16));
    option.setEventsEnabled(true);
    return option;
  }

  /**
   * Build a formatted number option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildFormattedNumber(final OptionDescriptor desc) throws Exception {
    FormattedNumberOption option = new FormattedNumberOption();
    option.setEventsEnabled(false);
    builder.initCommonOptionAttributes(option, desc);
    option.setValue(new Double(desc.getDouble("value")));
    option.setPattern(desc.getProperty("pattern"));
    option.createUI();
    option.setEventsEnabled(true);
    option.setEditable(desc.getBoolean("editable", true));
    return option;
  }

  /**
   * Build a spinner number option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildSpinnerNumber(final OptionDescriptor desc) throws Exception {
    SpinnerNumberOption option = new SpinnerNumberOption();
    option.setEventsEnabled(false);
    builder.initCommonOptionAttributes(option, desc);
    option.setStep(desc.getDouble("step", 1d));
    option.setMin(desc.getDouble("minValue", 0d));
    option.setMax(desc.getDouble("maxValue", 1d));
    option.setValue(desc.getDouble("value", 0d));
    option.setPattern(desc.getString("pattern", "0"));
    option.createUI();
    option.setEventsEnabled(true);
    return option;
  }

  /**
   * Build a check box option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildBoolean(final OptionDescriptor desc) throws Exception {
    BooleanOption option = new BooleanOption();
    option.setEventsEnabled(false);
    builder.initCommonOptionAttributes(option, desc);
    option.setValue(Boolean.valueOf(desc.getBoolean("value")));
    option.createUI();
    option.setEventsEnabled(true);
    return option;
  }

  /**
   * Build a radio button option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildRadio(final OptionDescriptor desc) throws Exception {
    RadioButtonOption option = new RadioButtonOption();
    option.setEventsEnabled(false);
    builder.initCommonOptionAttributes(option, desc);
    option.setValue(Boolean.valueOf(desc.getBoolean("value")));
    option.createUI();
    option.setEventsEnabled(true);
    return option;
  }

  /**
   * Build a combo box option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildComboBox(final OptionDescriptor desc) throws Exception {
    ComboBoxOption option = new ComboBoxOption();
    option.setEventsEnabled(false);
    builder.initCommonOptionAttributes(option, desc);
    List<Object> items = new ArrayList<>();
    for (ItemDescriptor itemDesc: desc.items) items.add(itemDesc.name);
    option.setItems(items);
    option.setValue(desc.getProperty("value"));
    option.createUI();
    option.setEventsEnabled(true);
    return option;
  }

  /**
   * Build a list option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildList(final OptionDescriptor desc) throws Exception {
    ListOption option = new ListOption();
    option.setEventsEnabled(false);
    builder.initCommonOptionAttributes(option, desc);
    List<Object> items = new ArrayList<>();
    for (ItemDescriptor itemDesc: desc.items) items.add(itemDesc.name);
    int selMode = "single".equals(desc.getProperty("selection"))
    ? ListSelectionModel.SINGLE_SELECTION : ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
    option.setSelMode(selMode);
    option.setItems(items);
    option.setValue(new ArrayList<>());
    option.createUI();
    option.setEventsEnabled(true);
    return option;
  }

  /**
   * Build a filler option from the specified option descriptor.
   * @param desc the descriptor to get the properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildFiller(final OptionDescriptor desc) throws Exception {
    int width = desc.getInt("width", 1);
    int height = desc.getInt("height", 1);
    return new FillerOption(width, height);
  }

  /**
   * Build a toolbar separator option from the specified option descriptor.
   * @param desc the descriptor to get the properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildToolbarSeparator(final OptionDescriptor desc) throws Exception {
    int width = desc.getInt("width", 5);
    int height = desc.getInt("height", 5);
    String text = desc.getString("text", " ");
    return new ToolbarSeparatorOption(text, width, height);
  }

  /**
   * Build a file chooser option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildFileChooser(final OptionDescriptor desc) throws Exception   {
    FileChooserOption option = new FileChooserOption();
    builder.initCommonOptionAttributes(option, desc);
    int dlgType = "open".equals(desc.getProperty("type")) ? FileChooserOption.OPEN : FileChooserOption.SAVE;
    option.setDialogType(dlgType);
    option.setExtensions(desc.getProperty("extensions"));
    option.setValue(desc.getProperty("value"));
    option.createUI();
    return option;
  }

  /**
   * Build a pick list option from the specified option descriptor.
   * @param desc the descriptor to get the page properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildPickList(final OptionDescriptor desc) throws Exception   {
    PickListOption option = new PickListOption();
    builder.initCommonOptionAttributes(option, desc);
    option.createUI();
    PickList<?> pickList = option.getPickList();
    String s = desc.getProperty("leftTitle", null);
    if (s != null) pickList.setLeftTitle(LocalizationUtils.getLocalized(builder.getBaseName(), s));
    s = desc.getProperty("rightTitle", null);
    if (s != null) pickList.setRightTitle(LocalizationUtils.getLocalized(builder.getBaseName(), s));
    return option;
  }

  /**
   * Build a toolbar option from the specified option descriptor.
   * @param desc the descriptor to get the properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public List<OptionElement> loadImport(final OptionDescriptor desc) throws Exception {
    OptionsPageBuilder builder = new OptionsPageBuilder(true);
    List<OptionElement> list = new ArrayList<>();
    String source = desc.getProperty("source");
    String location = desc.getProperty("location");
    if (StringUtils.isOneOf(source, true, "url", "file")) {
      boolean enabled = true;
      String pluggableViewName = desc.getProperty("pluggableView");
      if ((pluggableViewName != null) && !"".equals(pluggableViewName.trim())) {
        enabled = JPPFConfiguration.getProperties().getBoolean(String.format("jppf.admin.console.view.%s.enabled", pluggableViewName), true);
      }
      if (enabled) {
        OptionElement elt = "url".equalsIgnoreCase(source) ? builder.buildPageFromURL(location, builder.getBaseName()) : builder.buildPage(location, null);
        list.add(elt);
        OptionsHandler.getPluggableViewHandler().addView(pluggableViewName, elt);
        if (JPPFConfiguration.getProperties().getBoolean("jppf.ui.debug.enabled", false)) addDebugComp(elt, source, location);
      }
    } else if ("plugin".equalsIgnoreCase(source)) {
      List<String> pathList = new ServiceFinder().findServiceDefinitions(location, getClass().getClassLoader());
      Set<String> names = new HashSet<>();
      for (String def: pathList) {
        OptionElement elt = builder.buildPage(def, null);
        if (!names.contains(elt.getName())) {
          names.add(elt.getName());
          list.add(elt);
          if (JPPFConfiguration.getProperties().getBoolean("jppf.ui.debug.enabled", false)) addDebugComp(elt, source, def);
        }
      }
    } else if ("script".equalsIgnoreCase(source)) {
      if (!desc.scripts.isEmpty()) {
        ScriptDescriptor scriptDesc = desc.scripts.get(0);
        ScriptRunner runner = null;
        try {
          runner = ScriptRunnerFactory.getScriptRunner(scriptDesc.language);
          String path = (String) runner.evaluate(scriptDesc.content, new HashMap<String, Object>());
          if (path != null) list.add(builder.buildPage(path, null));
        } finally {
          ScriptRunnerFactory.releaseScriptRunner(runner);
        }
      }
    }
    return list;
  }

  /**
   * Add a component from which to get a popup menu to reload the page.
   * @param elt the option to debug.
   * @param source determines whether the XML is loaded from a url or file location.
   * @param location where to load the xml descriptor from.
   */
  void addDebugComp(final OptionElement elt, final String source, final String location) {
    if (elt instanceof TabbedPaneOption) return;
    JLabel label = new JLabel(" X ");
    label.setOpaque(true);
    label.setBackground(Color.RED);
    JComponent comp = elt.getUIComponent();
    if (comp instanceof JScrollPane) comp = (JComponent) ((JScrollPane) comp).getViewport().getView();
    comp.add(label, "w 20:20:20, h 12:12:12", 0);
    label.addMouseListener(new DebugMouseListener(elt, source, location));
  }

  /**
   * Build an option with a UI component created from a Java class.
   * @param desc the descriptor to get the properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildJavaOption(final OptionDescriptor desc) throws Exception {
    JavaOption option = new JavaOption();
    builder.initCommonOptionAttributes(option, desc);
    option.setClassName(desc.getProperty("class"));
    option.setMouseListenerClassName(desc.getProperty("mouseListenerClass"));
    option.createUI();
    return option;
  }

  /**
   * Build a custom option implemented as a Java class.
   * @param desc the descriptor to get the properties from.
   * @return an <code>Option</code> instance, or null if the option could not be build.
   * @throws Exception if an error was raised while building the option.
   */
  public Option buildCustomOption(final OptionDescriptor desc) throws Exception {
    String className = desc.getString("impl.class");
    Class<?> clazz = Class.forName(className);
    AbstractOption option = (AbstractOption) clazz.newInstance();
    builder.initCommonOptionAttributes(option, desc);
    option.createUI();
    return option;
  }
}
