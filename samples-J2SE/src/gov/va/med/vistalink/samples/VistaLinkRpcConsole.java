package gov.va.med.vistalink.samples;

import gov.va.med.vistalink.adapter.cci.VistaLinkConnection;
import gov.va.med.vistalink.rpc.RpcRequest;
import gov.va.med.vistalink.rpc.RpcRequestFactory;
import gov.va.med.vistalink.rpc.RpcResponse;
import gov.va.med.exception.ExceptionUtils;
import gov.va.med.exception.FoundationsException;
import gov.va.med.vistalink.security.CallbackHandlerUnitTest;
import gov.va.med.vistalink.security.VistaKernelPrincipalImpl;
import gov.va.med.vistalink.security.VistaLoginModuleException;
import gov.va.med.vistalink.security.VistaLoginModuleLoginsDisabledException;
import gov.va.med.vistalink.security.VistaLoginModuleNoJobSlotsAvailableException;
import gov.va.med.vistalink.security.VistaLoginModuleNoPathToListenerException;
import gov.va.med.vistalink.security.VistaLoginModuleTooManyInvalidAttemptsException;
import gov.va.med.vistalink.security.VistaLoginModuleUserCancelledException;
import gov.va.med.vistalink.security.VistaLoginModuleUserTimedOutException;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Console application demonstration of VistaLink's Vista connectivity and RPC
 * functionality.
 * <p>
 * <b>Command-Line Parameters </b>
 * <ul>
 * <li>-s server appname to use from the JAAS config file (required)
 * <li>-a access code (required)
 * <li>-v verify code (required)
 * <li>-d division ien (optional)
 * </ul>
 * Also, a JAAS configuration file must be specified. The easiest way is to
 * specify the location/name of the JAAS configuration file with the
 * -Djava.security.auth.login.config VM argument.
 * <p>
 * For example:
 * <p>
 * <code>
 *  java -Djava.security.auth.login.config=./jaas.config VistaLinkRpcConsole -s MyServer -a ac!@#$12 -v vc123!@#
 * </code>
 * <p>
 * <b>Log4J Initialization </b>
 * <p>
 * For Log4J initialization, the Log4J config file 'log4JConfig.xml' is expected
 * to be in the classpath location:
 * <ul>
 * <li>props/log4jConfig.xml
 * </ul>*
 * 
 */
public final class VistaLinkRpcConsole {

	// static values
	private static final String RPCCONTEXT = "XOBV VISTALINK TESTER";

	// values must be supplied from the command line
	private String cfgName = "";
	private String accessCode = "";
	private String verifyCode = "";
	private String division = "";
	
	private static final boolean USE_PROPRIETARY_MESSAGE_FORMAT = true;

	// Log4J setup
	private static final Logger logger = Logger.getLogger(VistaLinkRpcConsole.class);
	
	/*
	 * @param args see the online help sent to system output when launched
	 */
	public static void main(String[] args) {

		DOMConfigurator.configure("props/log4jConfig.xml");
		logger.debug("Starting to run samples...");
		new VistaLinkRpcConsole(args);
	}

	/**
	 * @param args command-line arguments
	 */
	VistaLinkRpcConsole(String[] args) {

		doCommandLineHelp();
		try {
			parseArgs(args);
		} catch (Exception e) {
			System.out.println("Error parsing command-line arguments. " + e.getMessage());
			System.exit(-1);
		}

		VistaKernelPrincipalImpl myPrincipal = null;
		LoginContext lc = null;

		logger.debug("=====================================");
		logger.debug("Configuration name: " + cfgName);
		logger.debug("=====================================");

		int exitFlag = 0;

		try {
			// create the callbackhandler for JAAS login
			CallbackHandlerUnitTest cbhSilentSimple = new CallbackHandlerUnitTest(accessCode, verifyCode, division);

			// create the JAAS LoginContext for login
			lc = new LoginContext(cfgName, cbhSilentSimple);

			// login to server
			lc.login();

			// get JAAS principal after login
			myPrincipal = VistaKernelPrincipalImpl.getKernelPrincipal(lc.getSubject());
			logger.debug(myPrincipal);

			// get connection from JAAS principal
			VistaLinkConnection myConnection = myPrincipal.getAuthenticatedConnection();

				logger.debug("--------------------------------------------------");
				makeRpcCalls(myConnection);
				logger.debug("--------------------------------------------------");

		} catch (VistaLoginModuleLoginsDisabledException e) {
			System.err.println("Login error: Logins are disabled on the M system.");
			exitFlag = 1;
		} catch (VistaLoginModuleNoJobSlotsAvailableException e) {
			System.err.println("Login error: No job slots are available on the M system.");
			exitFlag = 1;
		} catch (VistaLoginModuleUserCancelledException e) {
			System.err.println("Login error: User cancelled Login.");
			exitFlag = 1;
		} catch (VistaLoginModuleUserTimedOutException e) {
			System.err.println("Login error: User timed out.");
			exitFlag = 1;
		} catch (VistaLoginModuleNoPathToListenerException e) {
			System.err.println("Login error: No path found to specified listener.");
			exitFlag = 1;
		} catch (VistaLoginModuleTooManyInvalidAttemptsException e) {
			System.err.println("Login error: Login cancelled due to too many invalid login attempts.");
			exitFlag = 1;
		} catch (VistaLoginModuleException e) {
			System.err.println("Login error: " + e.getClass().getName() + ", " + e.getMessage());
			exitFlag = 1;
		} catch (LoginException e) {
			System.err.println("Login error: " + e.getClass().getName() + ", " + e.getMessage());
			exitFlag = 1;
		} catch (SecurityException e) {
			System.err.println("Login error: " + e.getClass().getName() + ", " + e.getMessage());
			exitFlag = 1;
		} catch (Exception e) {
			System.err.println("Login error: " + e.getClass().getName() + ", " + e.getMessage());
			exitFlag = 1;
		} finally {
			try {
				lc.logout();
			} catch (LoginException e1) {
				System.err.println(ExceptionUtils.getFullStackTrace(e1));
			} finally {
				System.exit(exitFlag);
			}
		}
	}

	private void makeRpcCalls(VistaLinkConnection myConnection) throws FoundationsException {

		RpcRequest vReq = null;
		RpcResponse vResp = null;

		// call XOBV TEST PING rpc
		// ---------------------------------------------------------------------------------------
		vReq = RpcRequestFactory.getRpcRequest();
		vReq.setUseProprietaryMessageFormat(USE_PROPRIETARY_MESSAGE_FORMAT);
		vReq.setRpcContext(RPCCONTEXT);
		vReq.setRpcName("XOBV TEST PING");
		vResp = myConnection.executeRPC(vReq);
		logger.debug("");
		logger.debug("============================================================================");
		logger.debug("RPC Ping Result: ");
		logger.debug(vResp.getResults());
		logger.debug("============================================================================");
		logger.debug("");

		// do XOBV TEST WORD PROCESSING rpc
		vReq.setRpcName("XOBV TEST WORD PROCESSING");
		vReq.clearParams();
		vResp = myConnection.executeRPC(vReq);
		logger.debug("");
		logger.debug("============================================================================");
		logger.debug("RPC Word Processing Result: ");
		logger.debug(vResp.getResults());
		logger.debug("============================================================================");
		logger.debug("");

		// ---------------------------------------------------------------------------------------
		// do string test
		vReq.setRpcName("XOBV TEST STRING");
		vReq.clearParams();
		vReq.getParams().setParam(1, "string", "This is a test string!");
		vResp = myConnection.executeRPC(vReq);
		logger.debug("");
		logger.debug("============================================================================");
		logger.debug("RPC String Result: ");
		logger.debug(vResp.getResults());
		logger.debug("============================================================================");
		logger.debug("");
	}


	/**
	 * parses the command-line arguments
	 * 
	 * @param args
	 */
	private void parseArgs(String[] args) throws Exception {

		int i = 0;
		if (args.length < 1) {

			// if no args passed, always show help so defaults being used are
			// known.
			throw new Exception("Expected parameters, did not recieve any.");

		} else {

			do {
				// first do checks
				if (args[i].substring(0, 1).equals("-")) {
					if (i + 1 < args.length) {
						if (((String) args[i + 1]).substring(0, 1).equals("-")) {
							String errStr = "Argument Error: '" + args[i]
									+ "' command line parameter cannot be followed by a flag (" + args[i + 1] + ")";
							System.err.println(errStr);
							throw new Exception(errStr);
						}
					} else {
						String errStr = "Argument Error: '" + args[i]
								+ "' command line parameter must be followed by a value";
						System.err.println(errStr);
						throw new Exception(errStr);
					}
				} else {

					String errStr = "Argument Error: '" + args[i]
							+ "' command line parameter expected to be a -flag parameter.";
					System.err.println(errStr);
					throw new Exception(errStr);

				}

				// passed checks
				if (args[i].equals("-s")) {
					cfgName = args[++i];
				} else if (args[i].equals("-a")) {
					accessCode = args[++i];
				} else if (args[i].equals("-v")) {
					verifyCode = args[++i];
				} else if (args[i].equals("-d")) {
					division = args[++i];
				} else {
					String errStr = "Argument Error: '" + args[i] + "' is an unsupported flag.";
					System.err.println(errStr);
					throw new Exception(errStr);
				}

				i++;

			} while (i < args.length);
		}

	}

	/**
	 * Prints help on command-line arguments needed for this app to the console.
	 */
	void doCommandLineHelp() {

		System.out.println("\nYou must supply following connection arguments:\n");
		System.out.println("    -s server alias to use from the JAAS config file (required)");
		System.out.println("    -a access code (required)");
		System.out.println("    -v verify code (required)");
		System.out.println("\nThe following values are defaults:");
		System.out.println("    Division:  " + division);
		System.out.println("\n Override defaults with the following optional arguments:\n");
		System.out.println("    -d division ien (optional, to override default)");
		System.out
				.println("\ne.g.: java -Djava.security.auth.login.config=./jaas.config VistaLinkRpcConsole -s MyServer -a ac!@#$12 -v vc123!@#");
	}
}