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

package org.jppf.admin.web.admin;

import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.*;

/**
 * The JPPF client configuration panel of the admin page.
 * @author Laurent Cohen
 */
public class ConfigPanel extends Panel {
  /**
   * The config editor component.
   */
  private final TextArea<String> config;
  /**
   * The field that handles the file to upload.
   */
  private final FileUploadField fileUploadField;
  /**
   * Reference to the 'upload' image. 
   */
  private final ContextImage cimg;

  /**
   * 
   */
  public ConfigPanel() {
    super("admin.config");
    Form<String> form = new Form<>("admin.config.form");
    add(form);
    form.add(new SortLink(AdminConfigConstants.SORT_ASC_ACTION, true));
    form.add(new SortLink(AdminConfigConstants.SORT_DESC_ACTION, false));
    form.add(new SaveLink());
    form.add(new RevertLink());
    form.add(new ResetClientLink());
    form.add(new DownloadLink());
    form.add(new UploadLink());
    form.add(fileUploadField = new FileUploadField("admin.config.upload.browse"));
    cimg = new ContextImage("admin.config.upload.img", "images/toolbar/upload.png");
    form.add(cimg);
    form.add(config = new TextArea<>("admin.config.properties.field", Model.of(JPPFWebConsoleApplication.get().getAdminData().getConfig().asString())));
  }

  /**
   * @return the text area containing the edited configuration.
   */
  public TextArea<String> getConfig() {
    return config;
  }

  /**
   * @return the field that handles the file to upload.
   */
  public FileUploadField getFileUploadField() {
    return fileUploadField;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    //((AbstractJPPFPage) getPage()).setTooltip(fileUploadField);
    ((AbstractJPPFPage) getPage()).setTooltip(cimg);
  }
}
