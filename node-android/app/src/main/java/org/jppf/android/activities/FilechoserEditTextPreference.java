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
package org.jppf.android.activities;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A preference widget which captures the Uri for a file accessible via the
 * <a href="http://developer.android.com/guide/topics/providers/document-provider.html">Storage Access Framework</a>.
 * <p>It has a vertical layout which contains an {@link EditText}, a 'Browse' button that pops up a file picker
 * and the standard OK/Cancel buttons.
 * @author Laurent Cohen
 */
public class FilechoserEditTextPreference extends DialogPreference {
  /**
   * Tag used for logging.
   */
  private final static String LOG_TAG = FilechoserEditTextPreference.class.getSimpleName();
  /**
   * The layout for this preference widget.
   */
  private final LinearLayout layout = new LinearLayout(this.getContext());
  /**
   * Editable text field where the selected URI is displayed.
   */
  private final EditText editText = new EditText(this.getContext());
  /**
   * The 'Browse' button.
   */
  private final Button button = new Button(this.getContext());
  /**
   * The settings fragment which contains this preference widget.
   */
  private transient SettingsFragment fragment = null;

  /**
   * Called when addPreferencesFromResource() is called. Initializes basic paraaeters.
   * @param context the context in which this preference is instantiated.
   * @param attrs the attributes for this preference.
   */
  public FilechoserEditTextPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setPersistent(true);
    button.setText("Browse / Search");
    button.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_dialog_dialer, 0, 0, 0);
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View v) {
        Log.v(LOG_TAG, "in chooseFile()");
        fragment.startFileChooser(FilechoserEditTextPreference.this);
      }
    });
    layout.setOrientation(LinearLayout.VERTICAL);
  }

  @Override
  protected View onCreateDialogView() {
    layout.addView(editText);
    layout.addView(button);
    return layout;
  }

  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);
    editText.setText(getPersistedString(""), TextView.BufferType.NORMAL);
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    // persist the URI
    if (positiveResult && shouldPersist()) persistString(editText.getText().toString());
    // remove the custom fields from the dialog
    ((ViewGroup) editText.getParent()).removeView(editText);
    ((ViewGroup) button.getParent()).removeView(button);
    ((ViewGroup) layout.getParent()).removeView(layout);
    notifyChanged();
  }

  /**
   * Set the {@link SettingsFragment} which displays this preference.
   * @param fragment the fragment to set.
   */
  void setFragment(SettingsFragment fragment) {
    this.fragment = fragment;
  }

  /**
   * Called when the value of this preference has changed.
   * @param value the new value for this preference.
   */
  void onValueChanged(String value) {
    Log.v(LOG_TAG, "onValueChanged('" + value + "')");
    editText.setText(value);
  }
}
