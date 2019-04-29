/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.triphase.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import es.gob.afirma.core.AOException;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.signers.AOSignConstants;
import es.gob.afirma.core.signers.CounterSignTarget;
import es.gob.afirma.core.signers.ExtraParamsProcessor;
import es.gob.afirma.core.signers.TriphaseData;
import es.gob.afirma.triphase.server.document.DocumentManager;
import es.gob.afirma.triphase.signer.processors.AutoTriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.CAdESASiCSTriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.CAdESTriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.FacturaETriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.PAdESTriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.Pkcs1TriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.TriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.XAdESASiCSTriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.XAdESTriPhasePreProcessor;

/** Servicio de firma electr&oacute;nica en 3 fases. */
public final class SignatureService extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	private static DocumentManager DOC_MANAGER;

	private static final String URL_DEFAULT_CHARSET = "utf-8"; //$NON-NLS-1$

	private static final String PARAM_NAME_OPERATION = "op"; //$NON-NLS-1$

	private static final String PARAM_VALUE_OPERATION_PRESIGN = "pre"; //$NON-NLS-1$
	private static final String PARAM_VALUE_OPERATION_POSTSIGN = "post"; //$NON-NLS-1$

	private static final String PARAM_NAME_SUB_OPERATION = "cop"; //$NON-NLS-1$

	private static final String PARAM_VALUE_SUB_OPERATION_SIGN = "sign"; //$NON-NLS-1$
	private static final String PARAM_VALUE_SUB_OPERATION_COSIGN = "cosign"; //$NON-NLS-1$
	private static final String PARAM_VALUE_SUB_OPERATION_COUNTERSIGN = "countersign"; //$NON-NLS-1$

	// Parametros que necesitamos para la prefirma
	private static final String PARAM_NAME_DOCID = "doc"; //$NON-NLS-1$
	private static final String PARAM_NAME_ALGORITHM = "algo"; //$NON-NLS-1$
	private static final String PARAM_NAME_FORMAT = "format"; //$NON-NLS-1$
	private static final String PARAM_NAME_EXTRA_PARAM = "params"; //$NON-NLS-1$
	private static final String PARAM_NAME_SESSION_DATA = "session"; //$NON-NLS-1$
	private static final String PARAM_NAME_CERT = "cert"; //$NON-NLS-1$

	/** Separador que debe usarse para incluir varios certificados dentro del mismo par&aacute;metro. */
	private static final String PARAM_NAME_CERT_SEPARATOR = ","; //$NON-NLS-1$

	/** Nombre del par&aacute;metro que identifica los nodos que deben contrafirmarse. */
	private static final String PARAM_NAME_TARGET_TYPE = "target"; //$NON-NLS-1$

	/** Indicador de finalizaci&oacute;n correcta de proceso. */
	private static final String SUCCESS = "OK NEWID="; //$NON-NLS-1$

	private static final String CONFIG_FILE = "tpsconfig.properties"; //$NON-NLS-1$

	private static final String OLD_CONFIG_FILE = "config.properties"; //$NON-NLS-1$

	/** Variable de entorno que determina el directorio en el que buscar el fichero de configuraci&oacute;n. */
	private static final String ENVIRONMENT_VAR_CONFIG_DIR = "clienteafirma.config.path"; //$NON-NLS-1$

	private static final String CONFIG_PARAM_DOCUMENT_MANAGER_CLASS = "document.manager"; //$NON-NLS-1$
	private static final String CONFIG_PARAM_ALLOW_ORIGIN = "Access-Control-Allow-Origin"; //$NON-NLS-1$
	private static final String CONFIG_PARAM_INSTALL_XMLDSIG = "alternative.xmldsig"; //$NON-NLS-1$

	private static final String EXTRA_PARAM_HEADLESS = "headless"; //$NON-NLS-1$

	/** Or&iacute;genes permitidos por defecto desde los que se pueden realizar peticiones al servicio. */
	private static final String ALL_ORIGINS_ALLOWED = "*"; //$NON-NLS-1$

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

		if (!config.containsKey(CONFIG_PARAM_DOCUMENT_MANAGER_CLASS)) {
			throw new IllegalArgumentException(
				"No se ha indicado el document manager (" + CONFIG_PARAM_DOCUMENT_MANAGER_CLASS + ") en el fichero de propiedades" //$NON-NLS-1$ //$NON-NLS-2$
			);
		}

		Class<?> docManagerClass;
		try {
			docManagerClass = Class.forName(config.getProperty(CONFIG_PARAM_DOCUMENT_MANAGER_CLASS));
		}
		catch (final ClassNotFoundException e) {
			throw new RuntimeException(
				"La clase DocumentManager indicada no existe (" + config.getProperty(CONFIG_PARAM_DOCUMENT_MANAGER_CLASS) + "): " + e, e //$NON-NLS-1$ //$NON-NLS-2$
			);
		}

		try {
			final Constructor<?> docManagerConstructor = docManagerClass.getConstructor(Properties.class);
			DOC_MANAGER = (DocumentManager) docManagerConstructor.newInstance(config);
		}
		catch (final Exception e) {
			try {
				DOC_MANAGER = (DocumentManager) docManagerClass.newInstance();
			}
			catch (final Exception e2) {
				throw new RuntimeException(
					"No se ha podido inicializar el DocumentManager. Debe tener un constructor vacio o que reciba un Properties: " + e2, e //$NON-NLS-1$
				);
			}
		}
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

	@Override
	protected void service(final HttpServletRequest request, final HttpServletResponse response) {

		LOGGER.info("== INICIO FIRMA TRIFASICA =="); //$NON-NLS-1$

		final Map<String, String> parameters = new HashMap<>();
		final String[] params;
		try {
			params = new String(AOUtil.getDataFromInputStream(request.getInputStream())).split("&"); //$NON-NLS-1$
		}
		catch (final Exception e) {
			LOGGER.severe("No se pudieron leer los parametros de la peticion: " + e); //$NON-NLS-1$
			return;
		}

		for (final String param : params) {
			if (param.indexOf('=') != -1) {
				try {
					parameters.put(param.substring(0, param.indexOf('=')), URLDecoder.decode(param.substring(param.indexOf('=') + 1), URL_DEFAULT_CHARSET));
				}
				catch (final Exception e) {
					LOGGER.warning("Error al decodificar un parametro de la peticion: " + e); //$NON-NLS-1$
				}
			}
		}

		final String allowOrigin = config.getProperty(CONFIG_PARAM_ALLOW_ORIGIN, ALL_ORIGINS_ALLOWED);

		response.setHeader("Access-Control-Allow-Origin", allowOrigin); //$NON-NLS-1$
		response.setContentType("text/plain"); //$NON-NLS-1$
		response.setCharacterEncoding("utf-8"); //$NON-NLS-1$

		// Obtenemos el codigo de operacion
		try (
			final PrintWriter out = response.getWriter();
		) {

			final String operation = parameters.get(PARAM_NAME_OPERATION);
			if (operation == null) {
				LOGGER.severe("No se ha indicado la operacion trifasica a realizar"); //$NON-NLS-1$
				out.print(ErrorManager.getErrorMessage(1));
				out.flush();
				return;
			}

			// Obtenemos el codigo de operacion
			final String subOperation = parameters.get(PARAM_NAME_SUB_OPERATION);
			if (subOperation == null || !PARAM_VALUE_SUB_OPERATION_SIGN.equalsIgnoreCase(subOperation)
					&& !PARAM_VALUE_SUB_OPERATION_COSIGN.equalsIgnoreCase(subOperation)
					&& !PARAM_VALUE_SUB_OPERATION_COUNTERSIGN.equalsIgnoreCase(subOperation)) {
				out.print(ErrorManager.getErrorMessage(13));
				out.flush();
				return;
			}


			// Obtenemos el formato de firma
			final String format = parameters.get(PARAM_NAME_FORMAT);
			LOGGER.info("Formato de firma seleccionado: " + format); //$NON-NLS-1$
			if (format == null) {
				LOGGER.warning("No se ha indicado formato de firma"); //$NON-NLS-1$
				out.print(ErrorManager.getErrorMessage(4));
				out.flush();
				return;
			}

			// Obtenemos los parametros adicionales para la firma
			Properties extraParams = new Properties();
			try {
				if (parameters.containsKey(PARAM_NAME_EXTRA_PARAM)) {
					extraParams.load(
						new ByteArrayInputStream(
							Base64.decode(parameters.get(PARAM_NAME_EXTRA_PARAM).trim(), true)
						)
					);
				}
			}
			catch (final Exception e) {
				LOGGER.severe("El formato de los parametros adicionales suministrado es erroneo: " +  e); //$NON-NLS-1$
				out.print(ErrorManager.getErrorMessage(6) + ": " + e); //$NON-NLS-1$);
				out.flush();
				return;
			}

			// Introducimos los parametros necesarios para que no se traten
			// de mostrar dialogos en servidor
			extraParams.setProperty(EXTRA_PARAM_HEADLESS, Boolean.TRUE.toString());

			try {
				extraParams = ExtraParamsProcessor.expandProperties(
						extraParams,
						null,
						format
						);
			}
			catch (final Exception e) {
				LOGGER.severe("Se han indicado una politica de firma y un formato incompatibles: "  + e); //$NON-NLS-1$

			}

			// Obtenemos los parametros adicionales para la firma
			byte[] sessionData = null;
			try {
				if (parameters.containsKey(PARAM_NAME_SESSION_DATA)) {
					sessionData = Base64.decode(parameters.get(PARAM_NAME_SESSION_DATA).trim(), true);
				}
			}
			catch (final Exception e) {
				LOGGER.severe("El formato de los datos de sesion suministrados es erroneo: "  + e); //$NON-NLS-1$
				out.print(ErrorManager.getErrorMessage(6) + ": " + e); //$NON-NLS-1$
				out.flush();
				return;
			}

			if (sessionData != null) {
				LOGGER.info("Recibidos los siguientes datos de sesion para '" + operation + "':\n" + new String(sessionData)); //$NON-NLS-1$ //$NON-NLS-2$
			}

			// Obtenemos el certificado
			final String cert = parameters.get(PARAM_NAME_CERT);
			if (cert == null) {
				LOGGER.warning("No se ha indicado certificado de firma"); //$NON-NLS-1$
				out.print(ErrorManager.getErrorMessage(5));
				out.flush();
				return;
			}

			final String[] receivedCerts = cert.split(PARAM_NAME_CERT_SEPARATOR);
			final X509Certificate[] signerCertChain = new X509Certificate[receivedCerts.length];
			for (int i = 0; i<receivedCerts.length; i++) {
				try {
					signerCertChain[i] =
						(X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate( //$NON-NLS-1$
							new ByteArrayInputStream(
								Base64.decode(receivedCerts[i], true)
							)
						)
					;
				}
				catch(final Exception e) {

					LOGGER.log(Level.SEVERE, "Error al decodificar el certificado: " + receivedCerts[i], e);  //$NON-NLS-1$
					out.print(ErrorManager.getErrorMessage(7));
					out.flush();
					return;
				}
			}

			byte[] docBytes = null;
			final String docId = parameters.get(PARAM_NAME_DOCID);
			if (docId != null) {
				try {
					LOGGER.info("Recuperamos el documento mediante el DocumentManager"); //$NON-NLS-1$
					docBytes = DOC_MANAGER.getDocument(docId, signerCertChain, extraParams);
					LOGGER.info(
						"Recuperado documento de " + docBytes.length + " octetos" //$NON-NLS-1$ //$NON-NLS-2$
					);
				}
				catch (final Throwable e) {
					LOGGER.warning("Error al recuperar el documento: " + e); //$NON-NLS-1$
					out.print(ErrorManager.getErrorMessage(14) + ": " + e); //$NON-NLS-1$
					out.flush();
					return;
				}
			}

			// Obtenemos el algoritmo de firma
			final String algorithm = parameters.get(PARAM_NAME_ALGORITHM);
			if (algorithm == null) {
				LOGGER.warning("No se ha indicado algoritmo de firma"); //$NON-NLS-1$
				out.print(ErrorManager.getErrorMessage(3));
				out.flush();
				return;
			}

			// Instanciamos el preprocesador adecuado
			final TriPhasePreProcessor prep;
			if (AOSignConstants.SIGN_FORMAT_PADES.equalsIgnoreCase(format) ||
				AOSignConstants.SIGN_FORMAT_PADES_TRI.equalsIgnoreCase(format)) {
						prep = new PAdESTriPhasePreProcessor();
			}
			else if (AOSignConstants.SIGN_FORMAT_CADES.equalsIgnoreCase(format) ||
					 AOSignConstants.SIGN_FORMAT_CADES_TRI.equalsIgnoreCase(format)) {
						prep = new CAdESTriPhasePreProcessor();
			}
			else if (AOSignConstants.SIGN_FORMAT_XADES.equalsIgnoreCase(format) ||
					 AOSignConstants.SIGN_FORMAT_XADES_TRI.equalsIgnoreCase(format)) {
						final boolean installXmlDSig = Boolean.parseBoolean(
							config.getProperty(
								CONFIG_PARAM_INSTALL_XMLDSIG, Boolean.FALSE.toString()
							)
						);
						prep = new XAdESTriPhasePreProcessor(installXmlDSig);
			}
			else if (AOSignConstants.SIGN_FORMAT_CADES_ASIC_S.equalsIgnoreCase(format) ||
					 AOSignConstants.SIGN_FORMAT_CADES_ASIC_S_TRI.equalsIgnoreCase(format)) {
						prep = new CAdESASiCSTriPhasePreProcessor();
			}
			else if (AOSignConstants.SIGN_FORMAT_XADES_ASIC_S.equalsIgnoreCase(format) ||
					 AOSignConstants.SIGN_FORMAT_XADES_ASIC_S_TRI.equalsIgnoreCase(format)) {
						final boolean installXmlDSig = Boolean.parseBoolean(
							config.getProperty(
								CONFIG_PARAM_INSTALL_XMLDSIG, Boolean.FALSE.toString()
							)
						);
						prep = new XAdESASiCSTriPhasePreProcessor(installXmlDSig);
			}
			else if (AOSignConstants.SIGN_FORMAT_FACTURAE.equalsIgnoreCase(format) ||
					 AOSignConstants.SIGN_FORMAT_FACTURAE_TRI.equalsIgnoreCase(format) ||
					 AOSignConstants.SIGN_FORMAT_FACTURAE_ALT1.equalsIgnoreCase(format)) {
						final boolean installXmlDSig = Boolean.parseBoolean(
							config.getProperty(
								CONFIG_PARAM_INSTALL_XMLDSIG, Boolean.FALSE.toString()
							)
						);
						prep = new FacturaETriPhasePreProcessor(installXmlDSig);
			}
			else if (AOSignConstants.SIGN_FORMAT_PKCS1.equalsIgnoreCase(format) ||
					 AOSignConstants.SIGN_FORMAT_PKCS1_TRI.equalsIgnoreCase(format)) {
						prep = new Pkcs1TriPhasePreProcessor();
			}
			else if (AOSignConstants.SIGN_FORMAT_AUTO.equalsIgnoreCase(format)) {
				final boolean installXmlDSig = Boolean.parseBoolean(
					config.getProperty(
						CONFIG_PARAM_INSTALL_XMLDSIG, Boolean.FALSE.toString()
					)
				);
				prep = new AutoTriPhasePreProcessor(installXmlDSig);
			}
			else {
				LOGGER.severe("Formato de firma no soportado: " + format); //$NON-NLS-1$
				out.print(ErrorManager.getErrorMessage(8));
				out.flush();
				return;
			}

			if (PARAM_VALUE_OPERATION_PRESIGN.equalsIgnoreCase(operation)) {

				LOGGER.info(" == PREFIRMA en servidor"); //$NON-NLS-1$

				final TriphaseData preRes;
				try {
					if (PARAM_VALUE_SUB_OPERATION_SIGN.equalsIgnoreCase(subOperation)) {
						preRes = prep.preProcessPreSign(
							docBytes,
							algorithm,
							signerCertChain,
							extraParams
						);
					}
					else if (PARAM_VALUE_SUB_OPERATION_COSIGN.equalsIgnoreCase(subOperation)) {
						preRes = prep.preProcessPreCoSign(
							docBytes,
							algorithm,
							signerCertChain,
							extraParams
						);
					}
					else if (PARAM_VALUE_SUB_OPERATION_COUNTERSIGN.equalsIgnoreCase(subOperation)) {

						CounterSignTarget target = CounterSignTarget.LEAFS;
						if (extraParams.containsKey(PARAM_NAME_TARGET_TYPE)) {
							final String targetValue = extraParams.getProperty(PARAM_NAME_TARGET_TYPE).trim();
							if (CounterSignTarget.TREE.toString().equalsIgnoreCase(targetValue)) {
								target = CounterSignTarget.TREE;
							}
						}

						preRes = prep.preProcessPreCounterSign(
							docBytes,
							algorithm,
							signerCertChain,
							extraParams,
							target
						);
					}
					else {
						throw new AOException("No se reconoce el codigo de sub-operacion: " + subOperation); //$NON-NLS-1$
					}

					LOGGER.info("Se ha calculado el resultado de la prefirma y se devuelve"); //$NON-NLS-1$
				}
				catch (final Exception e) {
					LOGGER.log(Level.SEVERE, "Error en la prefirma: " + e, e); //$NON-NLS-1$
					out.print(ErrorManager.getErrorMessage(9) + ": " + e); //$NON-NLS-1$
					out.flush();
					return;
				}

				out.print(
					Base64.encode(
						preRes.toString().getBytes(),
						true
					)
				);

				out.flush();

				LOGGER.info("== FIN PREFIRMA"); //$NON-NLS-1$
			}
			else if (PARAM_VALUE_OPERATION_POSTSIGN.equalsIgnoreCase(operation)) {

				LOGGER.info(" == POSTFIRMA en servidor"); //$NON-NLS-1$

				final byte[] signedDoc;
				try {
					if (PARAM_VALUE_SUB_OPERATION_SIGN.equals(subOperation)) {
						signedDoc = prep.preProcessPostSign(
							docBytes,
							algorithm,
							signerCertChain,
							extraParams,
							sessionData
						);
					}
					else if (PARAM_VALUE_SUB_OPERATION_COSIGN.equals(subOperation)) {
						signedDoc = prep.preProcessPostCoSign(
							docBytes,
							algorithm,
							signerCertChain,
							extraParams,
							sessionData
						);
					}
					else if (PARAM_VALUE_SUB_OPERATION_COUNTERSIGN.equals(subOperation)) {

						CounterSignTarget target = CounterSignTarget.LEAFS;
						if (extraParams.containsKey(PARAM_NAME_TARGET_TYPE)) {
							final String targetValue = extraParams.getProperty(PARAM_NAME_TARGET_TYPE).trim();
							if (CounterSignTarget.TREE.toString().equalsIgnoreCase(targetValue)) {
								target = CounterSignTarget.TREE;
							}
						}

						signedDoc = prep.preProcessPostCounterSign(
							docBytes,
							algorithm,
							signerCertChain,
							extraParams,
							sessionData,
							target
						);
					}
					else {
						throw new AOException("No se reconoce el codigo de sub-operacion: " + subOperation); //$NON-NLS-1$
					}
				}
				catch (final Exception e) {
					LOGGER.log(Level.SEVERE, "Error en la postfirma: " + e, e); //$NON-NLS-1$
					out.print(ErrorManager.getErrorMessage(12) + ": " + e); //$NON-NLS-1$
					out.flush();
					return;
				}

				// Establecemos parametros adicionales que se pueden utilizar para guardar el documento
				if (!extraParams.containsKey(PARAM_NAME_FORMAT)) {
					extraParams.setProperty(PARAM_NAME_FORMAT, format);
				}

				LOGGER.info(" Se ha calculado el resultado de la postfirma y se devuelve. Numero de bytes: " + signedDoc.length); //$NON-NLS-1$

				// Devolvemos al servidor documental el documento firmado
				LOGGER.info("Almacenamos la firma mediante el DocumentManager"); //$NON-NLS-1$
				final String newDocId;
				try {
					newDocId = DOC_MANAGER.storeDocument(docId, signerCertChain, signedDoc, extraParams);
				}
				catch(final Throwable e) {
					LOGGER.severe("Error al almacenar el documento: " + e); //$NON-NLS-1$
					out.print(ErrorManager.getErrorMessage(10) + ": " + e); //$NON-NLS-1$
					out.flush();
					return;
				}
				LOGGER.info("Documento almacenado"); //$NON-NLS-1$

				out.println(
					new StringBuilder(newDocId.length() + SUCCESS.length()).
					append(SUCCESS).append(newDocId).toString()
				);
				out.flush();

				LOGGER.info("== FIN POSTFIRMA"); //$NON-NLS-1$
			}
			else {
				out.println(ErrorManager.getErrorMessage(11));
			}
		}
        catch (final Exception e) {
        	LOGGER.severe("No se pudo contestar a la peticion: " + e); //$NON-NLS-1$
        	try {
				response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, "No se pude contestar a la peticion: " + e); //$NON-NLS-1$
			}
        	catch (final IOException e1) {
        		LOGGER.severe("No se pudo enviar un error HTTP 500: " + e1); //$NON-NLS-1$
			}
        	return;
        }
	}
}
