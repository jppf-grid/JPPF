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
rem add node certificate to the client trust store so node management can work
rem (necessary for the admin console for example)
rem call createUpdateTruststore node client

rem create node trust store with driver certificate
call createUpdateTruststore driver node

rem create driver trust store with node certificate
call createUpdateTruststore node driver
call createUpdateTruststore driver driver
rem create driver_client trust store with client certificate
call createUpdateTruststore client driver_client
call createUpdateTruststore driver driver_client

rem convert node keystore and trustore to BKS format for use with Android
call toBKS node