@echo off
del *.cer
del *.ks

call createKeystore.bat
call exportCert.bat
call createTruststore.bat
