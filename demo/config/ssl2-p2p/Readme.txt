This config/ssl2-p2p folder provides an example of SSL configuration with mutual authentication,
in the context of a topoplogy with servers connected in p2p.

Provided are the scripts that generate separate public/private key pairs, keystores,
self-signed certificates and trust stores for each type of JPPF component.

The client trust store contains the driver certificate

The driver trust store contains both client and node certificates, so they can authenticate
themselves with the server. It also contains a server certificates so 2 servers can
mutually authenticcate.

The node trust store contains the driver certificate.

To regenerate all keys, certificates and stores:
- on Linux/Unix: ./createAll.sh
- on Windows   : createAll.bat

