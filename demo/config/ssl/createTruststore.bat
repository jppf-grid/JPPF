@echo off
rem use "password" as the truststore password
keytool -import -alias jppf -keypass password -file jppf.cer -keystore truststore.ks
