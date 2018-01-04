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

package org.jppf.admin.web.utils;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.IResourceStream;

/**
 *
 * @author Laurent Cohen
 */
public abstract class AJAXDownload extends AbstractAjaxBehavior {
  /**
   * Whether to disable the cache.
   */
  private boolean addAntiCache;

  /**
   *
   */
  public AJAXDownload() {
    this(true);
  }

  /**
   * @param addAntiCache whether to disable the cache.
   */
  public AJAXDownload(final boolean addAntiCache) {
    super();
    this.addAntiCache = addAntiCache;
  }

  /**
   * Call this method to initiate the download.
   * @param target .
   */
  public void initiate(final AjaxRequestTarget target) {
    String url = getCallbackUrl().toString();
    if (addAntiCache) {
      url = url + (url.contains("?") ? "&" : "?");
      url = url + "antiCache=" + System.currentTimeMillis();
    }
    // the timeout is needed to let Wicket release the channel
    target.appendJavaScript("setTimeout(\"window.location.href='" + url + "'\", 100);");
  }

  @Override
  public void onRequest() {
    final ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(getResourceStream(), getFileName());
    handler.setContentDisposition(ContentDisposition.ATTACHMENT);
    getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
  }

  /**
   * Override this method for a file name which will let the browser prompt with a save/open dialog.
   * @return a file name.
   * @see ResourceStreamRequestTarget#getFileName()
   */
  protected String getFileName() {
    return null;
  }

  /**
   * Hook method providing the actual resource stream.
   * @return the resource stream.
   */
  protected abstract IResourceStream getResourceStream();
}
