call "%VS_TOOLS_DIR%/vsvars32.bat"
call Csc.exe %*
@IF ERRORLEVEL 1 EXIT /B 1
