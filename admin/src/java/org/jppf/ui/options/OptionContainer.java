/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

/**
 * Interface for dynamic UI components representing a page (or panel) container.
 * @author Laurent Cohen
 */
public interface OptionContainer extends OptionElement
{
  /**
   * Add an element to this options page.
   * @param element the element to add.
   */
  void add(OptionElement element);
  /**
   * Remove an element from this options page.
   * @param element the element to remove.
   */
  void remove(OptionElement element);
  /**
   * Get the options in this page.
   * @return a list of <code>Option</code> instances.
   */
  List<OptionElement> getChildren();
}
