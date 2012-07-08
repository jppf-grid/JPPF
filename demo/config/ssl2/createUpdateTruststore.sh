#! /bin/sh

# use "password" as the truststore password
keytool -import -alias $1 -keypass password -file $1.cer -keystore $2"_truststore.ks" -storepass password -noprompt

