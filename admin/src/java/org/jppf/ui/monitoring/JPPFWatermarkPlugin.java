/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
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
import org.jvnet.substance.plugin.SubstanceWatermarkPlugin;
import org.jvnet.substance.watermark.WatermarkInfo;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFWatermarkPlugin implements SubstanceWatermarkPlugin
{
	/**
	 * @return .
	 * @see org.jvnet.substance.plugin.SubstanceWatermarkPlugin#getWatermarks()
	 */
	public Set<WatermarkInfo> getWatermarks()
	{
		WatermarkInfo wi = new WatermarkInfo("JPPF", JPPFTiledWatermark.class.getName());
		Set<WatermarkInfo> set = new HashSet<WatermarkInfo>();
		set.add(wi);
		return set;
	}

	/**
	 * @return .
	 * @see org.jvnet.substance.plugin.SubstanceWatermarkPlugin#getDefaultWatermarkClassName()
	 */
	public String getDefaultWatermarkClassName()
	{
		return JPPFTiledWatermark.class.getName();
	}
}
