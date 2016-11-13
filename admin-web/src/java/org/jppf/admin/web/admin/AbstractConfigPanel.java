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
public class AbstractConfigPanel extends Panel {
  /**
   * The form associated with the panel.
   */
  protected final Form<String> form;
  /**
   * The config editor component.
   */
  protected final TextArea<String> config;
  /**
   * The field that handles the file to upload.
   */
  protected final FileUploadField fileUploadField;
  /**
   * Reference to the 'upload' image. 
   */
  protected final ContextImage cimg;
  /**
   * The type of config panel to add this button to.
   */
  protected final PanelType type;

  /**
   * @param type the type of config panel to add this button to.
   */
  public AbstractConfigPanel(final PanelType type) {
    super(type.getPrefix());
    this.type = type;
    add(form = new Form<>(type.getPrefix() + ".form"));
    form.add(new SortLink(type, true));
    form.add(new SortLink(type, false));
    form.add(new SaveLink(type));
    form.add(new RevertLink(type));
    form.add(new DownloadLink(type));
    form.add(new UploadLink(type));
    form.add(fileUploadField = new FileUploadField(type.getPrefix() + ".upload.browse"));
    form.add(cimg = new ContextImage(type.getPrefix() + ".upload.img", "images/toolbar/upload.png"));
    form.add(config = new TextArea<>(type.getPrefix() + ".properties.field", Model.of(JPPFWebConsoleApplication.get().getConfig(type).getProperties().asString())));
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
    ((AbstractJPPFPage) getPage()).setTooltip(cimg);
  }
}
