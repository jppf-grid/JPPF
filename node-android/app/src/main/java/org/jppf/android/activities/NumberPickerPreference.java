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
package org.jppf.android.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import org.jppf.android.R;

/**
 * A preference widget which captures the Uri for a file accessible via the
 * <a href="http://developer.android.com/guide/topics/providers/document-provider.html">Storage Access Framework</a>.
 * <p>It has a vertical layout which contains an {@link EditText}, a 'Browse' button that pops up a file picker
 * and the standard OK/Cancel buttons.
 * @author Laurent Cohen
 */
public class NumberPickerPreference extends DialogPreference {
  /**
   * Tag used for logging.
   */
  private final static String LOG_TAG = NumberPickerPreference.class.getSimpleName();
  /**
   * The layout for this preference widget.
   */
  private final LinearLayout layout = new LinearLayout(this.getContext());
  /**
   * Editable text field where the selected URI is displayed.
   */
  private final NumberPicker picker = new NumberPicker(this.getContext());
  /**
   * The minimum value.
   */
  private int minValue;
  /**
   * The maximum value.
   */
  private int maxValue;
  /**
   * The default value.
   */
  private int defValue;

  /**
   * Called when addPreferencesFromResource() is called. Initializes basic paraaeters.
   * @param context the context in which this preference is instantiated.
   * @param attrs the attributes for this preference.
   */
  public NumberPickerPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setPersistent(true);
    layout.setOrientation(LinearLayout.VERTICAL);
    TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference, 0, 0);
    try {
      minValue = ta.getInt(R.styleable.NumberPickerPreference_minValue, 0);
      maxValue = ta.getInt(R.styleable.NumberPickerPreference_maxValue, 100);
      defValue = ta.getInt(R.styleable.NumberPickerPreference_defValue, 50);
    } finally {
      ta.recycle();
    }
  }

  @Override
  protected View onCreateDialogView() {
    picker.setMinValue(minValue);
    picker.setMaxValue(maxValue);
    int value = defValue;
    try {
      value = getPersistedInt(1);
    } catch(ClassCastException e) {
      value = Integer.valueOf(getPersistedString("1"));
      SharedPreferences.Editor editor = getSharedPreferences().edit();
      editor.remove(getKey());
      editor.putInt(getKey(), value);
      editor.commit();
    }
    picker.setValue(value);
    layout.addView(picker);
    return layout;
  }

  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    // persist the URI
    if (positiveResult && shouldPersist()) persistInt(picker.getValue());
    // remove the custom fields from the dialog
    ((ViewGroup) picker.getParent()).removeView(picker);
    ((ViewGroup) layout.getParent()).removeView(layout);
    notifyChanged();
  }
}
