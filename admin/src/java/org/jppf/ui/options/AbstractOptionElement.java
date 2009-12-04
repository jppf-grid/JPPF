/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import javax.swing.tree.TreePath;

/**
 * Default abstract implementation of the <code>OptionElement</code> interface.
 * @author Laurent Cohen
 */
public abstract class AbstractOptionElement extends AbstractOptionProperties implements OptionElement
{
	/**
	 * The parent panel for this option element.
	 */
	protected OptionElement parent = null;
	/**
	 * The root of the option tree this option belongs to.
	 */
	protected OptionElement root = null;

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	protected AbstractOptionElement()
	{
	}

	/**
	 * Get the parent panel for this option.
	 * @return an <code>ElementOption</code> instance.
	 * @see org.jppf.ui.options.OptionElement#getParent()
	 */
	public OptionElement getParent()
	{
		return parent;
	}

	/**
	 * Set the parent panel for this option.
	 * @param parent an <code>ElementOption</code> instance.
	 */
	public void setParent(OptionElement parent)
	{
		this.parent = parent;
		if (parent == null) root = null;
	}

	/**
	 * Get the root of the option tree this option belongs to.
	 * @return a <code>OptionElement</code> instance. 
	 * @see org.jppf.ui.options.OptionElement#getRoot()
	 */
	public OptionElement getRoot()
	{
		if (root == null)
		{
			OptionElement elt = this;
			while (elt.getParent() != null) elt = elt.getParent();
			root = elt;
		}
		return root;
	}

	/**
	 * Get the path of this element in the option tree.
	 * @return a <code>TreePath</code> whose components are <code>OptionElement</code> instances. 
	 * @see org.jppf.ui.options.OptionElement#getPath()
	 */
	public TreePath getPath()
	{
		List<OptionElement> list = new ArrayList<OptionElement>();
		OptionElement elt = this;
		while (elt != null)
		{
			list.add(0, elt);
			elt = elt.getParent();
		}
		return new TreePath(list.toArray(new OptionElement[0]));
	}

	/**
	 * Get the path of this element in the option tree.
	 * @return a <code>TreePath</code> whose components are <code>OptionElement</code> instances. 
	 * @see org.jppf.ui.options.OptionElement#getPath()
	 */
	public String getStringPath()
	{
		List<String> list = new ArrayList<String>();
		OptionElement elt = this;
		while (elt != null)
		{
			list.add(0, elt.getName());
			elt = elt.getParent();
		}
		StringBuilder sb = new StringBuilder("/");
		for (int i=0; i<list.size(); i++) sb.append("/").append(list.get(i));
		return sb.toString();
	}

	/**
	 * Find the element with the specified path in the options tree. The path can be absolute,
	 * in which case it starts with a &quote;/&quote, otherwise it is considered relative to the requesting element.
	 * @param path the path of the element to find.
	 * @return an <code>OptionElement</code> instance, or null if no element could be found with the specfied path. 
	 * @see org.jppf.ui.options.OptionElement#findElement(java.lang.String)
	 */
	public OptionElement findElement(String path)
	{
		if (path == null) return null;
		else if ("".equals(path)) return this;
		if (path.startsWith("/")) return getRoot().findElement(path.substring(1));
		if (path.startsWith(".."))
		{
			int idx = path.indexOf('/');
			return (idx < 0) ? getParent() : getParent().findElement(path.substring(idx + 1));
		}
		int idx = path.indexOf('/');
		if (idx < 0) return getChildForName(path);
		String s = path.substring(0, idx);
		OptionElement child = getChildForName(s);
		return (child == null) ? null : child.findElement(path.substring(idx + 1));
	}

	/**
	 * Find the child element of this option element with the specified name.
	 * @param childName the name of the child to find.
	 * @return the child with the specified name, or null if this element is not an option page, or if no child
	 * was found with the given name.
	 */
	protected OptionElement getChildForName(String childName)
	{
		if (!(this instanceof OptionsPage)) return null;
		OptionsPage page = 	(OptionsPage) this;
		for (OptionElement elt: page.getChildren())
		{
			if (childName.equals(elt.getName())) return elt;
		}
		return null;
	}

	/**
	 * Get a string representation of this element.
	 * @return a strign providing information about this option element.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[").append((this instanceof OptionsPage) ? "Page" : "Option").append(" : ");
		sb.append(getClass().getName()).append("] ");
		sb.append("name=").append(name);
		sb.append("; label=").append(label);
		sb.append("; path=").append(getStringPath());
		return sb.toString();
	}

	/**
	 * Find all the elements with the specified name in the subtree of which this element is the root. 
	 * @param name the name of the elements to find.
	 * @return a list of <code>OptionElement</code> instances, or null if no element
	 * could be found with the specfied name. The resulting list can be empty, but never null.
	 * @see org.jppf.ui.options.OptionElement#findAllWithName(java.lang.String)
	 */
	public List<OptionElement> findAllWithName(String name)
	{
		if (name.startsWith("/"))
		{
			name = name.substring(1);
			return getRoot().findAllWithName(name);
		}
		List<OptionElement> list = new ArrayList<OptionElement>();
		findAll(name, list);
		return list;
	}

	/**
	 * Find the first element with the specified name in the subtree of which this element is the root. 
	 * @param name the name of the element to find.
	 * @return an <code>OptionElement</code> instance, or null if no element could be found with the specfied name.
	 * @see org.jppf.ui.options.OptionElement#findFirstWithName(java.lang.String)
	 */
	public OptionElement findFirstWithName(String name)
	{
		List<OptionElement> list = findAllWithName(name);
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * Find the last element with the specified name in the subtree of which this element is the root. 
	 * The notion of last element relates to a depth-first search in the tree. 
	 * @param name the name of the element to find.
	 * @return an <code>OptionElement</code> instance, or null if no element could be found with the specfied name.
	 * @see org.jppf.ui.options.OptionElement#findLastWithName(java.lang.String)
	 */
	public OptionElement findLastWithName(String name)
	{
		List<OptionElement> list = findAllWithName(name);
		return list.isEmpty() ? null : list.get(list.size() - 1);
	}

	/**
	 * Find all the elements with the specified name in the subtree of which
	 * this element is the root. 
	 * @param name the name of the elements to find.
	 * @param list a list of <code>OptionElement</code> instances, to fill with the elements found.
	 * could be found with the specfied name. The resulting list can be empty, but never null.
	 */
	protected void findAll(String name, List<OptionElement> list)
	{
		if (name.equals(getName())) list.add(this);
		if (this instanceof OptionsPage)
		{
			OptionsPage page = (OptionsPage) this;
			for (OptionElement elt: page.getChildren()) ((AbstractOptionElement) elt).findAll(name, list);
		}
	}
}
