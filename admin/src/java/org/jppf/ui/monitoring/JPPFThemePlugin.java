/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.ui.monitoring;

import java.util.*;
import org.jvnet.substance.plugin.SubstanceThemePlugin;
import org.jvnet.substance.theme.ThemeInfo;
import org.jvnet.substance.theme.SubstanceTheme.ThemeKind;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFThemePlugin implements SubstanceThemePlugin
{
	/**
	 * 
	 * @return .
	 * @see org.jvnet.substance.plugin.SubstanceThemePlugin#getDefaultThemeClassName()
	 */
	public String getDefaultThemeClassName()
	{
		return JPPFTheme.class.getName();
	}

	/**
	 * 
	 * @return .
	 * @see org.jvnet.substance.plugin.SubstanceThemePlugin#getThemes()
	 */
	public Set<ThemeInfo> getThemes()
	{
		Set<ThemeInfo> set = new HashSet<ThemeInfo>();
		set.add(new ThemeInfo("JPPF", JPPFTheme.class.getName(), ThemeKind.COLD));
		return set;
	}
}
