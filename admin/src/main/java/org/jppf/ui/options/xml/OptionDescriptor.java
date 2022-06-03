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
package org.jppf.ui.options.xml;

import java.io.Serializable;
import java.util.*;

import org.jppf.utils.TypedProperties;

/**
 * Instances of this class are used to represented an XML document describing an options page.
 * @author Laurent Cohen
 */
public class OptionDescriptor extends TypedProperties
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Type of option component.
   * The possible values are:
   * <ul>
   * <li>&quot;page&quot; for an options page</li>
   * <li>&quot;FormattedNumber&quot; for a formatted number text field</li>
   * <li>&quot;SpinnerNumber&quot; for numbers edited with a spinner control</li>
   * <li>&quot;TextArea&quot; for a text area option</li>
   * <li>&quot;Boolean&quot; for a checkbox option</li>
   * <li>&quot;ComboBox&quot; for dropdown lists</li>
   * <li>&quot;PlainText&quot; for a simple, single-line text field</li>
   * <li>&quot;Button&quot; for a button</li>
   * <li>&quot;Password&quot; for a password field</li>
   * </ul>
   */
  public String type = null;
  /**
   * Name of the option element.
   */
  public String name = null;
  /**
   * Path to the folder containing the localized resource bundles for a page (doesn't apply to non-page elements).
   */
  public String i18n = null;
  /**
   * Children of this option element.
   */
  public List<OptionDescriptor> children = new ArrayList<>();
  /**
   * Listeners of this option element.
   */
  public List<ListenerDescriptor> listeners = new ArrayList<>();
  /**
   * Items used in list boxes or combo boxes.
   */
  public List<ItemDescriptor> items = new ArrayList<>();
  /**
   * Items used in list boxes or combo boxes.
   */
  public List<ScriptDescriptor> scripts = new ArrayList<>();
  /**
   * Initializer.
   */
  public ListenerDescriptor initializer = null;
  /**
   * Finalizer.
   */
  public ListenerDescriptor finalizer = null;
  /**
   * Mouse listener.
   */
  public ListenerDescriptor mouseListener = null;
  /**
   * Component shown only When debug attribute is {@code true}.
   */
  public boolean debug = false;

  /**
   * Descriptor for listeners set on option elements.
   */
  public static class ListenerDescriptor implements Serializable
  {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Type of listener.
     * The possible values are:
     * <ul>
     * <li>&quot;action&quot; for an action listener set on a button</li>
     * <li>&quot;value&quot; for a <code>ValueChangeListener</code></li>
     * </ul>
     */
    public String type = null;
    /**
     * Name of the listener class to instantiate to set the listener on an option element,
     * if this listener is Java-based.
     */
    public String className = null;
    /**
     * The source of the script to execute if this listener is script-based.
     */
    public ScriptDescriptor script = null;
  }

  /**
   * Descriptor for script elements.
   */
  public static class ScriptDescriptor implements Serializable
  {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * The content of the script to execute if this listener is script-based.
     */
    public String content = null;
    /**
     * The language in which the script is written, for instance JavaScript, Groovy, etc.
     */
    public String language = null;
    /**
     * The source of the script content, can be either a file path or a url, or null if the script is inlined.
     */
    public String source = null;
  }

  /**
   * Descriptor for listeners set on option elements.
   */
  public static class ItemDescriptor implements Serializable
  {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * The name of this item.
     */
    public String name = null;
    /**
     * Name of the listener class to instantiate to set the listener on an option element.
     */
    public String selected = null;
  }
}
