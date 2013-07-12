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

import java.util.*;

/**
 * Container for other UI elements.
 * @author Laurent Cohen
 */
public abstract class AbstractOptionContainer extends AbstractOptionElement implements OptionContainer
{
  /**
   * The list of children of this options page.
   */
  protected List<OptionElement> children = new ArrayList<>();

  /**
   * Constructor provided as a convenience to facilitate the creation of
   * option elements through reflexion.
   */
  public AbstractOptionContainer()
  {
  }

  @Override
  public List<OptionElement> getChildren()
  {
    return children;
  }

  @Override
  public void add(final OptionElement element)
  {
    children.add(element);
    if (element instanceof AbstractOptionElement) ((AbstractOptionElement) element).setParent(this);
  }

  @Override
  public void remove(final OptionElement element)
  {
    children.remove(element);
    if (element instanceof AbstractOption) ((AbstractOption) element).setParent(null);
  }

  @Override
  public void setEnabled(final boolean enabled)
  {
    if (UIComponent != null) UIComponent.setEnabled(enabled);
    for (OptionElement elt: children) elt.setEnabled(enabled);
  }

  @Override
  public void setEventsEnabled(final boolean enabled)
  {
    for (OptionElement elt: children) elt.setEventsEnabled(enabled);
  }
}
