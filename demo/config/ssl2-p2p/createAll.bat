@echo off
del *.cer
del *.ks

call createKeystore driver
call createKeystore client
call createKeystore node

call exportCert driver
call exportCert client
call exportCert node

rem create client trust store with driver certificate
call createUpdateTruststore driver client

rem create node trust store with driver certificate
call createUpdateTruststore driver node

rem create driver trust store with client, node and driver certificates
call createUpdateTruststore node   driver
call createUpdateTruststore client driver
call createUpdateTruststore driver driver
