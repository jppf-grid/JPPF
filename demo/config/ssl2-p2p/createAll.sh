#! /bin/sh

echo "before rm"

rm *.cer
rm *.ks

./createKeystore.sh "driver"
./createKeystore.sh client
./createKeystore.sh node

./exportCert.sh driver
./exportCert.sh client
./exportCert.sh node

# create client trust store with driver certificate
./createUpdateTruststore.sh driver client
# add node certificate to the client trust store so node management can work
# (necessary for the admin console for example)
./createUpdateTruststore.sh node client

# create node trust store with driver certificate
./createUpdateTruststore.sh driver node

# create driver trust store with client, node and driver certificates
./createUpdateTruststore.sh node   driver
./createUpdateTruststore.sh client driver
./createUpdateTruststore.sh driver driver

