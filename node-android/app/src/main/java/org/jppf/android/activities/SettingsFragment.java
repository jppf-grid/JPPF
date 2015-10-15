/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.jppf.android.AndroidHelper;
import org.jppf.android.PreferenceUtils;
import org.jppf.android.R;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.net.ssl.SSLSocketFactory;

/**
 * This class manages the settings screen and updates the JPPF configuration whenever a preference changes.
 * @author Laurent Cohen
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
  /**
   * Tag used for logging.
   */
  private final static String LOG_TAG = SettingsFragment.class.getSimpleName();
  /**
   * The set of default enabled cipher suites.
   */
  private final static Set<String> ENABLED_CIPHER_SUITES = new TreeSet<>();
  /**
   * The set of supported cipher suites.
   */
  private final static Set<String> SUPPORTED_CIPHER_SUITES = new TreeSet<>();
  /**
   * The current key being modified (for prefs linked to a file chooser).
   */
  private Preference currentPref = null;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    try {
      super.onCreate(savedInstanceState);
      // Load the preferences screen from an XML resource
      addPreferencesFromResource(R.xml.preferences);
      String[] pickerKeys = { PreferenceUtils.TRUST_STORE_LOCATION_KEY, PreferenceUtils.KEY_STORE_LOCATION_KEY };
      for (String key: pickerKeys) {
        FilechoserEditTextPreference picker = (FilechoserEditTextPreference) findPreference(key);
        picker.setFragment(this);
      }
      PreferenceScreen pref = (PreferenceScreen) findPreference("pref_security");
      if (SUPPORTED_CIPHER_SUITES.isEmpty()) {
        SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        ENABLED_CIPHER_SUITES.addAll(Arrays.asList(ssf.getDefaultCipherSuites()));
        SUPPORTED_CIPHER_SUITES.addAll(Arrays.asList(ssf.getSupportedCipherSuites()));
      }
      MultiSelectListPreference ciphersPref = (MultiSelectListPreference) findPreference(PreferenceUtils.ENABLED_CIPHER_SUITES_KEY);
      ciphersPref.setDefaultValue(ENABLED_CIPHER_SUITES.toArray(new String[ENABLED_CIPHER_SUITES.size()]));
      ciphersPref.setEntryValues(SUPPORTED_CIPHER_SUITES.toArray(new String[SUPPORTED_CIPHER_SUITES.size()]));
      ciphersPref.setEntries(SUPPORTED_CIPHER_SUITES.toArray(new String[SUPPORTED_CIPHER_SUITES.size()]));
    } catch(Throwable t) {
      t.printStackTrace();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onPause() {
    super.onPause();
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
    AndroidHelper.changeConfigFromPrefs(prefs, key);
  }

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
    Log.v(LOG_TAG, String.format("onActivityResult(requestCode=%d, resultCode=%d, resultData=%s)", requestCode, resultCode, resultData));
    if ((requestCode == PreferenceUtils.READ_REQUEST_CODE) && (resultCode == Activity.RESULT_OK)) {
      if (resultData != null) {
        try {
          Uri uri = resultData.getData();
          Log.v(LOG_TAG, "Uri: " + uri);
          final int takeFlags = resultData.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
          getActivity().grantUriPermission(getActivity().getPackageName(), uri, Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
          getActivity().grantUriPermission(getActivity().getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
          Preference pref = currentPref;
          currentPref = null;
          if (pref instanceof FilechoserEditTextPreference) ((FilechoserEditTextPreference) pref).onValueChanged(uri.toString());
        } catch(Exception e) {
          Log.e(LOG_TAG, "exception in onActivityResult(): ", e);
        }
      }
    }
  }

  /**
   * Initiiate the file picker for the specified preference.
   * @param pref the preference whose value will be the uri of the selected file, if any.
   */
  public void startFileChooser(Preference pref) {
    Log.v(LOG_TAG, "startFileChooser(Preference)");
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.setType("*/*");
    String value = pref.getSharedPreferences().getString(pref.getKey(), null);
    Log.v(LOG_TAG, "startFileChooser() value = " + value);
    currentPref = pref;
    startActivityForResult(intent, PreferenceUtils.READ_REQUEST_CODE);
  }

  @Override
  public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
    super.onPreferenceTreeClick(preferenceScreen, preference);
    // If the user clicks on a preference screen, set up the action bar
    if (preference instanceof PreferenceScreen) initializeActionBar((PreferenceScreen) preference);
    Log.v(LOG_TAG, "clicked on preference " + preference);
    return false;
  }

  /**
   * Sets up the action bar for an {@link PreferenceScreen}.
   * @param preferenceScreen the preference screen on which to set the action bar.
   */
  private static void initializeActionBar(PreferenceScreen preferenceScreen) {
    final Dialog dialog = preferenceScreen.getDialog();
    if (dialog != null) {
      dialog.getActionBar().setDisplayHomeAsUpEnabled(true);
      View homeBtn = dialog.findViewById(android.R.id.home);
      if (homeBtn != null) {
        View.OnClickListener dismissDialogClickListener = new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            dialog.dismiss();
          }
        };
        ViewParent homeBtnContainer = homeBtn.getParent();
        if (homeBtnContainer instanceof FrameLayout) {
          ViewGroup containerParent = (ViewGroup) homeBtnContainer.getParent();
          if (containerParent instanceof LinearLayout) containerParent.setOnClickListener(dismissDialogClickListener);
          else ((FrameLayout) homeBtnContainer).setOnClickListener(dismissDialogClickListener);
        } else  homeBtn.setOnClickListener(dismissDialogClickListener);
      }
    }
  }
}
