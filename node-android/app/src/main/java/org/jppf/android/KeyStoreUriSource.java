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
package org.jppf.android;

import android.net.Uri;
import android.util.Log;

import org.jppf.android.activities.SettingsFragment;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.concurrent.Callable;

/**
 * Key store or trust store source which retrieves the content from a specified URI parameter.
 * @author Laurent Cohen
 */
public class KeyStoreUriSource implements Callable<InputStream> {
  /**
   * Tag used for logging.
   */
  private final static String LOG_TAG = SettingsFragment.class.getSimpleName();
  private final Uri uri;

  /**
   * Initiialize this store source.
   * @param args only the first arg is used and must contain a non-encoded URI.
   */
  public KeyStoreUriSource(String...args) {
    //uri = Uri.parse(PreferenceUtils.encodeURL(args[0]));
    uri = Uri.parse(args[0]);
  }

  @Override
  public InputStream call() throws Exception {
    Log.d(LOG_TAG, "getting stream from uri = " + uri);
    InputStream is = AndroidHelper.getContext().getContentResolver().openInputStream(uri);
    return (is instanceof BufferedInputStream) ? is : new BufferedInputStream(is);
  }
}
