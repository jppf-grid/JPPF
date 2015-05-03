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

package test.generic;

import java.util.regex.*;

import org.jppf.utils.TypedProperties;
import org.jppf.utils.configuration.*;

/**
 * 
 * @author Laurent Cohen
 */
public class RegexRunner {
  /**
   * 
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      performScriptedProperties();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Test scripted properties.
   * @throws Exception if any error occurs.
   */
  private static void performScriptedProperties() throws Exception {
    TypedProperties config = new TypedProperties();
    //props.setString("jppf.script.default.language", "$script{ 'groo' + 'vy' }$");
    config.setString("jppf.script.default.language", "$script::url{ http://localhost:8880/test.js }$");
    int count = 0;
    config.setString("prop.0", "hello miscreant world");
    config.setString("prop.1", "hello $script:javascript{ 2 + 3 }$ world");
    config.setString("prop.2", "hello $script:glouglou:{ 2 + 3 }$ world");
    config.setString("prop.3", "hello $script{ return 2 + 3 }$ world");
    config.setString("prop.4", "hello $script:groovy{ return 2 + 3 }$ dear $script:javascript{'' + (2 + 5)}$ world");
    config.setString("prop.5", "hello $script{ return '${prop.0} ' + (2 + 3) }$ world");
    config.setString("prop.6", "hello $script{ return thisProperties.getString('prop.0') + (2 + 3) }$ universe");
    TypedProperties oldProps = new TypedProperties(config);
    config = new SubstitutionsHandler().resolve(config);
    ScriptHandler sh = new ScriptHandler();
    sh.process(config);
    for (String key: oldProps.stringPropertyNames()) {
      System.out.printf("name=%s, before=%s, after=%s\n", key, oldProps.getString(key), config.getString(key));
    }
  }

  /**
   * Test regular expressios.
   * @throws Exception if any error occurs.
   */
  private static void performRegex() throws Exception {
    //Pattern p = Pattern.compile("\\$script\\:(.*)\\:(.*)\\{(.*)\\}\\$");
    //Pattern p = Pattern.compile("\\$script(?:\\:(.*))?(?:\\:(.*))?\\{(.*)\\}\\$");
    Pattern p = Pattern.compile("\\$script(?:\\:([^:]*))?(?:\\:(.*))?\\{(.*)\\}\\$");
    //String s = "$script:javascript:inline{ 2 + 3 }$";
    String[] strings = {"$script{ 2 + 3 }$", "$script:{ 2 + 3 }$", "$script::{ 2 + 3 }$", "$script::inline{ 2 + 3 }$", "$script:javascript{ 2 + 3 }$", "$script:javascript:{ 2 + 3 }$", "$script:javascript:inline{ 2 + 3 }$"};
    for (String s: strings) {
      System.out.println("***** test for '" + s + "' *****");
      Matcher m = p.matcher(s);
      boolean found = m.find();
      System.out.println("  pattern found = " + found);
      int n = m.groupCount();
      System.out.println("  group count = " + n);
      for (int i=1; i<= n; i++) {
        String g = m.group(i);
        System.out.println("  group " + i + " = " + g);
      }
    }
  }
}
