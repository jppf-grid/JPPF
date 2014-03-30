This config/ssl2 folder provides an example of SSL configuration with mutual authentication,
where the nodes and clients share the same certificate

Provided are the scripts that generate separate public/private key pairs, keystores,
self-signed certificates and trust stores for each type of JPPF component.

The client/node trust store contains the driver certificate.

The driver trust store contains the shared client/node certificate, so nodes and clients
can authenticate themselves with the server.
