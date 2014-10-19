@echo off
rem %1 is the name of the certificate to import into the trust store
rem %2 is the name of the trust store to create or update
keytool -import -alias %1 -keypass password -file %1.cer -keystore %2_truststore.ks -storepass password -noprompt
