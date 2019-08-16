/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.signers.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import es.gob.afirma.triphase.server.SignatureService;

/**
 * Gestiona la configuraci&oacute;n espec&iacute;fica del proceso de firma de lotes.
 */
public class TriConfigManager {

	private static Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	private static final String CONFIG_FILE = "tpsconfig.properties"; //$NON-NLS-1$

	private static final String OLD_CONFIG_FILE = "config.properties"; //$NON-NLS-1$

	/** Variable de entorno que determina el directorio en el que buscar el fichero de configuraci&oacute;n. */
	private static final String ENVIRONMENT_VAR_CONFIG_DIR = "clienteafirma.config.path"; //$NON-NLS-1$

	private static final String CONFIG_PARAM_ALLOW_ORIGIN = "Access-Control-Allow-Origin"; //$NON-NLS-1$

	private static final String CONFIG_PARAM_INSTALL_XMLDSIG = "alternative.xmldsig"; //$NON-NLS-1$

	/** Or&iacute;genes permitidos por defecto desde los que se pueden realizar peticiones al servicio. */
	private static final String DEFAULT_CONFIG_PARAM_ALLOW_ORIGIN = "*"; //$NON-NLS-1$

	private static String configDir;

	private static final Properties config;

	static {

		try {
			configDir = System.getProperty(ENVIRONMENT_VAR_CONFIG_DIR);
		}
		catch (final Exception e) {
			LOGGER.warning(
					"No se ha podido obtener el directorio del fichero de configuracion: " + e);//$NON-NLS-1$
			configDir = null;
		}

		// Cargamos la configuracion del servicio
		Properties configProperties = loadConfigFile(CONFIG_FILE);
		if (configProperties == null) {
			configProperties = loadConfigFile(OLD_CONFIG_FILE);
		}

		if (configProperties == null) {
			throw new RuntimeException("No se ha encontrado el fichero de configuracion del servicio"); //$NON-NLS-1$
		}

		config = configProperties;
	}

	public static String getAllowOrigin() {
		return config.getProperty(CONFIG_PARAM_ALLOW_ORIGIN, DEFAULT_CONFIG_PARAM_ALLOW_ORIGIN);
	}

	public static boolean needInstallXmlDSig() {
		return Boolean.parseBoolean(config.getProperty(CONFIG_PARAM_INSTALL_XMLDSIG, Boolean.FALSE.toString()));
	}

	/**
	 * Intenta cargar un fichero propiedades del directorio indicado mediante variable de entorno
	 * o del classpath si no se encuentra.
	 * @param configFilename Nombre del fichero de propedades.
	 * @return Propiedades cargadas o {@code null} si no se pudo cargar el fichero.
	 */
	private static Properties loadConfigFile(final String configFilename) {


		LOGGER.info("Se cargara el fichero de configuracion " + configFilename); //$NON-NLS-1$

		Properties configProperties = null;

		if (configDir != null) {
			try {
				final File configFile = new File(configDir, configFilename).getCanonicalFile();
				try (final InputStream configIs = new FileInputStream(configFile);) {
					configProperties = new Properties();
					configProperties.load(configIs);
				}
			}
			catch (final Exception e) {
				LOGGER.warning(
						"No se pudo cargar el fichero de configuracion " + configFilename + //$NON-NLS-1$
						" desde el directorio " + configDir + ": " + e); //$NON-NLS-1$ //$NON-NLS-2$
				configProperties = null;
			}
		}

		if (configProperties == null) {
			LOGGER.info(
					"Se cargara el fichero de configuracion " + configFilename + " desde el CLASSPATH"); //$NON-NLS-1$ //$NON-NLS-2$

			try (final InputStream configIs = SignatureService.class.getClassLoader().getResourceAsStream(configFilename);) {
					configProperties = new Properties();
					configProperties.load(configIs);
			}
			catch (final Exception e) {
				LOGGER.warning(
						"No se pudo cargar el fichero de configuracion " + configFilename + " desde el CLASSPATH: " + e); //$NON-NLS-1$ //$NON-NLS-2$
				configProperties = null;
			}
		}

		return configProperties;
	}
}
