function langFromExtension(name) {
  var lang = "txt";
  if (name.endsWith(".java")) lang = "java";
  else if (name.endsWith(".properties")) lang = "conf";
  else if (name.endsWith(".xml")) lang = "xml";
  return lang;
}

var fs = project.createDataType("fileset");
var dir = new java.io.File(project.getProperty("basedir") + "/../samples-pack/" + attributes.get("sample"))
fs.setDir(dir);
fs.setIncludes("**/*.java,**/*.properties,**/*.xml");
fs.setExcludes("**/package-info.java");
self.log("converting files from " + dir.getCanonicalPath());
var ds = fs.getDirectoryScanner();
var n = ds.getIncludedFilesCount();
//self.log("selected " + n + " files");
var includedFiles = ds.getIncludedFiles();
for (i=0; i<n; i++) {
  var name = includedFiles[i];
  var file = new java.io.File(dir, name);
  var lang = langFromExtension(name);
  self.log("converting " + file + " to html with lang=" + lang);
  var task = project.createTask("tohtml");
  task.setDynamicAttribute("in", file.getCanonicalPath());
  task.setDynamicAttribute("lang", lang);
  task.perform();
}
