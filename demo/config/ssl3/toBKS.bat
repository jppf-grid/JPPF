@echo off

rem %1 is the prefix of both keystore and trust store to convert: node | driver | client

rem use "password" as the keystore password

del %1*.bks

set BC_OPTIONS=-provider org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath ../../../JPPF/lib/BouncyCastke/bcprov-jdk15on-152.jar
set COMMON_OPTIONS=-keyalg RSA -srcstoretype JKS -deststoretype BKS -srcstorepass password -deststorepass password %BC_OPTIONS% -noprompt

keytool -importkeystore -srckeystore %1_keystore.ks   -destkeystore %1_keystore.bks   %COMMON_OPTIONS%
keytool -importkeystore -srckeystore %1_truststore.ks -destkeystore %1_truststore.bks %COMMON_OPTIONS%
