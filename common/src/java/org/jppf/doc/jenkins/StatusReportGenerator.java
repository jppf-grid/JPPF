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

package org.jppf.doc.jenkins;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.jppf.utils.*;

/**
 *Generates a PHP status page for Jenkins build results
 * @author Laurent Cohen
 */
public class StatusReportGenerator {
  /**
   *
   * @param args are used as follows:
   * <ul>
   * <li>{@code args[0]} is the path to {@code .jenkins}</li>
   * <li>{@code args[1]} is a comma-separated list of project names, e.g. "JPPF trunk, JPPF 5.2" (including the quotes)</li>
   * <li>{@code args[2]} is the path to the output file</li>
   * <li>{@code args[3]} is the path to tempalte file where the generated html is to be inserted</li>
   * </ul>
   */
  public static void main(final String[] args) {
    try {
      String jenkinsPath = args[0];
      String[] projectNames = StringUtils.parseStringArray(args[1], ",");
      //String projectName = args[1];
      String outputPath = args[2];
      String templatePath = args[3];

      List<Project> projects = new ArrayList<>();
      for (String projectName: projectNames) {
        File dir = new File(jenkinsPath + "/jobs/" + projectName + "/builds");
        FileFilter filter = new FileFilter() {
          @Override
          public boolean accept(final File file) {
            if (!file.isDirectory()) return false;
            int buildNumber = -1;
            try {
              buildNumber = Integer.valueOf(file.getName());
            } catch(@SuppressWarnings("unused") NumberFormatException ignore) {
            }
            return buildNumber >= 0;
          }
        };
        File[] buildDirs = dir.listFiles(filter);
        Project project = new Project(projectName);
        for (File buildDir: buildDirs) {
          int buildNumber = Integer.valueOf(buildDir.getName());
          File buildFile = new File(buildDir, "build.xml");
          Build build = parseBuild(buildFile);
          build.setNumber(buildNumber);
          project.getBuilds().add(build);
        }
        Collections.sort(project.getBuilds(), new Comparator<Build>() {
          @Override
          public int compare(final Build o1, final Build o2) {
            return ((Integer) o2.getNumber()).compareTo(o1.getNumber());
          }
        });
        System.out.printf("results: %s%n", project);
        projects.add(project);
      }
      if (!projects.isEmpty()) {
        HTMLPrinter printer = new HTMLPrinter();
        StringBuilder htmlLeft = new StringBuilder();
        StringBuilder htmlRight = new StringBuilder();
        for (int i=0; i<projects.size(); i+=2) htmlLeft.append(printer.generate(projects.get(i))).append('\n');
        for (int i=1; i<projects.size(); i+=2) htmlRight.append(printer.generate(projects.get(i))).append('\n');
        String template = FileUtils.readTextFile(templatePath);
        template = template.replace("@@column_left@@", htmlLeft);
        template = template.replace("@@column_right@@", htmlRight);
        FileUtils.writeTextFile(outputPath, template);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Parse the specified build file.
   * @param buildFile the file to parse.
   * @return a {@link Build} instance.
   * @throws Exception if any error occurs.
   */
  private static Build parseBuild(final File buildFile) throws Exception {
    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
    Handler handler = new Handler();
    parser.parse(buildFile, handler);
    Build build = handler.build;
    //System.out.printf("resulting build = %s%n", build);
    return build;
  }
}
