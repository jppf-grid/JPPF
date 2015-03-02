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

package org.jppf.utils;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public class FileReplacerEx {
  /**
   * 
   */
  private static String exts = "php";
  /**
   * 
   */
  private static String[][] arguments = {
    //{ "C:/Workspaces/temp/jppf", "C:/Workspaces/temp", exts, "pervasiv_", "lolocohe_" },
    //{ "C:/Workspaces/SourceForgeSVN", "C:/Workspaces/temp", exts, "pervasiv_", "lolocohe_" },
    //{ "C:/Workspaces/temp/jppf", "C:/Workspaces/temp", "php,tpl,htaccess", "pervasiv", "lolocohe" },
    { "C:/Workspaces/temp/jppf", "C:/Workspaces/temp", "java,xml,xsd,css,html,properties,php", "pervasiv", "lolocohe" },
  };

  /**
   * 
   */
  private static String hr = StringUtils.padRight("", '*', 120);

  /**
   * 
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      boolean searchOnly = Boolean.valueOf(args[0]);
      for (int i=0; i<arguments.length; i++) {
        String root = arguments[i][0];
        String ext = arguments[i][2];
        String in = arguments[i][1] + "/in.txt";
        String out = arguments[i][1] + "/out.txt";
        FileUtils.writeTextFile(in, arguments[i][3]);
        FileUtils.writeTextFile(out, arguments[i][4]);
        System.out.println(hr);
        System.out.println("Root dir: '" + root + "', in all '" + ext + "' replacing '" + arguments[i][3] + "' with '" + arguments[i][4] + "'");
        System.out.println(hr);
        FileReplacer.main(root, in, out, ext, args[0]);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
