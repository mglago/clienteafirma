/* Copyright (C) 2018 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@correo.gob.es
 */

package es.gob.afirma.standalone.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.text.NumberFormat;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import es.gob.afirma.core.misc.Platform;
import es.gob.afirma.core.ui.AOUIFactory;
import es.gob.afirma.standalone.LookAndFeelManager;
import es.gob.afirma.standalone.SimpleAfirmaMessages;
import es.gob.afirma.standalone.VisorFirma;
import es.gob.afirma.standalone.crypto.CertAnalyzer;
import es.gob.afirma.standalone.crypto.CertificateInfo;

final class MassiveResultProcessPanel extends JPanel {

    /** Serial Id. */
	private static final long serialVersionUID = -1721238760620367554L;

	private final JLabel certDescText = new JLabel();
    private final JLabel dirPathText = new JLabel();
    private final JLabel certIcon = new JLabel();
    private final JEditorPane certDescription = new JEditorPane();
    private final JButton validateCertButton = null;

    static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

    MassiveResultProcessPanel(final List<SignOperationConfig> signConfigList, final File outDir,
    		final X509Certificate cert) {
        SwingUtilities.invokeLater(() -> createUI(signConfigList, outDir, cert));
    }

    void createUI(final List<SignOperationConfig> signConfigList, final File outDir,
    		final X509Certificate cert) {

        // Texto con la ruta del directorio de salida
        final JTextField outDirPath = new JTextField();
        outDirPath.getAccessibleContext().setAccessibleName(SimpleAfirmaMessages.getString("MassiveResultProcessPanel.0")); //$NON-NLS-1$
        outDirPath.getAccessibleContext().setAccessibleDescription(SimpleAfirmaMessages.getString("MassiveResultProcessPanel.1")); //$NON-NLS-1$
        outDirPath.setBorder(BorderFactory.createEmptyBorder());

        outDirPath.setEditable(false);
        outDirPath.setFocusable(false);
        outDirPath.setText(outDir.getAbsolutePath());
        outDirPath.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent me) {
                // me.isPopupTrigger() depende del Look & Feel y no se puede usar
                if (me.getButton() == MouseEvent.BUTTON3 && me.getClickCount() == 1) {
                    new CopyMenuItem(outDirPath, SimpleAfirmaMessages.getString("MassiveResultProcessPanel.2")).show(me.getComponent(), me.getX(), me.getY()); //$NON-NLS-1$
                }
            }
        });

        // Etiqueta encima del cuadro con la ruta de fichero
        this.dirPathText.setText(SimpleAfirmaMessages.getString("MassiveResultProcessPanel.3")); //$NON-NLS-1$
        this.dirPathText.setLabelFor(outDirPath);

        final JPanel dirPathPanel = new JPanel();
        dirPathPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        dirPathPanel.setLayout(new BoxLayout(dirPathPanel, BoxLayout.X_AXIS));
        dirPathPanel.add(Box.createRigidArea(new Dimension(0, 40)));
        dirPathPanel.add(Box.createRigidArea(new Dimension(5, 0)));


        // Boton de apertura del fichero firmado
        JButton openDirButton = null;
        openDirButton = new JButton(SimpleAfirmaMessages.getString("MassiveResultProcessPanel.4")); //$NON-NLS-1$
        openDirButton.setPreferredSize(new Dimension(150, 24));
        openDirButton.setMnemonic('b');
        openDirButton.setToolTipText(SimpleAfirmaMessages.getString("MassiveResultProcessPanel.5")); //$NON-NLS-1$
        openDirButton.getAccessibleContext().setAccessibleName(SimpleAfirmaMessages.getString("MassiveResultProcessPanel.6")); //$NON-NLS-1$
        openDirButton.getAccessibleContext().setAccessibleDescription(SimpleAfirmaMessages.getString("MassiveResultProcessPanel.7")); //$NON-NLS-1$
        openDirButton.addActionListener(new ActionListener() {
        	@Override
        	public void actionPerformed(final ActionEvent ae) {
        		try {
        			Desktop.getDesktop().open(outDir);
        		}
        		catch (final Exception e) {
        			LOGGER.warning(
        					"Error abriendo el directorio con las firmas generadas: " + e //$NON-NLS-1$
        					);
        			AOUIFactory.showErrorMessage(
        					this,
        					SimpleAfirmaMessages.getString("MassiveResultProcessPanel.8"), //$NON-NLS-1$
        					SimpleAfirmaMessages.getString("SimpleAfirma.7"), //$NON-NLS-1$
        					JOptionPane.ERROR_MESSAGE
        					);
        		}
        	}
        });


        dirPathPanel.add(Box.createRigidArea(new Dimension(11, 0)));
        dirPathPanel.add(outDirPath);
        dirPathPanel.add(Box.createRigidArea(new Dimension(11, 0)));
        dirPathPanel.add(openDirButton);
        dirPathPanel.add(Box.createRigidArea(new Dimension(5, 0)));

        JPanel certDescPanel = null;

        // Panel con los datos del certificado
        if (cert != null) {
            final CertificateInfo certInfo = CertAnalyzer.getCertInformation(cert);

            if (certInfo != null) {

	            this.certIcon.setIcon(certInfo.getIcon());
	            this.certIcon.setToolTipText(certInfo.getIconTooltip());
	            this.certIcon.setFocusable(false);

	            // Para que se detecten apropiadamente los hipervinculos hay que establecer
	            // el tipo de contenido antes que el contenido
	            this.certDescription.setContentType("text/html"); //$NON-NLS-1$
	            setFocusable(false);
	            this.certDescription.setEditable(false);
	            this.certDescription.setOpaque(false);
	            this.certDescription.setText(certInfo.getDescriptionText());
	            this.certDescription.setToolTipText(SimpleAfirmaMessages.getString("SignDataPanel.12")); //$NON-NLS-1$
	            this.certDescription.getAccessibleContext().setAccessibleName(SimpleAfirmaMessages.getString("SignDataPanel.13")); //$NON-NLS-1$
	            this.certDescription.getAccessibleContext().setAccessibleDescription(SimpleAfirmaMessages.getString("SignDataPanel.14")); //$NON-NLS-1$

	            final EditorFocusManager editorFocusManager = new EditorFocusManager (this.certDescription, (he, linkIndex) -> openCertificate(cert, MassiveResultProcessPanel.this));
                this.certDescription.addFocusListener(editorFocusManager);
                this.certDescription.addKeyListener(editorFocusManager);
	            this.certDescription.addHyperlinkListener(editorFocusManager);
            }
            certDescPanel = new JPanel();
            certDescPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            certDescPanel.setLayout(new BoxLayout(certDescPanel, BoxLayout.X_AXIS));
            certDescPanel.add(Box.createRigidArea(new Dimension(0, 40)));
            certDescPanel.add(Box.createRigidArea(new Dimension(5, 0)));
            certDescPanel.add(this.certIcon);
            certDescPanel.add(Box.createRigidArea(new Dimension(11, 0)));
            certDescPanel.add(this.certDescription);
            certDescPanel.add(Box.createRigidArea(new Dimension(11, 0)));
            if (this.validateCertButton != null) {
                certDescPanel.add(this.validateCertButton);
                certDescPanel.add(Box.createRigidArea(new Dimension(5, 0)));
            }
            if (!LookAndFeelManager.HIGH_CONTRAST) {
                certDescPanel.setBackground(LookAndFeelManager.WINDOW_COLOR);
            }

            this.certDescText.setText(SimpleAfirmaMessages.getString("SignDataPanel.21")); //$NON-NLS-1$
            this.certDescText.setLabelFor(this.certDescription);
        }

        // Barra de titulo del listado de resultados
		final SignatureResultTitleRenderer resultTitlePanel = new SignatureResultTitleRenderer();
		resultTitlePanel.setFileNameColumnTitle(SimpleAfirmaMessages.getString("MassiveResultProcessPanel.9")); //$NON-NLS-1$
		resultTitlePanel.setSizeColumnTitle(SimpleAfirmaMessages.getString("MassiveResultProcessPanel.10")); //$NON-NLS-1$
		resultTitlePanel.setResultColumnTitle(SimpleAfirmaMessages.getString("MassiveResultProcessPanel.11")); //$NON-NLS-1$
		resultTitlePanel.setBorder(BorderFactory.createMatteBorder(0,  0,  1,  0, Color.GRAY));

        final JScrollPane resultListPanel = new JScrollPane(
    		getSignResultList(signConfigList, this)
		);
        resultListPanel.setBorder(BorderFactory.createEmptyBorder());

        // En Apple siempre hay barras, y es el SO el que las pinta o no depende de si hacen falta
        if (Platform.OS.MACOSX.equals(Platform.getOS())) {
        	resultListPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        	resultListPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        }
        else {
        	resultListPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }

        // Creamos un panel que contenga el titulo del listado y el propio listado
        final JPanel resultPanel = new JPanel(new GridBagLayout());
        resultPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        final GridBagConstraints resultConstraints = new GridBagConstraints();
        resultConstraints.fill = GridBagConstraints.BOTH;
        resultConstraints.weightx = 1.0;
        resultConstraints.gridy = 0;
        resultPanel.add(resultTitlePanel, resultConstraints);
        resultConstraints.weighty = 1.0;
        resultConstraints.gridy++;
        resultPanel.add(resultListPanel, resultConstraints);

        // Establecemos la configuracion de color
        if (!LookAndFeelManager.HIGH_CONTRAST) {
            setBackground(LookAndFeelManager.WINDOW_COLOR);
            outDirPath.setBackground(LookAndFeelManager.WINDOW_COLOR);
            dirPathPanel.setBackground(LookAndFeelManager.WINDOW_COLOR);
        }

        setLayout(new GridBagLayout());

        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridy = 0;
        c.insets = new Insets(11, 0, 0, 0);
        this.add(this.dirPathText, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        this.add(dirPathPanel, c);
        c.gridy++;
        c.insets = new Insets(11, 0, 0, 0);
        if (certDescPanel != null) {
            this.add(this.certDescText, c);
            c.gridy++;
            c.insets = new Insets(0, 0, 0, 0);
            this.add(certDescPanel, c);
            c.gridy++;
            c.insets = new Insets(11, 0, 0, 0);
        }
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.gridy++;
        c.insets = new Insets(11, 0, 0, 0);
        this.add(resultPanel, c);
    }

	private static Component getSignResultList(final List<SignOperationConfig> signConfigList,
			final Component parent) {

        final JList<SignOperationConfig> resultList =
        		new JList<>(signConfigList.toArray(new SignOperationConfig[signConfigList.size()]));
        resultList.setCellRenderer(new SignatureResultCellRenderer());

        // Definimos que al hacer doble clic o pulsar intro sobre una firma del listado, se visualicen sus datos
        resultList.addMouseListener(new MouseListener() {
			@Override public void mouseReleased(final MouseEvent e) { /* No hacemos nada */ }
			@Override public void mousePressed(final MouseEvent e) { /* No hacemos nada */ }
			@Override public void mouseExited(final MouseEvent e) { /* No hacemos nada */ }
			@Override public void mouseEntered(final MouseEvent e) { /* No hacemos nada */ }
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() == 2 && e.getSource() instanceof JList<?>) {
					final JList<SignOperationConfig> list = (JList<SignOperationConfig>) e.getSource();
					final int index = list.locationToIndex(e.getPoint());
					openSignatureItem(list, index, parent);
				}
			}
		});

        resultList.addKeyListener(new KeyListener() {
			@Override public void keyTyped(final KeyEvent e) { /* No hacemos nada */ }
			@Override public void keyPressed(final KeyEvent e) { /* No hacemos nada */ }
			@Override
			public void keyReleased(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					final JList<SignOperationConfig> list = (JList<SignOperationConfig>) e.getSource();
					final int index = resultList.getSelectedIndex();
					openSignatureItem(list, index, parent);
				}
			}
		});

		return resultList;
	}

	/**
	 * Abre una firma de la lista.
	 * @param list Lista con las firmas resultantes.
	 * @param index Indice de la firma seleccionada.
	 * @param parent Componente padre sobre el que mostrar el dialogo con la informaci&oacute;n.
	 */
	static void openSignatureItem(final JList<SignOperationConfig> list, final int index, final Component parent) {
		if (index >= 0) {
			final SignOperationConfig item = list.getModel().getElementAt(index);
			if (item.getSignatureFile() != null) {
				SwingUtilities.invokeLater(() -> new VisorFirma(false, parent).initialize(false, item.getSignatureFile()));
			}
		}
	}

	static void openCertificate(final X509Certificate cert, final Component parent) {
        try {
            final File tmp = File.createTempFile("afirma", ".cer");  //$NON-NLS-1$//$NON-NLS-2$
            tmp.deleteOnExit();
            try (
        		final OutputStream bos = new BufferedOutputStream(new FileOutputStream(tmp));
    		) {
            	bos.write(cert.getEncoded());
            }
            Desktop.getDesktop().open(tmp);
        }
        catch(final Exception e) {
        	LOGGER.warning(
    			"Error abriendo el fichero con el visor por defecto: " + e //$NON-NLS-1$
			);
        	AOUIFactory.showErrorMessage(
                parent,
                SimpleAfirmaMessages.getString("SignDataPanel.23"), //$NON-NLS-1$
                SimpleAfirmaMessages.getString("SimpleAfirma.7"), //$NON-NLS-1$
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

	/**
	 * Renderer de los t&iacute;tulos del listado de firmas realizadas.
	 */
	private static class SignatureResultTitleRenderer extends JPanel {

		/** Serial Id. */
		private static final long serialVersionUID = 6994842579055905859L;

		private final JLabel fileNameLabel;
		private final JLabel sizeLabel;
		private final JLabel resultLabel;

		public SignatureResultTitleRenderer() {

			this.fileNameLabel = new JLabel();

			this.sizeLabel = new JLabel();
			this.sizeLabel.setPreferredSize(new Dimension(60, 14));

			this.resultLabel = new JLabel();
			this.resultLabel.setPreferredSize(new Dimension(52, 14));

			// Establecemos la configuracion de color
			Color bgColor = Color.WHITE;
			// Configuramos los colores
			if (!LookAndFeelManager.HIGH_CONTRAST && !Platform.OS.MACOSX.equals(Platform.getOS())) {
				bgColor = LookAndFeelManager.WINDOW_COLOR;
			}

			setBackground(bgColor);

			setLayout(new GridBagLayout());

			final GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.insets = new Insets(3, 11, 3, 0);

			c.gridx = 0;
			c.weightx = 1.0;
			add(this.fileNameLabel, c);

			c.weightx = 0;
			c.gridx++;
			add(this.sizeLabel, c);

			c.gridx++;
			c.insets = new Insets(0,  5,  0,  5);
			add(this.resultLabel, c);
		}


    	void setFileNameColumnTitle(final String title) {
    		this.fileNameLabel.setText(title);
    	}


    	void setSizeColumnTitle(final String title) {
    		this.sizeLabel.setText(title);
    	}

    	void setResultColumnTitle(final String title) {
    		this.resultLabel.setText(title);
    	}

	}

	/**
	 * Renderer de los elementos del listado de firmas realizadas.
	 */
	private static class SignatureResultCellRenderer extends JPanel implements ListCellRenderer<SignOperationConfig> {

		/** Serial Id. */
		private static final long serialVersionUID = -8817021555833690354L;

		private final Dimension iconDimension;

		private final JLabel iconLabel;
		private final JLabel fileNameLabel;
		private final JLabel sizeLabel;
		private final JLabel resultIcon;

		private Image okIcon = null;
		private Image koIcon = null;

		private final NumberFormat formatter;

    	private final Border focusedBorder;
    	private final Border unfocusedBorder;

    	private int basePathLength = 0;

		public SignatureResultCellRenderer() {

			this.iconDimension = new Dimension(32, 32);

			this.iconLabel = new JLabel();
			this.iconLabel.setPreferredSize(this.iconDimension);

			this.fileNameLabel = new JLabel();

			this.sizeLabel = new JLabel();
			this.sizeLabel.setPreferredSize(new Dimension(60, 32));

			this.resultIcon = new JLabel();
			this.resultIcon.setPreferredSize(this.iconDimension);

			this.formatter = NumberFormat.getNumberInstance();

			this.focusedBorder = BorderFactory.createDashedBorder(Color.GRAY);
			this.unfocusedBorder = BorderFactory.createEmptyBorder(1,  1,  1,  1);

			// Establecemos la configuracion de color
			Color bgColor = Color.WHITE;
			// Configuramos los colores
			if (!LookAndFeelManager.HIGH_CONTRAST && !Platform.OS.MACOSX.equals(Platform.getOS())) {
				bgColor = LookAndFeelManager.WINDOW_COLOR;
			}

			setBackground(bgColor);

			setLayout(new GridBagLayout());

			final GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.insets = new Insets(3, 11, 3, 0);

			c.gridx = 0;
			add(this.iconLabel, c);

			c.gridx++;
			c.weightx = 1.0;
			add(this.fileNameLabel, c);

			c.weightx = 0;
			c.gridx++;
			add(this.sizeLabel, c);
			c.gridx++;
			c.insets = new Insets(0,  15,  0,  15);
			add(this.resultIcon, c);
		}

		@Override
		public Component getListCellRendererComponent(final JList<? extends SignOperationConfig> list,
				final SignOperationConfig value, final int index, final boolean isSelected, final boolean cellHasFocus) {

			if (this.basePathLength == 0) {
				this.basePathLength = calculateBasePathLength(list.getModel());
			}

			final ScalablePane typeIcon = (ScalablePane) value.getFileType().getIcon();
			typeIcon.setPreferredSize(this.iconDimension);

			this.iconLabel.setIcon(new ImageIcon(typeIcon.getScaledInstanceToFit(typeIcon.getMaster(), this.iconDimension)));

			// Si la operacion ha terminado bien, mostramos la informacion de la firma; si no,
			// la ruta de los datos que se firmaban y el icono de error
			if (value.getSignatureFile() != null) {
				this.fileNameLabel.setText(value.getSignatureFile().getAbsolutePath().substring(this.basePathLength));
				this.sizeLabel.setText(calculateSize(value.getSignatureFile().length()));
				this.resultIcon.setIcon(new ImageIcon(getResultIcon(true)));
			}
			else {
				this.fileNameLabel.setText(value.getDataFile().getAbsolutePath());
				this.sizeLabel.setText(calculateSize(0));
				this.resultIcon.setIcon(new ImageIcon(getResultIcon(false)));
			}

			setBorder(cellHasFocus ? this.focusedBorder : this.unfocusedBorder);

			return this;
		}

		private static int calculateBasePathLength(final ListModel<? extends SignOperationConfig> signConfigs) {
	        int parentLength = Integer.MAX_VALUE;
	        for (int i = 0; i < signConfigs.getSize(); i++) {
	        	final SignOperationConfig config = signConfigs.getElementAt(i);
	        	if (config.getSignatureFile() != null &&
	        			config.getSignatureFile().getParentFile() != null) {
	        		final int length = config.getSignatureFile().getParentFile().getAbsolutePath().length();
	        		if (length < parentLength) {
	        			parentLength = length;
	        		}
	        	}
	        }
	        if (parentLength > 0) {
	        	parentLength++;
	        }

	        return parentLength;
		}

		/**
		 * Recupera el icono de exito o error.
		 * @param ok {@code true} para solicitar el icono de firma correcta, {@code false}
		 * para solicitar el de error.
		 * @return Componente con el icono deseado.
		 */
		private Image getResultIcon(final boolean ok) {

			Image icon;
			if (ok) {
				if (this.okIcon == null) {
					this.okIcon = buildIcon(ok);
				}
				icon = this.okIcon;
			}
			else {
				if (this.koIcon == null) {
					this.koIcon = buildIcon(ok);
				}
				icon = this.koIcon;
			}
			return icon;
		}

		private static Image buildIcon(final boolean ok) {
			Image resultIcon;
			final String imageName = ok ? "ok_icon.png" : "ko_icon.png"; //$NON-NLS-1$ //$NON-NLS-2$
	        try (
	    		final InputStream is = MassiveResultProcessPanel.class.getResourceAsStream("/resources/" + imageName); //$NON-NLS-1$
			) {
	        	final Image image = ImageIO.read(is);
	        	final ScalablePane resultOperationIcon = new ScalablePane(image);
	            resultOperationIcon.setBackground(new Color(255, 255, 255, 0));
	            resultOperationIcon.setFocusable(false);
	            resultIcon = resultOperationIcon.getScaledInstanceToFit(image, new Dimension(32,  32));
	        }
	        catch (final Exception e) {
	            LOGGER.warning("No se ha podido cargar el icono de resultado o validez de firma, este no se mostrara: " + e); //$NON-NLS-1$
	            resultIcon = null;
	        }
	        return resultIcon;
		}

		/**
		 * Obtiene el tama&ntilde;o formateado del fichero en KiloBytes.
		 * @param size Bytes del fichero.
		 * @return Tama&ntilde;o del fichero.
		 */
		private String calculateSize(final long size) {
			if (size == 0) {
				return "0 KB"; //$NON-NLS-1$
			}
			if (size < 1024) {
				return "1 KB"; //$NON-NLS-1$
			}
			return this.formatter.format(size / 1024) + " KB"; //$NON-NLS-1$
		}
	}
}
