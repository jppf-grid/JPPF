@echo off
SET DNAME="CN=JPPF %1, OU=JPPF, O=JPPF, L=Evreux, S=Eure, C=FR"
rem use "password" as the keystore password
keytool -genkey -dname %DNAME% -alias %1 -keypass password -keyalg RSA -validity 3650 -keystore %1_keystore.ks -storepass password