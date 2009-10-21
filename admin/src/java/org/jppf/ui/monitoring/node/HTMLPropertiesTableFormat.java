/*
 * JPPF.
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

package org.jppf.ui.monitoring.node;


/**
 * Implementation of a formatter that print the contents of a {@link java.util.Properties Properties} object as HTML.
 * @author Laurent Cohen
 */
public class HTMLPropertiesTableFormat extends PropertiesTableFormat
{
	/**
	 * Initialize this formatter with the specified title.
	 * @param docTitle the title of the whole document.
	 */
	public HTMLPropertiesTableFormat(String docTitle)
	{
		super(docTitle);
	}
	
	/**
	 * Write the prologue for the formatted text.
	 * @see org.jppf.ui.monitoring.node.PropertiesTableFormat#start()
	 */
	public void start()
	{
		sb.append("<html><head></head><body style=\"font-family: Arial; font-size: 12pt\">");
		sb.append("<h1>").append(docTitle).append("</h1>");
	}

	/**
	 * Write the epilogue for the formatted text.
	 * @see org.jppf.ui.monitoring.node.PropertiesTableFormat#end()
	 */
	public void end()
	{
		sb.append("</body></html>");
	}

	/**
	 * Write the prologue of a table.
	 * @param title the title for the table.
	 * @see org.jppf.ui.monitoring.node.PropertiesTableFormat#tableStart(java.lang.String)
	 */
	public void tableStart(String title)
	{
		sb.append("<h2>").append(title).append("</h2>");
		sb.append("<table cellspacing=\"0\" cellpadding=\"1\">");
	}

	/**
	 * Write the prologue of a table.
	 * @see org.jppf.ui.monitoring.node.PropertiesTableFormat#tableEnd()
	 */
	public void tableEnd()
	{
		sb.append("</table>");
	}

	/**
	 * Write the prologue of a table row.
	 * @see org.jppf.ui.monitoring.node.PropertiesTableFormat#rowStart()
	 */
	public void rowStart()
	{
		sb.append("<tr>");
	}

	/**
	 * Write the prologue of a table row.
	 * @see org.jppf.ui.monitoring.node.PropertiesTableFormat#rowEnd()
	 */
	public void rowEnd()
	{
		sb.append("</tr>");
	}

	/**
	 * Write the prologue of a table cell.
	 * @see org.jppf.ui.monitoring.node.PropertiesTableFormat#cellStart()
	 */
	public void cellStart()
	{
		sb.append("<td>");
	}

	/**
	 * Write the prologue of a table cell.
	 * @see org.jppf.ui.monitoring.node.PropertiesTableFormat#cellEnd()
	 */
	public void cellEnd()
	{
		sb.append("</td>");
	}

	/**
	 * Write the separator between 2 cells.
	 * @see org.jppf.ui.monitoring.node.PropertiesTableFormat#cellSeparator()
	 */
	public void cellSeparator()
	{
		sb.append("<td width\"5\"/");
	}
}
