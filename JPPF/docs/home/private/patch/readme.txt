This patch fixes the bug [3095404 - Client does not receive results upon node error]

To appply this patch:

1. unzip "jppf-2.3-patch-01.zip"

2. to patch a JPPF server/driver:
- copy the files "jppf-server.jar" and "jppf-common.jar" in your server's /lib folder,
  this will replace the previous versions of the jar files.
- restart the server.

3. to patch a JPPF client:
- copy "jppf-common.jar" in your client application's library folder,
  this will replace the previous version of the jar file.
- restart your JPPF application.

This patch also contains the new sources in separate jars files,
"jppf-server-src.jar" and "jppf-common-src.jar"