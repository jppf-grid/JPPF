@echo off
rem use "password" as the keystore password
keytool -export -alias jppf -keystore keystore.ks -rfc -file jppf.cer