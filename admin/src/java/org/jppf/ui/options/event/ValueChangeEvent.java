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
package org.jppf.ui.options.event;

import java.util.EventObject;

import org.jppf.ui.options.OptionElement;

/**
 * Event generated when the value of an option has changed.
 * @author Laurent Cohen
 */
public class ValueChangeEvent extends EventObject
{
  /**
   * Initialize this event with the specified event source.
   * @param option the event source.
   */
  public ValueChangeEvent(final OptionElement option)
  {
    super(option);
  }

  /**
   * Get the source of this event as an option.
   * @return an <code>OptionElement</code> instance.
   */
  public OptionElement getOption()
  {
    return (OptionElement) getSource();
  }
}
