package gov.va.med.vistalink.samples;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * JAAS configuration class for VistaLink IP and Port that can be configured at runtime without reading ip and port
 * values from a jaas.config file.
 * 
 */
public class RuntimeJaasIpPortConfiguration extends Configuration {

	private AppConfigurationEntry[] vistaConfig = new AppConfigurationEntry[1];

	RuntimeJaasIpPortConfiguration(String ip, String port) {
		super();
		vistaConfig[0] = getAppConfigurationEntry(ip, port);
	}

	public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
		// don't care about the name... always return same entry
		// return copy to avoid exposing internal pointer
		AppConfigurationEntry[] returnVal = vistaConfig;
		return returnVal;
	}

	public void refresh() {
		// nothing to implement
	}

	/**
	 * return an JAAS-compatible, VistaLink-compatible application configuration entry for a given ip and port.
	 * @param ip
	 * @param port
	 * @return
	 */
	private AppConfigurationEntry getAppConfigurationEntry(String ip, String port) {

		Map optionMap = new HashMap();
		optionMap.put("gov.va.med.vistalink.security.ServerAddressKey", ip);
		optionMap.put("gov.va.med.vistalink.security.ServerPortKey", port);
		AppConfigurationEntry myEntry = new AppConfigurationEntry("gov.va.med.vistalink.security.VistaLoginModule",
				AppConfigurationEntry.LoginModuleControlFlag.REQUISITE, optionMap);
		return myEntry;
	}
}