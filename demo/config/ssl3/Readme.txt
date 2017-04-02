This config/ssl3 folder provides an example of SSL configuration with mutual authentication,
where the nodes, clients and server each have their own certificate, and the server uses
separate trust stores for the nodes and clients certificates.

Provided are the scripts that generate separate public/private key pairs, keystores,
self-signed certificates and trust stores for each type of JPPF component.

The client trust store contains the driver certificate

The driver trust store contains both client and node certificates, so they can authenticate
themselves with the server.

The node trust store contains the driver certificate.

To regenerate all keys, certificates and stores:
- on Linux/Unix: ./createAll.sh
- on Windows   : createAll.bat

