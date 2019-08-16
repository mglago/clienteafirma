/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.standalone.protocol;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.misc.Platform;
import es.gob.afirma.core.misc.protocol.ParameterException;
import es.gob.afirma.core.misc.protocol.ParameterLocalAccessRequestedException;
import es.gob.afirma.core.misc.protocol.ParameterNeedsUpdatedVersionException;
import es.gob.afirma.core.misc.protocol.ProtocolInvocationUriParser;
import es.gob.afirma.core.misc.protocol.ProtocolVersion;
import es.gob.afirma.core.misc.protocol.UrlParametersForBatch;
import es.gob.afirma.core.misc.protocol.UrlParametersToGetCurrentLog;
import es.gob.afirma.core.misc.protocol.UrlParametersToLoad;
import es.gob.afirma.core.misc.protocol.UrlParametersToSave;
import es.gob.afirma.core.misc.protocol.UrlParametersToSelectCert;
import es.gob.afirma.core.misc.protocol.UrlParametersToSign;
import es.gob.afirma.core.misc.protocol.UrlParametersToSignAndSave;
import es.gob.afirma.standalone.JMulticardUtilities;
import es.gob.afirma.standalone.protocol.ProtocolInvocationLauncherUtil.DecryptionException;
import es.gob.afirma.standalone.protocol.ProtocolInvocationLauncherUtil.InvalidEncryptedDataLengthException;
import es.gob.afirma.standalone.ui.MainMenu;
import es.gob.afirma.standalone.ui.OSXHandler;
import es.gob.afirma.standalone.ui.preferences.PreferencesManager;

/** Gestiona la ejecuci&oacute;n de AutoFirma en una invocaci&oacute;n
 * por protocolo y bajo un entorno compatible <code>Swing</code>.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s */
public final class ProtocolInvocationLauncher {

    private static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

    private static final String RESULT_OK = "OK"; //$NON-NLS-1$

    static final ProtocolVersion MAX_PROTOCOL_VERSION_SUPPORTED = ProtocolVersion.VERSION_1;

    /** Clave privada fijada para reutilizarse en operaciones sucesivas. */
	private static PrivateKeyEntry stickyKeyEntry = null;

	/**
	 * Hilo para solicitar activamente a traves del servidor intermedio que se
	 * espere a que termine de ejecutarse la aplicaci&oacute;n.
	 */
    private static Thread activeWaitingThread = null;

	/** Recupera la entrada con la clave y certificado prefijados para las
	 * operaciones con certificados.
	 * @return Entrada con el certificado y la clave prefijados. */
	public static PrivateKeyEntry getStickyKeyEntry() {
		return stickyKeyEntry;
	}

	/** Establece una clave y certificado prefijados para las
	 * operaciones con certificados.
	 * @param stickyKeyEntry Entrada con el certificado y la clave prefijados. */
	public static void setStickyKeyEntry(final PrivateKeyEntry stickyKeyEntry) {
		ProtocolInvocationLauncher.stickyKeyEntry = stickyKeyEntry;
	}

    /** Lanza la aplicaci&oacute;n y realiza las acciones indicadas en la URL.
     * Este m&eacute;todo usa siempre comunicaci&oacute;n mediante servidor intermedio, nunca localmente.
     * @param urlString URL de invocaci&oacute;n por protocolo.
     * @return Resultado de la operaci&oacute;n. */
    public static String launch(final String urlString)  {
        return launch(urlString, false);
    }

    @SuppressWarnings({ "unused", "static-method" })
	void showAbout(final EventObject event) {
    	MainMenu.showAbout(null);
    }

    /** Lanza la aplicaci&oacute;n y realiza las acciones indicadas en la URL.
     * @param urlString URL de invocaci&oacute;n por protocolo.
     * @param bySocket Si se establece a <code>true</code> se usa una comuicaci&oacute;n de vuelta mediante conexi&oacute;n
     *                 HTTP local (a <code>localhost</code>), si se establece a <code>false</code> se usa un servidor intermedio
     *                 para esta comunicaci&oacute;n de vuelta.
     * @return Resultado de la operaci&oacute;n. */
    public static String launch(final String urlString, final boolean bySocket)  {
        // En macOS sobrecargamos el "Acerca de..." del sistema operativo, que tambien
        // aparece en la invocacion por protocolo
        if (Platform.OS.MACOSX.equals(Platform.getOS())) {
	    	try {
	    		final Method aboutMethod = ProtocolInvocationLauncher.class.getDeclaredMethod("showAbout", new Class[]{EventObject.class}); //$NON-NLS-1$
	    		OSXHandler.setAboutHandler(null, aboutMethod);
	    	}
	    	catch (final Exception e) {
	    		LOGGER.warning("No ha sido posible establecer el menu 'Acerca de...' de OS X: " + e); //$NON-NLS-1$
			}
        }

        if (urlString == null) {
            LOGGER.severe("No se ha proporcionado una URL para la invocacion"); //$NON-NLS-1$
            ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_NULL_URI);
            return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_NULL_URI);
        }
        if (!urlString.startsWith("afirma://")) { //$NON-NLS-1$
            LOGGER.severe("La URL de invocacion no comienza por 'afirma://'"); //$NON-NLS-1$
            ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_UNSUPPORTED_PROTOCOL);
            return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_UNSUPPORTED_PROTOCOL);
        }

        // Configuramos el uso de JMulticard segun lo establecido en el dialogo de preferencias
        final boolean jMulticardEnabled = PreferencesManager.getBoolean(
        		PreferencesManager.PREFERENCE_GENERAL_ENABLED_JMULTICARD);
        JMulticardUtilities.configureJMulticard(jMulticardEnabled);

        // Se invoca la aplicacion para iniciar la comunicacion por socket
        if (urlString.startsWith("afirma://websocket?") || urlString.startsWith("afirma://websocket/?")) { //$NON-NLS-1$ //$NON-NLS-2$
        	 LOGGER.info("Se inicia el modo de comunicacion por websockets: " + urlString); //$NON-NLS-1$
        	 try {
        		 AfirmaWebSocketServer.startService(urlString);
        	 }
             catch(final UnsupportedProtocolException e) {
             	LOGGER.severe("La version del protocolo no esta soportada (" + e.getVersion() + "): " + e); //$NON-NLS-1$ //$NON-NLS-2$
             	final String errorCode = ProtocolInvocationLauncherErrorManager.ERROR_UNSUPPORTED_PROCEDURE;
             	ProtocolInvocationLauncherErrorManager.showError(errorCode);
             	return ProtocolInvocationLauncherErrorManager.getErrorMessage(errorCode);
             }
        	 catch(final GeneralSecurityException | IOException e) {
              	LOGGER.log(Level.SEVERE, "Ocurrio un error durante la carga del almacen de claves", e); //$NON-NLS-1$
              	final String errorCode = e instanceof IOException ?
              			ProtocolInvocationLauncherErrorManager.ERROR_CANNOT_FIND_SSL_KEYSTORE :
              			ProtocolInvocationLauncherErrorManager.ERROR_CANNOT_ACCESS_SSL_KEYSTORE;
              	ProtocolInvocationLauncherErrorManager.showError(errorCode);
              	return ProtocolInvocationLauncherErrorManager.getErrorMessage(errorCode);
              }

             return RESULT_OK;
        }
        // Se invoca la aplicacion para iniciar la comunicacion por socket
        else if (urlString.startsWith("afirma://service?") || urlString.startsWith("afirma://service/?")) { //$NON-NLS-1$ //$NON-NLS-2$
            LOGGER.info("Se inicia el modo de comunicacion por sockets: " + urlString); //$NON-NLS-1$
            try {
            	ServiceInvocationManager.startService(urlString);
            }
            catch(final UnsupportedProtocolException e) {
            	LOGGER.severe("La version del protocolo no esta soportada (" + e.getVersion() + "): " + e); //$NON-NLS-1$ //$NON-NLS-2$
            	final String errorCode = e.isNewVersionNeeded() ?
            			ProtocolInvocationLauncherErrorManager.ERROR_UNSUPPORTED_PROCEDURE :
            				ProtocolInvocationLauncherErrorManager.ERROR_UNSUPPORTED_PROCEDURE;
            	ProtocolInvocationLauncherErrorManager.showError(errorCode);
            	return ProtocolInvocationLauncherErrorManager.getErrorMessage(errorCode);
            }

            return RESULT_OK;
        }
        // Se solicita una operacion de firma batch
        else if (urlString.startsWith("afirma://batch?") || urlString.startsWith("afirma://batch/?")) { //$NON-NLS-1$ //$NON-NLS-2$
        	LOGGER.info("Se invoca a la aplicacion para el procesado de un lote de firma"); //$NON-NLS-1$
            try {

                UrlParametersForBatch params = ProtocolInvocationUriParser.getParametersForBatch(urlString);

                // Si se indica un identificador de fichero, es que el XML de definicion de lote se tiene que
                // descargar desde el servidor intermedio
                if (params.getFileId() != null) {
                    final byte[] xmlBatchDefinition;
                    try {
                        xmlBatchDefinition = ProtocolInvocationLauncherUtil.getDataFromRetrieveServlet(params);
                    }
                    catch(final InvalidEncryptedDataLengthException e) {
                        LOGGER.log(Level.SEVERE, "No se pueden recuperar los datos del servidor: " + e, e); //$NON-NLS-1$
                        ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_RECOVERING_DATA);
                        return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_RECOVERING_DATA);
                    }
                    catch(final DecryptionException e) {
                        LOGGER.severe("Error al descifrar los datos obtenidos: " + e); //$NON-NLS-1$
                        ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_DECRYPTING_DATA);
                        return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_DECRYPTING_DATA);
                    }

                    params = ProtocolInvocationUriParser.getParametersForBatch(xmlBatchDefinition);
                }

                // En caso de comunicacion por servidor intermedio, solicitamos, si corresponde,
                // que se espere activamente hasta el fin de la tarea
                if (!bySocket && params.isActiveWaiting()) {
                	requestWait(params.getStorageServletUrl(), params.getId());
                }

                try {
                    return  ProtocolInvocationLauncherBatch.processBatch(params, bySocket);
                }
                catch(final SocketOperationException e) {
                    LOGGER.severe("Error durante la operacion de firma por lotes: " + e); //$NON-NLS-1$
                    if (e.getErrorCode() == ProtocolInvocationLauncherBatch.getResultCancel()){
                        sendErrorToServer(e.getErrorCode(), params.getStorageServletUrl().toString(), params.getId());
                    }
                    else {
                        sendErrorToServer(
                    		ProtocolInvocationLauncherErrorManager.getErrorMessage(e.getErrorCode()),
                    		params.getStorageServletUrl().toString(),
                    		params.getId()
                		);
                    }
                }
            }
            catch(final ParameterException e) {
                LOGGER.log(Level.SEVERE, "Error en los parametros de firma por lote: " + e, e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showErrorDetail(
                		ProtocolInvocationLauncherErrorManager.ERROR_PARAMS,
                		e.getMessage()
                		);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
            catch(final Exception e) {
                LOGGER.log(Level.SEVERE, "Error en los parametros de firma por lote: " + e, e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
        }
        // Se solicita una operacion de seleccion de certificado
        else if (urlString.startsWith("afirma://selectcert?") || urlString.startsWith("afirma://selectcert/?")) { //$NON-NLS-1$ //$NON-NLS-2$
        	LOGGER.info("Se invoca a la aplicacion para la seleccion de un certificado"); //$NON-NLS-1$
            try {
                final UrlParametersToSelectCert params = ProtocolInvocationUriParser.getParametersToSelectCert(urlString);

                // En caso de comunicacion por servidor intermedio, solicitamos, si corresponde,
                // que se espere activamente hasta el fin de la tarea
                if (!bySocket && params.isActiveWaiting()) {
                	requestWait(params.getStorageServletUrl(), params.getId());
                }

                try {
                    return ProtocolInvocationLauncherSelectCert.processSelectCert(params, bySocket);
                }
                catch (final AOCancelledOperationException e) {
                	return ProtocolInvocationLauncherSelectCert.getResultCancel();
                }
                catch (final SocketOperationException e) {
                    LOGGER.severe("Error durante la operacion de seleccion de certificados: " + e); //$NON-NLS-1$
                   	return ProtocolInvocationLauncherErrorManager.getErrorMessage(e.getErrorCode());
                }
            }
            catch (final ParameterException e) {
                LOGGER.log(Level.SEVERE, "Error en los parametros de seleccion de certificados: " + e, e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showErrorDetail(
                		ProtocolInvocationLauncherErrorManager.ERROR_PARAMS,
                		e.getMessage()
                		);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
            catch (final Exception e) {
                LOGGER.log(Level.SEVERE, "Error en los parametros de seleccion de certificados: " + e, e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
        }
        // Se solicita una operacion de guardado
        else if (urlString.startsWith("afirma://save?") || urlString.startsWith("afirma://save/?")) { //$NON-NLS-1$ //$NON-NLS-2$
            LOGGER.info("Se invoca a la aplicacion para el guardado de datos"); //$NON-NLS-1$

            try {
                UrlParametersToSave params = ProtocolInvocationUriParser.getParametersToSave(urlString);
                LOGGER.info("Cantidad de datos a guardar: " + (params.getData() == null ? 0 : params.getData().length)); //$NON-NLS-1$

                // Si se indica un identificador de fichero, es que la configuracion se tiene que
                // descargar desde el servidor intermedio
                if (params.getFileId() != null) {

                    final byte[] xmlData;
                    try {
                        xmlData = ProtocolInvocationLauncherUtil.getDataFromRetrieveServlet(params);
                    }
                    catch(final InvalidEncryptedDataLengthException e) {
                    	LOGGER.log(Level.SEVERE, "No se pueden recuperar los datos del servidor: " + e, e); //$NON-NLS-1$
                        ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_RECOVERING_DATA);
                        return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_RECOVERING_DATA);
                    }
                    catch(final DecryptionException e) {
                        LOGGER.severe("Error al descifrar: " + e); //$NON-NLS-1$
                        ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_DECRYPTING_DATA);
                        return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_DECRYPTING_DATA);
                    }

                    params = ProtocolInvocationUriParser.getParametersToSave(xmlData);
                }

                // En caso de comunicacion por servidor intermedio, solicitamos, si corresponde,
                // que se espere activamente hasta el fin de la tarea
                if (!bySocket && params.isActiveWaiting()) {
                	requestWait(params.getStorageServletUrl(), params.getId());
                }

                try {
                	return  ProtocolInvocationLauncherSave.processSave(params, bySocket);
                }
                // solo entra en la excepcion en el caso de que haya que devolver errores a traves del servidor intermedio
                catch (final SocketOperationException e) {
                    LOGGER.log(Level.SEVERE, "Error en la operacion de guardado: " + e, e); //$NON-NLS-1$
                    if (e.getErrorCode() == ProtocolInvocationLauncherSign.getResultCancel()){
                        sendErrorToServer(e.getErrorCode(), params.getStorageServletUrl().toString(), params.getId());
                    }
                    else {
                        sendErrorToServer(ProtocolInvocationLauncherErrorManager.getErrorMessage(e.getErrorCode()), params.getStorageServletUrl().toString(), params.getId());
                    }
                }
            }
            catch(final ParameterNeedsUpdatedVersionException e) {
                LOGGER.severe("Se necesita una version mas moderna de AutoFirma para procesar la peticion: " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_OBSOLETE_APP);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_OBSOLETE_APP);
            }
            catch(final ParameterLocalAccessRequestedException e) {
                LOGGER.severe("Se ha pedido un acceso a una direccion local (localhost o 127.0.0.1): " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_LOCAL_ACCESS_BLOCKED);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_LOCAL_ACCESS_BLOCKED);
            }
            catch (final ParameterException e) {
            	LOGGER.log(Level.SEVERE, "Error en los parametros de guardado: " + e, e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showErrorDetail(
                		ProtocolInvocationLauncherErrorManager.ERROR_PARAMS,
                		e.getMessage()
                		);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
            catch (final Exception e) {
            	LOGGER.log(Level.SEVERE, "Error en los parametros de guardado: " + e, e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
        }

        // Se solicita una operacion de firma/multifirma seguida del guardado del resultado
        else if (urlString.startsWith("afirma://signandsave?") || urlString.startsWith("afirma://signandsave/?")) { //$NON-NLS-1$ //$NON-NLS-2$
            LOGGER.info("Se invoca a la aplicacion para la firma/multifirma y el guardado del resultado"); //$NON-NLS-1$

            try {
                UrlParametersToSignAndSave params = ProtocolInvocationUriParser.getParametersToSignAndSave(urlString);
                LOGGER.info("Cantidad de datos a firmar y guardar: " + (params.getData() == null ? 0 : params.getData().length)); //$NON-NLS-1$

                // Si se indica un identificador de fichero, es que la configuracion se tiene que
                // descargar desde el servidor intermedio
                if (params.getFileId() != null) {

                    final byte[] xmlData;
                    try {
                        xmlData = ProtocolInvocationLauncherUtil.getDataFromRetrieveServlet(params);
                    }
                    catch(final InvalidEncryptedDataLengthException e) {
                    	LOGGER.log(Level.SEVERE, "No se pueden recuperar los datos del servidor: " + e, e); //$NON-NLS-1$
                        ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_RECOVERING_DATA);
                        return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_RECOVERING_DATA);
                    }
                    catch(final DecryptionException e) {
                        LOGGER.severe("Error al descifrar: " + e); //$NON-NLS-1$
                        ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_DECRYPTING_DATA);
                        return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_DECRYPTING_DATA);
                    }

                    params = ProtocolInvocationUriParser.getParametersToSignAndSave(xmlData);
                }

                // En caso de comunicacion por servidor intermedio, solicitamos, si corresponde,
                // que se espere activamente hasta el fin de la tarea
                if (!bySocket && params.isActiveWaiting()) {
                	requestWait(params.getStorageServletUrl(), params.getId());
                }

                try {
                	return  ProtocolInvocationLauncherSignAndSave.process(params, bySocket);
                }
                // solo entra en la excepcion en el caso de que haya que devolver errores a traves del servidor intermedio
                catch(final SocketOperationException e) {
                    if (e.getErrorCode() == ProtocolInvocationLauncherSign.getResultCancel()){
                        sendErrorToServer(e.getErrorCode(), params.getStorageServletUrl().toString(), params.getId());
                    }
                    else {
                       sendErrorToServer(ProtocolInvocationLauncherErrorManager.getErrorMessage(e.getErrorCode()), params.getStorageServletUrl().toString(), params.getId());
                    }
                }
            }
            catch(final ParameterNeedsUpdatedVersionException e) {
                LOGGER.severe("Se necesita una version mas moderna de AutoFirma para procesar la peticion: " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_OBSOLETE_APP);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_OBSOLETE_APP);
            }
            catch(final ParameterLocalAccessRequestedException e) {
                LOGGER.severe("Se ha pedido un acceso a una direccion local (localhost o 127.0.0.1): " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_LOCAL_ACCESS_BLOCKED);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_LOCAL_ACCESS_BLOCKED);
            }
            catch (final ParameterException e) {
                LOGGER.log(Level.SEVERE, "Error en los parametros de firma y guardado: " + e, e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showErrorDetail(
                		ProtocolInvocationLauncherErrorManager.ERROR_PARAMS,
                		e.getMessage()
                		);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
            catch (final Exception e) {
                LOGGER.log(Level.SEVERE, "Error en los parametros de firma y guardado: " + e, e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
        }

        // Se solicita una operacion de firma/cofirma/contrafirma
        else if (urlString.startsWith("afirma://sign?")        || urlString.startsWith("afirma://sign/?") || //$NON-NLS-1$ //$NON-NLS-2$
                 urlString.startsWith("afirma://cosign?")      || urlString.startsWith("afirma://cosign/?") || //$NON-NLS-1$ //$NON-NLS-2$
                 urlString.startsWith("afirma://countersign?") || urlString.startsWith("afirma://countersign/?") //$NON-NLS-1$ //$NON-NLS-2$
        ) {
            LOGGER.info("Se invoca a la aplicacion para realizar una operacion de firma/multifirma"); //$NON-NLS-1$

            try {
                UrlParametersToSign params = ProtocolInvocationUriParser.getParametersToSign(urlString);

                // Si se indica un identificador de fichero, es que la configuracion se tiene que
                // descargar desde el servidor intermedio
                if (params.getFileId() != null) {

                    final byte[] xmlData;
                    try {
                        xmlData = ProtocolInvocationLauncherUtil.getDataFromRetrieveServlet(params);
                    }
                    catch(final InvalidEncryptedDataLengthException | IOException e) {
                    	LOGGER.log(Level.SEVERE, "No se pueden recuperar los datos del servidor: " + e, e); //$NON-NLS-1$
                        ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_RECOVERING_DATA);
                        return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_RECOVERING_DATA);
                    }
                    catch(final DecryptionException e) {
                        LOGGER.log(Level.SEVERE, "Error al descifrar" + e, e); //$NON-NLS-1$
                        ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_DECRYPTING_DATA);
                        return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_DECRYPTING_DATA);
                    }
                    params = ProtocolInvocationUriParser.getParametersToSign(xmlData);
                }

                // En caso de comunicacion por servidor intermedio, solicitamos, si corresponde,
                // que se espere activamente hasta el fin de la tarea
                if (!bySocket && params.isActiveWaiting()) {
                	requestWait(params.getStorageServletUrl(), params.getId());
                }

                LOGGER.info("Se inicia la operacion de firma"); //$NON-NLS-1$

                try {
                    return ProtocolInvocationLauncherSign.processSign(params, bySocket);
                }
                // solo entra en la excepcion en el caso de que haya que devolver errores a traves del servidor intermedio
                catch(final SocketOperationException e) {
                    LOGGER.severe("La operacion indicada no esta soportada: " + e); //$NON-NLS-1$
                    if (e.getErrorCode() == ProtocolInvocationLauncherSign.getResultCancel()){
                        sendErrorToServer(e.getErrorCode(), params.getStorageServletUrl().toString(), params.getId());
                    }
                    else {
                       sendErrorToServer(ProtocolInvocationLauncherErrorManager.getErrorMessage(e.getErrorCode()), params.getStorageServletUrl().toString(), params.getId());
                    }
                }
            }
            catch(final ParameterNeedsUpdatedVersionException e) {
                LOGGER.severe("Se necesita una version mas moderna de AutoFirma para procesar la peticion: " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_OBSOLETE_APP);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_OBSOLETE_APP);
            }
            catch(final ParameterLocalAccessRequestedException e) {
                LOGGER.severe("Se ha pedido un acceso a una direccion local (localhost o 127.0.0.1): " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_LOCAL_ACCESS_BLOCKED);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_LOCAL_ACCESS_BLOCKED);
            }
            catch (final ParameterException e) {
            	LOGGER.log(Level.SEVERE, "Error en los parametros de firma: " + e, e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showErrorDetail(
                		ProtocolInvocationLauncherErrorManager.ERROR_PARAMS,
                		e.getMessage()
                		);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
            catch (final Exception e) {
            	LOGGER.log(Level.SEVERE, "Error en los parametros de firma: " + e, e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
        }

        // Se solicita una operacion de carga de ficheros
        else if (urlString.startsWith("afirma://load?")) { //$NON-NLS-1$
            LOGGER.info("Se invoca a la aplicacion para realizar una operacion de carga de uno o varios ficheros"); //$NON-NLS-1$

            try {
                UrlParametersToLoad params = ProtocolInvocationUriParser.getParametersToLoad(urlString);

                // Si se indica un identificador de fichero, es que la configuracion se tiene que
                // descargar desde el servidor intermedio
                if (params.getFileId() != null) {

                    final byte[] xmlData;
                    try {
                        xmlData = ProtocolInvocationLauncherUtil.getDataFromRetrieveServlet(params);
                    }
                    catch(final InvalidEncryptedDataLengthException | IOException e) {
                    	LOGGER.log(Level.SEVERE, "No se pueden recuperar los datos del servidor: " + e, e); //$NON-NLS-1$
                        ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_RECOVERING_DATA);
                        return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_RECOVERING_DATA);
                    }
                    catch(final DecryptionException e) {
                        LOGGER.severe("Error al descifrar" + e); //$NON-NLS-1$
                        ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_DECRYPTING_DATA);
                        return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_DECRYPTING_DATA);
                    }
                    params = ProtocolInvocationUriParser.getParametersToLoad(xmlData);
                }

                // En caso de comunicacion por servidor intermedio, solicitamos, si corresponde,
                // que se espere activamente hasta el fin de la tarea
                if (!bySocket && params.isActiveWaiting()) {
                	requestWait(params.getStorageServletUrl(), params.getId());
                }

                LOGGER.info("Se inicia la operacion de carga"); //$NON-NLS-1$

                try {
                    return ProtocolInvocationLauncherLoad.processLoad(params, bySocket);
                }
                // solo entra en la excepcion en el caso de que haya que devolver errores a traves del servidor intermedio
                catch(final SocketOperationException e) {
                    LOGGER.severe("La operacion indicada no esta soportada: " + e); //$NON-NLS-1$
                    if (e.getErrorCode() == ProtocolInvocationLauncherSign.getResultCancel()){
                        sendErrorToServer(e.getErrorCode(), params.getStorageServletUrl().toString(), params.getId());
                    }
                    else {
                       sendErrorToServer(ProtocolInvocationLauncherErrorManager.getErrorMessage(e.getErrorCode()), params.getStorageServletUrl().toString(), params.getId());
                    }
                }
            }
            catch(final ParameterNeedsUpdatedVersionException e) {
                LOGGER.severe("Se necesita una version mas moderna de AutoFirma para procesar la peticion: " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_OBSOLETE_APP);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_OBSOLETE_APP);
            }
            catch(final ParameterLocalAccessRequestedException e) {
                LOGGER.severe("Se ha pedido un acceso a una direccion local (localhost o 127.0.0.1): " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_LOCAL_ACCESS_BLOCKED);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_LOCAL_ACCESS_BLOCKED);
            }
            catch (final ParameterException e) {
                LOGGER.severe("Error en los parametros de carga: " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showErrorDetail(
                		ProtocolInvocationLauncherErrorManager.ERROR_PARAMS,
                		e.getMessage()
                		);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
            catch (final Exception e) {
                LOGGER.severe("Error en los parametros de carga: " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
        }
        // Se solicita una operacion de recuperacion de logs
        else if (urlString.startsWith("afirma://getLog?")) { //$NON-NLS-1$
            LOGGER.info("Se invoca a la aplicacion para realizar una operacion de obtencion del log actual de la aplicacion"); //$NON-NLS-1$

            try {
                final UrlParametersToGetCurrentLog params = ProtocolInvocationUriParser.getParametersToGetCurrentLog(urlString);

                // En caso de comunicacion por servidor intermedio, solicitamos, si corresponde,
                // que se espere activamente hasta el fin de la tarea
                if (!bySocket && params.isActiveWaiting()) {
                	requestWait(params.getStorageServletUrl(), params.getId());
                }

                LOGGER.info("Se inicia la operacion de obtencion de log actual"); //$NON-NLS-1$

                return ProtocolInvocationLauncherGetCurrentLog.processGetCurrentLog(params, bySocket);

            }
            catch(final ParameterNeedsUpdatedVersionException e) {
                LOGGER.severe("Se necesita una version mas moderna de AutoFirma para procesar la peticion: " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_OBSOLETE_APP);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_OBSOLETE_APP);
            }
            catch(final ParameterLocalAccessRequestedException e) {
                LOGGER.severe("Se ha pedido un acceso a una direccion local (localhost o 127.0.0.1): " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_LOCAL_ACCESS_BLOCKED);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_LOCAL_ACCESS_BLOCKED);
            }
            catch (final ParameterException e) {
                LOGGER.severe("Error en los parametros de carga: " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showErrorDetail(
                		ProtocolInvocationLauncherErrorManager.ERROR_PARAMS,
                		e.getMessage()
                		);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
            catch (final Exception e) {
                LOGGER.severe("Error en los parametros de carga: " + e); //$NON-NLS-1$
                ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
                return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_PARAMS);
            }
        }
        else {
        	LOGGER.severe(
    			"La operacion indicada en la URL no esta soportada: " +  //$NON-NLS-1$
    					urlString.substring(0, Math.min(30, urlString.length())) + "..." //$NON-NLS-1$
			);
            ProtocolInvocationLauncherErrorManager.showError(ProtocolInvocationLauncherErrorManager.ERROR_UNSUPPORTED_OPERATION);
            return ProtocolInvocationLauncherErrorManager.getErrorMessage(ProtocolInvocationLauncherErrorManager.ERROR_UNSUPPORTED_OPERATION);
        }
        throw new IllegalStateException("Estado no permitido"); //$NON-NLS-1$
    }

    /**
     * Inicia el proceso de solicitud de espera activa a trav&eacute;s del servidor intermedio.
     * @param storageServletUrl URL del servicio de guardado en el servidor intermedio.
     * @param id Identificador de la transacci&oacute;n para la que se le solicita la espera.
     */
    private static void requestWait(final URL storageServletUrl, final String id) {
    	activeWaitingThread = new ActiveWaitingThread(storageServletUrl.toString(), id);
    	activeWaitingThread.start();
	}

	/** Env&iacute;a un mensaje de error al servidor intermedio e interrumpe la espera
	 * declarada en este servidor.
     * @param data Cadena de texto.
     * @param serviceUrl URL del servicio de env&iacute;o de datos.
     * @param id Identificador del mensaje en el servidor. */
	private static void sendErrorToServer(final String data, final String serviceUrl, final String id) {
		synchronized (IntermediateServerUtil.getUniqueSemaphoreInstance()) {
			final Thread waitingThread = getActiveWaitingThread();
			if (waitingThread != null) {
				waitingThread.interrupt();
			}
			try {
				IntermediateServerUtil.sendData(data, serviceUrl, id);
			}
			catch (final IOException e) {
				LOGGER.log(Level.SEVERE, "Error al enviar los datos del error al servidor intermedio: " + e, e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Obtiene el hilo encargado de solicitar a trav&eacute;s del servidor intermedio que se
	 * realice una espera activa del resultado de la operaci&oacute;n actual.
	 * @return Hilo que solicita reiteradamente la espera o {@code null} si no se inici&oacute;
	 * la espera activa.
	 */
	public static Thread getActiveWaitingThread() {
		return activeWaitingThread;
	}
}
