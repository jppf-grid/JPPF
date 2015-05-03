@echo off
rem use "password" as the keystore password
keytool -export -alias %1 -keypass password -keystore %1_keystore.ks -storepass password -rfc -file %1.cer