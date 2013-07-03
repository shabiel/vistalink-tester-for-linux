package gov.va.med.vistalink.samples;

import gov.va.med.vistalink.adapter.cci.VistaLinkConnection;
import gov.va.med.vistalink.rpc.RpcRequest;
import gov.va.med.vistalink.rpc.RpcRequestFactory;
import gov.va.med.vistalink.rpc.RpcResponse;
import gov.va.med.exception.FoundationsException;
import gov.va.med.vistalink.security.CallbackHandlerSwing;
import gov.va.med.vistalink.security.VistaKernelPrincipalImpl;
import gov.va.med.vistalink.security.VistaLoginModuleException;
import gov.va.med.vistalink.security.VistaLoginModuleNoPathToListenerException;

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
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Swing application demonstration of VistALink's Vista connectivity and RPC functionality.
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
 * <code>java -Djava.security.auth.login.config="./jaas.config" gov.va.med.vistalink.samples.VistaLinkRpcSwingSimple -sRpcSample
 * </code>
 * <p>
 * <b>Log4J Initialization</b>
 * <p>
 * For Log4J initialization, the Log4J config file 'log4JConfig.xml' is expected to be in the classpath location:
 * <ul>
 * <li>props/log4jConfig.xml
 * </ul>
 */
public final class VistaLinkRpcSwingSimple {

	// logger
	private static final Logger LOGGER = Logger.getLogger(VistaLinkRpcSwingSimple.class);

	// globally accessible controls (contents need to be updated by various methods)
	private JTextArea wpResultsTextArea = null;
	private JButton wpButton;
	private JFrame topFrame = null;
	private JLabel statusLabel = null;

	// other globally accessible variables
	private VistaKernelPrincipalImpl userPrincipal = null; // user principal
	private LoginContext loginContext = null; // JAAS login context
	private String serverName = ""; // Server appname to connect to

	// constants for general operation
	private static final String RPC_CONTEXT = "XOBV VISTALINK TESTER";

	// constants for UI controls values
	private static final String STATUS_LABEL_CONNECTED_TEXT = "VistALink Connection Status: Connected";
	private static final String STATUS_LABEL_DISCONNECTED_TEXT = "VistALink Connection Status: Disconnected";
	private static final String STATUS_LABEL_TOOLTIP = "Status of the server connection (connected or disconnected)";

	private static final String WP_BUTTON_TEXT = "Get Word Processing";
	private static final String WP_BUTTON_TOOLTIP = "Retrieve Word Processing data from VistA";
	private static final char WP_BUTTON_MNEMONIC = KeyEvent.VK_W;

	private static final int RESULT_ROW_COUNT = 15;
	private static final int RESULT_COL_COUNT = 45;

	private static final String RESULT_TOOLTIP = "RPC Results";
	
	// text vars
	private static final String TEXT_LOGIN_ERROR = "Login error";
	private static final String TEXT_ARG_ERROR = "Argument Error: '";

	/**
	 * One command-line argument needs to be passed to the application (the configuration to use from the JAAS config
	 * file), and another must be passed to the JVM (the JAAS login configuration file).
	 * @param args -s servername -> server config name to use from the JAAS config file
	 */
	VistaLinkRpcSwingSimple(String[] args) {

		doCommandLineHelp();
		// parse command line arguments
		try {
			parseArgs(args);
		} catch (Exception e) {
			LOGGER.debug("Error parsing command-line: ", e);
			logout(-1);
		}

		// create the main window
		topFrame = new JFrame("VistaLink Simple RPC Demonstration");

		// add contents to it.
		Component contents = createComponents();
		topFrame.getContentPane().add(contents, BorderLayout.CENTER);

		// set system look and feel, pack frame and set position
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.updateComponentTreeUI(topFrame);
		} catch (ClassNotFoundException e1) {
			LOGGER.error(e1);
		} catch (InstantiationException e1) {
			LOGGER.error(e1);
		} catch (IllegalAccessException e1) {
			LOGGER.error(e1);
		} catch (UnsupportedLookAndFeelException e1) {
			LOGGER.error(e1);
		}
		topFrame.pack();
		setFramePosition();

		// set default button
		topFrame.getRootPane().setDefaultButton(this.wpButton);

		// set up "close" event to force clean M/Kernel logoff when the window is closed
		topFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		topFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				logout(0);
			}
		});

		// make it visible, set default focus
		topFrame.setVisible(true);

		// logon
		login();
	}

	/**
	 * @param args see the online help sent to system output when launched
	 */
	public static void main(String[] args) {

		DOMConfigurator.configure("props/log4jConfig.xml");
		LOGGER.debug("Starting to run simple Swing sample...");
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
			new VistaLinkRpcSwingSimple(args);
		}
	}

	/**
	 * Do the login
	 */
	private void login() {

		if (userPrincipal != null) {

			statusLabel.setText(STATUS_LABEL_CONNECTED_TEXT);

		} else {

			try {

				// create the callback handler
				CallbackHandlerSwing cbhSwing = new CallbackHandlerSwing(topFrame);

				// create the LoginContext
				loginContext = new LoginContext(serverName, cbhSwing);

				// login to server
				loginContext.login();

				// get principal
				userPrincipal = VistaKernelPrincipalImpl.getKernelPrincipal(loginContext.getSubject());

				statusLabel.setText(STATUS_LABEL_CONNECTED_TEXT);

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
	 */
	private void logout(int exitCode) {

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
				wpResultsTextArea.setText(vResp.getResults());
				wpResultsTextArea.setCaretPosition(0);

			} catch (Exception e) {

				JOptionPane.showMessageDialog(topFrame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

			}
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
		mainPanel.add(createWordProcessingPanel(), BorderLayout.CENTER);

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
	private JPanel createWordProcessingPanel() {
		JPanel myPanel = new JPanel();
		myPanel.setLayout(new BorderLayout());

		// button
		JPanel buttonPanel = new JPanel();
		FlowLayout buttonLayout = new FlowLayout();
		buttonLayout.setAlignment(FlowLayout.LEFT);
		buttonPanel.setLayout(buttonLayout);
		wpButton = new JButton();
		wpButton.setText(WP_BUTTON_TEXT);
		wpButton.setToolTipText(WP_BUTTON_TOOLTIP);
		wpButton.setMnemonic(WP_BUTTON_MNEMONIC);
		wpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doWordProcessingRpc();
			}
		});
		buttonPanel.add(wpButton);
		myPanel.add(buttonPanel, BorderLayout.NORTH);

		// text area and scrollpane
		wpResultsTextArea = new JTextArea();
		wpResultsTextArea.setEditable(false);
		wpResultsTextArea.setRows(RESULT_ROW_COUNT);
		wpResultsTextArea.setColumns(RESULT_COL_COUNT);
		wpResultsTextArea.setToolTipText(RESULT_TOOLTIP);

		JScrollPane scrollPane = new JScrollPane(wpResultsTextArea);
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
							LOGGER.error(errStr);
							throw new IllegalArgumentException(errStr);
						}
					} else {
						String errStr = TEXT_ARG_ERROR + args[i]
								+ "' command line parameter must be followed by a value";
						LOGGER.error(errStr);
						throw new IllegalArgumentException(errStr);
					}
				} else {

					String errStr = TEXT_ARG_ERROR + args[i]
							+ "' command line parameter expected to be a -flag parameter.";
					LOGGER.error(errStr);
					throw new IllegalArgumentException(errStr);

				}

				// passed checks
				if (args[i].equals("-s")) {

					this.serverName = args[++i];

				} else {
					String errStr = TEXT_ARG_ERROR + args[i] + "' is an unsupported flag.";
					LOGGER.error(errStr);
					throw new IllegalArgumentException(errStr);
				}

				i++;

			} while (i < args.length);
		}
	}

	/**
	 * Prints help on command-line arguments needed for this app to the console.
	 */
	static void doCommandLineHelp() {
		System.out.println("\nVistaLinkRpcSwingSimple required program command-line argument:");
		System.out.println("\n    -s server    (appname from JAAS configuration to connect to)");
		System.out.println("\nRequired VM argument (if configuring JAAS w/JAAS configuration file):");
		System.out.println("\n    -Djava.security.auth.login.config=configFile");
		System.out.println("\ne.g., a total command line would look like:");
		System.out.println("\njava -Djava.security.auth.login.config=./jaas.config VistaLinkRpcSwingSimple -s TestServer");
		System.out.println("\nAlso, for optional Log4J initialization, the Log4J config file 'log4JConfig.xml' is");
		System.out.println("looked for in the classpath location props/log4jConfig.xml");
	}

}
