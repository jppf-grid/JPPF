@echo off
rem use "password" as the truststore password
keytool -import -alias jppf -file jppf.cer -keystore truststore.ks
