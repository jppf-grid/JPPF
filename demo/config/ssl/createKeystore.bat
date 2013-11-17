@echo off
SET DNAME="CN=JPPF Team, OU=JPPF, O=JPPF, L=Evreux, S=Eure, C=FR"
rem use "password" as the keystore password
keytool -genkey -dname %DNAME% -alias jppf -keypass password -keyalg RSA -validity 3650 -keystore keystore.ks