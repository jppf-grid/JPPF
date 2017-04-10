#! /bin/sh

# $1 is the name of the certificate to import into the trust store
# $2 is the name of the trust store to create or update
keytool -import -alias $1 -keypass password -file $1.cer -keystore $2"_truststore.ks" -storepass password -noprompt

