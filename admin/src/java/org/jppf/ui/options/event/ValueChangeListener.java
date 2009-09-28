/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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

import java.util.EventListener;

/**
 * Listener interface for notification of changes to the value of an option.
 * @author Laurent Cohen
 */
public interface ValueChangeListener extends EventListener
{
	/**
	 * Method called when the value of an option has changed.
	 * @param event the event encapsulating the source of the event.
	 */
	void valueChanged(ValueChangeEvent event);
}
