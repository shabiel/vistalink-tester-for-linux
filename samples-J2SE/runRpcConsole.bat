# -- runs the VistaLinkRpcConsole application, in the VistaLink samples jar
# 
# -- Depends on variables CLASSPATH and JAVA_HOME, both set in setVistaLinkEnvironment.bat.
# -- You will need to adjust the locations of the various jars and other files in
# -- setVistaLinkEnvironment.bat to match the locations of these files on your system.
# 
. setVistaLinkEnvironment.bat
$JAVA_HOME/bin/java -Djava.security.auth.login.config="./jaas.config" -cp $CLASSPATH gov.va.med.vistalink.samples.VistaLinkRpcConsole -s LocalServer -a shabiel12 -v catdog.33
