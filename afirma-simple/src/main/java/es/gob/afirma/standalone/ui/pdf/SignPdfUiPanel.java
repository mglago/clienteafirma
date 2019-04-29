/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.standalone.ui.pdf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

import es.gob.afirma.core.misc.Platform;
import es.gob.afirma.core.ui.AOUIFactory;
import es.gob.afirma.signers.pades.PdfExtraParams;
import es.gob.afirma.standalone.ui.pdf.PageLabel.PageLabelListener;

final class SignPdfUiPanel extends JPanel implements
											PageLabel.PageLabelListener,
											KeyListener,
											FocusListener,
											ActionListener,
											DocumentListener {

	private static final long serialVersionUID = 8109653789776305491L;

	private static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	private static final int PREFERRED_WIDTH = 470;
	private static final int PREFERRED_HEIGHT = 600;
	private static final int PAGEPANEL_PREFERRED_WIDTH = 466;
	private static final int PAGEPANEL_PREFERRED_HEIGHT = 410;

	interface SignPdfUiPanelListener {
		void positionSelected(final Properties extraParams);
		void positionCancelled();
		void nextPanel(final Properties p, final BufferedImage im);
	}

	private Properties extraParamsForLocation = null;
	Properties getExtraParamsForLocation() {
		return this.extraParamsForLocation;
	}

	private int currentPage = 1;
	int getCurrentPage() {
		return this.currentPage;
	}

	private int currentScale = 100;

	private final SignPdfUiPanelListener listener;
	SignPdfUiPanelListener getListener() {
		return this.listener;
	}

	private JPanel pagePanel;
	private  List<BufferedImage> pdfPages;

	private final boolean isSignPdf;
	private BufferedImage appendPage;
	private final List<Dimension> pdfPageSizes;
	private PageLabel pageLabel;
	private final JButton okButton = new JButton(SignPdfUiMessages.getString("SignPdfUiPanel.0")); //$NON-NLS-1$
	private final JTextField posX = new JTextField(4);
	private final JTextField posY = new JTextField(4);
	private final JTextField width = new JTextField(4);
	private final JTextField height = new JTextField(4);
	private final JLabel indexLabel = new JLabel();

	final JButton firstPageButton = new JButton("<<"); //$NON-NLS-1$
	final JButton previousPageButton = new JButton("<"); //$NON-NLS-1$
	final JButton nextPageButton = new JButton(">"); //$NON-NLS-1$
	final JButton lastPageButton = new JButton(">>"); //$NON-NLS-1$

	private final PdfDocument pdfDocument;
	private int pressButton = 0;

	SignPdfUiPanel(final boolean isSign,
				   final List<BufferedImage> pages,
				   final List<Dimension> pageSizes,
				   final byte[] pdf,
			       final SignPdfUiPanelListener spul) {

		if (pages == null || pages.isEmpty()) {
			throw new IllegalArgumentException(
				"La lista de paginas no puede ser nula ni vacia" //$NON-NLS-1$
			);
		}
		if (pageSizes == null || pages.size() != pageSizes.size()) {
			throw new IllegalArgumentException(
				"Las dimensiones de las paginas tienen que corresponderse con la lista de paginas proporcionada" //$NON-NLS-1$
			);
		}
		if (spul == null) {
			throw new IllegalArgumentException(
				"La clase a la que notificar la seleccion no puede ser nula" //$NON-NLS-1$
			);
		}
		this.pdfPages = pages;
		this.pdfPageSizes = pageSizes;
		this.listener = spul;
		this.isSignPdf = isSign;

		this.pdfDocument = new PdfDocument();
		this.pdfDocument.setBytesPdf(pdf);

		createUI();
	}

	public void setPdfPages(final List<BufferedImage> pages) {
		this.pdfPages=pages;
	}

	public List<BufferedImage> getPdfPages() {
		return this.pdfPages;
	}

	private void setProperties(final Properties p) {
		this.extraParamsForLocation = p;
	}

	private PageLabel createPageLabel(final BufferedImage page,
			                       final PageLabelListener pll,
			                       final KeyListener kl,
			                       final Component parentFrame,
			                       final Dimension pdfPageOriginalDimension) {

		int pageWidth = page.getWidth();
		int pageHeight = page.getHeight();

		final Dimension screen = parentFrame.getPreferredSize();

		// Si la previsualizacion de la pagina es mayor que la propia pantalla,
		// hacemos reducciones incrementales del 10%
		while (pageWidth > screen.width || pageHeight > screen.height) {
			pageWidth = (int) Math.round(pageWidth * 0.9);
			pageHeight = (int) Math.round(pageHeight * 0.9);
		}

		// Comprobacion de si las paginas estan rotadas
		final float pdfRatio;
		if (page.getWidth() <= page.getHeight() && pdfPageOriginalDimension.width <= pdfPageOriginalDimension.height) {
			// No rotada
			pdfRatio = (float) pdfPageOriginalDimension.height / pageHeight;
		}
		else {
			// Pagina rotada
			pdfRatio = (float) pdfPageOriginalDimension.width / pageWidth;
		}

		final float aspectRatio = (float) pageWidth / page.getWidth();
		this.currentScale = Math.round(100 * aspectRatio);

		// Recalculamos las dimensiones para asegurarnos de que los sucesivos redondeos del reescalado
		// no han provocado que se altere la proporcion de altura y anchura de la pagina
		pageWidth = Math.round(page.getWidth() * aspectRatio);
		pageHeight = Math.round(page.getHeight() * aspectRatio);

		final PageLabel ret = new PageLabel(
			page.getScaledInstance(pageWidth, pageHeight, Image.SCALE_SMOOTH),
			pageWidth,
			pageHeight,
			pll,
			pdfRatio
		);

		ret.addKeyListener(kl);
		ret.setFocusable(false);

		ret.getAccessibleContext().setAccessibleDescription(
				SignPdfUiMessages.getString("SignPdfUiPanel.6") //$NON-NLS-1$
		);

		return ret;
	}

	void createUI() {

		addKeyListener(this);

		getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPanel.1") //$NON-NLS-1$
		);

		// Establecemos un tamano preferido cualquiera para que se redimensione
		// correctamente el panel de scroll en el que se mostrara este panel
		setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
		setLayout(new GridBagLayout());

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.gridy = 0;

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());
		final TitledBorder tb = BorderFactory.createTitledBorder(SignPdfUiMessages.getString("SignPdfUiPanel.7")); //$NON-NLS-1$
		tb.setTitleFont(getFont().deriveFont(Font.PLAIN));
		mainPanel.setBorder(tb);

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.PAGE_START;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridy = 0;

		mainPanel.add(createMessageLabel(), c);

		c.gridy++;

		mainPanel.add(createCoordenatesPanel(), c);

		c.gridy++;
		c.ipady = 3;
		c.fill = GridBagConstraints.BOTH;

		mainPanel.add(createPagePanel(), c);

		c.ipady = 3;
		c.gridy++;
		c.anchor = GridBagConstraints.PAGE_END;
		c.fill = GridBagConstraints.HORIZONTAL;
		mainPanel.add(createPaginationPanel(), c);

		add(mainPanel, gbc);

		gbc.gridy++;
		gbc.weighty = 0.0;
		add(createButtonsPanel(), gbc);
		enableButtons();
	}

	/** Crea el panel con la previsualizaci&oacute;n de las p&aacute;ginas del PDF.
	 * @return Panel con la previsualizaci&oacute;n. */
	private JPanel createPagePanel() {

		this.pagePanel = new JPanel();
		this.pagePanel.setLayout(new GridBagLayout());
		this.pagePanel.setPreferredSize(new Dimension(PAGEPANEL_PREFERRED_WIDTH, PAGEPANEL_PREFERRED_HEIGHT));

		// Creamos la etiqueta y establecemos la primera pagina
		this.pageLabel = createPageLabel(
			this.pdfPages.get(0),
			this,
			this,
			this.pagePanel,
			this.pdfPageSizes.get(0)
		);

		this.pagePanel.add(this.pageLabel);

		return this.pagePanel;
	}

	/** Crea el panel para la paginaci&oacute;n del PDF.
	 * @return Panel para la paginaci&oacute;n del PDF. */
	private JPanel createPaginationPanel() {

		final JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

		this.indexLabel.setText(
			SignPdfUiMessages.getString(
				"SignPdfUiPanel.5", //$NON-NLS-1$
				Integer.toString(getCurrentPage()),
				Integer.toString(this.pdfPages.size()),
				Integer.toString(this.currentScale)
			)
		);
		this.indexLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.indexLabel.setFocusable(false);
		this.indexLabel.addKeyListener(this);

		this.firstPageButton.addActionListener(this);
		this.firstPageButton.addKeyListener(this);
		this.previousPageButton.addActionListener(this);
		this.previousPageButton.addKeyListener(this);
		this.nextPageButton.addActionListener(this);
		this.nextPageButton.addKeyListener(this);
		this.lastPageButton.addActionListener(this);
		this.lastPageButton.addKeyListener(this);

		panel.add(this.firstPageButton);
		panel.add(this.previousPageButton);
		panel.add(this.indexLabel);
		panel.add(this.nextPageButton);
		panel.add(this.lastPageButton);

		return panel;
	}

	/**
	 * Crea la etiqueta que explica la selecci&oacute;n del &aacute;rea de firma.
	 * @return Etiqueta.
	 */
	private static JLabel createMessageLabel() {
		final JLabel messageLabel = new JLabel(SignPdfUiMessages.getString("SignPdfUiPanel.14")); //$NON-NLS-1$
		messageLabel.getAccessibleContext().setAccessibleDescription(
				SignPdfUiMessages.getString("SignPdfUiPanel.15")); //$NON-NLS-1$

		return messageLabel;
	}

	/** Crea el panel con los elementos que muestran las coordenadas del cursor dentro
	 * del panel de firma.
	 * @return Panel con los componentes para la visualizacion de coordenadas. */
	private JPanel createCoordenatesPanel() {

		final JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER));

		final DocumentFilter docFilter = new NaturalNumbersDocFilter();

		final JLabel posXLabel = new JLabel(SignPdfUiMessages.getString("SignPdfUiPanel.8")); //$NON-NLS-1$
		posXLabel.setLabelFor(this.posX);
		panel.add(posXLabel);

		this.posX.getAccessibleContext().setAccessibleDescription(SignPdfUiMessages.getString("SignPdfUiPanel.16")); //$NON-NLS-1$
		this.posX.addKeyListener(this);
		this.posX.addFocusListener(this);
		PlainDocument doc = (PlainDocument) this.posX.getDocument();
		doc.setDocumentFilter(docFilter);
		doc.addDocumentListener(this);
		panel.add(this.posX);

		final JLabel posYLabel = new JLabel(SignPdfUiMessages.getString("SignPdfUiPanel.9")); //$NON-NLS-1$
		posYLabel.setLabelFor(this.posY);
		panel.add(posYLabel);

		this.posY.getAccessibleContext().setAccessibleDescription(SignPdfUiMessages.getString("SignPdfUiPanel.17")); //$NON-NLS-1$
		this.posY.addKeyListener(this);
		this.posY.addFocusListener(this);
		doc = (PlainDocument) this.posY.getDocument();
		doc.setDocumentFilter(docFilter);
		doc.addDocumentListener(this);
		panel.add(this.posY);

		final JLabel widthLabel = new JLabel(SignPdfUiMessages.getString("SignPdfUiPanel.12")); //$NON-NLS-1$
		widthLabel.setLabelFor(this.width);
		panel.add(widthLabel);


		this.width.getAccessibleContext().setAccessibleDescription(SignPdfUiMessages.getString("SignPdfUiPanel.18")); //$NON-NLS-1$
		this.width.addKeyListener(this);
		this.width.addFocusListener(this);
		doc = (PlainDocument) this.width.getDocument();
		doc.setDocumentFilter(docFilter);
		doc.addDocumentListener(this);
		panel.add(this.width);

		final JLabel heightLabel = new JLabel(SignPdfUiMessages.getString("SignPdfUiPanel.13")); //$NON-NLS-1$
		heightLabel.setLabelFor(this.height);
		panel.add(heightLabel);

		this.height.getAccessibleContext().setAccessibleDescription(SignPdfUiMessages.getString("SignPdfUiPanel.19")); //$NON-NLS-1$
		this.height.addKeyListener(this);
		this.height.addFocusListener(this);
		doc = (PlainDocument) this.height.getDocument();
		doc.setDocumentFilter(docFilter);
		doc.addDocumentListener(this);
		panel.add(this.height);

		return panel;
	}

	/** Crea el panel con los botones de aceptar y cancelar.
	 * @return Panel de botones. */
	private JPanel createButtonsPanel() {

		final JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		this.okButton.setEnabled(false);
		this.okButton.setMnemonic('A');
		this.okButton.getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPanel.2") //$NON-NLS-1$
		);
		this.okButton.addActionListener(
			e -> {
				final Properties p = new Properties();
				if (getCurrentPage() > getPdfPages().size()) {
					p.put(PdfExtraParams.SIGNATURE_PAGE, "append"); //$NON-NLS-1$
				}
				else {
					p.put(PdfExtraParams.SIGNATURE_PAGE, Integer.toString(getCurrentPage()));
				}
				p.putAll(getExtraParamsForLocation());
				getListener().nextPanel(p, getFragmentImage(p));
			}
		);
		this.okButton.addKeyListener(this);
		panel.add(this.okButton);

		final JButton cancelButton = new JButton(SignPdfUiMessages.getString("SignPdfUiPanel.3")); //$NON-NLS-1$
		cancelButton.setMnemonic('C');
		cancelButton.getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPanel.4") //$NON-NLS-1$
		);
		cancelButton.addActionListener(
			e -> getListener().positionCancelled()
		);
		cancelButton.addKeyListener(this);

		// En Mac OS X el orden de los botones es distinto
		if (Platform.OS.MACOSX.equals(Platform.getOS())) {
			panel.add(cancelButton);
			panel.add(this.okButton);
		}
		else {
			panel.add(this.okButton);
			panel.add(cancelButton);
		}

		return panel;
	}

	BufferedImage getFragmentImage(final Properties p) {
		final int pageNumber;
		final BufferedImage page;
		if (p.getProperty("signaturePage").equals("append")) { //$NON-NLS-1$ //$NON-NLS-2$
			pageNumber = 0;
			page = this.appendPage;
		}
		else {
			pageNumber = Integer.parseInt(p.getProperty("signaturePage")) - 1; //$NON-NLS-1$
			page = this.pdfPages.get(Integer.parseInt(p.getProperty("signaturePage")) - 1); //$NON-NLS-1$
		}

		final int newWidth = (int) this.pdfPageSizes.get(pageNumber).getWidth();
		final int newHeight = (int) this.pdfPageSizes.get(pageNumber).getHeight();

		final BufferedImage im = new BufferedImage (
			newWidth,
			newHeight,
			this.pdfPages.get(pageNumber).getType()
		);

		final Graphics2D graphics2D = im.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
		RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(
			page,
			0,
			0,
			newWidth,
			newHeight,
			null
		);

		graphics2D.dispose();

		final int uxr = Math.max(0 ,Integer.parseInt(p.getProperty("signaturePositionOnPageUpperRightX")) - 4); //$NON-NLS-1$
		final int uyr = Math.max(0, Integer.parseInt(p.getProperty("signaturePositionOnPageUpperRightY"))); //$NON-NLS-1$
		final int lxl = Math.max(0, Integer.parseInt(p.getProperty("signaturePositionOnPageLowerLeftX"))); //$NON-NLS-1$
		final int lyl = Math.max(0, Integer.parseInt(p.getProperty("signaturePositionOnPageLowerLeftY"))); //$NON-NLS-1$
		final int y = Math.max(0, newHeight - uyr);

		final BufferedImage imSign = im.getSubimage(lxl, y, uxr - lxl, uyr - lyl);

		return imSign;
	}

	@Override
	public void setX(final String x) {
		this.posX.setText(x);
	}

	@Override
	public void setY(final String y) {
		this.posY.setText(y);
	}

	@Override
	public void setWidth(final String width) {
		this.width.setText(width);
	}

	@Override
	public void setHeight(final String height) {
		this.height.setText(height);
	}

	@Override public void keyTyped(final KeyEvent e) { /* vacio */ }
	@Override public void keyReleased(final KeyEvent e) { /* vacio */ }

	@Override
	public void keyPressed(final KeyEvent ke) {
		if (ke != null) {
			if (!(ke.getComponent() instanceof JTextComponent)) {
				if (ke.getKeyCode() == KeyEvent.VK_LEFT && getCurrentPage() > 1) {
					this.currentPage--;
					changePage();
				}
				else if (ke.getKeyCode() == KeyEvent.VK_RIGHT && getCurrentPage() < this.pdfPages.size()) {
					this.currentPage++;
					changePage();
				}
				else if (ke.getKeyCode() == KeyEvent.VK_RIGHT
						&& getCurrentPage() == this.pdfPages.size()
						&& !this.isSignPdf) {
					this.currentPage++;
					appendPage();
				}
			}
			if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
				this.currentPage++;
				getListener().positionCancelled();
			}
		}
	}

	@Override
	public void focusGained(final FocusEvent evt) {
		if (evt.getSource() instanceof JTextComponent) {
			((JTextComponent) evt.getSource()).selectAll();
		}
	}

	@Override
	public void focusLost(final FocusEvent evt) {
		if (evt.getSource() instanceof JTextComponent) {
			((JTextComponent) evt.getSource()).select(0,  0);
		}
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (e != null) {
			if (e.getSource() == this.firstPageButton && getCurrentPage() > 1) {
				this.currentPage = 1;
				changePage();
			}
			else if (e.getSource() == this.previousPageButton && getCurrentPage() > 1) {
				this.currentPage--;
				if (this.pressButton > 0) {
					try {
						preLoadImages(getCurrentPage() + 1, this.currentPage);
					} catch (final IOException ex) {
						LOGGER.log(Level.SEVERE, "Error durante la carga de las miniaturas anteriores: " + ex, ex); //$NON-NLS-1$
						this.currentPage++; // Deshacemos el cambio de pagina
						AOUIFactory.showErrorMessage(
								SignPdfUiPanel.this,
								SignPdfUiMessages.getString("SignPdfDialog.5"), //$NON-NLS-1$
								SignPdfUiMessages.getString("SignPdfDialog.1"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE
								);
					}
				}
				 if(this.pdfPages.get(getCurrentPage() - 1) != null) {
					 changePage();
					 this.pressButton++;
				 }
			}
			else if (e.getSource() == this.nextPageButton && getCurrentPage() < this.pdfPages.size()) {
				this.currentPage++;
				if (this.pressButton > 0) {
					try {
						preLoadImages(getCurrentPage() - 1, this.currentPage);
					} catch (final IOException ex) {
						LOGGER.log(Level.SEVERE, "Error durante la carga de las miniaturas siguientes: " + ex, ex); //$NON-NLS-1$
						this.currentPage--; // Deshacemos el cambio de pagina
						AOUIFactory.showErrorMessage(
								SignPdfUiPanel.this,
								SignPdfUiMessages.getString("SignPdfDialog.5"), //$NON-NLS-1$
								SignPdfUiMessages.getString("SignPdfDialog.1"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE
							);
					}
				}
				 if (this.pdfPages.get(getCurrentPage() - 1) != null) {
					 changePage();
					 this.pressButton++;
				 }
			}
			else if (e.getSource() == this.lastPageButton && getCurrentPage() < this.pdfPages.size()) {
				this.currentPage = this.pdfPages.size();
				changePage();
			}
			else if (e.getSource() == this.nextPageButton
					&& getCurrentPage() == this.pdfPages.size()
					&& !this.isSignPdf) {
				this.currentPage++;
				appendPage();
			}
		}
	}

	private void appendPage() {
		final int resp = AOUIFactory.showConfirmDialog(
			SignPdfUiPanel.this,
			SignPdfUiMessages.getString("SignPdfUiPanel.11"),  //$NON-NLS-1$
			SignPdfUiMessages.getString("SignPdfUiPanel.10"),  //$NON-NLS-1$
			AOUIFactory.YES_NO_OPTION,
			AOUIFactory.WARNING_MESSAGE
		);
		if (JOptionPane.YES_OPTION == resp) {
			final BufferedImage bi = new BufferedImage(
				(int) this.pdfPageSizes.get(0).getWidth(),
				(int) this.pdfPageSizes.get(0).getHeight(),
				this.pdfPages.get(0).getType()
	        );
			final Graphics2D ig2 = bi.createGraphics();
			ig2.setPaint (Color.WHITE);
			ig2.fillRect (0, 0, bi.getWidth(), bi.getHeight());
			ig2.dispose();

			this.pagePanel.remove(this.pageLabel);
			this.posX.setText(""); //$NON-NLS-1$
			this.posY.setText(""); //$NON-NLS-1$
			this.width.setText(""); //$NON-NLS-1$
			this.height.setText(""); //$NON-NLS-1$
			this.pageLabel = createPageLabel(
				bi,
				this,
				this,
				this.pagePanel,
				this.pdfPageSizes.get(0)
			);

			this.indexLabel.setText(
				SignPdfUiMessages.getString(
					"SignPdfUiPanel.5", //$NON-NLS-1$
					Integer.toString(getCurrentPage()),
					Integer.toString(this.pdfPages.size()),
					Integer.toString(this.currentScale)
				)
			);
			this.nextPageButton.setEnabled(false);
			this.previousPageButton.setEnabled(true);
			this.appendPage = bi;
			this.pagePanel.add(this.pageLabel);
			this.pagePanel.repaint();
		}
		else {
			this.currentPage--;
		}
	}

	private void changePage() {

		enableButtons();
		this.pagePanel.remove(this.pageLabel);
		this.posX.setText(""); //$NON-NLS-1$
		this.posY.setText(""); //$NON-NLS-1$
		this.width.setText(""); //$NON-NLS-1$
		this.height.setText(""); //$NON-NLS-1$
		this.pageLabel = createPageLabel(
			this.pdfPages.get(getCurrentPage() - 1),
			this,
			this,
			this.pagePanel,
			this.pdfPageSizes.get(getCurrentPage() - 1)
		);

		this.indexLabel.setText(
			SignPdfUiMessages.getString(
				"SignPdfUiPanel.5", //$NON-NLS-1$
				Integer.toString(getCurrentPage()),
				Integer.toString(this.pdfPages.size()),
				Integer.toString(this.currentScale)
			)
		);
		this.pagePanel.add(this.pageLabel);
		this.pagePanel.repaint();
	}

	private void enableButtons() {
		if (this.pdfPages.size() == 1) {
			this.lastPageButton.setEnabled(false);
			this.firstPageButton.setEnabled(false);
			if (this.isSignPdf) {
				this.nextPageButton.setEnabled(false);
			}
			else {
				this.nextPageButton.setEnabled(true);
			}
			this.previousPageButton.setEnabled(false);
		}
		else if (getCurrentPage() == 1) {
			this.firstPageButton.setEnabled(false);
			this.previousPageButton.setEnabled(false);
			this.nextPageButton.setEnabled(true);
			this.lastPageButton.setEnabled(true);
		}
		else if (getCurrentPage() == this.pdfPages.size()) {
			this.lastPageButton.setEnabled(false);
			this.firstPageButton.setEnabled(true);
			if (this.isSignPdf) {
				this.nextPageButton.setEnabled(false);
			}
			else {
				this.nextPageButton.setEnabled(true);
			}
			this.previousPageButton.setEnabled(true);
		}
		else {
			this.firstPageButton.setEnabled(true);
			this.previousPageButton.setEnabled(true);
			this.nextPageButton.setEnabled(true);
			this.lastPageButton.setEnabled(true);
		}
	}


	private void preLoadImages(final int actualPage, final int pageToLoad) throws IOException {

		int necessaryPage;
		// Pulsado boton izquierdo (anterior)
		if (actualPage > pageToLoad) {
			necessaryPage = pageToLoad - 2;
		}
		// Pulsado boton derecho (siguente)
		else {
			necessaryPage = pageToLoad + 1;
		}

		// Verificamos que sea una pagina valida
		if(necessaryPage < 0 || necessaryPage >= this.pdfPages.size()) {
			return;
		}

		// Si no tenemos la pagina que necesitamos, la cargamos
		if (this.pdfPages.get(necessaryPage) == null) {
			try {
				this.pdfDocument.loadNewPages(this.pdfPages, getCurrentPage() - 1);
			}
			catch (final OutOfMemoryError | Exception e) {
				throw new IOException("No se ha podido cargar la previsualizacion de la pagina", e); //$NON-NLS-1$
			}
		}
	}

	@Override
	public void changedUpdate(final DocumentEvent evt) {
		updateArea();
	}

	@Override
	public void insertUpdate(final DocumentEvent evt) {
		updateArea();
	}

	@Override
	public void removeUpdate(final DocumentEvent evt) {
		updateArea();
	}

	private void updateArea() {

		final float scale = this.pageLabel.getScale();

		Rectangle r = null;
		final int x = this.posX.getText().isEmpty() ? 0 : Integer.parseInt(this.posX.getText());
		final int rX = (int) (x * scale);
		final int y = this.posY.getText().isEmpty() ? 0 : Integer.parseInt(this.posY.getText());
		final int rY = (int) (y * scale);
		final int w = this.width.getText().isEmpty() ? 0 : Integer.parseInt(this.width.getText());
		final int rWidth = (int) (w * scale);
		final int h = this.height.getText().isEmpty() ? 0 : Integer.parseInt(this.height.getText());
		final int rHeight = (int) (h * scale);

		if (rWidth > 0 && rHeight > 0) {
			r = new Rectangle(rX, rY, rWidth, rHeight);
			setProperties(toPdfPosition(r));
		}
		else {
			setProperties(null);
		}

		this.okButton.setEnabled(r != null);

		this.pageLabel.setSelectionBounds(r);
		this.pageLabel.repaint();
	}

	private Properties toPdfPosition(final Rectangle original) {

		final Dimension currentPageDim = this.pdfPageSizes.get(getCurrentPage() - 1);

		// Si se ha indicado una posicion externa a la pagina, no se imprime la imagen
		if (original.x > currentPageDim.width || original.y > currentPageDim.height) {
			return null;
		}

		// Si se ha indicado un tamano superior a la imagen, se redimensiona al maximo de la imagen
		if (original.x + original.width > currentPageDim.width) {
			original.width = currentPageDim.width - original.x;
		}
		if (original.y + original.height > currentPageDim.height) {
			original.height = currentPageDim.height - original.y;
		}

		final int areaHeight = original.height + original.y > this.pageLabel.getHeight() ?
				this.pageLabel.getHeight() - original.y : original.height;
		final int areaWidth = original.width + original.x > this.pageLabel.getWidth() ?
				this.pageLabel.getWidth() - original.x : original.width;

		final Properties extraParams = new Properties();
		extraParams.put(
			PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_LOWER_LEFTX,
			Integer.toString(
				Math.round(
					original.x * this.pageLabel.getScale()
				)
			)
		);
		extraParams.put(
			PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_LOWER_LEFTY,
			Integer.toString(
				Math.round(
					(this.pageLabel.getHeight() - original.y - areaHeight) * this.pageLabel.getScale()
				)
			)
		);
		extraParams.put(
			PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_UPPER_RIGHTX,
			Integer.toString(
				Math.round(
					(original.x + areaWidth) * this.pageLabel.getScale()
				)
			)
		);
		extraParams.put(
			PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_UPPER_RIGHTY,
			Integer.toString(
				Math.round(
					(this.pageLabel.getHeight() - original.y) * this.pageLabel.getScale()
				)
			)
		);

		return extraParams;
	}
}
