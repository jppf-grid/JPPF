/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
/*
 * %W% %E%
 *
 * Copyright 1997, 1998 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer. 
 *   
 * - Redistribution in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution. 
 *   
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.  
 * 
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT OF OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THIS SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE 
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,   
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER  
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF 
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS 
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */
package org.jppf.ui.treetable;

import java.io.File;
import java.util.*;

/**
 * FileSystemModel is a TreeTableModel representing a hierarchical file system. Nodes in the FileSystemModel are
 * FileNodes which, when they are directory nodes, cache their children to avoid repeatedly querying the real file
 * system.
 * 
 * @version %I% %G%
 * 
 * @author Philip Milne
 * @author Scott Violet
 */
public class FileSystemModel extends AbstractTreeTableModel implements TreeTableModel
{
	/**
	 * Names of the columns.
	 */
	static protected String[] cNames = { "Name", "Size", "Type", "Modified" };

	/**
	 * Types of the columns.
	 */
	static protected Class[] cTypes = { TreeTableModel.class, Integer.class, String.class, Date.class };

	/**
	 * The the returned file length for directories.
	 */
	public static final Integer ZERO = Integer.valueOf(0);

	/**
	 * Default constructor.
	 */
	public FileSystemModel()
	{
		super(new FileNode(new File(File.separator)));
	}

	/**
	 * Get the file a node points to.
	 * @param node the node to get the file from.
	 * @return a <code>File</code> instance.
	 */
	protected File getFile(Object node)
	{
		FileNode fileNode = ((FileNode) node);
		return fileNode.getFile();
	}

	/**
	 * Get the children of the specified node.
	 * @param node the node to get the children of.
	 * @return the children as an array of objects
	 */
	protected Object[] getChildren(Object node)
	{
		FileNode fileNode = ((FileNode) node);
		return fileNode.getChildren();
	}

	//
	// The TreeModel interface
	//

	/**
	 * {@inheritDoc}
	 */
	public int getChildCount(Object node)
	{
		Object[] children = getChildren(node);
		return (children == null) ? 0 : children.length;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getChild(Object node, int i)
	{
		return getChildren(node)[i];
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isLeaf(Object node)
	{
		// The superclass's implementation would work, but this is more efficient.
		return getFile(node).isFile();
	}

	//
	// The TreeTableNode interface.
	//

	/**
	 * {@inheritDoc}
	 */
	public int getColumnCount()
	{
		return cNames.length;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getColumnName(int column)
	{
		return cNames[column];
	}

	/**
	 * {@inheritDoc}
	 */
	public Class getColumnClass(int column)
	{
		return cTypes[column];
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getValueAt(Object node, int column)
	{
		File file = getFile(node);
		try
		{
			switch (column)
			{
				case 0:
					return file.getName();
				case 1:
					return file.isFile() ? Integer.valueOf((int) file.length()) : ZERO;
				case 2:
					return file.isFile() ? "File" : "Directory";
				case 3:
					return new Date(file.lastModified());
			}
		}
		catch (SecurityException se)
		{
		}

		return null;
	}
}

/**
 * A FileNode is a derivative of the File class - though we delegate to the File object rather than subclassing it. It
 * is used to maintain a cache of a directory's children and therefore avoid repeated access to the underlying file
 * system during rendering.
 */
class FileNode
{
	/**
	 * The referenced file.
	 */
	File file;
	/**
	 * This node's children.
	 */
	Object[] children;

	/**
	 * Initialize this node with thespecified file.
	 * @param file the referenced file.
	 */
	public FileNode(File file)
	{
		this.file = file;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString()
	{
		return file.getName();
	}

	/**
	 * Get the referenced file.
	 * @return a {@link File} instance.
	 */
	public File getFile()
	{
		return file;
	}

	/**
	 * Loads the children, caching the results in the children ivar.
	 * @return the children as an array.
	 */
	protected Object[] getChildren()
	{
		if (children != null)
		{
			return children;
		}
		try
		{
			String[] files = file.list();
			if (files != null)
			{
				Arrays.sort(files);
				// fileMS.sort(files);
				children = new FileNode[files.length];
				String path = file.getPath();
				for (int i = 0; i < files.length; i++)
				{
					File childFile = new File(path, files[i]);
					children[i] = new FileNode(childFile);
				}
			}
		}
		catch (SecurityException se)
		{
		}
		return children;
	}
}
