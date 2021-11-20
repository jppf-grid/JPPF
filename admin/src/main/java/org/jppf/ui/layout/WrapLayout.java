/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
package org.jppf.ui.layout;

import java.awt.*;

import javax.swing.*;

import org.jppf.ui.utils.GuiUtils;

/**
 * FlowLayout subclass that fully supports wrapping of components.
 * <p>Disclaimer: this class is based on the code provided at <a href="http://tips4java.wordpress.com/2008/11/06/wrap-layout/">http://tips4java.wordpress.com/2008/11/06/wrap-layout/</a>.
 */
public class WrapLayout extends FlowLayout {
  /**
   * Constructs a new {@code WrapLayout} with a left
   * alignment and a default 5-unit horizontal and vertical gap.
   */
  public WrapLayout() {
    super();
  }

  /**
   * Constructs a new {@code FlowLayout} with the specified
   * alignment and a default 5-unit horizontal and vertical gap.
   * The value of the alignment argument must be one of
   * {@code WrapLayout}, {@code WrapLayout},
   * or {@code WrapLayout}.
   * @param align the alignment value
   */
  public WrapLayout(final int align) {
    super(align);
  }

  /**
   * Creates a new flow layout manager with the indicated alignment
   * and the indicated horizontal and vertical gaps.
   * <p>
   * The value of the alignment argument must be one of
   * {@code WrapLayout}, {@code WrapLayout},
   * or {@code WrapLayout}.
   * @param align the alignment value
   * @param hgap the horizontal gap between components
   * @param vgap the vertical gap between components
   */
  public WrapLayout(final int align, final int hgap, final int vgap) {
    super(align, hgap, vgap);
  }

  /**
   * Returns the preferred dimensions for this layout given the
   * <i>visible</i> components in the specified target container.
   * @param target the component which needs to be laid out
   * @return the preferred dimensions to lay out the
   * subcomponents of the specified container
   */
  @Override
  public Dimension preferredLayoutSize(final Container target) {
    return layoutSize(target, true);
  }

  /**
   * Returns the minimum dimensions needed to layout the <i>visible</i>
   * components contained in the specified target container.
   * @param target the component which needs to be laid out
   * @return the minimum dimensions to lay out the
   * subcomponents of the specified container
   */
  @Override
  public Dimension minimumLayoutSize(final Container target) {
    final Dimension minimum = layoutSize(target, false);
    minimum.width -= (getHgap() + 1);
    return minimum;
  }

  /**
   * Returns the minimum or preferred dimension needed to layout the target container.
   * @param target target to get layout size for
   * @param preferred should preferred size be calculated
   * @return the dimension to layout the target container
   */
  private Dimension layoutSize(final Container target, final boolean preferred) {
    synchronized (target.getTreeLock()) {
      final JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
      GuiUtils.adjustScrollbarsThickness(scrollPane);
      //  Each row must fit with the width allocated to the containter.
      //  When the container width = 0, the preferred width of the container
      //  has not yet been calculated so lets ask for the maximum.
      int targetWidth = (scrollPane != null) ? scrollPane.getSize().width : target.getSize().width;
      if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

      final int hgap = getHgap();
      final int vgap = getVgap();
      final Insets insets = target.getInsets();
      final int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
      final int maxWidth = targetWidth - horizontalInsetsAndGap;

      //  Fit components into the allowed width
      final Dimension dim = new Dimension(0, 0);
      int rowWidth = 0;
      int rowHeight = 0;

      final int nmembers = target.getComponentCount();

      for (int i = 0; i < nmembers; i++) {
        final Component m = target.getComponent(i);
        if (m.isVisible()) {
          final Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
          //  Can't add the component to current row. Start a new row.
          if (rowWidth + d.width > maxWidth) {
            addRow(dim, rowWidth, rowHeight);
            rowWidth = 0;
            rowHeight = 0;
          }
          //  Add a horizontal gap for all components after the first
          if (rowWidth != 0) rowWidth += hgap;
          rowWidth += d.width;
          rowHeight = Math.max(rowHeight, d.height);
        }
      }

      addRow(dim, rowWidth, rowHeight);
      dim.width += horizontalInsetsAndGap;
      dim.height += insets.top + insets.bottom + vgap * 2;

      //  When using a scroll pane or the DecoratedLookAndFeel we need to
      //  make sure the preferred size is less than the size of the
      //  target container so shrinking the container size works
      //  correctly. Removing the horizontal gap is an easy way to do this.
      if (scrollPane != null) {
        if (target.isValid()) dim.width -= (hgap + 1);
      }
      return dim;
    }
  }

  /**
   * A new row has been completed. Use the dimensions of this row
   * to update the preferred size for the container.
   *
   * @param dim update the width and height when appropriate
   * @param rowWidth the width of the row to add
   * @param rowHeight the height of the row to add
   */
  private void addRow(final Dimension dim, final int rowWidth, final int rowHeight) {
    dim.width = Math.max(dim.width, rowWidth);
    if (dim.height > 0) dim.height += getVgap();
    dim.height += rowHeight;
  }
}
