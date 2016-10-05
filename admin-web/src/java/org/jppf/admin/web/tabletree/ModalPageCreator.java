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

package org.jppf.admin.web.tabletree;

import java.lang.reflect.Constructor;

import org.apache.wicket.Page;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.PageCreator;
import org.apache.wicket.markup.html.form.Form;

/**
 * @param <F> the type of the form to use.
 * @param <P> the type of modal page.
 * @author Laurent Cohen
 */
public class ModalPageCreator<F extends Form<String>, P extends Page> implements PageCreator {
  /**
   * The form to add to the page.
   */
  private final F form;
  /**
   * The class of th epage to instantiate.
   */
  private final Class<P> pageClass;

  /**
   * 
   * @param form the form to add to the page.
   * @param pageClass the class of th epage to instantiate.
   */
  public ModalPageCreator(final F form, final Class<P> pageClass) {
    if (form == null) throw new IllegalArgumentException("the form cannot be null");
    if (pageClass == null) throw new IllegalArgumentException("the page class cannot be null");
    this.form = form;
    this.pageClass = pageClass;
  }
  
  @Override
  public Page createPage() {
    try {
      Constructor<P> c = pageClass.getConstructor(form.getClass());
      return c.newInstance(form);
    } catch (Exception e) {
      throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    }
  }
}
