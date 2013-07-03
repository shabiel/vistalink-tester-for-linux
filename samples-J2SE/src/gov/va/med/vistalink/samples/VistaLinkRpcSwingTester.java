package gov.va.med.vistalink.samples;

import gov.va.med.vistalink.adapter.cci.VistaLinkConnection;
import gov.va.med.vistalink.rpc.RpcReferenceType;
import gov.va.med.vistalink.rpc.RpcRequest;
import gov.va.med.vistalink.rpc.RpcRequestFactory;
import gov.va.med.vistalink.rpc.RpcResponse;
import gov.va.med.exception.FoundationsException;
import gov.va.med.vistalink.security.CallbackHandlerSwing;
import gov.va.med.vistalink.security.DialogConfirm;
import gov.va.med.vistalink.security.VistaKernelPrincipalImpl;
import gov.va.med.vistalink.security.VistaLoginModuleException;
import gov.va.med.vistalink.security.VistaLoginModuleLoginsDisabledException;
import gov.va.med.vistalink.security.VistaLoginModuleNoJobSlotsAvailableException;
import gov.va.med.vistalink.security.VistaLoginModuleNoPathToListenerException;
import gov.va.med.vistalink.security.VistaLoginModuleTooManyInvalidAttemptsException;
import gov.va.med.vistalink.security.VistaLoginModuleUserCancelledException;
import gov.va.med.vistalink.security.VistaLoginModuleUserTimedOutException;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.accessibility.AccessibleContext;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.FontUIResource;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Swing application demonstration of VistALink's Vista connectivity and RPC functionality.
 * <p>
 * NOTE: This application is a demonstration of how to use VistALink. It is *not* meant to be a demonstration of Swing
 * development &quot;best practices&quot;.
 * <p>
 * <b>Command-Line Parameters </b>
 * <p>
 * This application optionally uses a JAAS configuration file path name to read in configuration settings to connect to
 * a Vista server. Specify it on the command line as follows:
 * <ul>
 * <li>-Djava.security.auth.login.config==./jaas.config
 * </ul>
 * <b>Log4J Initialization </b>
 * <p>
 * For Log4J initialization, the Log4J config file 'log4JConfig.xml' is expected to be in the classpath location:
 * <ul>
 * <li>props/log4jConfig.xml
 * </ul>
 */
public final class VistaLinkRpcSwingTester implements ActionListener /* , PropertyChangeListener */{

	// globally accessible controls (contents need to be updated by various methods)
	private JLabel serverComboBoxLabel;
	private JLabel statusLabel;
	private JTextField statusTextField;
	private JLabel stringResultLabel;
	private JTextField stringResultTextField;
	private JTextField variableResultTextField;
	private JComboBox refTypeComboBox;
	private JComboBox arrayTypeComboBox;
	private JTextArea wpResultsTextArea;
	private JTextArea userTextArea;
	private JTextArea arrayTextArea;
	private JTextArea rpcResultsTextArea;
	private JTextField rpcListNamespaceTextField;
	private JTextField stringTextField;
	private JComboBox serverComboBox;
	private JTabbedPane tabPane;
	private JPanel messageFormatPanel;
	private JPanel mainPanel;
	private JRadioButton xmlRadioButton;
	private JRadioButton proprietaryRadioButton;
	private JButton closeButton;
	private JButton connectButton;
	private JButton pingButton;
	private JButton clearButton;
	private JFrame topFrame;
	private JButton wpButton;
	private JButton userButton;
	private JButton arrayButton;
	private JButton stringButton;
	private JButton variableButton;
	private JButton rpcButton;
	private JTextField rpcCountTextField;
	private JCheckBoxMenuItem windowsMenuItem;
	private JCheckBoxMenuItem javaMenuItem;
	private JCheckBoxMenuItem systemMenuItem;
	private JCheckBoxMenuItem motifMenuItem;
	private JCheckBoxMenuItem gtkMenuItem;
	private JScrollPane wpResultsScrollPane;
	private JScrollPane rpcResultsScrollPane;
	private JScrollPane arrayTextScrollPane;
	private JScrollPane userTextScrollPane;
	private JLabel ipLabel;
	private JLabel portLabel;
	private JTextField ipTextField;
	private JTextField portTextField;

	// focus traversal policies
	private ConnectedFocusTraversalPolicy connectedFocusTraversalPolicy;
	private DisconnectedFocusTraversalPolicy disconnectedFocusTraversalPolicy;

	// other globally accessible variables
	private int rpcCount = 0; // rpc exeuction counter
	private VistaKernelPrincipalImpl userPrincipal; // user principal
	private LoginContext loginContext; // JAAS login context
	private int timeout;

	// constants for general operation
	private static final String RPC_CONTEXT = "XOBV VISTALINK TESTER";
	private static final Logger logger = Logger.getLogger(VistaLinkRpcSwingTester.class);
	//	private static final String CONFIG_FILE_DEFAULT_PATH_NAME = "/gov/va/med/foundations/samples/ServerConfig.xml";
	private static final int RESULT_ROW_COUNT = 15;
	private static final int TEXT_FIELD_COLUMNS = 15;
	private static final int DEFAULT_TIMEOUT = 300;
	private static final String VISIBLE_PROPERTY_CHANGE_STR = "VISIBLE_PROPERTY_CHANGE";

	// constants for UI controls values
	private static final String REF_TYPE_TRUE = "DT, Using RefType Param";
	private static final String REF_TYPE_FALSE = "DTIME, Not Using RefType Param";
	private static final String ARRAY_TYPE_LOCAL = "Local Array";
	private static final String ARRAY_TYPE_GLOBAL = "Global Array";

	private static final String XML_BUTTON_STRING = "XML";
	private static final char XML_BUTTON_MNEMONIC = KeyEvent.VK_X;
	private static final String XML_BUTTON_TOOLTIP = "Set message exchange mode with M server to XML format";

	private static final String PROPRIETARY_BUTTON_STRING = "Proprietary";
	private static final char PROPRIETARY_BUTTON_MNEMONIC = KeyEvent.VK_O;
	private static final String PROPRIETARY_BUTTON_TOOLTIP = "Set message exchange mode with M server to proprietary format";

	private static final String CONNECT_BUTTON_TEXT = "Connect";
	private static final char CONNECT_BUTTON_MNEMONIC = KeyEvent.VK_C;
	private static final String CONNECT_BUTTON_TOOLTIP = "Connect to the selected M server";

	private static final String DISCONNECT_BUTTON_TEXT = "Disconnect";
	private static final char DISCONNECT_BUTTON_MNEMONIC = KeyEvent.VK_D;
	private static final String DISCONNECT_BUTTON_TOOLTIP = "Disconnect from the M server";

	private static final String PING_BUTTON_TEXT = "Ping";
	private static final char PING_BUTTON_MNEMONIC = KeyEvent.VK_P;
	private static final String PING_BUTTON_TOOLTIP = "Ping the M server.";

	private static final String CLEAR_BUTTON_TEXT = "Clear Results";
	private static final char CLEAR_BUTTON_MNEMONIC = KeyEvent.VK_C;
	private static final String CLEAR_BUTTON_TOOLTIP = "Clears all results";

	private static final String STATUS_LABEL_TEXT = "Status: ";
	private static final String STATUS_LABEL_TOOLTIP = "Status of the server connection (connected or disconnected)";
	private static final String STATUS_CONNECTED_TEXT = " Connected ";
	private static final String STATUS_DISCONNECTED_TEXT = " Disconnected ";
	private static final char STATUS_MNEMONIC = KeyEvent.VK_S;

	private static final String SERVER_PANEL_TITLE = "VistA Server";

	private static final String MESSAGE_FORMAT_TEXT = "Message Format";

	private static final String IP_LABEL_TEXT = "IP: ";
	private static final String IP_LABEL_TOOLTIP = "IP address of server to connect to";
	private static final char IP_LABEL_MNEMONIC = KeyEvent.VK_I;
	private static final int IP_TEXTFIELD_COLUMNS = 20;

	private static final String PORT_LABEL_TEXT = "Port:";
	private static final String PORT_LABEL_TOOLTIP = "Port of server to connect to";
	private static final char PORT_LABEL_MNEMONIC = KeyEvent.VK_P;
	private static final int PORT_TEXTFIELD_COLUMNS = 6;

	private static final String SERVER_ALIAS_LABEL_TEXT = "Or enter JAAS Config Name: ";
	private static final char SERVER_ALIAS_LABEL_MNEMONIC = KeyEvent.VK_J;
	private static final String SERVER_ALIAS_LABEL_TOOLTIP = "Select server to connect to";

	private static final String SERVER_COMBOBOX_TOOLTIP = "List of configured servers to connect to";

	//	private static final String TAB_PANE_TOOLTIP = "Execute Demonstration RPCs";

	private static final String TAB_USER_TEXT = "User Info";
	private static final String TAB_USER_BUTTON_TEXT = "Get user information";
	private static final String TAB_USER_NO_RPC_LABEL_TEXT = "User Information Result (note: Get user information does not execute an RPC):";
	private static final String TAB_USER_TOOLTIP = "Retrieve information about logged-on user from VistaKernelPrincipal object";
	private static final char TAB_USER_MNEMONIC = KeyEvent.VK_U;

	private static final String TAB_WP_TEXT = "WP";
	private static final String TAB_WP_BUTTON_TEXT = "Get Word Processing";
	private static final String TAB_WP_TOOLTIP = "Retrieve Word Processing data from VistA";
	private static final char TAB_WP_MNEMONIC = KeyEvent.VK_W;

	private static final String TAB_STRING_TEXT = "String";
	private static final String TAB_STRING_TEXTFIELD_TOOLTIP = "Enter text to echo:";
	private static final char TAB_STRING_TEXTFIELD_MNEMONIC = KeyEvent.VK_E;
	private static final String TAB_STRING_LABEL_TEXT = "Enter a string: ";
	private static final String TAB_STRING_BUTTON_TEXT = "Echo/Get string back from M";
	private static final String TAB_STRING_TOOLTIP = "Echo a string to/from VistA";
	private static final char TAB_STRING_MNEMONIC = KeyEvent.VK_N;

	private static final String TAB_RPCLIST_TEXT = "RPC List";
	private static final String TAB_RPCLIST_LABEL_TEXT = "Enter namespace: ";
	private static final String TAB_RPCLIST_TEXTFIELD_TOOLTIP = "Enter case-sensitive partial namespace (no wildcards)";
	private static final String TAB_RPCLIST_BUTTON_TEXT = "Get RPC List";
	private static final String TAB_RPCLIST_TOOLTIP = "Retrieve list of \"partial-match\" RPC entries from VistA";
	private static final char TAB_RPCLIST_MNEMONIC = KeyEvent.VK_I;

	private static final String TAB_VARIABLE_TEXT = "Variable";
	private static final String TAB_VARIABLE_BUTTON_TEXT = "Get...";
	private static final String TAB_VARIABLE_LABEL_TEXT = "Variable type: ";
	private static final char TAB_VARIABLE_LABEL_MNEMONIC = KeyEvent.VK_E;
	private static final String TAB_VARIABLE_COMBOBOX_TOOLTIP = "Select type of variable to retrieve";
	private static final String TAB_VARIABLE_TOOLTIP = "Retrieve variable from VistA";
	private static final char TAB_VARIABLE_MNEMONIC = KeyEvent.VK_V;

	private static final String TAB_ARRAY_TEXT = "Array";
	private static final String TAB_ARRAY_BUTTON_TEXT = "Echo/Get ...";
	private static final String TAB_ARRAY_COMBOBOX_TEXT = "Type of Array to Echo";
	private static final char TAB_ARRAY_LABEL_MNEMONIC = KeyEvent.VK_E;
	private static final String TAB_ARRAY_TOOLTIP = "Echo Array to/from VistA";
	private static final char TAB_ARRAY_MNEMONIC = KeyEvent.VK_Y;

	private static final String MENU_LANDF_TEXT = "Set Look and Feel";
	private static final String MENU_LANDF_TOOLTIP = "Choose look and feel of application from a variety of choices";
	private static final int MENU_LANDF_MNEMONIC = KeyEvent.VK_L;

	private static final String MENU_WINDOWS_TEXT = "Windows";
	private static final String MENU_WINDOWS_TOOLTIP = "Set Look and Feel to Windows";
	private static final int MENU_WINDOWS_MNEMONIC = KeyEvent.VK_W;

	private static final String MENU_JAVA_TEXT = "Java";
	private static final String MENU_JAVA_TOOLTIP = "Set Look and Feel to Java";
	private static final int MENU_JAVA_MNEMONIC = KeyEvent.VK_J;

	private static final String MENU_MOTIF_TEXT = "Motif";
	private static final String MENU_MOTIF_TOOLTIP = "Set Look and Feel to Motif";
	private static final int MENU_MOTIF_MNEMONIC = KeyEvent.VK_M;

	private static final String MENU_GTK_TEXT = "GTK (1.4.2 or above)";
	private static final String MENU_GTK_TOOLTIP = "Set Look and Feel to GTK";
	private static final int MENU_GTK_MNEMONIC = KeyEvent.VK_G;

	private static final String MENU_SYSTEM_TEXT = "System";
	private static final String MENU_SYSTEM_TOOLTIP = "Set Look and Feel to System Default";
	private static final int MENU_SYSTEM_MNEMONIC = KeyEvent.VK_S;

	private static final String MENU_HELP_TEXT = "Help";
	private static final String MENU_HELP_TOOLTIP = "Information About This Application";
	private static final int MENU_HELP_MNEMONIC = KeyEvent.VK_H;

	private static final String MENU_508_TEXT = "Section 508 Information";
	private static final String MENU_508_TOOLTIP = "Display Section 508 compliance information for this application";
	private static final int MENU_508_MNEMONIC = KeyEvent.VK_5;
	private static final String[] SECTION_508_DISCLAIMER = {
			"V H A’s Office of Information, Health Systems Design & Development staff have made every",
			"effort during the design, development and testing of this application to ensure full",
			"accessibility to all users in compliance with Section 508 of the Rehabilitation Act of 1973,",
			"as amended. Please send any comments, questions or concerns regarding the accessibility",
			"of this application to s d d migration @ m e d dot v a dot gov [sddmigration@med.va.gov]." };

	private static final String RESULT_LABEL_TEXT = "Result: ";
	private static final String RESULT_TOOLTIP = "RPC Results";
	private static final char RESULT_MNEMONIC = KeyEvent.VK_R;

	private static final String RPC_COUNT_LABEL_TEXT = "Application RPC Execution Count: ";
	private static final String RPC_COUNT_TOOLTIP = "Number of RPC executions for this session";
	private static final char RPC_COUNT_MNEMONIC = KeyEvent.VK_A;

	private static final char GET_MNEMONIC = KeyEvent.VK_G;

	private static final int TOOLTIP_INITIAL_DELAY = 2500;
	private static final int TOOLTIP_RESHOW_DELAY = 0;

	private Border focusBorder;
	private Border noFocusBorder;

	private Configuration defaultJaasConfiguration;

	/**
	 * @param args
	 *            see the online help sent to system output when launched
	 */
	public static void main(String[] args) {

		// COMMAND LINE PARAMETERS
		// none (the VM takes requires a -D command line parameter, however)
		// -DconfigFileName (JAAS configuration file path + name)
		//
		// e.g.: java -Dc:\myapp\jass.config VistaLinkRpcSwingTester

		DOMConfigurator.configure("props/log4jConfig.xml");
		doCommandLineHelp();
		logger.debug("Starting to run Swing Tester...");
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
			new VistaLinkRpcSwingTester(args);
		}
	}

	/**
	 * 
	 * @param args
	 *            none required
	 */
	VistaLinkRpcSwingTester(String[] args) {

		timeout = DEFAULT_TIMEOUT;

		// create the main window
		topFrame = new JFrame("VistaLink Test/Demo");
		topFrame
				.getAccessibleContext()
				.setAccessibleDescription(
						"This application lets you test the functionality of various RPC calls against a VistaLink listener of your choice.");

		//add contents to it.
		Component contents = createComponents();
		topFrame.getContentPane().add(contents, BorderLayout.CENTER);
		topFrame.setJMenuBar(createMenuBar());

		// set font size from current desktop properties, update UI control tree,
		Font winMessageBoxFont = (java.awt.Font) Toolkit.getDefaultToolkit().getDesktopProperty("win.messagebox.font");
		if (winMessageBoxFont != null) {
			updateUIManagerDefaultFonts(winMessageBoxFont);
		}

		// set up "close" event to force logoff if the window is closed
		topFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		topFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				logout();
				System.exit(0);
			}
		});

		// call to change all the fonts when desktop properties changed by user while app is running.
		final Runnable doFontChange = new Runnable() {
			public void run() {
				Font winMessageBoxFont = (java.awt.Font) Toolkit.getDefaultToolkit().getDesktopProperty(
						"win.messagebox.font");
				changeActiveFont(topFrame, winMessageBoxFont);
				updateUIManagerDefaultFonts(winMessageBoxFont);
				setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				topFrame.pack();
				setFramePosition();
			}
		};

		// listen for desktop property changes -- update fonts used in all controls -- for 508 purposes.
		Toolkit.getDefaultToolkit().addPropertyChangeListener("win.messagebox.font", new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				SwingUtilities.invokeLater(doFontChange);
			}
		});

		// set the tooltip delay
		ToolTipManager.sharedInstance().setInitialDelay(TOOLTIP_INITIAL_DELAY);
		ToolTipManager.sharedInstance().setReshowDelay(TOOLTIP_RESHOW_DELAY);

		// set the look and feel to System l+f
		setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		// pack frame, set position, set default focus, make it visible
		topFrame.pack();
		setFramePosition();
		ipTextField.requestFocusInWindow();
		topFrame.setVisible(true);
	}

	private void setLookAndFeel(String lookAndFeelName) {
		// set UIManager look and feel
		try {
			UIManager.setLookAndFeel(lookAndFeelName);
			setMenuSelections();
			SwingUtilities.updateComponentTreeUI(topFrame);
			topFrame.pack();

		} catch (Exception e) {

			DialogConfirm.showDialogConfirm(topFrame, "Could not set look and feel: " + e.getMessage(), "Login Error",
					DialogConfirm.MODE_ERROR_MESSAGE, timeout);
			setMenuSelections();
		}
	}

	private void changeActiveFont(Container c, Font font) {
		if (c != null) {
			Component[] components = c.getComponents();
			for (int i = 0; i < components.length; i++) {
				components[i].setFont(font);
				if (components[i] instanceof Container) {
					changeActiveFont((Container) components[i], font);
				}
			}
		}
	}

	/**
	 * reset default fonts
	 */
	private void updateUIManagerDefaultFonts(Font font) {
		Object[] objs = UIManager.getLookAndFeel().getDefaults().keySet().toArray();
		for (int i = 0; i < objs.length; i++) {
			if (objs[i].toString().toUpperCase().indexOf(".FONT") != -1) {
				UIManager.put(objs[i], new FontUIResource(font));
			}
		}
		Frame[] frames = Frame.getFrames();
		for (int i = 0; i < frames.length; i++) {
			Window[] ownedWindows = frames[i].getOwnedWindows();
			for (int j = 0; j < ownedWindows.length; j++) {
				SwingUtilities.updateComponentTreeUI(ownedWindows[j]);
				ownedWindows[j].validate();
			}
		}
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
		topFrame.getRootPane().setDefaultButton(this.connectButton);
	}

	/**
	 * Prints help on command-line arguments needed for this app to the console.
	 */
	static void doCommandLineHelp() {
		StringBuffer sb = new StringBuffer();
		sb.append("\nVistaLinkRpcSwingTester doesn't require any command-line arguments. However, if");
		sb.append("\nyou wish to pass M server configurations via a jaas.config file, you must use the ");
		sb.append("\nfollowing optional VM argument, which must be passed with a double-equals (==):\n");
		sb.append("\n -Djava.security.auth.login.config==configFile\n");
		sb.append("\ne.g.: java -Djava.security.auth.login.config==./jaas.config VistaLinkRpcSwingTester\n");
		sb
				.append("\nAlso, for Log4J initialization, the Log4J config file 'log4JConfig.xml' is expected to be in the classpath location");
		sb.append("\nprops/log4jConfig.xml\n");
		System.out.println(sb.toString());
	}

	/**
	 * create all components for the window
	 * @return Component
	 */
	private Component createComponents() {

		try {
			// create textarea/textfield borders with Java look and feel (haven't set system L&F yet)
			createFocusBorders();
		} catch (Exception e) {
			DialogConfirm.showDialogConfirm(topFrame, e.getMessage(), "Error", DialogConfirm.MODE_INFORMATION_MESSAGE,
					this.timeout);
		}

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		//create Panel for Connection controls
		mainPanel.add(createConnectionPanel(), BorderLayout.NORTH);

		//create Panel for RPC tabs
		this.tabPane = (JTabbedPane) createTabPanel();
		mainPanel.add(this.tabPane, BorderLayout.CENTER);

		//create Panel for RPC counter
		mainPanel.add(createRpcCounterPane(), BorderLayout.SOUTH);

		// set controls to "not logged on" state
		disconnectedControlsEnable();

		connectedFocusTraversalPolicy = new ConnectedFocusTraversalPolicy();
		disconnectedFocusTraversalPolicy = new DisconnectedFocusTraversalPolicy();
		mainPanel.setFocusCycleRoot(true);
		mainPanel.setFocusTraversalPolicy(disconnectedFocusTraversalPolicy);

		return mainPanel;
	}

	private JMenuBar createMenuBar() {

		// menubar
		JMenuBar menuBar = new JMenuBar();

		// "Look and Feel" menu
		JMenu lookAndFeelMenu = new JMenu(MENU_LANDF_TEXT);
		lookAndFeelMenu.setToolTipText(MENU_LANDF_TOOLTIP);
		lookAndFeelMenu.setMnemonic(MENU_LANDF_MNEMONIC);
		menuBar.add(lookAndFeelMenu);

		// Help menu
		JMenu helpMenu = new JMenu(MENU_HELP_TEXT);
		helpMenu.setToolTipText(MENU_HELP_TOOLTIP);
		helpMenu.setMnemonic(MENU_HELP_MNEMONIC);
		menuBar.add(helpMenu);

		// "Look and Feel" menu items
		windowsMenuItem = new JCheckBoxMenuItem(MENU_WINDOWS_TEXT, false);
		windowsMenuItem.setToolTipText(MENU_WINDOWS_TOOLTIP);
		windowsMenuItem.setMnemonic(MENU_WINDOWS_MNEMONIC);
		windowsMenuItem.addActionListener(this);
		lookAndFeelMenu.add(windowsMenuItem);

		javaMenuItem = new JCheckBoxMenuItem(MENU_JAVA_TEXT, false);
		javaMenuItem.setToolTipText(MENU_JAVA_TOOLTIP);
		javaMenuItem.setMnemonic(MENU_JAVA_MNEMONIC);
		javaMenuItem.addActionListener(this);
		lookAndFeelMenu.add(javaMenuItem);

		motifMenuItem = new JCheckBoxMenuItem(MENU_MOTIF_TEXT, false);
		motifMenuItem.setToolTipText(MENU_MOTIF_TOOLTIP);
		motifMenuItem.setMnemonic(MENU_MOTIF_MNEMONIC);
		motifMenuItem.addActionListener(this);
		lookAndFeelMenu.add(motifMenuItem);

		gtkMenuItem = new JCheckBoxMenuItem(MENU_GTK_TEXT, false);
		gtkMenuItem.setToolTipText(MENU_GTK_TOOLTIP);
		gtkMenuItem.setMnemonic(MENU_GTK_MNEMONIC);
		gtkMenuItem.addActionListener(this);
		lookAndFeelMenu.add(gtkMenuItem);

		systemMenuItem = new JCheckBoxMenuItem(MENU_SYSTEM_TEXT, false);
		systemMenuItem.setToolTipText(MENU_SYSTEM_TOOLTIP);
		systemMenuItem.setMnemonic(MENU_SYSTEM_MNEMONIC);
		systemMenuItem.addActionListener(this);
		lookAndFeelMenu.add(systemMenuItem);

		// Help menu items
		JMenuItem section508MenuItem = new JMenuItem(MENU_508_TEXT);
		section508MenuItem.setToolTipText(MENU_508_TOOLTIP);
		section508MenuItem.setMnemonic(MENU_508_MNEMONIC);
		section508MenuItem.addActionListener(this);
		helpMenu.add(section508MenuItem);

		return menuBar;
	}

	/**
	 * listener handler for the ActionListener interface implemented by the application class
	 * @param e
	 *            the action event that is being processed
	 * @va.exclude
	 */
	public void actionPerformed(ActionEvent e) {
		// Process menu item events
		JMenuItem source = (JMenuItem) (e.getSource());
		if (MENU_SYSTEM_TEXT.equals(source.getText())) {
			if (!(UIManager.getLookAndFeel().toString().equals(UIManager.getSystemLookAndFeelClassName()))) {
				setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				topFrame.pack();
				setFramePosition();
			}
		} else if (MENU_WINDOWS_TEXT.equals(source.getText())) {
			if (!("com.sun.java.swing.plaf.windows.WindowsLookAndFeel".equals(UIManager.getLookAndFeel().toString()))) {
				setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
				topFrame.pack();
				setFramePosition();
			}
		} else if (MENU_JAVA_TEXT.equals(source.getText())) {
			if (!(UIManager.getLookAndFeel().toString().equals(UIManager.getCrossPlatformLookAndFeelClassName()))) {
				setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
				topFrame.pack();
				setFramePosition();
			}
		} else if (MENU_MOTIF_TEXT.equals(source.getText())) {
			if (!("com.sun.java.swing.plaf.motif.MotifLookAndFeel".equals(UIManager.getLookAndFeel().toString()))) {
				setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
				topFrame.pack();
				setFramePosition();
			}
		} else if (MENU_GTK_TEXT.equals(source.getText())) {
			if (!("com.sun.java.swing.plaf.gtk.GTKLookAndFeel".equals(UIManager.getLookAndFeel().toString()))) {
				setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
				topFrame.pack();
				setFramePosition();
			}
		} else if (MENU_508_TEXT.equals(source.getText())) {

			DialogConfirm.showDialogConfirm(topFrame, getStringFromArray(SECTION_508_DISCLAIMER),
					"Section 508 Information", DialogConfirm.MODE_INFORMATION_MESSAGE, this.timeout);
		}
	}

	private void setMenuSelections() {

		this.javaMenuItem.setSelected(false);
		this.motifMenuItem.setSelected(false);
		this.windowsMenuItem.setSelected(false);
		this.systemMenuItem.setSelected(false);
		this.gtkMenuItem.setSelected(false);

		String currentLookAndFeelClassName = UIManager.getLookAndFeel().getClass().getName();
		String currentLookAndFeelId = UIManager.getLookAndFeel().getID();

		if (currentLookAndFeelClassName.equals(UIManager.getSystemLookAndFeelClassName())) {
			this.systemMenuItem.setSelected(true);
		}

		if ("Metal".equals(currentLookAndFeelId)) {
			this.javaMenuItem.setSelected(true);
		}

		if ("Motif".equals(currentLookAndFeelId)) {
			this.motifMenuItem.setSelected(true);
		}

		if ("Windows".equals(currentLookAndFeelId)) {
			this.windowsMenuItem.setSelected(true);
		}

		if ("GTK".equals(currentLookAndFeelId)) {
			this.gtkMenuItem.setSelected(true);
		}

	}

	private JPanel createRpcCounterPane() {
		JPanel myPanel = new JPanel();
		FlowLayout myFlowLayout = new FlowLayout();
		myFlowLayout.setAlignment(FlowLayout.LEFT);

		rpcCountTextField = new JTextField(Integer.toString(this.rpcCount));
		rpcCountTextField.setFocusable(true);
		rpcCountTextField.setEditable(false);
		rpcCountTextField.setColumns(5);
		rpcCountTextField.setMargin(new Insets(1, 4, 1, 4));
		rpcCountTextField.setToolTipText(RPC_COUNT_TOOLTIP);
		rpcCountTextField.getAccessibleContext().setAccessibleName(
				RPC_COUNT_LABEL_TEXT + Integer.toString(this.rpcCount));
		rpcCountTextField.setBorder(noFocusBorder);
		rpcCountTextField.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				rpcCountTextField.setBorder(focusBorder);
			}

			public void focusLost(FocusEvent e) {
				rpcCountTextField.setBorder(noFocusBorder);
			}
		});

		JLabel rpcCountTextLabel = new JLabel();
		rpcCountTextLabel.setFocusable(true);
		rpcCountTextLabel.setText(RPC_COUNT_LABEL_TEXT);
		rpcCountTextLabel.setToolTipText(RPC_COUNT_TOOLTIP);
		rpcCountTextLabel.setDisplayedMnemonic(RPC_COUNT_MNEMONIC);
		rpcCountTextLabel.setLabelFor(rpcCountTextField);
		rpcCountTextLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				rpcCountTextField.requestFocusInWindow();
			}
		});

		myPanel.add(rpcCountTextLabel);
		myPanel.add(rpcCountTextField);
		return myPanel;
	}

	private JPanel createConnectionPanel() {

		JPanel myPanel = new JPanel();
		GridBagLayout myLayout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		myPanel.setLayout(myLayout);

		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 2;
		JComponent leftPanel = createConnectionPanelLeft();
		myPanel.add(leftPanel);
		myLayout.setConstraints(leftPanel, c);

		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 1;
		messageFormatPanel = (JPanel) createConnectionPanelRight();
		myPanel.add(messageFormatPanel);
		myLayout.setConstraints(messageFormatPanel, c);

		c.gridx = 1;
		c.gridy = 1;
		c.gridheight = 1;
		JPanel clearPanel = (JPanel) createClearPanel();
		myPanel.add(clearPanel);
		myLayout.setConstraints(clearPanel, c);

		return myPanel;

	}

	private JPanel createClearPanel() {

		JPanel buttonPanel = new JPanel();

		this.clearButton = new JButton();
		this.clearButton.setText(CLEAR_BUTTON_TEXT);
		this.clearButton.setMnemonic(CLEAR_BUTTON_MNEMONIC);
		this.clearButton.setToolTipText(CLEAR_BUTTON_TOOLTIP);
		this.clearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doClear();
			}
		});

		buttonPanel.add(clearButton);

		return buttonPanel;

	}

	private JPanel createConnectionPanelRight() {

		// radio button list
		this.xmlRadioButton = new JRadioButton(XML_BUTTON_STRING);
		this.xmlRadioButton.setMnemonic(XML_BUTTON_MNEMONIC);
		this.xmlRadioButton.setToolTipText(XML_BUTTON_TOOLTIP);
		this.xmlRadioButton.setSelected(false);

		this.proprietaryRadioButton = new JRadioButton(PROPRIETARY_BUTTON_STRING);
		this.proprietaryRadioButton.setSelected(true);
		this.proprietaryRadioButton.setMnemonic(PROPRIETARY_BUTTON_MNEMONIC);
		this.proprietaryRadioButton.setToolTipText(PROPRIETARY_BUTTON_TOOLTIP);

		ButtonGroup messageButtonGroup = new ButtonGroup();
		messageButtonGroup.add(this.proprietaryRadioButton);
		messageButtonGroup.add(this.xmlRadioButton);

		// set up the panel
		JPanel myPanel = new JPanel();
		myPanel.setLayout(new GridLayout(0, 1));
		myPanel.add(this.proprietaryRadioButton);
		myPanel.add(this.xmlRadioButton);

		// border
		TitledBorder titledBorder = new TitledBorder(new EtchedBorder(), MESSAGE_FORMAT_TEXT);
		titledBorder.setTitlePosition(TitledBorder.TOP);
		titledBorder.setTitleJustification(TitledBorder.LEFT);
		Insets insets = new Insets(8, 8, 8, 8);
		EmptyBorder emptyBorder = new EmptyBorder(insets);
		myPanel.setBorder(new CompoundBorder(emptyBorder, titledBorder));

		return myPanel;

	}

	/**
	 * create the panel that contains all the connection controls
	 * @return JComponent
	 */
	private JPanel createConnectionPanelLeft() {

		JPanel myPanel = new JPanel();
		GridBagLayout myLayout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		myPanel.setLayout(myLayout);

		// IP/port fields
		c.gridx = 0;
		c.gridy = 0;
		JComponent ipPortPanel = createIpPortPanel();
		myPanel.add(ipPortPanel);

		// Connect button
		c.gridx = 1;
		c.gridy = 0;
		JPanel connectButtonPanel = new JPanel();
		FlowLayout connectButtonPanelLayout = new FlowLayout();
		connectButtonPanelLayout.setAlignment(FlowLayout.CENTER);
		connectButtonPanel.setLayout(connectButtonPanelLayout);

		this.connectButton = new JButton();
		this.connectButton.setText(CONNECT_BUTTON_TEXT);
		this.connectButton.setToolTipText(CONNECT_BUTTON_TOOLTIP);
		this.connectButton.setMnemonic(CONNECT_BUTTON_MNEMONIC);

		this.connectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// store default JAAS configuration
				if (defaultJaasConfiguration == null) {
					try {
						defaultJaasConfiguration = Configuration.getConfiguration();
					} catch (RuntimeException e1) {
						// swallow
					}
				}
				if (serverComboBox.getSelectedIndex() == 0) {
					// switch to custom one
					Configuration.setConfiguration(new RuntimeJaasIpPortConfiguration(ipTextField.getText(),
							portTextField.getText()));
				} else {
					Configuration.setConfiguration(defaultJaasConfiguration);
				}
				login();
			}
		});
		connectButtonPanel.add(this.connectButton);
		myPanel.add(connectButtonPanel, c);

		// Ping button
		c.gridx = 1;
		c.gridy = 1;
		this.pingButton = new JButton();
		this.pingButton.setText(PING_BUTTON_TEXT);
		this.pingButton.setMnemonic(PING_BUTTON_MNEMONIC);
		this.pingButton.setToolTipText(PING_BUTTON_TOOLTIP);
		this.pingButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doPing();
			}
		});

		myPanel.add(this.pingButton, c);

		// Alias label and combo box
		c.gridx = 0;
		c.gridy = 1;
		JComponent aliasPanel = createAliasPanel();
		myPanel.add(aliasPanel, c);

		// status label and textarea
		c.gridx = 0;
		c.gridy = 2;
		c.anchor = GridBagConstraints.CENTER;
		myPanel.add(createStatusPanel(), c);

		// Close button
		c.gridx = 1;
		c.gridy = 2;
		JPanel closeButtonPanel = new JPanel();
		FlowLayout closeButtonPanelLayout = new FlowLayout();
		closeButtonPanelLayout.setAlignment(FlowLayout.CENTER);
		closeButtonPanel.setLayout(closeButtonPanelLayout);
		this.closeButton = new JButton();
		this.closeButton.setText(DISCONNECT_BUTTON_TEXT);
		this.closeButton.setMnemonic(DISCONNECT_BUTTON_MNEMONIC);
		this.closeButton.setToolTipText(DISCONNECT_BUTTON_TOOLTIP);
		this.closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doClear();
				logout();
			}
		});
		closeButtonPanel.add(this.closeButton);
		myPanel.add(closeButtonPanel, c);

		// border
		TitledBorder titledBorder = new TitledBorder(new EtchedBorder(), SERVER_PANEL_TITLE);
		titledBorder.setTitlePosition(TitledBorder.TOP);
		titledBorder.setTitleJustification(TitledBorder.LEFT);
		Insets insets = new Insets(8, 8, 8, 8);
		EmptyBorder emptyBorder = new EmptyBorder(insets);
		myPanel.setBorder(new CompoundBorder(emptyBorder, titledBorder));

		return myPanel;
	}

	private JPanel createStatusPanel() {

		JPanel statusPanel = new JPanel();

		// Status label
		statusLabel = new JLabel();
		statusLabel.setText(STATUS_LABEL_TEXT);
		statusLabel.setToolTipText(STATUS_LABEL_TOOLTIP);
		statusLabel.setFocusable(true);
		statusLabel.setDisplayedMnemonic(STATUS_MNEMONIC);
		statusLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				statusTextField.requestFocusInWindow();
			}
		});
		statusPanel.add(statusLabel);

		// status text area
		statusTextField = new JTextField(STATUS_DISCONNECTED_TEXT);
		statusTextField
				.setColumns((STATUS_DISCONNECTED_TEXT.length() > STATUS_CONNECTED_TEXT.length()) ? STATUS_DISCONNECTED_TEXT
						.length()
						: STATUS_CONNECTED_TEXT.length());
		statusTextField.setEditable(false);
		statusTextField.setEnabled(true);
		statusTextField.setToolTipText(STATUS_LABEL_TOOLTIP);
		statusTextField.setFocusable(true);
		statusTextField.setMargin(new Insets(1, 3, 1, 3));
		statusPanel.add(statusTextField);
		statusTextField.setBorder(noFocusBorder);
		statusTextField.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				statusTextField.setBorder(focusBorder);
			}

			public void focusLost(FocusEvent e) {
				statusTextField.setBorder(noFocusBorder);
			}
		});

		statusLabel.setLabelFor(statusTextField);
		statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);

		return statusPanel;
	}

	private JPanel createIpPortPanel() {

		JPanel myPanel = new JPanel();

		// IP TextField
		ipTextField = new JTextField();
		ipTextField.setToolTipText(IP_LABEL_TOOLTIP);
		ipTextField.setColumns(IP_TEXTFIELD_COLUMNS);
		ipTextField.setBorder(BorderFactory.createEtchedBorder());
		ipTextField.addFocusListener(new IpPortFocusListener());

		// IP label
		ipLabel = new JLabel();
		ipLabel.setText(IP_LABEL_TEXT);
		ipLabel.setToolTipText(IP_LABEL_TOOLTIP);
		ipLabel.setDisplayedMnemonic(IP_LABEL_MNEMONIC);
		ipLabel.setLabelFor(ipTextField);
		ipLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				ipTextField.requestFocusInWindow();
			}
		});

		// Port TextField
		portTextField = new JTextField();
		portTextField.setToolTipText(PORT_LABEL_TOOLTIP);
		portTextField.setColumns(PORT_TEXTFIELD_COLUMNS);
		portTextField.setBorder(BorderFactory.createEtchedBorder());
		portTextField.addFocusListener(new IpPortFocusListener());

		// Port label
		portLabel = new JLabel();
		portLabel.setText(PORT_LABEL_TEXT);
		portLabel.setToolTipText(PORT_LABEL_TOOLTIP);
		portLabel.setDisplayedMnemonic(PORT_LABEL_MNEMONIC);
		portLabel.setLabelFor(portTextField);
		portLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				portTextField.requestFocusInWindow();
			}
		});

		myPanel.add(ipLabel);
		myPanel.add(ipTextField);
		myPanel.add(portLabel);
		myPanel.add(portTextField);

		GridBagLayout freeFormLayout = new GridBagLayout();
		myPanel.setLayout(freeFormLayout);

		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = c.ipady = 3;

		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy = 1;
		c.anchor = GridBagConstraints.EAST;
		myPanel.add(ipLabel, c);
		c.gridy = 2;
		myPanel.add(portLabel, c);

		c.gridx = 3;
		c.gridy = 1;
		c.anchor = GridBagConstraints.WEST;
		myPanel.add(ipTextField, c);
		c.gridy = 2;
		myPanel.add(portTextField, c);

		return myPanel;
	}

	/**
	 * Panel that contains alias label and combobox
	 * @return JComponent
	 */
	private JPanel createAliasPanel() {

		JPanel myPanel = new JPanel();

		// combobox
		serverComboBox = new JComboBox();
		serverComboBox.setToolTipText(SERVER_COMBOBOX_TOOLTIP);
		serverComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (serverComboBox.getSelectedIndex() > 0) {
					ipTextField.setText("");
					portTextField.setText("");
				}
			}
		});

		// Server Label
		serverComboBoxLabel = new JLabel();
		serverComboBoxLabel.setText(SERVER_ALIAS_LABEL_TEXT);
		serverComboBoxLabel.setToolTipText(SERVER_ALIAS_LABEL_TOOLTIP);
		serverComboBoxLabel.setDisplayedMnemonic(SERVER_ALIAS_LABEL_MNEMONIC);
		serverComboBoxLabel.setLabelFor(this.serverComboBox);
		serverComboBoxLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				serverComboBox.requestFocusInWindow();
			}
		});
		myPanel.add(serverComboBoxLabel);

		// set the accessible name of the JComboBox's editor component
		Component editor = serverComboBox.getEditor().getEditorComponent();
		AccessibleContext ac = editor.getAccessibleContext();
		ac.setAccessibleName(serverComboBox.getAccessibleContext().getAccessibleName());

		// populate the combobox with the serverlist
		try {

			serverComboBox.addItem("<select>");
			Vector aliasList = this.getAppNames();
			for (int i = 0; i < aliasList.size(); i++) {
				serverComboBox.addItem(aliasList.get(i));
			}

		} catch (IOException e) {

			int returnVal = DialogConfirm.showDialogConfirm(topFrame, e.getMessage(), "Warning",
					DialogConfirm.MODE_INFORMATION_MESSAGE, timeout);
			if (DialogConfirm.CANCEL_OPTION == returnVal) {
				logout();
				System.exit(0);
			}

		} catch (Exception e) {
			// catch RuntimeExceptions thrown by getAppNames()
			int returnVal = DialogConfirm.showDialogConfirm(topFrame, e.getMessage(), "Warning",
					DialogConfirm.MODE_INFORMATION_MESSAGE, timeout);
			if (DialogConfirm.CANCEL_OPTION == returnVal) {
				logout();
				System.exit(0);
			}
		}

		myPanel.add(serverComboBox);
		return myPanel;
	}

	/**
	 * Create the tabbed panel for RPC execution
	 * @return JComponent
	 */
	private JTabbedPane createTabPanel() {

		JTabbedPane rpcTabPane = new JTabbedPane();
		//		rpcTabPane.setToolTipText(TAB_PANE_TOOLTIP);

		rpcTabPane.addTab(TAB_WP_TEXT, createWordProcessingPanel());
		rpcTabPane.setToolTipTextAt(0, TAB_WP_TOOLTIP);
		rpcTabPane.setMnemonicAt(0, TAB_WP_MNEMONIC);

		rpcTabPane.addTab(TAB_STRING_TEXT, createStringPanel());
		rpcTabPane.setToolTipTextAt(1, TAB_STRING_TOOLTIP);
		rpcTabPane.setMnemonicAt(1, TAB_STRING_MNEMONIC);

		rpcTabPane.addTab(TAB_RPCLIST_TEXT, createRpcListPanel());
		rpcTabPane.setToolTipTextAt(2, TAB_RPCLIST_TOOLTIP);
		rpcTabPane.setMnemonicAt(2, TAB_RPCLIST_MNEMONIC);

		rpcTabPane.addTab(TAB_VARIABLE_TEXT, createVariablePanel());
		rpcTabPane.setToolTipTextAt(3, TAB_VARIABLE_TOOLTIP);
		rpcTabPane.setMnemonicAt(3, TAB_VARIABLE_MNEMONIC);

		rpcTabPane.addTab(TAB_ARRAY_TEXT, createArrayPanel());
		rpcTabPane.setToolTipTextAt(4, TAB_ARRAY_TOOLTIP);
		rpcTabPane.setMnemonicAt(4, TAB_ARRAY_MNEMONIC);

		rpcTabPane.addTab(TAB_USER_TEXT, createUserPanel());
		rpcTabPane.setToolTipTextAt(5, TAB_USER_TOOLTIP);
		rpcTabPane.setMnemonicAt(5, TAB_USER_MNEMONIC);

		rpcTabPane.setSelectedIndex(0);

		// border
		Insets insets = new Insets(8, 8, 8, 8);
		EmptyBorder emptyBorder = new EmptyBorder(insets);
		rpcTabPane.setBorder(emptyBorder);

		rpcTabPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				setTabPaneDefaultButton();
			}
		});

		return rpcTabPane;
	}

	/**
	 * Create panel for the User Info display
	 * @return JComponent
	 */
	private JPanel createUserPanel() {

		//button
		userButton = new JButton();
		userButton.setText(TAB_USER_BUTTON_TEXT);
		userButton.setToolTipText(TAB_USER_TOOLTIP);
		userButton.setMnemonic(GET_MNEMONIC);
		userButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doUserInfo();
			}
		});

		// result scrollpane
		userTextArea = new JTextArea();
		userTextArea.setEditable(false);
		userTextArea.setToolTipText(TAB_USER_TOOLTIP);
		userTextArea.setRows(RESULT_ROW_COUNT);
		userTextArea.setBorder(noFocusBorder);
		userTextArea.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				userTextArea.setBorder(focusBorder);
			}

			public void focusLost(FocusEvent e) {
				userTextArea.setBorder(noFocusBorder);
			}
		});

		userTextScrollPane = new JScrollPane(userTextArea);
		Border bevelOut = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		Border bevelIn = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
		userTextScrollPane.setBorder(BorderFactory.createCompoundBorder(bevelOut, bevelIn));

		// label
		JLabel resultLabel = new JLabel();
		resultLabel.setText(TAB_USER_NO_RPC_LABEL_TEXT);
		resultLabel.setToolTipText(TAB_USER_NO_RPC_LABEL_TEXT);
		resultLabel.setFocusable(true);
		resultLabel.setDisplayedMnemonic(RESULT_MNEMONIC);
		resultLabel.setLabelFor(userTextArea);
		resultLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				userTextArea.requestFocusInWindow();
			}
		});

		JPanel combinedPanel = new JPanel();
		GridBagLayout gridBagLayout = new GridBagLayout();
		combinedPanel.setLayout(gridBagLayout);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = c.gridy = 0;
		c.insets = new Insets(3, 3, 3, 3);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		combinedPanel.add(userButton, c);
		c.gridy = 1;
		combinedPanel.add(resultLabel, c);

		// create a panel for North part of main panel for button and combobox
		JPanel northPanel = new JPanel();
		FlowLayout myFlowLayout = new FlowLayout();
		myFlowLayout.setAlignment(FlowLayout.LEFT);
		northPanel.setLayout(myFlowLayout);
		northPanel.add(combinedPanel);

		// add to the panel
		JPanel myPanel = new JPanel();
		BorderLayout myLayout = new BorderLayout();
		myPanel.setLayout(myLayout);
		myPanel.add(northPanel, BorderLayout.NORTH);
		myPanel.add(userTextScrollPane, BorderLayout.CENTER);

		// redirect focus which, if sent from another tab pane via tab fastkey, and the focus was on a
		// control in the old tab pane, ends up "stuck" on the top-level panel of the new tab pane
		myPanel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				userButton.requestFocusInWindow();
			}
		});

		return myPanel;
	}

	/**
	 * Create panel for the WP RPC
	 * @return JComponent
	 */
	private JPanel createWordProcessingPanel() {

		//button
		wpButton = new JButton();
		wpButton.setText(TAB_WP_BUTTON_TEXT);
		wpButton.setToolTipText(TAB_WP_TOOLTIP);
		wpButton.setMnemonic(GET_MNEMONIC);
		wpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doWordProcessing();
			}
		});

		// text area and scrollpane
		wpResultsTextArea = new JTextArea();
		wpResultsTextArea.setEditable(false);
		wpResultsTextArea.setFocusable(true);
		wpResultsTextArea.setRows(RESULT_ROW_COUNT);
		wpResultsTextArea.setToolTipText(RESULT_TOOLTIP);
		wpResultsTextArea.setBorder(noFocusBorder);
		wpResultsTextArea.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				wpResultsTextArea.setBorder(focusBorder);
			}

			public void focusLost(FocusEvent e) {
				wpResultsTextArea.setBorder(noFocusBorder);
			}
		});

		wpResultsScrollPane = new JScrollPane(wpResultsTextArea);
		Border bevelOut = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		Border bevelIn = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
		wpResultsScrollPane.setBorder(BorderFactory.createCompoundBorder(bevelOut, bevelIn));

		// result label
		JLabel resultLabel = new JLabel();
		resultLabel.setFocusable(true);
		resultLabel.setText(RESULT_LABEL_TEXT);
		resultLabel.setToolTipText(RESULT_TOOLTIP);
		resultLabel.setLabelFor(wpResultsTextArea);
		resultLabel.setDisplayedMnemonic(RESULT_MNEMONIC);
		resultLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				wpResultsTextArea.requestFocusInWindow();
			}
		});

		JPanel combinedPanel = new JPanel();
		GridBagLayout gridBagLayout = new GridBagLayout();
		combinedPanel.setLayout(gridBagLayout);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = c.gridy = 0;
		c.insets = new Insets(3, 3, 3, 3);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		combinedPanel.add(wpButton, c);
		c.gridy = 1;
		combinedPanel.add(resultLabel, c);

		// create a panel for North part of main panel for button and combobox
		JPanel northPanel = new JPanel();
		FlowLayout myFlowLayout = new FlowLayout();
		myFlowLayout.setAlignment(FlowLayout.LEFT);
		northPanel.setLayout(myFlowLayout);
		northPanel.add(combinedPanel);

		JPanel myPanel = new JPanel();
		myPanel.setLayout(new BorderLayout());
		myPanel.add(northPanel, BorderLayout.NORTH);
		myPanel.add(wpResultsScrollPane, BorderLayout.CENTER);

		myPanel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				wpButton.requestFocusInWindow();
			}
		});

		return myPanel;
	}

	/**
	 * Create panel for the String rpc
	 * @return JComponent
	 */
	private JPanel createStringPanel() {

		//controls for button panel
		stringTextField = new JTextField();
		stringTextField.setToolTipText(TAB_STRING_TEXTFIELD_TOOLTIP);
		stringTextField.setColumns(TEXT_FIELD_COLUMNS);
		stringTextField.setBorder(BorderFactory.createEtchedBorder());

		JLabel stringLabel = new JLabel();
		stringLabel.setFocusable(true);
		stringLabel.setText(TAB_STRING_LABEL_TEXT);
		stringLabel.setToolTipText(TAB_STRING_TOOLTIP);
		stringLabel.setDisplayedMnemonic(TAB_STRING_TEXTFIELD_MNEMONIC);
		stringLabel.setLabelFor(stringTextField);
		stringLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				stringTextField.requestFocusInWindow();
			}
		});

		stringButton = new JButton();
		stringButton.setText(TAB_STRING_BUTTON_TEXT);
		stringButton.setToolTipText(TAB_STRING_TOOLTIP);
		stringButton.setMnemonic(GET_MNEMONIC);

		stringButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doString();
			}
		});

		stringResultTextField = new JTextField();
		stringResultTextField.setColumns(50);
		stringResultTextField.setEditable(false);
		stringResultTextField.setFocusable(true);
		stringResultTextField.setText("");
		stringResultTextField.setToolTipText(RESULT_TOOLTIP);
		stringResultTextField.setBorder(noFocusBorder);
		stringResultTextField.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				stringResultTextField.setBorder(focusBorder);
			}

			public void focusLost(FocusEvent e) {
				stringResultTextField.setBorder(noFocusBorder);
			}
		});

		stringResultLabel = new JLabel();
		stringResultLabel.setText(RESULT_LABEL_TEXT);
		stringResultLabel.setToolTipText(RESULT_TOOLTIP);
		stringResultLabel.setLabelFor(stringResultTextField);
		stringResultLabel.setDisplayedMnemonic(RESULT_MNEMONIC);
		stringResultLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				stringResultTextField.requestFocusInWindow();
			}
		});

		// button panel
		JPanel buttonPanel = new JPanel();
		FlowLayout buttonFlowLayout = new FlowLayout();
		buttonFlowLayout.setAlignment(FlowLayout.LEFT);
		buttonPanel.setLayout(buttonFlowLayout);
		buttonPanel.add(stringLabel);
		buttonPanel.add(stringTextField);
		buttonPanel.add(stringButton);

		// resultpanel
		JPanel resultPanel = new JPanel();
		FlowLayout resultFlowLayout = new FlowLayout();
		resultFlowLayout.setAlignment(FlowLayout.LEFT);
		resultPanel.setLayout(resultFlowLayout);
		resultPanel.add(stringResultLabel);
		resultPanel.add(stringResultTextField);

		// create main Panel
		JPanel myPanel = new JPanel();
		myPanel.setLayout(new BorderLayout());
		myPanel.add(buttonPanel, BorderLayout.NORTH);
		myPanel.add(resultPanel, BorderLayout.CENTER);

		// redirect focus which, if sent from another tab pane via tab fastkey, and the focus was on a
		// control in the old tab pane, ends up "stuck" on the top-level panel of the new tab pane
		myPanel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				stringTextField.requestFocusInWindow();
			}
		});

		return myPanel;
	}

	/**
	 * Create panel for the RPC List RPC
	 * @return JComponent
	 */
	private JPanel createRpcListPanel() {

		rpcListNamespaceTextField = new JTextField();
		rpcListNamespaceTextField.setColumns(TEXT_FIELD_COLUMNS);
		rpcListNamespaceTextField.setBorder(BorderFactory.createEtchedBorder());
		rpcListNamespaceTextField.setToolTipText(TAB_RPCLIST_TEXTFIELD_TOOLTIP);

		JLabel rpcListLabel = new JLabel();
		rpcListLabel.setText(TAB_RPCLIST_LABEL_TEXT);
		rpcListLabel.setFocusable(true);
		rpcListLabel.setToolTipText(TAB_RPCLIST_TOOLTIP);
		rpcListLabel.setDisplayedMnemonic(TAB_STRING_TEXTFIELD_MNEMONIC);
		rpcListLabel.setLabelFor(rpcListNamespaceTextField);
		rpcListLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				rpcListNamespaceTextField.requestFocusInWindow();
			}
		});

		rpcButton = new JButton();
		rpcButton.setText(TAB_RPCLIST_BUTTON_TEXT);
		rpcButton.setToolTipText(TAB_RPCLIST_TOOLTIP);
		rpcButton.setMnemonic(GET_MNEMONIC);
		rpcButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doRpcList();
			}
		});

		// Text area and scrollpane
		rpcResultsTextArea = new JTextArea();
		rpcResultsTextArea.setEditable(false);
		rpcResultsTextArea.setFocusable(true);
		rpcResultsTextArea.setRows(RESULT_ROW_COUNT);
		rpcResultsTextArea.setToolTipText(RESULT_TOOLTIP);
		rpcResultsTextArea.setBorder(noFocusBorder);
		rpcResultsTextArea.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				rpcResultsTextArea.setBorder(focusBorder);
			}

			public void focusLost(FocusEvent e) {
				rpcResultsTextArea.setBorder(noFocusBorder);
			}
		});

		rpcResultsScrollPane = new JScrollPane(rpcResultsTextArea);
		Border bevelOut = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		Border bevelIn = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
		rpcResultsScrollPane.setBorder(BorderFactory.createCompoundBorder(bevelOut, bevelIn));

		// result label
		JLabel resultLabel = new JLabel();
		resultLabel.setFocusable(true);
		resultLabel.setText(RESULT_LABEL_TEXT);
		resultLabel.setToolTipText(RESULT_TOOLTIP);
		resultLabel.setLabelFor(rpcResultsTextArea);
		resultLabel.setDisplayedMnemonic(RESULT_MNEMONIC);
		resultLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				rpcResultsTextArea.requestFocusInWindow();
			}
		});

		JPanel combinedPanel = new JPanel();
		GridBagLayout gridBagLayout = new GridBagLayout();
		combinedPanel.setLayout(gridBagLayout);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = c.gridy = 0;
		c.insets = new Insets(3, 3, 3, 3);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		combinedPanel.add(rpcListLabel, c);
		c.gridx = 1;
		combinedPanel.add(rpcListNamespaceTextField, c);
		c.gridx = 2;
		combinedPanel.add(rpcButton, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 3;
		combinedPanel.add(resultLabel, c);

		// create a panel for North part of main panel for button and combobox
		JPanel northPanel = new JPanel();
		FlowLayout myFlowLayout = new FlowLayout();
		myFlowLayout.setAlignment(FlowLayout.LEFT);
		northPanel.setLayout(myFlowLayout);
		northPanel.add(combinedPanel);

		JPanel myPanel = new JPanel();
		myPanel.setLayout(new BorderLayout());
		myPanel.add(northPanel, BorderLayout.NORTH);
		myPanel.add(rpcResultsScrollPane, BorderLayout.CENTER);

		// redirect focus which, if sent from another tab pane via tab fastkey, and the focus was on a
		// control in the old tab pane, ends up "stuck" on the top-level panel of the new tab pane
		myPanel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				rpcListNamespaceTextField.requestFocusInWindow();
			}
		});

		return myPanel;
	}

	/**
	 * Create panel for the Get Variable RPCs
	 * @return JComponent
	 */
	private JPanel createVariablePanel() {

		//button
		variableButton = new JButton();
		variableButton.setText(TAB_VARIABLE_BUTTON_TEXT);
		variableButton.setToolTipText(TAB_VARIABLE_TOOLTIP);
		variableButton.setMnemonic(GET_MNEMONIC);
		variableButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doVariable((String) refTypeComboBox.getSelectedItem());
			}
		});

		// combobox
		refTypeComboBox = new JComboBox();
		refTypeComboBox.addItem(REF_TYPE_TRUE);
		refTypeComboBox.addItem(REF_TYPE_FALSE);
		refTypeComboBox.setToolTipText(TAB_VARIABLE_COMBOBOX_TOOLTIP);

		// label
		JLabel varTypeLabel = new JLabel(TAB_VARIABLE_LABEL_TEXT);
		varTypeLabel.setFocusable(true);
		varTypeLabel.setDisplayedMnemonic(TAB_VARIABLE_LABEL_MNEMONIC);
		varTypeLabel.setToolTipText(TAB_VARIABLE_COMBOBOX_TOOLTIP);
		varTypeLabel.setLabelFor(refTypeComboBox);
		varTypeLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				refTypeComboBox.requestFocusInWindow();
			}
		});

		//input panel
		JPanel inputPanel = new JPanel();
		FlowLayout inputFlowLayout = new FlowLayout();
		inputFlowLayout.setAlignment(FlowLayout.LEFT);
		inputPanel.setLayout(inputFlowLayout);
		inputPanel.add(varTypeLabel);
		inputPanel.add(refTypeComboBox);
		inputPanel.add(variableButton);

		// result panel
		JPanel resultPanel = new JPanel();
		FlowLayout resultFlowLayout = new FlowLayout();
		resultFlowLayout.setAlignment(FlowLayout.LEFT);
		resultPanel.setLayout(resultFlowLayout);

		// result panel text area
		variableResultTextField = new JTextField();
		variableResultTextField.setColumns(25);
		variableResultTextField.setEditable(false);
		variableResultTextField.setFocusable(true);
		variableResultTextField.setText("");
		variableResultTextField.setToolTipText(RESULT_TOOLTIP);
		variableResultTextField.setBorder(noFocusBorder);
		variableResultTextField.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				variableResultTextField.setBorder(focusBorder);
			}

			public void focusLost(FocusEvent e) {
				variableResultTextField.setBorder(noFocusBorder);
			}
		});

		// result panel label
		JLabel variableLabel = new JLabel();
		variableLabel.setText(RESULT_LABEL_TEXT);
		variableLabel.setToolTipText(RESULT_TOOLTIP);
		variableLabel.setLabelFor(variableResultTextField);
		variableLabel.setDisplayedMnemonic(RESULT_MNEMONIC);
		variableLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				variableResultTextField.requestFocusInWindow();
			}
		});

		resultPanel.add(variableLabel);
		resultPanel.add(variableResultTextField);

		// create main panel
		JPanel myPanel = new JPanel();
		myPanel.setLayout(new BorderLayout());
		// add sub-panels to main panel
		myPanel.add(inputPanel, BorderLayout.NORTH);
		myPanel.add(resultPanel, BorderLayout.CENTER);

		// redirect focus which, if sent from another tab pane via tab fastkey, and the focus was on a
		// control in the old tab pane, ends up "stuck" on the top-level panel of the new tab pane
		myPanel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				refTypeComboBox.requestFocusInWindow();
			}
		});

		return myPanel;
	}

	/**
	 * Create panel for the Array RPCs
	 * @return JComponent
	 */
	private JPanel createArrayPanel() {

		//button
		arrayButton = new JButton();
		arrayButton.setText(TAB_ARRAY_BUTTON_TEXT);
		arrayButton.setToolTipText(TAB_ARRAY_TOOLTIP);
		arrayButton.setMnemonic(GET_MNEMONIC);
		arrayButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doArray((String) arrayTypeComboBox.getSelectedItem());
			}
		});

		// combobox
		arrayTypeComboBox = new JComboBox();
		arrayTypeComboBox.addItem(ARRAY_TYPE_LOCAL);
		arrayTypeComboBox.addItem(ARRAY_TYPE_GLOBAL);
		arrayTypeComboBox.setToolTipText(TAB_ARRAY_COMBOBOX_TEXT);

		// label
		JLabel arrayTypeLabel = new JLabel(TAB_ARRAY_COMBOBOX_TEXT);
		arrayTypeLabel.setFocusable(true);
		arrayTypeLabel.setDisplayedMnemonic(TAB_ARRAY_LABEL_MNEMONIC);
		arrayTypeLabel.setToolTipText(TAB_ARRAY_COMBOBOX_TEXT);
		arrayTypeLabel.setLabelFor(arrayTypeComboBox);
		arrayTypeLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				arrayTypeComboBox.requestFocusInWindow();
			}
		});

		// test area/scrollpane
		arrayTextArea = new JTextArea();
		arrayTextArea.setEditable(false);
		arrayTextArea.setFocusable(true);
		arrayTextArea.setToolTipText(RESULT_TOOLTIP);
		arrayTextArea.setBorder(noFocusBorder);
		arrayTextArea.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				arrayTextArea.setBorder(focusBorder);
			}

			public void focusLost(FocusEvent e) {
				arrayTextArea.setBorder(noFocusBorder);
			}
		});

		arrayTextScrollPane = new JScrollPane(arrayTextArea);
		Border bevelOut = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		Border bevelIn = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
		arrayTextScrollPane.setBorder(BorderFactory.createCompoundBorder(bevelOut, bevelIn));

		// result label
		JLabel resultLabel = new JLabel(RESULT_LABEL_TEXT);
		resultLabel.setFocusable(true);
		resultLabel.setToolTipText(RESULT_TOOLTIP);
		resultLabel.setLabelFor(arrayTextArea);
		resultLabel.setDisplayedMnemonic(RESULT_MNEMONIC);
		resultLabel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				arrayTextArea.requestFocusInWindow();
			}
		});

		JPanel combinedPanel = new JPanel();
		GridBagLayout gridBagLayout = new GridBagLayout();
		combinedPanel.setLayout(gridBagLayout);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = c.gridy = 0;
		c.insets = new Insets(3, 3, 3, 3);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		combinedPanel.add(arrayTypeLabel, c);
		c.gridx = 1;
		combinedPanel.add(arrayTypeComboBox, c);
		c.gridx = 2;
		combinedPanel.add(arrayButton, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 3;
		combinedPanel.add(resultLabel, c);

		// create a panel for North part of main panel for button and combobox
		JPanel northPanel = new JPanel();
		FlowLayout myFlowLayout = new FlowLayout();
		myFlowLayout.setAlignment(FlowLayout.LEFT);
		northPanel.setLayout(myFlowLayout);
		northPanel.add(combinedPanel);

		JPanel myPanel = new JPanel();
		BorderLayout myLayout = new BorderLayout();
		myLayout.setHgap(5);
		myLayout.setVgap(5);
		myPanel.setLayout(myLayout);

		myPanel.add(northPanel, BorderLayout.NORTH);
		myPanel.add(arrayTextScrollPane, BorderLayout.CENTER);

		// redirect focus which, if sent from another tab pane via tab fastkey, and the focus was on a
		// control in the old tab pane, ends up "stuck" on the top-level panel of the new tab pane
		myPanel.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				arrayTypeComboBox.requestFocusInWindow();
			}
		});

		return myPanel;
	}

	private void doRpcConfirm() {
		this.rpcCount++;
		this.rpcCountTextField.setText(Integer.toString(rpcCount));
		rpcCountTextField.getAccessibleContext().setAccessibleName(
				RPC_COUNT_LABEL_TEXT + Integer.toString(this.rpcCount));
		this.rpcCountTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
				Integer.toString(rpcCount - 1), rpcCountTextField.getText());
	}

	/**
	 * event handler for show user info button
	 */
	private void doUserInfo() {
		userTextArea.setText("");
		userTextArea.append("DUZ: " + this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_DUZ)
				+ "\n");
		userTextArea.append("VPID: " + this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_VPID)
				+ "\n");
		userTextArea.append("Name (New Person .01): "
				+ this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_NEWPERSON01) + "\n");
		userTextArea.append("Name (Display): "
				+ this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_DISPLAY) + "\n");
		userTextArea.append("Name, Prefix: "
				+ this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_PREFIX) + "\n");
		userTextArea.append("Name, Given First: "
				+ this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_GIVENFIRST) + "\n");
		userTextArea.append("Name, Middle: "
				+ this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_MIDDLE) + "\n");
		userTextArea.append("Name, Family Last: "
				+ this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_FAMILYLAST) + "\n");
		userTextArea.append("Name, Suffix: "
				+ this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_SUFFIX) + "\n");
		userTextArea.append("Name, Degree: "
				+ this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_NAME_DEGREE) + "\n");
		userTextArea.append("Title: " + this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_TITLE)
				+ "\n");
		userTextArea.append("Service/Section: "
				+ this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_SERVICE_SECTION) + "\n");
		userTextArea.append("Division IEN: "
				+ this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_DIVISION_IEN) + "\n");
		userTextArea
				.append("Division Name: "
						+ this.userPrincipal
								.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_DIVISION_STATION_NAME) + "\n");
		userTextArea.append("Division Number: "
				+ this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_DIVISION_STATION_NUMBER)
				+ "\n");
		userTextArea.append("DTIME: " + this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_DTIME)
				+ "\n");
		userTextArea.append("Language: "
				+ this.userPrincipal.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_LANGUAGE) + "\n");

		userTextArea.setCaretPosition(0);
	}

	/**
	 * event handler for clear button press
	 */
	private void doClear() {

		userTextArea.setText("");
		wpResultsTextArea.setText("");
		stringResultTextField.setText("");
		rpcResultsTextArea.setText("");
		variableResultTextField.setText("");
		arrayTextArea.setText("");

		userTextArea.setCaretPosition(0);
		wpResultsTextArea.setCaretPosition(0);
		rpcResultsTextArea.setCaretPosition(0);
		arrayTextArea.setCaretPosition(0);
	}

	/**
	 * event handler for ping button press
	 */
	private void doPing() {

		if (userPrincipal == null) {

			DialogConfirm.showDialogConfirm(topFrame, "Not logged on.", "Error", DialogConfirm.MODE_ERROR_MESSAGE,
					this.timeout);

		} else {

			try {

				VistaLinkConnection myConnection = userPrincipal.getAuthenticatedConnection();
				RpcRequest vReq = RpcRequestFactory.getRpcRequest(RPC_CONTEXT, "XOBV TEST PING");
				if (this.xmlRadioButton.isSelected()) {
					vReq.setUseProprietaryMessageFormat(false);
				} else {
					vReq.setUseProprietaryMessageFormat(true);
				}
				RpcResponse vResp = myConnection.executeRPC(vReq);
				doRpcConfirm();
				DialogConfirm.showDialogConfirm(topFrame, vResp.getResults(), "Ping Result",
						DialogConfirm.MODE_INFORMATION_MESSAGE, this.timeout);

			} catch (Exception e) {

				DialogConfirm.showDialogConfirm(null, "Error encountered while executing RPC. "
						+ "Error encountered while executing RPC. " + e.getMessage(), "Error",
						DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);

			}
		}
	}

	/**
	 * event handler for WP button press
	 */
	private void doWordProcessing() {

		if (userPrincipal == null) {

			DialogConfirm.showDialogConfirm(topFrame, "Not logged on.", "Error", DialogConfirm.MODE_ERROR_MESSAGE,
					this.timeout);

		} else {

			try {

				VistaLinkConnection myConnection = userPrincipal.getAuthenticatedConnection();
				RpcRequest vReq = RpcRequestFactory.getRpcRequest(RPC_CONTEXT, "XOBV TEST WORD PROCESSING");
				if (this.xmlRadioButton.isSelected()) {
					vReq.setUseProprietaryMessageFormat(false);
				} else {
					vReq.setUseProprietaryMessageFormat(true);
				}
				RpcResponse vResp = myConnection.executeRPC(vReq);
				wpResultsTextArea.setText(vResp.getResults());
				wpResultsTextArea.setCaretPosition(0);
				doRpcConfirm();

			} catch (Exception e) {

				DialogConfirm.showDialogConfirm(null, "Error encountered while executing RPC. " + e.getMessage(),
						"Error", DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);

			}
		}
	}

	/**
	 * event handler for String button press
	 */
	private void doString() {

		if (userPrincipal == null) {

			DialogConfirm.showDialogConfirm(topFrame, "Not logged on.", "Error", DialogConfirm.MODE_ERROR_MESSAGE,
					this.timeout);

		} else {

			try {

				VistaLinkConnection myConnection = userPrincipal.getAuthenticatedConnection();
				RpcRequest vReq = RpcRequestFactory.getRpcRequest(RPC_CONTEXT, "XOBV TEST STRING");
				if (this.xmlRadioButton.isSelected()) {
					vReq.setUseProprietaryMessageFormat(false);
				} else {
					vReq.setUseProprietaryMessageFormat(true);
				}
				vReq.getParams().setParam(1, "string", this.stringTextField.getText());
				RpcResponse vResp = myConnection.executeRPC(vReq);
				stringResultTextField.setText(vResp.getResults());
				doRpcConfirm();

			} catch (Exception e) {

				DialogConfirm.showDialogConfirm(null, "Error encountered while executing RPC. " + e.getMessage(),
						"Error", DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);

			}
		}
	}

	/**
	 * event handler for Rpc List button press
	 */
	private void doRpcList() {

		if (userPrincipal == null) {

			DialogConfirm.showDialogConfirm(topFrame, "Not logged on.", "Error", DialogConfirm.MODE_ERROR_MESSAGE,
					this.timeout);

		} else {

			try {

				VistaLinkConnection myConnection = userPrincipal.getAuthenticatedConnection();
				RpcRequest vReq = RpcRequestFactory.getRpcRequest(RPC_CONTEXT, "XOBV TEST RPC LIST");
				if (this.xmlRadioButton.isSelected()) {
					vReq.setUseProprietaryMessageFormat(false);
				} else {
					vReq.setUseProprietaryMessageFormat(true);
				}
				vReq.getParams().setParam(1, "string", this.rpcListNamespaceTextField.getText());
				RpcResponse vResp = myConnection.executeRPC(vReq);
				rpcResultsTextArea.setText(vResp.getResults());
				rpcResultsTextArea.setCaretPosition(0);
				doRpcConfirm();

			} catch (Exception e) {

				DialogConfirm.showDialogConfirm(null, "Error encountered while executing RPC. " + e.getMessage(),
						"Error", DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);

			}
		}

	}

	/**
	 * event handler for Variable button press
	 */
	private void doVariable(String useReferenceType) {

		if (userPrincipal == null) {

			DialogConfirm.showDialogConfirm(topFrame, "Not logged on.", "Error", DialogConfirm.MODE_ERROR_MESSAGE,
					this.timeout);

		} else {

			try {

				VistaLinkConnection myConnection = userPrincipal.getAuthenticatedConnection();
				RpcRequest vReq = RpcRequestFactory.getRpcRequest(RPC_CONTEXT, "XWB GET VARIABLE VALUE");
				if (this.xmlRadioButton.isSelected()) {
					vReq.setUseProprietaryMessageFormat(false);
				} else {
					vReq.setUseProprietaryMessageFormat(true);
				}
				if (useReferenceType.equals(REF_TYPE_TRUE)) {
					vReq.getParams().setParam(1, "ref", "DT");
				} else {
					ArrayList params = new ArrayList();
					params.add(new RpcReferenceType("DTIME"));
					vReq.setParams(params);
				}
				RpcResponse vResp = myConnection.executeRPC(vReq);
				variableResultTextField.setText(vResp.getResults());
				doRpcConfirm();

			} catch (Exception e) {

				DialogConfirm.showDialogConfirm(null, "Error encountered while executing RPC. " + e.getMessage(),
						"Error", DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);

			}
		}

	}

	/**
	 * event handler for Array button press
	 */
	private void doArray(String localOrGlobal) {

		if (userPrincipal == null) {

			DialogConfirm.showDialogConfirm(topFrame, "Not logged on.", "Error", DialogConfirm.MODE_ERROR_MESSAGE,
					this.timeout);

		} else {

			try {

				VistaLinkConnection myConnection = userPrincipal.getAuthenticatedConnection();
				RpcRequest vReq = null;
				if (localOrGlobal.equals(ARRAY_TYPE_LOCAL)) {
					vReq = RpcRequestFactory.getRpcRequest(RPC_CONTEXT, "XOBV TEST LOCAL ARRAY");
					HashMap hm = new HashMap();
					hm.put("1", "Apple");
					hm.put("2", "Orange");
					hm.put("3", "Pear");
					hm.put("4", "'nana");
					vReq.getParams().setParam(1, "array", hm);
				} else {
					vReq = RpcRequestFactory.getRpcRequest(RPC_CONTEXT, "XOBV TEST GLOBAL ARRAY");
					HashMap hm = new HashMap();
					hm.put("1", "CD");
					hm.put("2", "Tape");
					hm.put("3", "DVD");
					vReq.getParams().setParam(1, "array", hm);
				}
				if (this.xmlRadioButton.isSelected()) {
					vReq.setUseProprietaryMessageFormat(false);
				} else {
					vReq.setUseProprietaryMessageFormat(true);
				}
				RpcResponse vResp = myConnection.executeRPC(vReq);
				arrayTextArea.setText(vResp.getResults());
				arrayTextArea.setCaretPosition(0);
				doRpcConfirm();

			} catch (Exception e) {

				DialogConfirm.showDialogConfirm(null, "Error encountered while executing RPC. " + e.getMessage(),
						"Error", DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);

			}
		}

	}

	/**
	 * Do the login
	 */
	private void login() {

		if (userPrincipal != null) {

			if (userPrincipal.getAuthenticatedConnection() != null) {

				statusTextField.setText(STATUS_CONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_DISCONNECTED_TEXT, STATUS_CONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_CONNECTED_TEXT);
			} else {

				statusTextField.setText(STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_CONNECTED_TEXT, STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);
			}
		}

		if (STATUS_DISCONNECTED_TEXT.equals(statusTextField.getText())) {

			try {

				// create the callback handler
				CallbackHandlerSwing cbhSwing = new CallbackHandlerSwing(topFrame);

				// create the LoginContext
				loginContext = new LoginContext((String) serverComboBox.getSelectedItem(), cbhSwing);

				// login to server
				loginContext.login();

				// get principal
				userPrincipal = VistaKernelPrincipalImpl.getKernelPrincipal(loginContext.getSubject());

				statusTextField.setText(STATUS_CONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_DISCONNECTED_TEXT, STATUS_CONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_CONNECTED_TEXT);
				connectedControlsEnable();
				this.timeout = Integer.parseInt(this.userPrincipal
						.getUserDemographicValue(VistaKernelPrincipalImpl.KEY_DTIME));

			} catch (VistaLoginModuleLoginsDisabledException e) {

				DialogConfirm.showDialogConfirm(null, "Logins are disabled; try later.", "Login error",
						DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);
				statusTextField.setText(STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_CONNECTED_TEXT, STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);

			} catch (VistaLoginModuleNoJobSlotsAvailableException e) {

				DialogConfirm.showDialogConfirm(null, "No job slots are available; try later.", "Login error",
						DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);
				statusTextField.setText(STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_CONNECTED_TEXT, STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);

			} catch (VistaLoginModuleUserTimedOutException e) {

				DialogConfirm.showDialogConfirm(topFrame, "User timed out.", "Login error",
						DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);
				statusTextField.setText(STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_CONNECTED_TEXT, STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);

			} catch (VistaLoginModuleUserCancelledException e) {

				DialogConfirm.showDialogConfirm(topFrame, "User cancelled login.", "Login error",
						DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);
				statusTextField.setText(STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_CONNECTED_TEXT, STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);

			} catch (VistaLoginModuleNoPathToListenerException e) {

				DialogConfirm.showDialogConfirm(null, "No path found to specified listener.", "Login error",
						DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);
				statusTextField.setText(STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_CONNECTED_TEXT, STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);

			} catch (VistaLoginModuleTooManyInvalidAttemptsException e) {

				DialogConfirm.showDialogConfirm(null, "Login cancelled due to too many invalid login attempts.",
						"Login error", DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);
				statusTextField.setText(STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_CONNECTED_TEXT, STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);

			} catch (VistaLoginModuleException e) {

				DialogConfirm.showDialogConfirm(topFrame, e.getMessage(), "Login error",
						DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);
				statusTextField.setText(STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_CONNECTED_TEXT, STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);

			} catch (LoginException e) {

				DialogConfirm.showDialogConfirm(topFrame, e.getMessage(), "Login error",
						DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);
				statusTextField.setText(STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_CONNECTED_TEXT, STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);

			} catch (SecurityException e) {

				DialogConfirm.showDialogConfirm(topFrame, e.getMessage(), "Login error",
						DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);
				statusTextField.setText(STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_CONNECTED_TEXT, STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);

			} catch (FoundationsException e) {

				DialogConfirm.showDialogConfirm(topFrame, e.getMessage(), "Login error",
						DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);
				statusTextField.setText(STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_CONNECTED_TEXT, STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);

			}
		}
	}

	/**
	 * Do the logout
	 */
	private void logout() {

		// Kernel logout
		if (this.userPrincipal != null) {

			try {

				loginContext.logout();

			} catch (LoginException e) {

				DialogConfirm.showDialogConfirm(topFrame, e.getMessage(), "Logout error",
						DialogConfirm.MODE_ERROR_MESSAGE, this.timeout);
				statusTextField.setText(STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR,
						STATUS_CONNECTED_TEXT, STATUS_DISCONNECTED_TEXT);
				statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);

			}

			statusTextField.setText(STATUS_DISCONNECTED_TEXT);
			statusTextField.getAccessibleContext().firePropertyChange(VISIBLE_PROPERTY_CHANGE_STR, STATUS_CONNECTED_TEXT,
					STATUS_DISCONNECTED_TEXT);
			statusTextField.getAccessibleContext().setAccessibleName(STATUS_LABEL_TEXT + STATUS_DISCONNECTED_TEXT);
			disconnectedControlsEnable();
			userPrincipal = null;
		}
		this.timeout = DEFAULT_TIMEOUT;
	}

	private void setTabPaneDefaultButton() {
		switch (tabPane.getSelectedIndex()) {
		case 0:
			topFrame.getRootPane().setDefaultButton(wpButton);
			break;
		case 1:
			topFrame.getRootPane().setDefaultButton(stringButton);
			break;
		case 2:
			topFrame.getRootPane().setDefaultButton(rpcButton);
			break;
		case 3:
			topFrame.getRootPane().setDefaultButton(variableButton);
			break;
		case 4:
			topFrame.getRootPane().setDefaultButton(arrayButton);
			break;
		case 5:
			topFrame.getRootPane().setDefaultButton(userButton);
			break;
		}
	}

	private void createFocusBorders() throws UnsupportedLookAndFeelException, IllegalAccessException,
			ClassNotFoundException, InstantiationException {

		// set the look and feel to java.
		UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

		// get border from current LaF
		Border defaultBorder = UIManager.getBorder("TextField.border");
		focusBorder = BorderFactory.createCompoundBorder(UIManager.getBorder("List.focusCellHighlightBorder"),
				defaultBorder);
		noFocusBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(
				UIManager.getColor("control"), 1), defaultBorder);
	}

	private void connectedControlsEnable() {

		disableOrEnableContainer(this.tabPane, true);
		disableOrEnableContainer(this.messageFormatPanel, true);

		this.closeButton.setEnabled(true);
		this.clearButton.setEnabled(true);
		this.pingButton.setEnabled(true);

		this.connectButton.setEnabled(false);
		this.serverComboBox.setEnabled(false);
		this.ipTextField.setEnabled(false);
		this.portTextField.setEnabled(false);

		mainPanel.setFocusTraversalPolicy(connectedFocusTraversalPolicy);
		setTabPaneDefaultButton();
		tabPane.requestFocusInWindow();
	}

	private void disconnectedControlsEnable() {

		this.connectButton.setEnabled(true);
		this.serverComboBox.setEnabled(true);
		this.ipTextField.setEnabled(true);
		this.portTextField.setEnabled(true);

		disableOrEnableContainer(this.tabPane, false);
		disableOrEnableContainer(this.messageFormatPanel, false);

		this.closeButton.setEnabled(false);
		this.clearButton.setEnabled(false);
		this.pingButton.setEnabled(false);

		this.rpcCount = 0;
		this.rpcCountTextField.setText(Integer.toString(rpcCount));
		rpcCountTextField.getAccessibleContext().setAccessibleName(
				RPC_COUNT_LABEL_TEXT + Integer.toString(this.rpcCount));
		topFrame.getRootPane().setDefaultButton(this.connectButton);

		mainPanel.setFocusTraversalPolicy(disconnectedFocusTraversalPolicy);

		// do this last, so that the focus actually is set here
		ipTextField.requestFocusInWindow();
	}

	private void disableOrEnableContainer(Container c, boolean enableValue) {

		if (c != null) {
			c.setEnabled(enableValue);
			Component[] components = c.getComponents();
			for (int i = 0; i < components.length; i++) {
				components[i].setEnabled(enableValue);
				if (components[i] instanceof Container) {
					disableOrEnableContainer((Container) components[i], enableValue);
				}
			}
		}
	}

	private Vector getAppNames() throws IOException {

		Vector returnVal = new Vector();

		// get JAAS config file from VM argument -- for this program, we don't accept
		// JAAS config files set any other way, e.g., in the Java security properties file.
		// As such, we expect -D command line to set value with "==" which means to override any other
		// JAAS config file location.
		String configPath = System.getProperty("java.security.auth.login.config");
		if (configPath == null) {
			throw new RuntimeException(
					"JAAS configuration file not passed in using the VM parameter -Djava.security.auth.login.config;\nServer IP and port may be entered as free text only.");
		} else if (!configPath.startsWith("=")) {
			throw new RuntimeException(
					"Value of JAAS configuration file VM parameter -Djava.security.auth.login.config must be passed with \"==\".");
		}
		configPath = configPath.substring(1);
		File configFile = new File(configPath);

		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(new FileInputStream(configFile.getCanonicalPath()), "UTF-8");

			// Set up to tokenize the JAAS config file
			BufferedReader reader = new BufferedReader(isr);
			StreamTokenizer st = new StreamTokenizer(reader);
			st.quoteChar('"');
			st.wordChars('$', '$');
			st.wordChars('_', '_');
			st.wordChars('-', '-');
			st.lowerCaseMode(false);
			st.slashSlashComments(true);
			st.slashStarComments(true);
			st.eolIsSignificant(true);

			int token = 0;
			// parsing state variables
			boolean inBraces = false;
			boolean outOfBracesHaveAppName = false;

			token = st.nextToken();
			while (token != StreamTokenizer.TT_EOF) {
				switch (token) {

				case 123: // '{'
					if (!inBraces) {
						inBraces = true;
					} else {
						throw new RuntimeException("Error parsing config file, mismatched braces.");
					}
					break;
				case 125: // '}'
					if (inBraces) {
						inBraces = false;
						outOfBracesHaveAppName = false;
					} else {
						throw new RuntimeException("Error parsing config file, mismatched braces.");
					}
					break;
				case StreamTokenizer.TT_WORD:
					if (!inBraces) {
						if (!outOfBracesHaveAppName) {
							returnVal.add(st.sval);
							outOfBracesHaveAppName = true;
						} else {
							throw new RuntimeException(
									"Error parsing config file, multiple application names outside of braces.");
						}
					}
					// if inBraces == true, we don't need any of the settings underneath the app name
					break;
				}
				token = st.nextToken();
			}
		} catch (Exception e) {
			RuntimeException e1 = new RuntimeException("Error reading jaas.config file: " + e.getClass().getName() + " "
					+ e.getMessage());
			throw e1;
		} finally {
			if (isr != null) {
				isr.close();
			}
		}

		return returnVal;
	}

	private String getStringFromArray(String[] messages) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < messages.length; i++) {
			sb.append(messages[i]);
			sb.append('\n');
		}
		return sb.toString();
	}

	/**
	 * Provides a focus traversal policy for this dialog, when it's in disconnected mode
	 */
	public class DisconnectedFocusTraversalPolicy extends FocusTraversalPolicy {

		/**
		 * get the next component in the focus traversal
		 * @param focusCycleRoot
		 *            the root of the focus cycle
		 * @param aComponent
		 *            currently focused component
		 * @return returns the next component in the (forward) cycle
		 */
		public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {

			if (aComponent.equals(ipTextField)) {
				return portTextField;
			} else if (aComponent.equals(portTextField)) {
				return serverComboBox;
			} else if (aComponent.equals(serverComboBox)) {
				return connectButton;
			} else if (aComponent.equals(connectButton)) {
				return statusTextField;
			} else if (aComponent.equals(statusTextField)) {
				return ipTextField;

				/* now the cases outside the normal cycle */

			} else if (aComponent.equals(serverComboBoxLabel)) {
				return serverComboBox;
			} else if (aComponent.equals(statusLabel)) {
				return statusTextField;
			} else if (aComponent.equals(ipLabel)) {
				return ipTextField;
			} else if (aComponent.equals(portLabel)) {
				return portTextField;
			}
			return ipTextField;
		}

		/**
		 * get the previous (reverse direction) component in the focus traversal cycle
		 * @param focusCycleRoot
		 *            the root of the focus cycle
		 * @param aComponent
		 *            currently focused component
		 * @return returns the next component in the (reverse) cycle
		 */
		public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {

			if (aComponent.equals(connectButton)) {
				return serverComboBox;
			} else if (aComponent.equals(serverComboBox)) {
				return portTextField;
			} else if (aComponent.equals(portTextField)) {
				return ipTextField;
			} else if (aComponent.equals(ipTextField)) {
				return statusTextField;
			} else if (aComponent.equals(statusTextField)) {
				return connectButton;

				/* now the cases outside the normal cycle */

			} else if (aComponent.equals(serverComboBoxLabel)) {
				return portTextField;
			} else if (aComponent.equals(statusLabel)) {
				return connectButton;
			} else if (aComponent.equals(ipLabel)) {
				return statusTextField;
			} else if (aComponent.equals(portLabel)) {
				return ipTextField;
			}
			return ipTextField;
		}

		/**
		 * gets the default component to focus on
		 * @param focusCycleRoot
		 *            the root of the focus cycle
		 * @return the default component in the focus cycle
		 */
		public Component getDefaultComponent(Container focusCycleRoot) {
			return serverComboBox;
		}

		/**
		 * gets the last component in the focus cycle
		 * @param focusCycleRoot
		 *            the root of the focus cycle
		 * @return the last component in the focus cycle
		 */
		public Component getLastComponent(Container focusCycleRoot) {
			return connectButton;
		}

		/**
		 * gets the first component in the focus cycle
		 * @param focusCycleRoot
		 *            the root of the focus cycle
		 * @return the first component in the focus cycle
		 */
		public Component getFirstComponent(Container focusCycleRoot) {
			return serverComboBox;
		}
	}

	/**
	 * Provides a focus traversal policy for this dialog, when it's in connected mode
	 */
	public class ConnectedFocusTraversalPolicy extends FocusTraversalPolicy {

		/**
		 * get the next component in the focus traversal
		 * @param focusCycleRoot
		 *            the root of the focus cycle
		 * @param aComponent
		 *            currently focused component
		 * @return returns the next component in the (forward) cycle
		 */
		public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {

			if (aComponent.equals(pingButton)) {
				return statusTextField;
			} else if (aComponent.equals(statusTextField)) {
				return closeButton;
			} else if (aComponent.equals(closeButton)) {
				return xmlRadioButton;
			} else if (aComponent.equals(xmlRadioButton)) {
				return proprietaryRadioButton;
			} else if (aComponent.equals(proprietaryRadioButton)) {
				return clearButton;
			} else if (aComponent.equals(clearButton)) {
				return tabPane;
			} else if (aComponent.equals(tabPane)) {
				switch (tabPane.getSelectedIndex()) {
				case 0:
					return wpButton;
				case 1:
					return stringTextField;
				case 2:
					return rpcListNamespaceTextField;
				case 3:
					return refTypeComboBox;
				case 4:
					return arrayTypeComboBox;
				case 5:
					return userButton;
				default:
					break;
				}
			} else if (aComponent.equals(wpButton)) {
				return wpResultsTextArea;
			} else if (aComponent.equals(wpResultsTextArea)) {
				return rpcCountTextField;
			} else if (aComponent.equals(stringTextField)) {
				return stringButton;
			} else if (aComponent.equals(stringButton)) {
				return stringResultTextField;
			} else if (aComponent.equals(stringResultTextField)) {
				return rpcCountTextField;
			} else if (aComponent.equals(rpcListNamespaceTextField)) {
				return rpcButton;
			} else if (aComponent.equals(rpcButton)) {
				return rpcResultsTextArea;
			} else if (aComponent.equals(rpcResultsTextArea)) {
				return rpcCountTextField;
			} else if (aComponent.equals(refTypeComboBox)) {
				return variableButton;
			} else if (aComponent.equals(variableButton)) {
				return variableResultTextField;
			} else if (aComponent.equals(variableResultTextField)) {
				return rpcCountTextField;
			} else if (aComponent.equals(arrayTypeComboBox)) {
				return arrayButton;
			} else if (aComponent.equals(arrayButton)) {
				return arrayTextArea;
			} else if (aComponent.equals(arrayTextArea)) {
				return rpcCountTextField;
			} else if (aComponent.equals(userButton)) {
				return userTextArea;
			} else if (aComponent.equals(userTextArea)) {
				return rpcCountTextField;
			} else if (aComponent.equals(rpcCountTextField)) {
				return pingButton;
			}
			return pingButton;
		}

		/**
		 * get the previous (reverse direction) component in the focus traversal cycle
		 * @param focusCycleRoot
		 *            the root of the focus cycle
		 * @param aComponent
		 *            currently focused component
		 * @return returns the next component in the (reverse) cycle
		 */
		public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {

			if (aComponent.equals(userTextArea)) {
				return userButton;
			} else if (aComponent.equals(userButton)) {
				return tabPane;
			} else if (aComponent.equals(arrayTextArea)) {
				return arrayButton;
			} else if (aComponent.equals(arrayButton)) {
				return arrayTypeComboBox;
			} else if (aComponent.equals(arrayTypeComboBox)) {
				return tabPane;
			} else if (aComponent.equals(variableResultTextField)) {
				return variableButton;
			} else if (aComponent.equals(variableButton)) {
				return refTypeComboBox;
			} else if (aComponent.equals(refTypeComboBox)) {
				return tabPane;
			} else if (aComponent.equals(rpcResultsTextArea)) {
				return rpcButton;
			} else if (aComponent.equals(rpcButton)) {
				return rpcListNamespaceTextField;
			} else if (aComponent.equals(rpcListNamespaceTextField)) {
				return tabPane;
			} else if (aComponent.equals(stringResultTextField)) {
				return stringButton;
			} else if (aComponent.equals(stringButton)) {
				return stringTextField;
			} else if (aComponent.equals(stringTextField)) {
				return tabPane;
			} else if (aComponent.equals(wpResultsTextArea)) {
				return wpButton;
			} else if (aComponent.equals(wpButton)) {
				return tabPane;
			} else if (aComponent.equals(tabPane)) {
				return clearButton;
			} else if (aComponent.equals(clearButton)) {
				return proprietaryRadioButton;
			} else if (aComponent.equals(proprietaryRadioButton)) {
				return xmlRadioButton;
			} else if (aComponent.equals(xmlRadioButton)) {
				return closeButton;
			} else if (aComponent.equals(closeButton)) {
				return statusTextField;
			} else if (aComponent.equals(statusTextField)) {
				return pingButton;
			} else if (aComponent.equals(pingButton)) {
				return rpcCountTextField;
			} else if (aComponent.equals(rpcCountTextField)) {

				switch (tabPane.getSelectedIndex()) {
				case 0:
					return wpResultsTextArea;
				case 1:
					return stringResultTextField;
				case 2:
					return rpcResultsTextArea;
				case 3:
					return variableResultTextField;
				case 4:
					return arrayTextArea;
				case 5:
					return userTextArea;
				default:
					break;
				}
			}
			return pingButton;
		}

		/**
		 * gets the default component to focus on
		 * @param focusCycleRoot
		 *            the root of the focus cycle
		 * @return the default component in the focus cycle
		 */
		public Component getDefaultComponent(Container focusCycleRoot) {
			return closeButton;
		}

		/**
		 * gets the last component in the focus cycle
		 * @param focusCycleRoot
		 *            the root of the focus cycle
		 * @return the last component in the focus cycle
		 */
		public Component getLastComponent(Container focusCycleRoot) {
			switch (tabPane.getSelectedIndex()) {
			case 0:
				return wpResultsTextArea;
			case 1:
				return stringButton;
			case 2:
				return rpcResultsTextArea;
			case 3:
				return refTypeComboBox;
			case 4:
				return arrayTypeComboBox;
			case 5:
				return userTextArea;
			default:
				break;
			}
			return pingButton;
		}

		/**
		 * gets the first component in the focus cycle
		 * @param focusCycleRoot
		 *            the root of the focus cycle
		 * @return the first component in the focus cycle
		 */
		public Component getFirstComponent(Container focusCycleRoot) {
			return closeButton;
		}
	}

	/**
	 * pass IP and port as one value object
	 */
	class IpAndPort {

		private String name;
		private String ip;
		private String port;

		IpAndPort(String name, String ip, String port) {
			this.name = name;
			this.ip = ip;
			this.port = port;
		}

		public String getName() {
			return this.name;
		}

		public String getIp() {
			return this.ip;
		}

		public String getPort() {
			return this.port;
		}

		public String toString() {
			return name + " " + ip + "/" + port;
		}
	}

	private class IpPortFocusListener implements FocusListener {
		public void focusLost(FocusEvent e) {
			if ((ipTextField.getText().length() + portTextField.getText().length()) > 0) {
				serverComboBox.setSelectedIndex(0);
			}
		}

		public void focusGained(FocusEvent e) {
		}
	}
}