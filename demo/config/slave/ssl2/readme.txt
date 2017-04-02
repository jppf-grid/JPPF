This config/ssl2 folder provides an example of SSL configuration with client authentication,
where the nodes, clients and server each have their own certificate.

Provided are the scripts that generate separate public/private key pairs, keystores,
self-signed certificates and trust stores for each type of JPPF component.

The client trust store contains both driver and node certificates
(node certificate is required for node management, in particular in the admin console)

The driver trust store contains both client and node certificates, so they can authenticate
themselves with the server.

The node trust store contains only the driver certificate.

To regenerate all keys, certificates and stores:
- on Linux/Unix: ./createAll.sh
- on Windows   : createAll.bat

