/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A customized {@link TextView} whose text color goes smoothly from a color to another and back while a job is executing.
 * @author Laurent Cohen
 */
public class GlowingTextView extends TextView {
  /**
   * The animator used to animate the text color.
   */
  private ValueAnimator animator;
  /**
   * The initial color of this text, when the view is first displayed.
   */
  private int initialColor;

  /**
   * Initialize with the specified {@link Context}
   * @param context the cpntext for this view.
   */
  public GlowingTextView(final Context context) {
    super(context);
  }

  /**
   * Initialize with the specified {@link Context}
   * @param context the cpntext for this view.
   */
  public GlowingTextView(final Context context, final AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Initialize with the specified {@link Context}
   * @param context the cpntext for this view.
   */
  public GlowingTextView(final Context context, final AttributeSet attrs, final int defStyle) {
    super(context, attrs, defStyle);
  }

  /**
   * Start the animation using the specified  duration, the intial text color of this view as start color
   * and the specified end color.
   * @param duration the animation duration.
   * @param endColor the end color.
   * @return this view.
   */
  public GlowingTextView startAnimation(long duration,int endColor) {
    return startAnimation(duration, getCurrentTextColor(), endColor);
  }

  /**
   * Start the animation using the specified  duration, start color and end color.
   * @param duration the animation duration.
   * @param startColor the start color.
   * @param endColor the end color.
   * @return this view.
   */
  public GlowingTextView startAnimation(long duration, int startColor, int endColor) {
    if (animator != null) {
      if (animator.isStarted() || animator.isRunning()) animator.end();
    }
    this.initialColor = getCurrentTextColor();
    animator = ValueAnimator.ofInt(startColor, endColor);
    animator.setDuration(duration);
    ValueAnimator.setFrameDelay(40L);
    animator.setRepeatCount(ValueAnimator.INFINITE);
    animator.setRepeatMode(ValueAnimator.REVERSE);
    animator.setEvaluator(new ArgbEvaluator());
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(final ValueAnimator animation) {
          if (getContext() instanceof Activity) {
            ((Activity) getContext()).runOnUiThread(new Runnable() {
              @Override
              public void run() {
                setTextColor((Integer) animation.getAnimatedValue() | 0xFF000000);
              }
            });
          }
        }
      });
    animator.start();
    return this;
  }

  /**
   * End the animation.
   * @return this view.
   */
  public GlowingTextView endAnimation() {
    if (animator != null) {
      if (animator.isStarted() || animator.isRunning()) animator.end();
      animator = null;
    }
    setTextColor(initialColor);
    return this;
  }
}
