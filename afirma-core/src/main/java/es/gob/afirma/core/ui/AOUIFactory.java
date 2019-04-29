/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.core.ui;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import es.gob.afirma.core.misc.Platform;

/** Factor&iacute;a de elementos de interfaz gr&aacute;fica.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class AOUIFactory {

    private AOUIFactory() {
        // No permitimos la instanciacion
    }

    /** JOptionPane.PLAIN_MESSAGE. */
    public static final int PLAIN_MESSAGE;

    /** JOptionPane.YES_NO_OPTION. */
    public static final int YES_NO_OPTION;

    /** JOptionPane.WARNING_MESSAGE. */
    public static final int WARNING_MESSAGE;

    /** JOptionPane.YES_OPTION. */
    public static final int YES_OPTION;

    /** JOptionPane.NO_OPTION. */
    public static final int NO_OPTION;

    /** JOptionPane.OK_CANCEL_OPTION. */
    public static final int OK_CANCEL_OPTION;

    /** JOptionPane.OK_OPTION. */
    public static final int OK_OPTION;

    /** JOptionPane.INFORMATION_MESSAGE. */
    public static final int INFORMATION_MESSAGE;

    /** JOptionPane.QUESTION_MESSAGE. */
    public static final int QUESTION_MESSAGE;

    /** JOptionPane.ERROR_MESSAGE. */
    public static final int ERROR_MESSAGE;

    private static AOUIManager uiManager;

    static {
        try {
            if (Platform.OS.ANDROID.equals(Platform.getOS())) {
                throw new UnsupportedOperationException("No se soporta GUI en Android"); //$NON-NLS-1$
            }
        	final String uiManagerClassName;
            if (Platform.OS.MACOSX.equals(Platform.getOS())) {
            	uiManagerClassName = "es.gob.afirma.ui.core.jse.AWTUIManager"; //$NON-NLS-1$
            }
            else {
            	uiManagerClassName = "es.gob.afirma.ui.core.jse.JSEUIManager"; //$NON-NLS-1$
            }
			try {
				uiManager = (AOUIManager) Class.forName(uiManagerClassName).getDeclaredConstructor().newInstance();
			}
			catch(final Exception e) {
				Logger.getLogger("es.gob.afirma").severe( //$NON-NLS-1$
					"No se ha podido instanciar la implementacion de gestor de interfaces graficas: " + e //$NON-NLS-1$
				);
			}
            PLAIN_MESSAGE = uiManager.getPlainMessageCode();
            YES_NO_OPTION = uiManager.getYesNoOptionCode();
            WARNING_MESSAGE = uiManager.getWarningMessageCode();
            YES_OPTION = uiManager.getYesOptionCode();
            NO_OPTION = uiManager.getNoOptionCode();
            OK_CANCEL_OPTION = uiManager.getOkCancelOptionCode();
            OK_OPTION = uiManager.getOkOptionCode();
            INFORMATION_MESSAGE = uiManager.getInformationMessageCode();
            QUESTION_MESSAGE = uiManager.getQuestionMessageCode();
            ERROR_MESSAGE = uiManager.getErrorMessageCode();
        }
        catch(final Exception e) {
            throw new UnsupportedOperationException("No se ha podido instanciar el gestor de interfaces graficas: " + e, e); //$NON-NLS-1$
        }
    }

    /** Establece el manejador de interfaces que gestionar&aacute; los di&aacute;logos
     * gr&aacute;ficos que utilizar&aacute; @firma para mostrar o solicitar informaci&oacute;n.
     * @param manager Manejador de interfaces. */
    public static void setUIManager(final AOUIManager manager) {
    	if (manager != null) {
    		uiManager = manager;
    	}
    }

    /** Pregunta al usuario por una contrase&ntilde;a.
     * @param text Texto que se muestra en el di&aacute;logo para pedir la
     *             contrase&ntilde;a
     * @param c Componente padre (para la modalidad)
     * @return Contrase&ntilde;a introducida por el usuario
     * @throws es.gob.afirma.core.AOCancelledOperationException Cuando el usuario cancela el proceso
     *                                                          de solicitud de contrase&ntilde;a */
    public static char[] getPassword(final String text, final Object c) {
        return uiManager.getPassword(text, c);
    }

    /** Pregunta al usuario por una contrase&ntilde;a. S&oacute;lo admite los caracteres
     * incluidos en el par&aacute;metro {@code charset} y, en caso de insertar un
     * car&aacute;cter no permitido, se emitir&aacute;a una advertencia no bloqueante
     * (sonido, vibraci&oacute;n...) si el par&aacute;metro {@code beep} est&aacute;
     * activado.
     * @param text Texto que se muestra en el di&aacute;logo para pedir la
     *             contrase&ntilde;a
     * @param icon Objeto de tipo {@code javax.swing.Icon} con el icono del di&aacute;logo
     * 		  o {@code null} para no mostrar icono.
     * @param charset Cadena con los caracteres permitidos para la contrase&ntilde;a.
     * @param beep Indica si se debe dar una se&ntilde;al al usuario al intentar insertar
     *             un caracter no v&aacute;lido para la contrase&ntilde;a.
     * @param c Componente padre (para la modalidad)
     * @return Contrase&ntilde;a introducida por el usuario
     * @throws es.gob.afirma.core.AOCancelledOperationException Cuando el usuario cancela el
     *                                                          proceso de solicitud de contrase&ntilde;a */
    public static char[] getPassword(final String text, final Object icon, final String charset, final boolean beep, final Object c) {
        return uiManager.getPassword(text, icon, charset, beep, c);
    }

    /** Pregunta al usuario dos veces una misma contrase&ntilde;a (deben coincidir).
     * Es el procedimiento normal cuando se pide el establecimiento de una nueva contrase&ntilde;a, para evitar errores.
     * @param text Texto con el que se solicitar&aacute; la entrada de texto al
     *             usuario (<i>prompt</i>).
     * @param text2 Texto con el que se solicitar&aacute; al usuario que repita la contrase&ntilde;a.
     * @param imageIcon Objeto de tipo {@code javax.swing.Icon} con el icono del di&aacute;logo o
     * 			   {@code null} para no mostrar icono.
     * @param charSet Juego de caracteres aceptados para la contrase&ntilde;a.
     * @param beep <code>true</code> si se desea un sonido de advertencia al
     *             introducir un caracter no v&aacute;lido, <code>false</code> en
     *             caso contrario.
     * @param c Componente padre (para la modalidad).
     * @return Array de caracteres del texto introducido como contrase&ntilde;a.
     * @throws es.gob.afirma.core.AOCancelledOperationException Cuando el usuario cancela o cierra el di&aacute;logo. */
	public static char[] getDoublePassword(final String text,
			                               final String text2,
			                               final Object imageIcon,
			                               final String charSet,
			                               final boolean beep,
			                               final Object c) {
		return uiManager.getDoublePassword(text, text2, imageIcon, charSet, beep, c);
	}

    /** JOptionPane.showConfirmDialog().
     * @param parentComponent Componente padre (se descarta si no es del tipo <code>java.awt.Component</code> en la implementaci&oacute;n Swing
     * @param message Mensaje
     * @param title Titulo del cuadro de di&aacute;logo
     * @param optionType Tipo de opciones a confirmar
     * @param messageType Tipo de mensaje
     * @return Opci&oacute;n seleccionada */
    public static int showConfirmDialog(final Object parentComponent,
    		                            final Object message,
    		                            final String title,
    		                            final int optionType,
    		                            final int messageType) {
        return uiManager.showConfirmDialog(parentComponent, message, title, optionType, messageType);
    }

    /** JOptionPane.showMessageDialog().
     * @param parentComponent Componente padre (se descarta si no es del tipo <code>java.awt.Component</code> en la implementaci&oacute;n Swing
     * @param message Mensaje
     * @param title Titulo del cuadro de di&aacute;logo
     * @param messageType Tipo de mensaje */
    public static void showMessageDialog(final Object parentComponent,
    		                            final Object message,
    		                            final String title,
    		                            final int messageType) {
        uiManager.showMessageDialog(parentComponent, message, title, messageType);
    }

    /** JOptionPane.showMessageDialog().
     * @param parentComponent Componente padre (se descarta si no es del tipo <code>java.awt.Component</code> en la implementaci&oacute;n Swing
     * @param message Mensaje.
     * @param title T&iacute;tulo del cuadro de di&aacute;logo.
     * @param messageType Tipo de mensaje.
     * @param icon Icono de la ventana de di&aacute;logo. */
    public static void showMessageDialog(final Object parentComponent,
    		                            final Object message,
    		                            final String title,
    		                            final int messageType,
    		                            final Object icon) {
        uiManager.showMessageDialog(parentComponent, message, title, messageType, icon);
    }

    /** Muestra un di&aacute;logo de error de forma modal. Difiere del normal mostrado con <code>JOptionPane</code>
     * en que, siguiendo la gu&iacute;a de estilo de interfaces de Microsoft, el bot&oacute;n no es "OK", sino
     * "Cerrar". El comportamiento por lo dem&aacute;s es igual, incluyendo los par&aacute;metros, a
     * <code>JOptionPane</code>.
     * @param parent Componente padre para la modalidad.
     * @param message Mensaje de error.
     * @param title Titulo de la ventana de error.
     * @param messageType Tipo de mensaje. */
    public static void showErrorMessage(final Object parent,
    		                            final Object message,
    		                            final String title,
    		                            final int messageType) {
    	uiManager.showErrorMessage(parent, message, title, messageType);
    }

    /** Di&aacute;logo de solicitud de un valor entre una lista de opciones (equivalente a <code>JOptionPane.showInputDialog()</code>).
     * @param parentComponent Componente padre (se descarta si no es del tipo <code>java.awt.Component</code> en la implementaci&oacute;n Swing
     * @param message Mensaje
     * @param title T&iacute;tulo del cuadro de di&aacute;logo
     * @param messageType Tipo de mensaje
     * @param icon Icono a mostrar en el di&aacute;logo
     * @param selectionValues Valores posibles para seleccionar
     * @param initialSelectionValue Valor seleccionado por defecto
     * @return Valor seleccionado */
    public static Object showInputDialog(final Object parentComponent, final Object message, final String title, final int messageType, final Object icon, final Object[] selectionValues, final Object initialSelectionValue) {
        return uiManager.showInputDialog(parentComponent, message, title, messageType, icon, selectionValues, initialSelectionValue);
    }

    /** Di&aacute;logo de selecci&oacute;n de certificados.
     * @param parentComponent Componente padre (se descarta si no es del tipo <code>java.awt.Component</code> en la implementaci&oacute;n Swing
     * @param dialogManager Gestor del di&aacute;logo
     * @return Alias del certificado seleccionado o {@code null} si no se seleccion&oacute; ninguno. */
    public static String showCertificateSelectionDialog(final Object parentComponent, final KeyStoreDialogManager dialogManager) {
        return uiManager.showCertificateSelectionDialog(parentComponent, dialogManager);
    }

    /** Pide al usuario que seleccione un fichero.
     * @param dialogTitle T&iacute;tulo de la ventana de di&aacute;logo
     * @param currentDir Directorio inicial del di&aacute;logo
     * @param filename Nombre del fichero a localizar
     * @param extensions Extensiones predeterminadas para el fichero
     * @param description Descripci&oacute;n del tipo de fichero correspondiente con las extensiones
     * @param selectDirectory {@code true} para permitir la selecci&oacute;n de directorios, {@code true}
     * 					  para selecci&oacute;n de ficheros. En caso de directorios el par&aacute;metro
     * 					  {@code multiselect} se ignorar&aacute;.
     * @param multiSelect {@code true} para permitir selecci&oacute;n m&uacute;ltiple, {@code false}
     *                    para selecci&oacute;n de un &uacute;nico fichero
     * @param icon Icono del di&aacute;logo de selecci&oacute;n.
     *             Si se especifica <code>null</code> y se indica un <code>Frame</code> como padre se
     *             hereda el icono de este.
     * @param parentComponent Componente padre (para la modalidad)
     * @return Nombre de fichero (con ruta) seleccionado por el usuario */
    public static File[] getLoadFiles(final String dialogTitle,
    									   final String currentDir,
    									   final String filename,
    		                               final String[] extensions,
    		                               final String description,
    		                               final boolean selectDirectory,
    		                               final boolean multiSelect,
    		                               final Object icon,
    		                               final Object parentComponent) {
        return uiManager.getLoadFiles(
    		dialogTitle,
    		currentDir,
    		filename,
    		extensions,
    		description,
    		selectDirectory,
    		multiSelect,
    		icon,
    		parentComponent
		);
    }

    /** Pregunta al usuario por la localizaci&oacute;n en la que se desean guardar
     * los datos y, si se proporcionan, los guarda en la misma.
     * Si ocurre un error durante el guardado, se vuelve a preguntar al usuario por una
     * localizaci&oacute;n,
     * Si el usuario cancela el di&aacute;logo, se lanza <code>AOCancelledOperationException</code>.
     * @param data Datos que se desean almacenar.
     * @param dialogTitle T&iacute;tulo del di&aacute;logo de guardado.
     * @param currentDir Directorio inicial del di&aacute;logo.
     * @param selectedFile Nombre por defecto del fichero.
     * @param filters Filtros del tipo de fichero a guardar.
     * @param parent Componente padre (para la modalidad).
     * @return Fichero en el que se almacenan los datos.
     * @throws IOException Si no se puede guardar el fichero*/
    public static File getSaveDataToFile(final byte[] data,
    									 final String dialogTitle,
    									 final String currentDir,
    		                             final String selectedFile,
    		                             final List<GenericFileFilter> filters,
    		                             final Object parent) throws IOException {
        return uiManager.saveDataToFile(
    		data,
    		dialogTitle,
    		currentDir,
    		selectedFile,
    		filters,
    		parent
		);
    }
}
