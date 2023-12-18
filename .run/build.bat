call robocopy target server\plugins *-SNAPSHOT.jar /XF original-*-SNAPSHOT.jar
if %errorlevel% neq 0 exit /b 0
exit /b 1