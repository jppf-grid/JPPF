/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.ui.options;

import java.awt.Dimension;
import java.util.*;
import javax.swing.JComponent;
import javax.swing.tree.TreePath;
import org.jppf.ui.options.event.ValueChangeListener;
import org.jppf.ui.options.xml.OptionDescriptor.ScriptDescriptor;

/**
 * Default abstract implementation of the <code>OptionElement</code> interface.
 * @author Laurent Cohen
 */
public abstract class AbstractOptionElement implements OptionElement
{
	/**
	 * The label or title displayed with the UI component.
	 */
	protected String label = null;
	/**
	 * The name of this option element.
	 */
	protected String name;
	/**
	 * The orientation of this panel's layout.
	 */
	protected int orientation = HORIZONTAL;
	/**
	 * The tooltip text displayed with the UI component.
	 */
	protected String toolTipText = null;
	/**
	 * The parent panel for this option element.
	 */
	protected OptionElement parent = null;
	/**
	 * The root of the option tree this option belongs to.
	 */
	protected OptionElement root = null;
	/**
	 * Get the UI component for this option element.
	 */
	protected JComponent UIComponent = null;
	/**
	 * Determines whether this page should be enclosed within a scroll pane.
	 */
	protected boolean scrollable = false;
	/**
	 * Determines whether this option has a border around it.
	 */
	protected boolean bordered = false;
	/**
	 * Preferred width of the UI component.
	 */
	protected int width = -1;
	/**
	 * Preferred height of the UI component.
	 */
	protected int height = -1;
	/**
	 * Scripts used by this option or its children.
	 */
	protected List<ScriptDescriptor> scripts = new ArrayList<ScriptDescriptor>();
	/**
	 * The action to fire immediately after the page is built, allowing to
	 * perform initializations before the page is displayed and used.
	 */
	protected ValueChangeListener initializer = null;

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	protected AbstractOptionElement()
	{
	}

	/**
	 * Create the UI components for this option.
	 */
	public abstract void createUI();

	/**
	 * Get the label displayed with the UI component.
	 * @return the label as a string.
	 * @see org.jppf.ui.options.Option#getLabel()
	 */
	public String getLabel()
	{
		return label;
	}

	/**
	 * Set the label displayed with the UI component.
	 * @param label the label as a string.
	 */
	public void setLabel(String label)
	{
		this.label = label;
	}

	/**
	 * Get the name of this option.
	 * @return the name as a string.
	 * @see org.jppf.ui.options.Option#getName()
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the name of this option.
	 * @param name the name as a string.
	 */
	public void setName(String name)
	{
		this.name = name;
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
	 * Get the UI component for this option.
	 * @return a <code>JComponent</code> instance.
	 * @see org.jppf.ui.options.Option#getUIComponent()
	 */
	public JComponent getUIComponent()
	{
		return UIComponent;
	}

	/**
	 * Set the UI component for this option.
	 * @param component a <code>JComponent</code> instance.
	 */
	public void setUIComponent(JComponent component)
	{
		UIComponent = component;
	}

	/**
	 * Determine the orientation of this page's layout.
	 * @return one of {@link #HORIZONTAL} or {@link #VERTICAL}.
	 * @see org.jppf.ui.options.OptionElement#getOrientation()
	 */
	public int getOrientation()
	{
		return orientation;
	}

	/**
	 * Set the orientation of this page's layout.
	 * @param orientation one of {@link #HORIZONTAL} or {@link #VERTICAL}.
	 */
	public void setOrientation(int orientation)
	{
		this.orientation = orientation;
	}

	/**
	 * Get the tooltip text displayed with the UI component.
	 * @return the tooltip as a string.
	 * @see org.jppf.ui.options.OptionElement#getToolTipText()
	 */
	public String getToolTipText()
	{
		return toolTipText;
	}

	/**
	 * Set the tooltip text displayed with the UI component.
	 * @param tooltip the tooltip as a string.
	 */
	public void setToolTipText(String tooltip)
	{
		if (((tooltip == null) || "".equals(tooltip.trim())))
		{
			tooltip = null;
		}
		else if (tooltip.indexOf("\\n") >= 0)
		{
			String s = tooltip.replace("\\n", "<br>");
			tooltip = "<html>"+s+"</html>";
		}
		this.toolTipText = tooltip;
	}

	/**
	 * Determine whether this page should be enclosed within a scroll pane.
	 * @return true if the page is to be enclosed in a scroll pabe, false otherwise.
	 * @see org.jppf.ui.options.OptionElement#isScrollable()
	 */
	public boolean isScrollable()
	{
		return scrollable;
	}

	/**
	 * Determine whether this page should be enclosed within a scroll pane.
	 * @param scrollable true if the page is to be enclosed in a scroll pane, false otherwise.
	 */
	public void setScrollable(boolean scrollable)
	{
		this.scrollable = scrollable;
	}

	/**
	 * Determine whether this page has a border around it.
	 * @return true if the page has a border, false otherwise.
	 * @see org.jppf.ui.options.OptionElement#isBordered()
	 */
	public boolean isBordered()
	{
		return bordered;
	}

	/**
	 * Determine whether this page has a border around it.
	 * @param bordered true if the page has a border, false otherwise.
	 */
	public void setBordered(boolean bordered)
	{
		this.bordered = bordered;
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
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<list.size(); i++)
		{
			sb.append("/").append(list.get(i));
		}
		return sb.toString();
	}

	/**
	 * Find the element with the specified path in the options tree.
	 * The path can be absolute, in which case it starts with a &quote;/&quote, otherwise it
	 * is considered relative to the requesting element.
	 * @param path the path of the element to find.
	 * @return an <code>OptionElement</code> instance, or null if no element could be found with
	 * the specfied path. 
	 * @see org.jppf.ui.options.OptionElement#findElement(java.lang.String)
	 */
	public OptionElement findElement(String path)
	{
		if (path == null) return null;
		else if ("".equals(path)) return this;
		if (path.startsWith("/"))
		{
			return getRoot().findElement(path.substring(1));
		}
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
		if (this instanceof OptionsPage)
			sb.append("[Page] ");
		else sb.append("[Option : ").append(getClass().getName()).append("] ");
		sb.append("name=").append(name);
		sb.append("; label=").append(label);
		return sb.toString();
	}

	/**
	 * Find all the elements with the specified name in the subtree of which
	 * this element is the root. 
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
	 * Find the first element with the specified name in the subtree of which
	 * this element is the root. 
	 * @param name the name of the element to find.
	 * @return an <code>OptionElement</code> instance, or null if no element
	 * could be found with the specfied name.
	 * @see org.jppf.ui.options.OptionElement#findFirstWithName(java.lang.String)
	 */
	public OptionElement findFirstWithName(String name)
	{
		List<OptionElement> list = findAllWithName(name);
		return list.isEmpty() ? null : list.get(0);
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

	/**
	 * Get the preferred height of the UI component.
	 * @return the height as an int value.
	 */
	public int getHeight()
	{
		return height;
	}

	/**
	 * Set the preferred height of the UI component.
	 * @return
	 * @param height the height as an int value.
	 */
	public void setHeight(int height)
	{
		this.height = height;
		if ((height >= 0) && (UIComponent != null))
		{
			Dimension d = UIComponent.getPreferredSize();
			UIComponent.setPreferredSize(new Dimension(d.width, height));
		}
	}

	/**
	 * Get the preferred width of the UI component.
	 * @return the width as an int value.
	 */
	public int getWidth()
	{
		return width;
	}

	/**
	 * Get the preferred width of the UI component.
	 * @param width the width as an int value.
	 */
	public void setWidth(int width)
	{
		this.width = width;
		if ((width >= 0) && (UIComponent != null))
		{
			Dimension d = UIComponent.getPreferredSize();
			UIComponent.setPreferredSize(new Dimension(width, d.height));
		}
	}

	/**
	 * Get the scripts used by this option or its children.
	 * @return a list of <code>ScriptDescriptor</code> instances.
	 * @see org.jppf.ui.options.OptionElement#getScripts()
	 */
	public List<ScriptDescriptor> getScripts()
	{
		return scripts;
	}

	/**
	 * Get the initializer for this option.
	 * @return a <code>ValueChangeListener</code> instance. 
	 * @see org.jppf.ui.options.OptionElement#getInitializer()
	 */
	public ValueChangeListener getInitializer()
	{
		return initializer;
	}

	/**
	 * Set the initializer for this option.
	 * @param initializer a <code>ValueChangeListener</code> instance.
	 */
	public void setInitializer(ValueChangeListener initializer)
	{
		this.initializer = initializer;
	}
}
