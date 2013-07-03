package gov.va.med.vistalink.samples;

import gov.va.med.vistalink.adapter.cci.VistaLinkConnection;
import gov.va.med.vistalink.rpc.RpcRequest;
import gov.va.med.vistalink.rpc.RpcRequestFactory;
import gov.va.med.vistalink.rpc.RpcResponse;
import gov.va.med.vistalink.security.CallbackHandlerSwingCCOW;
import gov.va.med.vistalink.security.VistaLoginModuleException;
import gov.va.med.vistalink.security.VistaLoginModuleNoPathToListenerException;
import gov.va.med.vistalink.security.VistaKernelPrincipalImpl;
import gov.va.med.exception.FoundationsException;
import gov.va.med.hds.cd.ccow.ContextItemNameFactory;
import gov.va.med.hds.cd.ccow.IClinicalContextBroker;
import gov.va.med.hds.cd.ccow.IContextItemName;
import gov.va.med.hds.cd.ccow.IContextObserver;
import gov.va.med.hds.cd.ccow.IContextParticipant;
import gov.va.med.hds.cd.ccow.internal.ContextModule;
import gov.va.med.hds.cd.ccow.ui.LinkIcon;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Swing application demonstration of VistaLink's Vista connectivity and RPC functionality, and the use of the
 * CCOW-enabled version of VistaLink for single signon.
 * <p>
 * NOTE: This application is a demonstration of how to use VistALink. It is *not* meant to be a demonstration of Swing
 * development &quot;best practices&quot;.
 * <p>
 * <p>
 * <b>Command-Line Parameters</b>
 * <p>
 * This application takes a program argument and a VM argument:
 * <ul>
 * <li>-s configname
 * <li>-Djava.security.auth.login.config=c:/wherever/jaas.config
 * </ul>
 * The "-s configname" program argument refers to a configuration name in the JAAS configuration file specified in the
 * VM argument -Djava.security.auth.login.config.
 * <p>
 * There are several ways to specify the location of the JAAS configuration file, perhaps the most convenient of which
 * is the VM argument. So this application could be launched as follows:
 * <p>
 * <code>java -Djava.security.auth.login.config="./jaas.config" gov.va.med.vistalink.samples.VistaLinkRpcSwingSimpleCcow -sRpcSample</code>
 * <p>
 * <b>Log4J Initialization</b>
 * <p>
 * For Log4J initialization, the Log4J config file 'log4JConfig.xml' is expected to be in the classpath location:
 * <ul>
 * <li>props/log4jConfig.xml
 * </ul>
 */
public final class VistaLinkRpcSwingSimpleCcow {

	// logger
	private static final Logger logger = Logger.getLogger(VistaLinkRpcSwingSimpleCcow.class);

	// globally accessible controls (contents need to be updated by various methods)
	private JTextArea userInfoTextArea = null;
	private JButton userInfoButton;
	private JButton wpRpcButton;
	private JFrame topFrame = null;
	private JLabel statusLabel = null;

	// other globally accessible variables
	private VistaKernelPrincipalImpl userPrincipal = null; // user principal
	private LoginContext loginContext = null; // JAAS login context
	private String jaasConfigName; // JAAS config name to retrieve

	// constants for general operation
	private static final String RPC_CONTEXT = "XOBV VISTALINK TESTER";

	// ccow constants
	private static final String CCOW_APPLICATION_NAME = "VistaLinkSwingSimpleCcow";
	private static final String CCOW_APPLICATION_PASSCODE = ""; // not a secure binding in the app

	// other ccow fields
	private IClinicalContextBroker ccowContextBroker;
	private ContextModule ccowContextModule;
	private LinkIcon ccowLinkIcon;
	private JPanel ccowLinkRpcCountPanel;
	private SampleAppContextParticipant sampleAppContextParticipant;
	private boolean ccowNoJoin = false;

	// constants for UI controls values
	private static final int TOOLTIP_INITIAL_DELAY = 2500;
	private static final int TOOLTIP_RESHOW_DELAY = 0;

	private static final String STATUS_LABEL_CONNECTED_TEXT = "VistALink Connection Status: Connected";
	private static final String STATUS_LABEL_DISCONNECTED_TEXT = "VistALink Connection Status: Disconnected";
	private static final String STATUS_LABEL_TOOLTIP = "Status of the server connection (connected or disconnected)";

	private static final String USERINFO_BUTTON_TEXT = "User Info";
	private static final String USERINFO_BUTTON_TOOLTIP = "Display User Information";
	private static final char USERINFO_BUTTON_MNEMONIC = KeyEvent.VK_U;

	private static final String WPRPC_BUTTON_TEXT = "Get Word Processing";
	private static final String WPRPC_BUTTON_TOOLTIP = "Get Word Processing text from VistA";
	private static final char WPRPC_BUTTON_MNEMONIC = KeyEvent.VK_W;

	private static final int TEXTAREA_ROW_COUNT = 15;
	private static final int TEXTAREA_COL_COUNT = 65;
	private static final String TEXTAREA_TOOLTIP = "Results";
	
	//text vars
	private static final String TEXT_LOGIN_ERROR = "Login error";
	private static final String TEXT_ARG_ERROR = "Argument Error: '";
	

	/**
	 * One command-line argument needs to be passed to the application (the configuration to use from the JAAS config
	 * file), and another must be passed to the JVM (the JAAS login configuration file).
	 * @param args -s servername -> server config name to use from the JAAS config file
	 */
	VistaLinkRpcSwingSimpleCcow(String[] args) {

		doCommandLineHelp();
		// parse command line arguments
		try {
			parseArgs(args);
		} catch (Exception e) {
			logger.debug("Error: " + e.getMessage());
			logout(-1);
		}

		// create ccow context module -- BEFORE creating the link icon and other visual components
		doCcowSetup();

		// create the main window
		topFrame = new JFrame("VistaLink Simple RPC Demonstration (SSO/UC CCOW-enabled)");

		// set the look and feel to System l+f, prior to creating the topFrame or adding controls
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// swallow
		}

		// setup tooltip delays so tooltips don't become annoying
		ToolTipManager.sharedInstance().setInitialDelay(TOOLTIP_INITIAL_DELAY);
		ToolTipManager.sharedInstance().setReshowDelay(TOOLTIP_RESHOW_DELAY);

		// set up "close" event to force clean M/Kernel logoff when the window is closed
		topFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		topFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				logout(0);
			}
		});

		// add contents to it.
		Component contents = createComponents();
		topFrame.getContentPane().add(contents, BorderLayout.CENTER);

		// set system look and feel, pack frame and set position
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.updateComponentTreeUI(topFrame);
		} catch (ClassNotFoundException e) {
			logger.error(e);
		} catch (InstantiationException e) {
			logger.error(e);
		} catch (IllegalAccessException e) {
			logger.error(e);
		} catch (UnsupportedLookAndFeelException e) {
			logger.error(e);
		}
		topFrame.pack();
		setFramePosition();

		// set default button
		topFrame.getRootPane().setDefaultButton(this.userInfoButton);

		// make it visible, set default focus
		topFrame.setVisible(true);

		// logon
		doLogin();
	}

	/**
	 * main launching point
	 * @param args see the online help sent to system output when launched
	 */
	public static void main(String[] args) {

		DOMConfigurator.configure("props/log4jConfig.xml");
		logger.debug("Starting to run simple Swing sample...");
		SwingUtilities.invokeLater(new launchGui(args));
	}

	/**
	 * Created this launcher as per advice at http://java.sun.com/docs/books/tutorial/uiswing/misc/threads.html.
	 * Essentially we provide this method so that the GUI can be created on the event-dispatching thread. This method
	 * should be called from SwingUtilities.invokeLater in the main() method. An explicit class is declared so we have a
	 * way to pass the command-line args in, otherwise we could have just created an anonymous inner class implementing
	 * Runnable.
	 */
	private static class launchGui implements Runnable {
		private String[] args;

		launchGui(String[] args) {
			this.args = args;
		}

		public void run() {
			new VistaLinkRpcSwingSimpleCcow(args);
		}
	}

	/**
	 * Do CCOW object instantiation/setup
	 */
	private void doCcowSetup() {

		// create ccow context module -- BEFORE creating the link icon and other visual components
		if (!this.ccowNoJoin) {
			ccowContextModule = new ContextModule(CCOW_APPLICATION_NAME, CCOW_APPLICATION_PASSCODE);
		}

		// create context participant
		sampleAppContextParticipant = new SampleAppContextParticipant();

		// create context broker
		if (ccowContextModule != null) {
			ccowContextBroker = ccowContextModule.getBroker(this, sampleAppContextParticipant);
		}

	}

	/**
	 * Do the login
	 */
	private void doLogin() {

		if (userPrincipal != null) {
			statusLabel.setText(STATUS_LABEL_CONNECTED_TEXT);
		} else {

			try {

				// create the callback handler
				CallbackHandlerSwingCCOW cbhSwing = new CallbackHandlerSwingCCOW(this.topFrame, null,
						this.ccowContextBroker);

				// create the LoginContext
				loginContext = new LoginContext(jaasConfigName, cbhSwing);

				// login to server
				loginContext.login();

				// get principal
				userPrincipal = VistaKernelPrincipalImpl.getKernelPrincipal(loginContext.getSubject());

				// set the app's VistaLink 'connected' status to connected.
				statusLabel.setText(STATUS_LABEL_CONNECTED_TEXT);

				// only necessary if we are connected
				if ((ccowContextBroker != null) && ccowContextBroker.isConnected()) {
					// Store user context state, based on VHA user context values
					boolean hadUserContext = CallbackHandlerSwingCCOW.hasNonNullUserContext(ccowContextBroker
							.getContextItems());
					sampleAppContextParticipant.setHadUserContextAtOneTimeInThePast(hadUserContext);
				}

			} catch (VistaLoginModuleNoPathToListenerException e) {

				JOptionPane.showMessageDialog(null, "No path found to specified listener.", TEXT_LOGIN_ERROR,
						JOptionPane.ERROR_MESSAGE);
				statusLabel.setText(STATUS_LABEL_DISCONNECTED_TEXT);
				logout(-1);

			} catch (VistaLoginModuleException e) {

				JOptionPane.showMessageDialog(topFrame, e.getMessage(), TEXT_LOGIN_ERROR, JOptionPane.ERROR_MESSAGE);
				statusLabel.setText(STATUS_LABEL_DISCONNECTED_TEXT);
				logout(-1);

			} catch (LoginException e) {

				JOptionPane.showMessageDialog(topFrame, e.getMessage(), TEXT_LOGIN_ERROR, JOptionPane.ERROR_MESSAGE);
				statusLabel.setText(STATUS_LABEL_DISCONNECTED_TEXT);
				logout(-1);

			} catch (FoundationsException e) {

				JOptionPane.showMessageDialog(topFrame, e.getMessage(), TEXT_LOGIN_ERROR, JOptionPane.ERROR_MESSAGE);
				statusLabel.setText(STATUS_LABEL_DISCONNECTED_TEXT);
				logout(-1);

			} catch (SecurityException e) {

				JOptionPane.showMessageDialog(topFrame, e.getMessage(), TEXT_LOGIN_ERROR, JOptionPane.ERROR_MESSAGE);
				statusLabel.setText(STATUS_LABEL_DISCONNECTED_TEXT);
				logout(-1);
			}
		}
	}

	/**
	 * Do the logout. Make sure to call this method from a window close listener so that connections are always closed
	 * when the application exits.
	 * @param exitCode the code to use as an argument to System.exit().
	 */
	private void logout(int exitCode) {

		logger.debug("in logout.");
		// Kernel logout
		if (this.userPrincipal != null) {

			try {

				loginContext.logout();

			} catch (LoginException e) {

				JOptionPane.showMessageDialog(topFrame, e.getMessage(), "Logout error", JOptionPane.ERROR_MESSAGE);
				statusLabel.setText(STATUS_LABEL_DISCONNECTED_TEXT);

			}

			statusLabel.setText(STATUS_LABEL_DISCONNECTED_TEXT);
			userPrincipal = null;
		}

		// always shut down ccow before exiting
		logger.debug("shutting down CCOW.");
		if (ccowContextBroker != null) {
			if (ccowContextBroker.isConnected()) {
				ccowContextBroker.breakExternalLink();
			}
		}
		if (ccowContextModule != null) {
			ccowContextModule.shutdown();
		}

		// terminate the application
		System.exit(exitCode);
	}

	/**
	 * event handler for WP button press
	 */
	private void doWordProcessingRpc() {

		if (userPrincipal == null) {

			JOptionPane.showMessageDialog(topFrame, "Not logged on.", "Error", JOptionPane.ERROR_MESSAGE);

		} else {

			try {

				VistaLinkConnection myConnection = userPrincipal.getAuthenticatedConnection();
				RpcRequest vReq = RpcRequestFactory.getRpcRequest(RPC_CONTEXT, "XOBV TEST WORD PROCESSING");
				RpcResponse vResp = myConnection.executeRPC(vReq);
				userInfoTextArea.setText(vResp.getResults());
				userInfoTextArea.setCaretPosition(0);

			} catch (Exception e) {

				JOptionPane.showMessageDialog(topFrame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

			}
		}
	}

	/**
	 * event handler for user info button press
	 */
	private void doUserInfoDisplay() {

		if (userPrincipal == null) {

			JOptionPane.showMessageDialog(topFrame, "Not logged on.", "Error", JOptionPane.ERROR_MESSAGE);

		} else {

			StringBuffer sb = new StringBuffer();
			if ((ccowContextBroker != null) && (ccowContextBroker.isConnected())) {

				Map items = ccowContextBroker.getContextItems();
				for (int i = 0; i < CallbackHandlerSwingCCOW.VHA_CCOW_USER_CONTEXT_KEYS.length; i++) {
					IContextItemName key = ContextItemNameFactory
							.getName(CallbackHandlerSwingCCOW.VHA_CCOW_USER_CONTEXT_KEYS[i]);
					sb.append(key);
					sb.append(": ");
					sb.append(items.get(key));
					sb.append('\n');
				}
				sb.append('\n');

			} else {

				sb.append("CCOW context broker is not connected.\n\n");
			}

			sb.append("DUZ: ").append(this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_DUZ));
			sb.append("\nVPID: ").append(this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_VPID));
			sb.append("\nName (New Person .01): ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_NEWPERSON01));
			sb.append("\nName, Standard Concatenated: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_DISPLAY));
			sb.append("\nName, Prefix: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_PREFIX));
			sb.append("\nName, Given First: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_GIVENFIRST));
			sb.append("\nName, Middle: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_MIDDLE));
			sb.append("\nName, Family Last: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_FAMILYLAST));
			sb.append("\nName, Suffix: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_SUFFIX));
			sb.append("\nName, Degree: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_DEGREE));
			sb.append("\nTitle: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_TITLE));
			sb.append("\nService/Section: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_SERVICE_SECTION));
			sb.append("\nDivision IEN: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_DIVISION_IEN));
			sb.append("\nDivision Name: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_DIVISION_STATION_NAME));
			sb.append("\nDivision Number: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_DIVISION_STATION_NUMBER));
			sb.append("\nDTIME: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_DTIME));
			sb.append("\nLanguage: ").append(
					this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_LANGUAGE)).append('\n');

			userInfoTextArea.setText(sb.toString());
			userInfoTextArea.setCaretPosition(0);

		}
	}

	/**
	 * create all components for the window
	 * @return Component
	 */
	private Component createComponents() {

		// create main panel
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		// add contained panels
		mainPanel.add(createStatusLabelPanel(), BorderLayout.NORTH);
		mainPanel.add(createTextAreaPanel(), BorderLayout.CENTER);
		mainPanel.add(createCcowIconPanel(), BorderLayout.SOUTH);

		return mainPanel;
	}

	private JPanel createStatusLabelPanel() {

		// Status label
		statusLabel = new JLabel();
		statusLabel.setText(STATUS_LABEL_DISCONNECTED_TEXT);
		statusLabel.setToolTipText(STATUS_LABEL_TOOLTIP);
		statusLabel.setFocusable(false);

		// panel for status label
		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new FlowLayout());
		labelPanel.add(statusLabel);

		return labelPanel;
	}

	/**
	 * Create panel for the WP RPC
	 * @return JComponent
	 */
	private JPanel createTextAreaPanel() {
		JPanel myPanel = new JPanel();
		myPanel.setLayout(new BorderLayout());

		// user info button
		userInfoButton = new JButton();
		userInfoButton.setText(USERINFO_BUTTON_TEXT);
		userInfoButton.setToolTipText(USERINFO_BUTTON_TOOLTIP);
		userInfoButton.setMnemonic(USERINFO_BUTTON_MNEMONIC);
		userInfoButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doUserInfoDisplay();
			}
		});

		// wp info button
		wpRpcButton = new JButton();
		wpRpcButton.setText(WPRPC_BUTTON_TEXT);
		wpRpcButton.setToolTipText(WPRPC_BUTTON_TOOLTIP);
		wpRpcButton.setMnemonic(WPRPC_BUTTON_MNEMONIC);
		wpRpcButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doWordProcessingRpc();
			}
		});

		// button panel
		JPanel buttonPanel = new JPanel();
		FlowLayout buttonLayout = new FlowLayout();
		buttonLayout.setAlignment(FlowLayout.LEFT);
		buttonPanel.setLayout(buttonLayout);
		buttonPanel.add(userInfoButton);
		buttonPanel.add(wpRpcButton);
		myPanel.add(buttonPanel, BorderLayout.NORTH);

		// text area
		userInfoTextArea = new JTextArea();
		userInfoTextArea.setEditable(false);
		userInfoTextArea.setRows(TEXTAREA_ROW_COUNT);
		userInfoTextArea.setColumns(TEXTAREA_COL_COUNT);
		userInfoTextArea.setToolTipText(TEXTAREA_TOOLTIP);

		// scrollpane for text area
		JScrollPane scrollPane = new JScrollPane(userInfoTextArea);
		Border bevelOut = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		Border bevelIn = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
		scrollPane.setBorder(BorderFactory.createCompoundBorder(bevelOut, bevelIn));
		myPanel.add(scrollPane, BorderLayout.CENTER);

		return myPanel;
	}

	private void setFramePosition() {

		Dimension frameDimension = topFrame.getSize();

		// position the topFrame
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (frameDimension.height > screenSize.height) {
			frameDimension.height = screenSize.height;
		}
		if (frameDimension.width > screenSize.width) {
			frameDimension.width = screenSize.width;
		}
		topFrame.setLocation((screenSize.width - frameDimension.width) / 2,
				(screenSize.height - frameDimension.height) / 2);
	}

	/**
	 * parses the command-line arguments
	 * @param args
	 */
	private void parseArgs(String[] args) {

		int i = 0;
		if (args.length < 1) {

			// if no args passed
			throw new IllegalArgumentException("Expected -s parameter, did not recieve one.");

		} else {

			do {
				// first do checks
				if (args[i].substring(0, 1).equals("-")) {
					if (i + 1 < args.length) {
						if (((String) args[i + 1]).substring(0, 1).equals("-")) {
							String errStr = TEXT_ARG_ERROR + args[i]
									+ "' command line parameter cannot be followed by a flag (" + args[i + 1] + ")";
							logger.error(errStr);
							throw new IllegalArgumentException(errStr);
						}
					} else {
						String errStr = TEXT_ARG_ERROR + args[i]
								+ "' command line parameter must be followed by a value";
						logger.error(errStr);
						throw new IllegalArgumentException(errStr);
					}
				} else {

					String errStr = TEXT_ARG_ERROR + args[i]
							+ "' command line parameter expected to be a -flag parameter.";
					logger.error(errStr);
					throw new IllegalArgumentException(errStr);

				}

				// passed checks
				if (args[i].equals("-s")) {

					this.jaasConfigName = args[++i];

				} else if (args[i].equals("-c")) {

					this.ccowNoJoin = "NO".equals((args[++i]).toUpperCase()) ? true : false;

				} else {
					String errStr = TEXT_ARG_ERROR + args[i] + "' is an unsupported flag.";
					logger.error(errStr);
					throw new IllegalArgumentException(errStr);
				}

				i++;

			} while (i < args.length);
		}
	}

	/**
	 * Create a JPanel for the CCOW link icon
	 * @return
	 */
	private JPanel createCcowIconPanel() {
		ccowLinkRpcCountPanel = new JPanel();
		ccowLinkRpcCountPanel.setName("CCOW Icon Panel");
		ccowLinkRpcCountPanel.setLayout(new BorderLayout());
		// link logo
		ccowLinkIcon = new LinkIcon(ccowContextModule, this);
		ccowLinkRpcCountPanel.add(ccowLinkIcon, BorderLayout.WEST);
		return ccowLinkRpcCountPanel;
	}

	/**
	 * This class implements the sample application's implementation of following context. The sample application does
	 * not follow patient context, only user context. The class implements the required interface for the HDS CCOW
	 * libraries for a context observer/participant. Each application should implements a class with these interfaces,
	 * specific to its own app-specific needs. An instantiated object of this class is then passed as a parameter by the
	 * app when creating IClinicalContextBroker object.
	 */
	private class SampleAppContextParticipant implements IContextObserver, IContextParticipant {

		// used for deciding whether to sign off based on user context
		private boolean hadUserContextAtOneTimeInThePast = false;
		// object for synchronization
		private Object syncObject = new Object();

		/**
		 * This method is called by the context management broker to alert the component to a change in context.
		 */
		public void contextChanged() {
			logger.debug("entered contextChanged.");
			logger.debug("ccowContextBroker.isConnected(): " + ccowContextBroker.isConnected());

			if (!ccowContextBroker.isConnected()) {
				// if we're not connected, then we're breaking context.
				this.setHadUserContextAtOneTimeInThePast(false);
			} else {
				// if we're connected, we're either 1) already in context,
				// or 2) joining context.
				Map currentContext = ccowContextBroker.getContextItems();
				if (CallbackHandlerSwingCCOW.hasNonNullUserContext(currentContext)) {
					// set user context flag to true since we have user
					// context after this change
					this.setHadUserContextAtOneTimeInThePast(true);
				} else if (getHadUserContextAtOneTimeInThePast()) {
					// shut down if a) already in context, b) believe
					// user context is null and is changing from not null
					logout(0);
				} else {
					// set user context flag to false sinec we don't have
					// user context after this change.
					this.setHadUserContextAtOneTimeInThePast(false);
				}
			}
		}

		/**
		 * The HDS CCOW libraries call this method to inform a context participant that an external context change
		 * operation has been requested.
		 * 
		 * The participant is not allowed to display any dialogs to the user at this time.
		 * @param contextItems a list of items in the context that could potentially change
		 * @return a string describing the consequences of changing the context (for example, loss of unsaved data). If
		 *         the proposed context changes would not have adverse consequences, this method should return null.
		 */
		public String reviewContextChange(Map contextItems) {
			logger.debug("entered reviewContextChange.");
			logger.debug("VHA user context items in proposed change:");
			outputVhaUserContextItems(contextItems);
			// this simple sample app never objects to a context change
			return null;
		}

		/**
		 * The HDS CCOW libraries call this method to inform a context participant that an internal context change
		 * operation is about to begin.
		 * 
		 * If this participant needs the user's permission to perform any action in preparation for the context change,
		 * the participant is allowed to display modal dialog boxes to the user asking for that permission. For example,
		 * an application might display a list of unsigned items and ask the user if they want to sign the items. These
		 * dialog boxes should include a Cancel button.
		 * 
		 * @param contextItems a list of items in the context that could potentially change
		 * @return false if the user selected a Cancel option, true otherwise
		 */
		public boolean prepareForContextChange(Set contextItems) {
			logger.debug("entered prepareForContextChange.");

			Iterator it = contextItems.iterator();
			while (it.hasNext()) {
				logger.debug("Context change item names: " + (IContextItemName) it.next());
			}
			// this simple sample app never objects to a context change
			return true;
		}

		/**
		 * Used by the application after login, to store the context item state post-login.
		 * @param postLoginContextItems
		 */
		public void setHadUserContextAtOneTimeInThePast(boolean hadContextInPast) {
			// we synchronize since two threads could write/access the
			// postLoginContextItems object
			synchronized (this.syncObject) {
				this.hadUserContextAtOneTimeInThePast = hadContextInPast;
			}
			logger.debug("Set hadUserContextAtOneTimeInThePast to " + hadContextInPast);
		}

		/**
		 * Use getter to always get the synchronization.
		 * @return current value of this property
		 */
		private boolean getHadUserContextAtOneTimeInThePast() {
			synchronized (this.syncObject) {
				return this.hadUserContextAtOneTimeInThePast;
			}
		}

		/**
		 * Required method; if this was clin. desktop, this method would be called by the context management plugin to
		 * find out which patient the context observer is displaying data for.
		 * 
		 * @returns a map containing one or more name/value pairs that correspond to data elements in the CCOW patient
		 *          subject.
		 */
		public Map getDisplayedPatient() {
			logger.debug("entered getDisplayedPatient.");
			// this sample app is not tracking patient
			return null;
		}

		/**
		 * Output VHA user context items to debug
		 * @param contextItems context items to output
		 */
		private void outputVhaUserContextItems(Map contextItems) {
			// output to debug, each of the items in the context
			for (int i = 0; i < CallbackHandlerSwingCCOW.VHA_CCOW_USER_CONTEXT_KEYS.length; i++) {
				IContextItemName key = ContextItemNameFactory
						.getName(CallbackHandlerSwingCCOW.VHA_CCOW_USER_CONTEXT_KEYS[i]);
				String value = (String) contextItems.get(key);
				logger.debug("context item key '" + key + "', value: '" + value + "'.");
			}
		}
	}

	/**
	 * Prints help on command-line arguments needed for this app to the console.
	 */
	static void doCommandLineHelp() {
		System.out.println("\nVistaLinkRpcSwingSimpleCcow command line arguments:");
		System.out.println("\nRequired program command-line argument:");
		System.out.println("\n    -s configName    (config name from JAAS configuration to connect to)");
		System.out.println("\nOptional program command-line argument:");
		System.out.println("\n    -c no    (for testing, tells test app not to join CCOW context)");
		System.out.println("\nRequired VM argument (if configuring JAAS w/JAAS configuration file):");
		System.out.println("\n    -Djava.security.auth.login.config=configFile");
		System.out.println("\ne.g., a total command line would look like:");
		System.out.println("\njava -Djava.security.auth.login.config=./jaas.config VistaLinkRpcSwingSimpleCcow -s TestServer");
		System.out.println("\nAlso, for optional Log4J initialization, the Log4J config file 'log4JConfig.xml' is");
		System.out.println("looked for in the classpath location props/log4jConfig.xml");
	}
}
