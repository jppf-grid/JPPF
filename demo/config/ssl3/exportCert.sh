#! /bin/sh

# use "password" as the keystore password
keytool -export -alias $1 -keypass password -keystore $1"_keystore.ks" -storepass password -rfc -file $1.cer

