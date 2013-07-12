/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package org.jppf.ui.options;

import java.util.List;

import javax.swing.tree.TreePath;


/**
 * Base interface for all UI components dynamically created from XML descriptors.
 * @author Laurent Cohen
 */
public interface OptionElement extends OptionProperties
{
  /**
   * Get the root of the option tree this option belongs to.
   * @return a <code>OptionElement</code> instance.
   */
  OptionElement getRoot();
  /**
   * Get the parent page for this options page.
   * @return an <code>OptionsPage</code> instance.
   */
  OptionElement getParent();
  /**
   * Get the path of this element in the option tree.
   * @return a <code>TreePath</code> whose components are <code>OptionElement</code> instances.
   */
  TreePath getPath();
  /**
   * Get the path of this element in the option tree, represented as a string.
   * The string path is a sequence of element names separated by slashes.
   * @return a <code>TreePath</code> whose components are <code>OptionElement</code> instances.
   */
  String getStringPath();
  /**
   * Find the first element with the specified name in the subtree of which
   * this element is the root.
   * The notion of first element relates to a depth-first search in the tree.
   * @param name the name of the element to find.
   * @return an <code>OptionElement</code> instance, or null if no element
   * could be found with the specified name.
   */
  OptionElement findFirstWithName(String name);
  /**
   * Find the last element with the specified name in the subtree of which
   * this element is the root.
   * The notion of last element relates to a depth-first search in the tree.
   * @param name the name of the element to find.
   * @return an <code>OptionElement</code> instance, or null if no element
   * could be found with the specified name.
   */
  OptionElement findLastWithName(String name);
  /**
   * Find all the elements with the specified name in the subtree of which
   * this element is the root.
   * @param name the name of the elements to find.
   * @return a list of <code>OptionElement</code> instances, or null if no element
   * could be found with the specified name.
   */
  List<OptionElement> findAllWithName(String name);
  /**
   * Find the element with the specified path in the options tree.
   * The path can be absolute, in which case it starts with a &quot;/&quot;, otherwise it
   * is considered relative to the requesting element.
   * @param path the path of the element to find.
   * @return an <code>OptionElement</code> instance, or null if no element could be found with
   * the specified path.
   */
  OptionElement findElement(String path);
}
