/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
package org.jppf.android.node.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A customized {@link TextView} whose background is used as a progress bar.
 * @author Laurent Cohen
 */
public class ProgressTextView extends TextView {
  /**
   * The paint object used in {@link #onDraw(Canvas)}.
   */
  private final Paint paint = new Paint();
  {
    paint.setColor(Color.GREEN);
    paint.setAlpha(128);
    paint.setStyle(Paint.Style.FILL);
  }
  private final Rect rect = new Rect(0, 0, 0 , 0);

  /**
   * Initialize with the specified {@link Context}
   * @param context the cpntext for this view.
   */
  public ProgressTextView(final Context context) {
    super(context);
  }

  /**
   * Initialize with the specified {@link Context}
   * @param context the cpntext for this view.
   */
  public ProgressTextView(final Context context, final AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Initialize with the specified {@link Context}
   * @param context the cpntext for this view.
   */
  public ProgressTextView(final Context context, final AttributeSet attrs, final int defStyle) {
    super(context, attrs, defStyle);
  }

  /**
   * Set the percentage of the width of this vew's background that will be filled with the paint's color.
   * @param pct the fill percentage as an {@code int} in the rage [0, 100].
   */
  public void setPct(int pct) {
    rect.right = (int)  (this.getWidth() * (pct / 100f));
    rect.bottom = this.getHeight();
  }

  @Override
  protected void onDraw(final Canvas canvas) {
    canvas.drawRect(rect, paint);
    super.onDraw(canvas);
  }
}
