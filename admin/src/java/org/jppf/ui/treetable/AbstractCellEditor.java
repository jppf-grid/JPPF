/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
 * Copyright 1998 Sun Microsystems, Inc.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jppf.ui.treetable;

import java.util.EventObject;

import javax.swing.CellEditor;
import javax.swing.event.*;

/**
 * Abstract cell editor for the tree table.
 * @author Philip Milne
 * @author Scott Violet
 */
public abstract class AbstractCellEditor implements CellEditor
{
  /**
   * The list of listeners for this cell editor.
   */
  protected EventListenerList listenerList = new EventListenerList();

  /**
   * Returns the value contained in the editor. This method always returns null.
   * @return <code>null</code>.
   * @see javax.swing.CellEditor#getCellEditorValue()
   */
  @Override
  public Object getCellEditorValue()
  {
    return null;
  }

  /**
   * Asks the editor if it can start editing using <code>anEvent</code>. This method always returns true.
   * @param	event	the event the editor should use to consider whether to begin editing or not.
   * @return <code>true</code>.
   * @see javax.swing.CellEditor#isCellEditable(java.util.EventObject)
   */
  @Override
  public boolean isCellEditable(final EventObject event)
  {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean shouldSelectCell(final EventObject anEvent)
  {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopCellEditing()
  {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cancelCellEditing()
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addCellEditorListener(final CellEditorListener l)
  {
    listenerList.add(CellEditorListener.class, l);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeCellEditorListener(final CellEditorListener l)
  {
    listenerList.remove(CellEditorListener.class, l);
  }

  /**
   * Notify all listeners that have registered interest for notification on this event type.
   * @see EventListenerList
   */
  protected void fireEditingStopped()
  {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2)
    {
      if (listeners[i] == CellEditorListener.class)
      {
        ((CellEditorListener) listeners[i + 1]).editingStopped(new ChangeEvent(this));
      }
    }
  }

  /**
   * Notify all listeners that have registered interest for notification on this event type.
   * @see EventListenerList
   */
  protected void fireEditingCanceled()
  {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2)
    {
      if (listeners[i] == CellEditorListener.class)
      {
        ((CellEditorListener) listeners[i + 1]).editingCanceled(new ChangeEvent(this));
      }
    }
  }
}
