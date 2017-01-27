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
package org.jppf.load.balancer.impl;

import org.jppf.load.balancer.*;

/**
 * This class provide a used defined bundle size jppf.load.balancing.profile.
 * It uses the size defined by admin in property file or the size defined by admin application.
 * @author Domingos Creado
 * @author Laurent Cohen
 */
public class FixedSizeBundler extends AbstractBundler<FixedSizeProfile> {
  /**
   * Initialize this bundler.
   * @param profile - contains the parameters for this bundler.
   */
  public FixedSizeBundler(final FixedSizeProfile profile) {
    super(profile);
  }

  /**
   * This method always returns a statically assigned bundle size.
   * @return the bundle size defined in the JPPF driver configuration.
   */
  @Override
  public int getBundleSize() {
    return profile.getSize();
  }
}
