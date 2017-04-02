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
package org.jppf.android.activities.documents;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.util.Log;

import org.jppf.android.R;
import org.jppf.utils.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DocumentsProvider} which provides access to the node app's internal storage.
 * @author Laurent Cohen
 */
public class AppStorageProvider extends DocumentsProvider {
  /**
   * Tag used for logging.
   */
  private final static String LOG_TAG = AppStorageProvider.class.getSimpleName();
  /**
   * Columns for the rootDir.
   */
  private static final String[] DEFAULT_ROOT_PROJECTION = {
    Root.COLUMN_ROOT_ID, Root.COLUMN_MIME_TYPES,
    Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
    Root.COLUMN_SUMMARY, Root.COLUMN_DOCUMENT_ID,
    Root.COLUMN_AVAILABLE_BYTES
  };
  /**
   * The rootDir id.
   */
  private final static String ROOT_ID = "root";
  /**
   * The root path.
   */
  private File rootDir;
  /**
   * The root path as a string.
   */
  private String root_path;
  /**
   * Columns for the a file.
   */
  private static final String[] DEFAULT_DOCUMENT_PROJECTION = {
    Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE,
    Document.COLUMN_DISPLAY_NAME, Document.COLUMN_LAST_MODIFIED,
    Document.COLUMN_FLAGS, Document.COLUMN_SIZE
  };

  @Override
  public boolean onCreate() {
    rootDir = this.getContext().getFilesDir();
    try {
      root_path = rootDir.getCanonicalPath();
    } catch(Exception e) {
      root_path = rootDir.getAbsolutePath();
    }
    Log.v(LOG_TAG, "rootDir dir = " + rootDir);
    return true;
  }

  @Override
  public Cursor queryRoots(final String[] projection) throws FileNotFoundException {
    Log.d(LOG_TAG, "queryRoots() root dir = " + rootDir);
    String[] resolvedProjection = resolveProjection(projection, DEFAULT_ROOT_PROJECTION);
    MatrixCursor result = new MatrixCursor(resolvedProjection);
    MatrixCursor.RowBuilder row = result.newRow();
    row.add(Root.COLUMN_ROOT_ID, ROOT_ID);
    row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE /*| Root.FLAG_SUPPORTS_RECENTS*/ | Root.FLAG_SUPPORTS_SEARCH);
    row.add(Root.COLUMN_TITLE, getContext().getString(R.string.app_name));
    row.add(Root.COLUMN_DOCUMENT_ID, ROOT_ID);
    row.add(Root.COLUMN_AVAILABLE_BYTES, rootDir.getFreeSpace());
    row.add(Root.COLUMN_ICON, R.mipmap.jppf_icon);
    return result;
  }

  @Override
  public Cursor queryDocument(final String documentId, final String[] projection) throws FileNotFoundException {
    try {
      File child = getFile(documentId);
      String[] resolvedProjection = resolveProjection(projection, DEFAULT_DOCUMENT_PROJECTION);
      Log.v(LOG_TAG, "queryDocument(documentId=" + documentId + ")");
      final MatrixCursor result = new MatrixCursor(resolvedProjection);
      createRowForChild(child, result);
      return result;
    } catch(RuntimeException|Error e) {
      Log.e(LOG_TAG, "exception in queryDocument(documentId=" + documentId + ")", e);
      throw e;
    }
  }

  @Override
  public Cursor queryChildDocuments(final String parentDocumentId, final String[] projection, final String sortOrder) throws FileNotFoundException {
    try {
      File dir = getFile(parentDocumentId);
      File[] children = dir.listFiles();
      String[] resolvedProjection = resolveProjection(projection, DEFAULT_DOCUMENT_PROJECTION);
      Log.v(LOG_TAG, "queryChildDocuments(parentDocumentId=" + parentDocumentId + ")");
      final MatrixCursor result = new MatrixCursor(resolvedProjection);
      for (File child : children)  createRowForChild(child, result);
      return result;
    } catch(RuntimeException|Error e) {
      Log.e(LOG_TAG, "exception in queryChildDocuments(parentDocumentId=" + parentDocumentId + ")", e);
      throw e;
    }
  }

  @Override
  public ParcelFileDescriptor openDocument(final String documentId, final String mode, final CancellationSignal signal) throws FileNotFoundException {
    final File file = getFile(documentId);
    Log.v(LOG_TAG, "openDocument(mode=" + mode + ", documentId=" + documentId + ") file = " + file);
    final int accessMode = ParcelFileDescriptor.parseMode(mode);
    final boolean isWrite = (mode.indexOf('w') != -1);
    if (isWrite) {
      try {
        Handler handler = new Handler(getContext().getMainLooper());
        return ParcelFileDescriptor.open(file, accessMode, handler, new ParcelFileDescriptor.OnCloseListener() {
            @Override
            public void onClose(IOException e) {
              Log.i(LOG_TAG, "A file with id " + documentId + " has been closed!");
            }
          });
      } catch (IOException e) {
        throw new FileNotFoundException("Failed to open document with id " + documentId + " and mode " + mode);
      }
    } else {
      return ParcelFileDescriptor.open(file, accessMode);
    }
  }

  @Override
  public Cursor queryRecentDocuments(final String rootId, final String[] projection) throws FileNotFoundException {
    String[] resolvedProjection = resolveProjection(projection, DEFAULT_DOCUMENT_PROJECTION);
    Log.v(LOG_TAG, "queryRecentDocuments(rootId=" +rootId + ")");
    return new MatrixCursor(resolvedProjection);
  }

  /**
   * Add a row in the specified cursor for the specified file.
   * @param child the file for which to add a row.
   * @param cursor the cursor to add a row to.
   */
  private void createRowForChild(File child, MatrixCursor cursor) {
    MatrixCursor.RowBuilder row = cursor.newRow();
    String type = getContext().getContentResolver().getType(Uri.fromFile(child));
    Log.v(LOG_TAG, "createRowForChild(child=" + child + ") type=" + type);
    if (type == null) type = child.isDirectory() ? Document.MIME_TYPE_DIR : "application/octet-stream";
    int flags = child.isDirectory()
      ? /*Document.FLAG_DIR_PREFERS_GRID |*/ Document.FLAG_DIR_PREFERS_LAST_MODIFIED | Document.FLAG_DIR_SUPPORTS_CREATE
      : /*Document.FLAG_SUPPORTS_WRITE*/ 0;
    row.add(Document.COLUMN_FLAGS, flags);
    row.add(Document.COLUMN_DISPLAY_NAME, child.getName());
    row.add(Document.COLUMN_DOCUMENT_ID, getDocumentId(child));
    row.add(Document.COLUMN_MIME_TYPE, type);
    row.add(Document.COLUMN_SIZE, child.isDirectory() ? child.getTotalSpace() - child.getFreeSpace() : child.length());
    row.add(Document.COLUMN_LAST_MODIFIED, null);
  }

  /**
   * Resolve the column names based on sets of requested and predefined columns.
   * @param requested the requested columns.
   * @param defined the prefined columns.
   * @return the requested ciolumns that are also in the predefined columns.
   */
  private String[] resolveProjection(String[] requested, String[] defined) {
    if (requested == null) return defined;
    List<String> result = new ArrayList<>();
    for (String s: requested) {
      if (StringUtils.isOneOf(s, false, defined)) result.add(s);
    }
    return result.toArray(new String[result.size()]);
  }

  /**
   * Get a document id from a file.
   * @param file the file from which to compute the document id.
   * @return a document id as a string.
   */
  private String getDocumentId(File file) {
    String path;
    try {
      path = file.getCanonicalPath();
    } catch(Exception e) {
      path = file.getAbsolutePath();
    }
    return root_path.equals(path) ? ROOT_ID : path.substring(root_path.length());
  }

  /**
   * Get a file from a document id.
   * @param documentId the documentId from which to locate file.
   * @return the file corresponding to the document id.
   */
  private File getFile(String documentId) {
    return ROOT_ID.equals(documentId) ? rootDir : new File(rootDir, documentId);
  }
}
