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

package org.jppf.scripting;

import org.jppf.utils.pooling.AbstractObjectPoolQueue;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
class ScriptRunnerPool extends AbstractObjectPoolQueue<ScriptRunner> {
  /**
   * The script engine language to use.
   */
  private final String language;

  /**
   * Initialize this pool for the specified script language.
   * @param language the script engine language to use.
   */
  ScriptRunnerPool(final String language) {
    this.language = language;
  }

  @Override
  protected ScriptRunner create() {
    try {
    return new ScriptRunnerImpl(language);
    } catch(@SuppressWarnings("unused") JPPFScriptingException e) {
      return null;
    }
  }
}
