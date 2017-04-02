var base_path = "/org/jppf/ui/resources/splash";
var res="";
for (i=1; i<=4; i++) {
  if (i>1) res += "|";
  res += base_path + i + ".gif";
}
java.lang.System.out.println("jppf.ui.splash.images = " + res);
res;
