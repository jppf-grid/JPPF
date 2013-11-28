@echo off
rem use "password" as the keystore password
keytool -export -alias jppf -keystore keystore.ks -keypass password -rfc -file jppf.cer