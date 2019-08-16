/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.signers.batch;

import es.gob.afirma.core.AOInvalidFormatException;
import es.gob.afirma.core.signers.AOSignConstants;
import es.gob.afirma.triphase.signer.processors.CAdESASiCSTriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.CAdESTriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.FacturaETriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.PAdESTriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.Pkcs1TriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.TriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.XAdESASiCSTriPhasePreProcessor;
import es.gob.afirma.triphase.signer.processors.XAdESTriPhasePreProcessor;

/** Constantes para la definici&oacute;n de una firma independiente.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class SingleSignConstants {

	/** Tipo de operaci&oacute;n de firma. */
	public enum SignSubOperation {

		/** Firma. */
		SIGN("sign"), //$NON-NLS-1$

		/** Cofirma. */
		COSIGN("cosign"), //$NON-NLS-1$

		/** Contrafirma. */
		COUNTERSIGN("countersign"); //$NON-NLS-1$

		private final String name;

		private SignSubOperation(final String n) {
			this.name = n;
		}

		@Override
		public String toString() {
			return this.name;
		}

		/** Obtiene el tipo de operaci&oacute;n de firma a partir de su nombre.
		 * @param name Nombre del tipo de operaci&oacute;n de firma.
		 * @return Tipo de operaci&oacute;n de firma. */
		public static SignSubOperation getSubOperation(final String name) {
			if (SIGN.toString().equalsIgnoreCase(name)) {
				return SIGN;
			}
			if (COSIGN.toString().equalsIgnoreCase(name)) {
				return COSIGN;
			}
			if (COUNTERSIGN.toString().equalsIgnoreCase(name)) {
				return COUNTERSIGN;
			}
			throw new IllegalArgumentException(
				"Tipo de operacion (suboperation) de firma no soportado: " + name //$NON-NLS-1$
			);
		}
	}

	/** Formato de firma. */
	public enum SignFormat {

		/** CAdES. */
		CADES(AOSignConstants.SIGN_FORMAT_CADES),

		/** CAdES ASiC. */
		CADES_ASIC(AOSignConstants.SIGN_FORMAT_CADES_ASIC_S),

		/** XAdES. */
		XADES(AOSignConstants.SIGN_FORMAT_XADES),

		/** XAdES ASiC. */
		XADES_ASIC(AOSignConstants.SIGN_FORMAT_XADES_ASIC_S),

		/** PAdES. */
		PADES(AOSignConstants.SIGN_FORMAT_PADES),

		/** FacturaE. */
		FACTURAE(AOSignConstants.SIGN_FORMAT_FACTURAE),

		/** PKCS#1. */
		PKCS1(AOSignConstants.SIGN_FORMAT_PKCS1);

		private final String name;

		private SignFormat(final String n) {
			this.name = n;
		}

		@Override
		public String toString() {
			return this.name;
		}

		/** Obtiene el formato de firma a partir de su nombre.
		 * @param name Nombre del formato de firma.
		 * @return Formato firma. */
		public static SignFormat getFormat(final String name) {
			if (name != null) {
				if (CADES.toString().equalsIgnoreCase(name.trim())) {
					return CADES;
				}
				if (XADES.toString().equalsIgnoreCase(name.trim())) {
					return XADES;
				}
				if (PADES.toString().equalsIgnoreCase(name.trim())) {
					return PADES;
				}
				if (FACTURAE.toString().equalsIgnoreCase(name.trim())) {
					return FACTURAE;
				}
			}
			throw new IllegalArgumentException(
				"Tipo de formato de firma no soportado: " + name //$NON-NLS-1$
			);
		}
	}

	/** Algoritmo de firma. */
	public enum SignAlgorithm {

		/** SHA1withRSA. */
		SHA1WITHRSA(AOSignConstants.SIGN_ALGORITHM_SHA1WITHRSA),

		/** SHA256withRSA. */
		SHA256WITHRSA(AOSignConstants.SIGN_ALGORITHM_SHA256WITHRSA),

		/** SHA284withRSA. */
		SHA384WITHRSA(AOSignConstants.SIGN_ALGORITHM_SHA384WITHRSA),

		/** SHA512withRSA. */
		SHA512WITHRSA(AOSignConstants.SIGN_ALGORITHM_SHA512WITHRSA);

		private final String name;

		private SignAlgorithm(final String n) {
			this.name = n;
		}

		@Override
		public String toString() {
			return this.name;
		}

		/** Obtiene el algoritmo de firma a partir de su nombre.
		 * @param name Nombre del algoritmo de firma.
		 * @return Algoritmo firma. */
		public static SignAlgorithm getAlgorithm(final String name) {
			if (SHA1WITHRSA.toString().equalsIgnoreCase(name)) {
				return SHA1WITHRSA;
			}
			if (SHA256WITHRSA.toString().equalsIgnoreCase(name)) {
				return SHA256WITHRSA;
			}
			if (SHA384WITHRSA.toString().equalsIgnoreCase(name)) {
				return SHA384WITHRSA;
			}
			if (SHA512WITHRSA.toString().equalsIgnoreCase(name)) {
				return SHA512WITHRSA;
			}
			throw new IllegalArgumentException(
				"Tipo de algoritmo de firma no soportado: " + name //$NON-NLS-1$
			);
		}
	}

	static TriPhasePreProcessor getTriPhasePreProcessor(final SingleSign sSign) throws AOInvalidFormatException {
		if (sSign == null) {
			throw new IllegalArgumentException("La firma no puede ser nula"); //$NON-NLS-1$
		}
		switch(sSign.getSignFormat()) {
			case PADES:
				return new PAdESTriPhasePreProcessor();
			case CADES:
				return new CAdESTriPhasePreProcessor();
			case CADES_ASIC:
				return new CAdESASiCSTriPhasePreProcessor();
			case XADES:
				return new XAdESTriPhasePreProcessor(TriConfigManager.needInstallXmlDSig());
			case XADES_ASIC:
				return new XAdESASiCSTriPhasePreProcessor(TriConfigManager.needInstallXmlDSig());
			case FACTURAE:
				return new FacturaETriPhasePreProcessor(TriConfigManager.needInstallXmlDSig());
			case PKCS1:
				return new Pkcs1TriPhasePreProcessor();
			default:
				throw new AOInvalidFormatException("Formato de firma no soportado: " + sSign.getSignFormat()); //$NON-NLS-1$
		}
	}

}
