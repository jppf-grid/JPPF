/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.admin.web.filter;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.*;
import org.jppf.admin.web.settings.UserSettings;
import org.jppf.utils.LocalizationUtils;
import org.slf4j.*;
import org.wicketstuff.wicket.mount.core.annotation.MountPath;

import com.googlecode.wicket.jquery.ui.widget.dialog.*;

/**
 * This is the node filter page.
 * @author Laurent Cohen
 */
@MountPath(AbstractJPPFPage.PATH_PREFIX + "nodefilter")
@AuthorizeInstantiation({"jppf-manager", "jppf-monitor"})
public class NodeFilterPage extends TemplatePage {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeFilterPage.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Base name for localization bundles.
   */
  static final String BASE = "org.jppf.ui.i18n.FilterPanel";
  /**
   * The form associated with the panel.
   */
  private final Form<String> form;
  /**
   * The config editor component.
   */
  private final TextArea<String> policyField;
  /**
   * The field that handles the file to upload.
   */
  private final FileUploadField fileUploadField;
  /**
   * The dialog that displays that the policy filter is valid.
   */
  private final MessageDialog validDialog;
  /**
   * The dialog that displays that the policy filter has errors.
   */
  private final MessageDialog errorDialog;

  /**
   *
   */
  public NodeFilterPage() {
    add(form = new Form<>("node.filter.form"));
    final String validTitle = LocalizationUtils.getLocalized(BASE, "node.filter.valid.title", JPPFWebSession.get().getLocale());
    add(validDialog = new MessageDialog("node.filter.valid.dialog", Model.of(validTitle), Model.of(""), DialogButtons.OK, DialogIcon.INFO) {
      @Override
      public void onClose(final IPartialPageRequestHandler handler, final DialogButton button) {
      }
    });
    final String errorTitle = LocalizationUtils.getLocalized(BASE, "node.filter.invalid.title", JPPFWebSession.get().getLocale());
    add(errorDialog = new MessageDialog("node.filter.error.dialog", Model.of(errorTitle), Model.of(""), DialogButtons.OK, DialogIcon.ERROR) {
      @Override
      public void onClose(final IPartialPageRequestHandler handler, final DialogButton button) {
      }
    });
    final UserSettings settings = JPPFWebSession.get().getUserSettings();
    final boolean active = settings.getProperties().getBoolean(JPPFWebSession.NODE_FILTER_ACTIVE_PROP, false);
    form.add(new AjaxCheckBox("node.filter.active", Model.of(active)) {
      @Override
      protected void onUpdate(final AjaxRequestTarget target) {
        final Boolean newSelection = this.getModelObject();
        onActiveStateChanged((newSelection != null) && newSelection);
      }
    });
    form.add(new ValidateLink());
    form.add(new SaveLink());
    form.add(new RevertLink());
    form.add(new DownloadLink());
    form.add(new UploadLink());
    form.add(fileUploadField = new FileUploadField("node.filter.upload.browse"));
    final ContextImage cimg = new ContextImage("node.filter.upload.img", "images/toolbar/upload.png");
    form.add(cimg);
    setTooltip(cimg);
    form.add(policyField = new TextArea<>("node.filter.policy.field", Model.of(JPPFWebSession.get().getNodeFilter().getXmlPolicy())));
  }

  /**
   * @return the tet area holding the xml policy.
   */
  public TextArea<String> getPolicyField() {
    return policyField;
  }

  /**
   * @return the field that handles the file to upload.
   */
  public FileUploadField getFileUploadField() {
    return fileUploadField;
  }

  /**
   * @return the dialog that displays that the policy filter is valid.
   */
  public MessageDialog getValidDialog() {
    return validDialog;
  }

  /**
   * @return the dialog that displays that the policy filter has errors.
   */
  public MessageDialog getErrorDialog() {
    return errorDialog;
  }

  /**
   * Called when the filter's active state is changed in the UI.
   * @param active the new active state.
   */
  private static void onActiveStateChanged(final boolean active) {
    if (debugEnabled) log.debug("changing active state to {}", active);
    final UserSettings settings = JPPFWebSession.get().getUserSettings();
    JPPFWebSession.get().getNodeFilter().setActive(active);
    settings.getProperties().setBoolean(JPPFWebSession.NODE_FILTER_ACTIVE_PROP, active);
    settings.save();
  }

  @Override
  protected void onConfigure() {
    super.onConfigure();
    final UserSettings settings = JPPFWebSession.get().getUserSettings();
    final boolean active = settings.getProperties().getBoolean(JPPFWebSession.NODE_FILTER_ACTIVE_PROP, false);
    JPPFWebSession.get().getNodeFilter().setActive(active);
    nodeFilterLink.add(new AttributeModifier("style", "color: " + (active ? "green" : "red")));
  }
}
