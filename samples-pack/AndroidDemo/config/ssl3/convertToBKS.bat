@echo off
rem use "password" as the keystore password
del %1*.bks
keytool -importkeystore -srckeystore %1_keystore.ks -destkeystore %1_keystore.bks -keyalg RSA -srcstoretype JKS -deststoretype BKS -srcstorepass password -deststorepass password -provider org.bouncycastle.jce.provider.BouncyCastleProvider -noprompt
keytool -importkeystore -srckeystore %1_truststore.ks -destkeystore %1_truststore.bks -keyalg RSA -srcstoretype JKS -deststoretype BKS -srcstorepass password -deststorepass password -provider org.bouncycastle.jce.provider.BouncyCastleProvider -noprompt
