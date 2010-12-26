This patch fixes the bug [3132907 - CNFE when custom class loader is used]

To apply this patch:

1. unzip "jppf-2.3-patch-05.zip"

2. to patch a JPPF node:
- copy the file "jppf-common-node.jar" to your node's /lib folder,
  this will replace the previous version of the jar file.
- restart the node.

3. to patch a JPPF server/driver:
- copy the files "jppf-common-node.jar" and "jppf-common.jar" to your server's /lib folder,
  this will replace the previous versions of the jar files.
- restart the server.

4. to patch a JPPF client / admin console:
- copy "jppf-common-node.jar" and "jppf-common.jar" and "jppf-client.jar" to your client application's library folder,
  this will replace the previous versions of the jar files.
- restart your JPPF application.

This patch also contains the new sources in separate jar files: "jppf-common-node-src.jar", "jppf-common-src.jar" and "jppf-client-src.jar" 