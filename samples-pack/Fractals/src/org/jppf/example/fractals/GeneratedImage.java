/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.example.fractals;

import java.awt.image.BufferedImage;

/**
 * 
 * @author Laurent Cohen
 */
public class GeneratedImage {
  /**
   * An identifier for the image.
   */
  private int id;
  /**
   * The generated image.
   */
  private BufferedImage image;

  /**
   * 
   * @param id the image identifier.
   * @param image the generated image.
   */
  public GeneratedImage(final int id, final BufferedImage image) {
    this.id = id;
    this.image = image;
  }

  /**
   * Get the image identifier.
   * @return the identifier as an int.
   */
  public int getId() {
    return id;
  }

  /**
   * Get the generated image.
   * @return a {@link BufferedImage} object.
   */
  public BufferedImage getImage() {
    return image;
  }
}
