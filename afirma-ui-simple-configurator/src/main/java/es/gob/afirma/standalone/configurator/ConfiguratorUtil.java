/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.standalone.configurator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class ConfiguratorUtil {

	private static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	static final String CERT_ALIAS = "SocketAutoFirma"; //$NON-NLS-1$

	private ConfiguratorUtil() {
		// No instanciable
	}

	/** Guarda datos en disco.
	 * @param data Datos a guardar.
	 * @param outFile Fichero de salida.
	 * @throws IOException Cuando ocurre un error durante el guardado. */
	static void installFile(final byte[] data, final File outFile) throws IOException {
		OutputStream os;
		if (AutoFirmaConfiguratiorJNLPUtils.isJNLPDeployment()) {
			os = AutoFirmaConfiguratiorJNLPUtils.selectFileToWrite(outFile);
		}
		else {
			os = new FileOutputStream(outFile);
		}
		try (final BufferedOutputStream bos = new BufferedOutputStream(os)) {
			bos.write(data);
		}
	}

	/** Descomprime un fichero ZIP de recurso al disco.
	 * @param resource Ruta del recurso ZIP.
	 * @param outDir Directorio local en el que descomprimir.
	 * @throws IOException Cuando ocurre un error al descomprimir.
	 **/
	static void uncompressResource(final String resource, final File outDir) throws IOException {
		int n;
		final boolean jnlpDeploy = AutoFirmaConfiguratiorJNLPUtils.isJNLPDeployment();
		final byte[] buffer = new byte[1024];
		try (final ZipInputStream zipIs = new ZipInputStream(ConfiguratorUtil.class.getResourceAsStream(resource));) {
			ZipEntry entry;
			while ((entry = zipIs.getNextEntry()) != null) {
				final File outFile = new File(outDir, entry.getName());

				// Si es un directorio y no existe en local, lo creamos
				if (entry.isDirectory()) {
					if (!outFile.exists()) {
						outFile.mkdirs();
					}
				}
				else {

					// Si no existe en local el directorio padre del fichero, lo creamos
					if (!outFile.getParentFile().exists()) {
						outFile.getParentFile().mkdirs();
					}

					try (final OutputStream outFis = jnlpDeploy ?
							AutoFirmaConfiguratiorJNLPUtils.selectFileToWrite(outFile) :
								new FileOutputStream(outFile);) {
						while ((n = zipIs.read(buffer)) > 0) {
							outFis.write(buffer, 0, n);
						}
						outFis.flush();
					}
				}
				zipIs.closeEntry();
			}

		}
	}

	/** Elimina un directorio con todo su contenido.
	 * @param targetDir Directorio a eliminar. */
	static void deleteDir(final File targetDir) {
		try {
			Files.walkFileTree(
				targetDir.toPath(),
				new SimpleFileVisitor<Path>() {
			         @Override
			         public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
			             Files.delete(file);
			             return FileVisitResult.CONTINUE;
			         }
			         @Override
			         public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
			             if (e != null) {
			            	 throw e;
			             }
			             Files.delete(dir);
		                 return FileVisitResult.CONTINUE;
			         }
			   }
			);
		}
		catch (final Exception e) {
			LOGGER.warning("No se pudo borrar el directorio '" + targetDir.getAbsolutePath() + "': " + e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}


	/**
	 * Guarda los comandos especificados en un buffer.
	 * @param comands Listado de comandos que almacenar.
	 * @param buffer Buffer de datos en el que se almacen el script.
	 */
	static void printScript(final String[] comands, final StringBuilder buffer) {
		for (final String s : comands) {
			buffer.append(s);
			buffer.append(' ');
		}
		buffer.append("\n"); //$NON-NLS-1$
	}

	/**
	 * Script un script en un fichero en disco. Si el fichero ya existiese, se agregarian
	 * las nuevas sentencias.
	 * @param buffer Buffer con el texto del script.
	 * @param outFile Fichero en el que guardar el script.
	 * @throws IOException Cuando el fichero no se puede crear o escribir.
	 */
	static void writeScript(final StringBuilder buffer, final File outFile) throws IOException {
		if (AutoFirmaConfiguratiorJNLPUtils.isJNLPDeployment()) {
			try (final OutputStream os = AutoFirmaConfiguratiorJNLPUtils.selectFileToWrite(outFile, true);) {
				os.write(buffer.toString().getBytes());
			}
		}
		else {
			try (final FileOutputStream os = new FileOutputStream(outFile, true);) {
				os.write(buffer.toString().getBytes());
			}
		}
	}

	/** Recupera el directorio en el que se encuentra la aplicaci&oacute;n actual.
	 * @return Directorio de ejecuci&oacute;n. */
	static File getApplicationDirectory() {
		try {
			return new File(ConfiguratorUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
		}
		catch (final Exception e) {
			LOGGER.warning("No se pudo localizar el directorio del fichero en ejecucion: " + e); //$NON-NLS-1$
		}
		return null;
	}
}
