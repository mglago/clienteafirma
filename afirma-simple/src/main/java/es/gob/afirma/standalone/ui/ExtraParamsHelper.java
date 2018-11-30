/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.standalone.ui;

import java.util.Properties;

import es.gob.afirma.core.signers.AOSigner;
import es.gob.afirma.signers.pades.AOPDFSigner;
import es.gob.afirma.signers.xades.AOFacturaESigner;
import es.gob.afirma.signers.xades.AOXAdESSigner;
import es.gob.afirma.standalone.ui.preferences.PreferencesManager;

final class ExtraParamsHelper {

	private ExtraParamsHelper() {
		// No permitimos la instanciacion
	}

	final static Properties loadExtraParamsForSigner(final AOSigner signer) {

		final Properties p;
		if (signer instanceof AOFacturaESigner) {
        	p = loadFacturaEExtraParams();
        }
		else if (signer instanceof AOXAdESSigner) {
        	p = loadXAdESExtraParams();
        }
        else if (signer instanceof AOPDFSigner) {
        	p = loadPAdESExtraParams();
        }
        else {
        	p = loadCAdESExtraParams();
        }

		return p;
	}

	/** Obtiene la configuraci&oacute;n para las firmas Factura-E.
	 * @return Propiedades para la configuraci&oacute;n de las firmas Factura-E. */
	private static Properties loadFacturaEExtraParams() {
		final Properties p = new Properties();

		// Metadatos sobre la "produccion" de la firma de la factura
		final String signatureCity = PreferencesManager.get(PreferencesManager.PREFERENCE_FACTURAE_SIGNATURE_PRODUCTION_CITY);
        if (signatureCity != null && !signatureCity.trim().isEmpty()) {
        	p.put("signatureProductionCity", signatureCity); //$NON-NLS-1$
        }
        final String signatureProvince = PreferencesManager.get(PreferencesManager.PREFERENCE_FACTURAE_SIGNATURE_PRODUCTION_PROVINCE);
        if (signatureProvince != null && !signatureProvince.trim().isEmpty()) {
        	p.put("signatureProductionProvince", signatureProvince); //$NON-NLS-1$
        }
        final String signaturePC = PreferencesManager.get(PreferencesManager.PREFERENCE_FACTURAE_SIGNATURE_PRODUCTION_POSTAL_CODE);
        if (signaturePC != null && !signaturePC.trim().isEmpty()) {
        	p.put("signatureProductionPostalCode", signaturePC); //$NON-NLS-1$
        }
        final String signatureCountry = PreferencesManager.get(PreferencesManager.PREFERENCE_FACTURAE_SIGNATURE_PRODUCTION_COUNTRY);
        if (signatureCountry != null && !signatureCountry.trim().isEmpty()) {
        	p.put("signatureProductionCountry", signatureCountry); //$NON-NLS-1$
        }

        // Papel del firmante de la factura, es un campo acotado
        final String signerRole = PreferencesManager.get(PreferencesManager.PREFERENCE_FACTURAE_SIGNER_ROLE);
        if (signerRole != null && !signerRole.trim().isEmpty()) {
        	p.put("signerClaimedRoles", signerRole); //$NON-NLS-1$
        }

		return p;
	}

	/** Obtiene la configuraci&oacute;n para las firmas XAdES.
	 * @return Propiedades para la configuraci&oacute;n de las firmas XAdES. */
	private static Properties loadXAdESExtraParams() {

		final Properties p = new Properties();
        p.put("ignoreStyleSheets", "false"); //$NON-NLS-1$ //$NON-NLS-2$

        // Preferencias de politica de firma
        final String policyId = PreferencesManager.get(PreferencesManager.PREFERENCE_XADES_POLICY_IDENTIFIER);

        if (policyId != null && !policyId.trim().isEmpty()) {
        	p.put("policyIdentifier", policyId); //$NON-NLS-1$

        	final String policyIdHash = PreferencesManager.get(PreferencesManager.PREFERENCE_XADES_POLICY_HASH);
        	if (policyIdHash != null && !policyIdHash.trim().isEmpty()) {
        		p.put("policyIdentifierHash", policyIdHash); //$NON-NLS-1$
        	}
        	final String policyHashAlgorithm = PreferencesManager.get(PreferencesManager.PREFERENCE_XADES_POLICY_HASH_ALGORITHM);
        	if (policyHashAlgorithm != null && !policyHashAlgorithm.trim().isEmpty()) {
        		p.put("policyIdentifierHashAlgorithm", policyHashAlgorithm); //$NON-NLS-1$
        	}
        	final String policyQualifier = PreferencesManager.get(PreferencesManager.PREFERENCE_XADES_POLICY_QUALIFIER);
        	if (policyQualifier != null && !policyQualifier.trim().isEmpty()) {
        		p.put("policyQualifier", policyQualifier); //$NON-NLS-1$
        	}
        }

        final String claimedRole = PreferencesManager.get(PreferencesManager.PREFERENCE_XADES_SIGNER_CLAIMED_ROLE);
        if (claimedRole != null && !claimedRole.trim().isEmpty()) {
            p.put("signerClaimedRoles", claimedRole); //$NON-NLS-1$
        }

		final String signatureCity = PreferencesManager.get(PreferencesManager.PREFERENCE_XADES_SIGNATURE_PRODUCTION_CITY);
        if (signatureCity != null && !signatureCity.trim().isEmpty()) {
        	p.put("signatureProductionCity", signatureCity); //$NON-NLS-1$
        }
        final String signatureProvince = PreferencesManager.get(PreferencesManager.PREFERENCE_XADES_SIGNATURE_PRODUCTION_PROVINCE);
        if (signatureProvince != null && !signatureProvince.trim().isEmpty()) {
        	p.put("signatureProductionProvince", signatureProvince); //$NON-NLS-1$
        }
        final String signaturePC = PreferencesManager.get(PreferencesManager.PREFERENCE_XADES_SIGNATURE_PRODUCTION_POSTAL_CODE);
        if (signaturePC != null && !signaturePC.trim().isEmpty()) {
        	p.put("signatureProductionPostalCode", signaturePC); //$NON-NLS-1$
        }
        final String signatureCountry = PreferencesManager.get(PreferencesManager.PREFERENCE_XADES_SIGNATURE_PRODUCTION_COUNTRY);
        if (signatureCountry != null && !signatureCountry.trim().isEmpty()) {
        	p.put("signatureProductionCountry", signatureCountry); //$NON-NLS-1$
        }

        final String signFormat = PreferencesManager.get(PreferencesManager.PREFERENCE_XADES_SIGN_FORMAT);
        if (signFormat != null && !signFormat.trim().isEmpty()) {
        	p.put("format", signFormat); //$NON-NLS-1$
        }

		return p;
	}

	/** Obtiene la configuraci&oacute;n para las firmas PAdES.
	 * @return Propiedades para la configuraci&oacute;n de las firmas PAdES. */
	private static Properties loadPAdESExtraParams() {

		final Properties p = new Properties();
        p.put("allowSigningCertifiedPdfs", "false"); //$NON-NLS-1$ //$NON-NLS-2$

        // Preferencias de politica de firma PAdES
        final String policyId = PreferencesManager.get(PreferencesManager.PREFERENCE_PADES_POLICY_IDENTIFIER);
        if (policyId != null && !policyId.trim().isEmpty()) {
        	p.put("policyIdentifier", policyId); //$NON-NLS-1$

        	final String policyIdHash = PreferencesManager.get(PreferencesManager.PREFERENCE_PADES_POLICY_HASH);
        	if (policyIdHash != null && !policyIdHash.trim().isEmpty()) {
        		p.put("policyIdentifierHash", policyIdHash); //$NON-NLS-1$
        	}
        	final String policyHashAlgorithm = PreferencesManager.get(PreferencesManager.PREFERENCE_PADES_POLICY_HASH_ALGORITHM);
        	if (policyHashAlgorithm != null && !policyHashAlgorithm.trim().isEmpty()) {
        		p.put("policyIdentifierHashAlgorithm", policyHashAlgorithm); //$NON-NLS-1$
        	}
        	final String policyQualifier = PreferencesManager.get(PreferencesManager.PREFERENCE_PADES_POLICY_QUALIFIER);
        	if (policyQualifier != null && !policyQualifier.trim().isEmpty()) {
        		p.put("policyQualifier", policyQualifier); //$NON-NLS-1$
        	}
        }

        // Metadatos PAdES
        final String signReason = PreferencesManager.get(PreferencesManager.PREFERENCE_PADES_SIGN_REASON);
        if (signReason != null && !signReason.trim().isEmpty()) {
        	p.put("signReason", signReason); //$NON-NLS-1$
        }
        final String productionCity = PreferencesManager.get(PreferencesManager.PREFERENCE_PADES_SIGN_PRODUCTION_CITY);
        if (productionCity != null && !productionCity.trim().isEmpty()) {
        	p.put("signatureProductionCity", productionCity); //$NON-NLS-1$
        }

        final String contact = PreferencesManager.get(PreferencesManager.PREFERENCE_PADES_SIGNER_CONTACT);
        if (contact != null && !contact.trim().isEmpty()) {
        	p.put("signerContact", contact); //$NON-NLS-1$
        }

        // PAdES BES/Basic
        final String subfilter = PreferencesManager.get(PreferencesManager.PREFERENCE_PADES_FORMAT);
        if (subfilter != null && !subfilter.trim().isEmpty()) {
        	p.put("signatureSubFilter", subfilter); //$NON-NLS-1$
        }

        // PAdES tamano maximo de firma
        final String signSize = PreferencesManager.get(PreferencesManager.PREFERENCE_PADES_SIGN_SIZE);
        if (signSize != null && !signSize.trim().isEmpty()) {
        	p.put("signSize", signSize); //$NON-NLS-1$
        }
		return p;
	}

	/** Obtiene la configuraci&oacute;n para las firmas CAdES.
	 * @return Propiedades para la configuraci&oacute;n de las firmas CAdES. */
	private static Properties loadCAdESExtraParams() {
		final Properties p = new Properties();

        // Preferencias de politica de firma
        final String policyId = PreferencesManager.get(PreferencesManager.PREFERENCE_CADES_POLICY_IDENTIFIER);
        if (policyId != null && !policyId.trim().isEmpty()) {
        	p.put("policyIdentifier", policyId); //$NON-NLS-1$

        	final String policyIdHash = PreferencesManager.get(PreferencesManager.PREFERENCE_CADES_POLICY_HASH);
        	if (policyIdHash != null && !policyIdHash.trim().isEmpty()) {
        		p.put("policyIdentifierHash", policyIdHash); //$NON-NLS-1$
        	}
        	final String policyHashAlgorithm = PreferencesManager.get(PreferencesManager.PREFERENCE_CADES_POLICY_HASH_ALGORITHM);
        	if (policyHashAlgorithm != null && !policyHashAlgorithm.trim().isEmpty()) {
        		p.put("policyIdentifierHashAlgorithm", policyHashAlgorithm); //$NON-NLS-1$
        	}
        	final String policyQualifier = PreferencesManager.get(PreferencesManager.PREFERENCE_CADES_POLICY_QUALIFIER);
        	if (policyQualifier != null && !policyQualifier.trim().isEmpty()) {
        		p.put("policyQualifier", policyQualifier); //$NON-NLS-1$
        	}
        }

        // Preferencias de CAdES
        // Esta propiedad se comparte con otros formatos, hay que comprobar que signer tenemos
        p.put(
    		"mode", //$NON-NLS-1$
    		PreferencesManager.getBoolean(PreferencesManager.PREFERENCE_CADES_IMPLICIT) ?
				"implicit" : //$NON-NLS-1$
					"explicit" //$NON-NLS-1$
		);
        return p;
	}
}
