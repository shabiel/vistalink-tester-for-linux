REM -- runs the VistaLinkRpcSwingSimple application, in the VistaLink samples jar
REM
REM -- Depends on variables CLASSPATH and JAVA_HOME, both set in setVistaLinkEnvironment.bat.
REM -- You will need to adjust the locations of the various jars and other files in
REM -- setVistaLinkEnvironment.bat to match the locations of these files on your system.
REM
call setVistaLinkEnvironment.bat
REM
REM -- run the sample application (assumes config file is in the current directory)
REM
REM -- run the sample application (assumes config file is in the current directory)
"%JAVA_HOME%\bin\java" -Djava.security.auth.login.config="./jaas.config" -cp "%CLASSPATH%" gov.va.med.vistalink.samples.VistaLinkRpcSwingSimple -s DemoServer
pause 
