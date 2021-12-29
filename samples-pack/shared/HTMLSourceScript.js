function langFromExtension(name) {
  var lang = "txt";
  if (name.endsWith(".java")) lang = "java";
  else if (name.endsWith(".properties")) lang = "conf";
  else if (name.endsWith(".yaml")) lang = "yaml";
  else if (name.endsWith(".xml")) lang = "xml";
  else if (name.endsWith(".sql")) lang = "sql";
  return lang;
}

var fs = project.createDataType("fileset");
var projectName = attributes.get("sample");
var dir = new java.io.File(project.getProperty("basedir"));
self.log("dir = " + dir);
fs.setDir(dir);
var includes = attributes.get("include");
if (includes == null) includes = "**/*.java,**/*.properties,**/*.xml,**/*.txt,**/*.yaml,**/*.sql";
fs.setIncludes(includes);
fs.setExcludes("**/package-info.java,pom.xml,build.xml,target/**/*.*");
self.log("converting files from " + dir.getCanonicalPath());
var ds = fs.getDirectoryScanner();
var n = ds.getIncludedFilesCount();
self.log("selected " + n + " files");
var includedFiles = ds.getIncludedFiles();
for (i=0; i<n; i++) {
  var name = includedFiles[i];
  var file = new java.io.File(dir, name);
  var lang = langFromExtension(name);
  self.log("converting " + file + " to html with lang=" + lang);
  var task = project.createTask("tohtml");
  var path = file.getCanonicalPath().replaceAll("\\\\", "/")
    .replace("src/main/java/", "target/tohtml/src/")
    .replace("src/main/resources/", "target/tohtml/src/")
    .replace(projectName + "/config/", projectName + "/target/tohtml/config/")
    .replace(projectName + "/data/", projectName + "/target/tohtml/data/")
    .replace(projectName + "/db/", projectName + "/target/tohtml/db/")
    ;
  var idx = path.lastIndexOf("/");
  new java.io.File(path.substring(0, idx)).mkdirs();
  task.setDynamicAttribute("in", file.getCanonicalPath());
  task.setDynamicAttribute("out", path + ".html");
  task.setDynamicAttribute("lang", lang);
  task.setDynamicAttribute("title", file.getName());
  self.log("calling task with:\n"
    + "  in = " + file.getCanonicalPath() + "\n"
    + "  out = " + path + ".html" + "\n"
    + "  lang = " + lang + "\n"
    + "  title = " + file.getName());
  task.perform();
}
