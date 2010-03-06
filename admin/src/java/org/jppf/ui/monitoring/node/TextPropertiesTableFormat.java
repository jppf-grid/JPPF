/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.ui.monitoring.node;


/**
 * 
 * @author Laurent Cohen
 */
public class TextPropertiesTableFormat extends PropertiesTableFormat
{
	/**
	 * Initialize this formatter with the specified title.
	 * @param docTitle the title of the whole document.
	 */
	public TextPropertiesTableFormat(String docTitle)
	{
		super(docTitle);
	}
	
	/**
	 * Write the prologue for the formatted text.
	 */
	public void start()
	{
		sb.append(docTitle).append("\n");
	}

	/**
	 * Write the prologue of a table.
	 * @param title the title for the table.
	 */
	public void tableStart(String title)
	{
		sb.append("\n\n").append(title).append("\n\n");
	}

	/**
	 * Write the prologue of a table row.
	 */
	public void rowEnd()
	{
		sb.append("\n");
	}

	/**
	 * Write the separator between 2 cells.
	 */
	public void cellSeparator()
	{
		sb.append(" = ");
	}
}
