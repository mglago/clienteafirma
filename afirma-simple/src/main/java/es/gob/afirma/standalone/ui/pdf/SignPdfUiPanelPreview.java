/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.standalone.ui.pdf;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.text.DefaultFormatter;

import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.misc.Platform;
import es.gob.afirma.core.ui.AOUIFactory;
import es.gob.afirma.standalone.SimpleAfirmaMessages;
import es.gob.afirma.standalone.ui.EditorFocusManager;
import es.gob.afirma.standalone.ui.pdf.SignPdfUiStamp.SignPdfUiStampListener;
import es.gob.afirma.standalone.ui.pdf.SignPdfUiPanel.SignPdfUiPanelListener;
import es.gob.afirma.standalone.ui.preferences.PreferencesManager;

final class SignPdfUiPanelPreview extends JPanel implements KeyListener, SignPdfUiStampListener {

	private static final long serialVersionUID = 1848879900511003335L;
	private static final int PREFERRED_WIDTH = 475;
	private static final int PREFERRED_HEIGHT = 140;
	private static final int MAX_TEXT_SIZE = 50;
	private static final int MIN_TEXT_SIZE = 1;
	private static final int STEP_TEXT_SIZE = 1;
	private static final int INIT_TEXT_SIZE = 12;
	private static final char NEWLINE = '\n';
	private static final String SPACE_SEPARATOR = " "; //$NON-NLS-1$
	private static final String SPLIT_REGEXP= "\\s+"; //$NON-NLS-1$
	static final String IMAGE_EXT[] = {"jpg", "jpeg", "png", "gif"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	private float scale = 1;
	private BufferedImage image;
	private BufferedImage signImage;
	BufferedImage getSignImage() {
		return this.signImage;
	}
	void setSignImage(final BufferedImage bi) {
		this.signImage = bi;
	}

	private Properties stampLocation;
	private BufferedImage stampImage;
	private String stampImagePath;
	BufferedImage getStampImage() {
		return this.stampImage;
	}
	void setStampImage(final BufferedImage bi) {
		this.stampImage = bi;
	}
	
	private final JDialog parent;
	public JDialog getParentDialog() {
		return this.parent;
	}
	
	private final boolean isPdfSign;
	private final byte[] pdfData;

	private final SignPdfUiPanelListener listener;
	SignPdfUiPanelListener getListener() {
		return this.listener;
	}

	private DropTarget dropTarget;

	private final Properties prop;
	Properties getProp() {
		return this.prop;
	}

	JLabel viewLabel;
	JLabel getViewLabel() {
		return this.viewLabel;
	}

	private Font font;
	Font getViewFont() {
		return this.font;
	}

	void setViewFont(final Font f) {
		this.font = f;
	}

	private int style;
	int getStyle() {
		return this.style;
	}
	void setStyle(final int style) {
		this.style = style;
	}

	private final JToggleButton boldButton = new JToggleButton("<html><b>N</b></html>"); //$NON-NLS-1$
	JToggleButton getBoldButton() {
		return this.boldButton;
	}

	private final JToggleButton italicButton = new JToggleButton("<html><i>K</i></html>"); //$NON-NLS-1$
	JToggleButton getItalicButton() {
		return this.italicButton;
	}

	private final JToggleButton underlineButton = new JToggleButton("<html><u>S</u></html>"); //$NON-NLS-1$
	JToggleButton getUnderlineButton() {
		return this.underlineButton;
	}

	private final JToggleButton strikethroughButton = new JToggleButton("<html><s>T</s></html>"); //$NON-NLS-1$
	JToggleButton getStrikethroughButton() {
		return this.strikethroughButton;
	}

	private final JCheckBox saveConfig = new JCheckBox(SignPdfUiMessages.getString("SignPdfUiPreview.33"), true); //$NON-NLS-1$
	JCheckBox getSaveConfig() {
		return this.saveConfig;
	}
	
	private final JButton addStampButton = new JButton(SignPdfUiMessages.getString("SignPdfUiPreview.30")); //$NON-NLS-1$
	private final JButton removeStampButton = new JButton(SignPdfUiMessages.getString("SignPdfUiPreview.34")); //$NON-NLS-1$

	private final JButton okButton = new JButton(SignPdfUiMessages.getString("SignPdfUiPreview.5")); //$NON-NLS-1$
	JButton getOkButton() {
		return this.okButton;
	}

	private CustomComboBox colorCombobox;
	CustomComboBox getColorCombobox() {
		return this.colorCombobox;
	}

	private JComboBox<FontResource> letterType;
	JComboBox<FontResource> getLetterType() {
		return this.letterType;
	}

	SpinnerNumberModel sizeSpinnerModel = new SpinnerNumberModel(
		INIT_TEXT_SIZE,
		MIN_TEXT_SIZE,
		MAX_TEXT_SIZE,
		STEP_TEXT_SIZE
	);
	JSpinner sizeSpinner = new JSpinner(this.sizeSpinnerModel);
	JSpinner getSizeSpinner() {
		return this.sizeSpinner;
	}
	int getSelectedSize() {
		return ((Integer)this.sizeSpinner.getValue()).intValue();
	}
	void setSelectedSize(final int size) {
		this.sizeSpinner.setValue(Integer.valueOf(size));
	}

	private final JTextArea textArea = new JTextArea();
	JTextArea getTextArea() {
		return this.textArea;
	}

	SignPdfUiPanelPreview (final SignPdfUiPanelListener spul,
						   final Properties p,
						   final BufferedImage im,
						   boolean isSign,
						   byte[] pdf,
						   JDialog parent) {

		if (spul == null) {
			throw new IllegalArgumentException(
				"La clase a la que notificar la seleccion no puede ser nula" //$NON-NLS-1$
			);
		}
		this.listener = spul;
		this.prop = p;
		this.style = 0;
		this.image = im;
		this.signImage = null;
		this.isPdfSign = isSign;
		this.pdfData = pdf;
		this.parent = parent;

		createUI();

		loadProperties();
	}

	void createUI() {

		addKeyListener(this);

		getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPreview.3") //$NON-NLS-1$
		);

		setLayout(new GridBagLayout());

		final GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(10, 5, 0, 5);
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.gridy = 0;
		add(createPreviewPanel(), gbc);
		gbc.gridy++;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 0.0;
		gbc.insets = new Insets(0, 5, 0, 5);
		add(createPreviewHelpPanel(), gbc);
		gbc.gridy++;
		gbc.insets = new Insets(10, 5, 0, 5);
		add(createFontPanel(), gbc);
		gbc.gridy++;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1.0;
		add(createTextAreaPanel(), gbc);
		gbc.gridy++;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 0.0;
		gbc.insets = new Insets(0, 5, 0, 5);
		add(createTextAreaHelpPanel(), gbc);
		gbc.gridy++;
		gbc.insets = new Insets(10, 5, 0, 5);
		add(createStampPanel(), gbc);
		gbc.gridy++;
		gbc.insets = new Insets(0, 15, 0, 5);
		add(this.saveConfig, gbc);
		gbc.gridy++;
		gbc.insets = new Insets(0, 5, 0, 5);
		add(createButtonsPanel(), gbc);
		showPreview();
		this.letterType.requestFocusInWindow();
	}

	private JPanel createPreviewPanel() {

		final JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		panel.setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
		panel.setBorder(BorderFactory.createTitledBorder(
			SignPdfUiMessages.getString("SignPdfUiPreview.0")) //$NON-NLS-1$
		);

		this.dropTarget = new DropTarget(panel, DnDConstants.ACTION_COPY, new DropTargetListener() {

            @Override
            public void dropActionChanged(final DropTargetDragEvent dtde) { /* No implementado */}

            @Override
            public void dragOver(final DropTargetDragEvent dtde) { /* No implementado */ }

            @Override
            public void dragExit(final DropTargetEvent dte) { /* No implementado */ }

            @Override
            public void drop(final DropTargetDropEvent dtde) {

                final Transferable tr = dtde.getTransferable();
                if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    final Object transData;
                    try {
                        transData = tr.getTransferData(DataFlavor.javaFileListFlavor);
                    }
                    catch (final Exception e) {
                    	Logger.getLogger("es.gob.afirma").warning( //$NON-NLS-1$
                            "Ha fallado la operacion de arrastrar y soltar: " + e //$NON-NLS-1$
                        );
                        dtde.dropComplete(false);
                        return;
                    }
                    if (transData instanceof List) {
                        dtde.getDropTargetContext().dropComplete(true);
                        final List<?> fileList = (List<?>) transData;
                        if (fileList.isEmpty()) {
                            dtde.dropComplete(false);
                            return;
                        }
                        if (fileList.size() > 1) {
                        	AOUIFactory.showErrorMessage(
                        		SignPdfUiPanelPreview.this,
                                SimpleAfirmaMessages.getString("SignPanel.18"), //$NON-NLS-1$
                                SimpleAfirmaMessages.getString("SimpleAfirma.48"), //$NON-NLS-1$
                                JOptionPane.WARNING_MESSAGE
                            );
                        }
                        File file = null;
                        final String filename = fileList.get(0).toString();
                        if (filename.startsWith("http://") || //$NON-NLS-1$
                        	filename.startsWith("https://") || //$NON-NLS-1$
                        	filename.startsWith("ftp://") //$NON-NLS-1$
                        ) {
                        	AOUIFactory.showErrorMessage(
                        		SignPdfUiPanelPreview.this,
                                SimpleAfirmaMessages.getString("SignPanel.24"), //$NON-NLS-1$
                                SimpleAfirmaMessages.getString("SimpleAfirma.7"), //$NON-NLS-1$
                                JOptionPane.ERROR_MESSAGE
                            );
                            dtde.dropComplete(false);
                            return;
                        }
                        else if (filename.startsWith("file://")) { //$NON-NLS-1$
                            try {
                            	file = new File(new URI(filename));
                            }
                            catch (final Exception e) {
                            	Logger.getLogger("es.gob.afirma").warning( //$NON-NLS-1$
                            		"Ha fallado la operacion de arrastrar y soltar al obtener la ruta del fichero arrastrado: " + e //$NON-NLS-1$
                                );
                                dtde.dropComplete(false);
                                return;
                            }
                        }
                        try {
                            loadFile(file != null ? file : new File(filename));
                        }
                        catch (final Exception e) {
                        	Logger.getLogger("es.gob.afirma").warning( //$NON-NLS-1$
                                "Ha fallado la operacion de arrastrar y soltar al cargar el fichero arrastrado: " + e //$NON-NLS-1$
                            );
                            dtde.dropComplete(false);
                        }
                    }
                }
                else {
                    dtde.rejectDrop();
                    dtde.dropComplete(false);
                }
            }

            @Override
            public void dragEnter(final DropTargetDragEvent dtde) {
                if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.rejectDrag();
                }
            }
        }, true);

		panel.setDropTarget(this.dropTarget);

		if (getProp().getProperty("signaturePage") != null && //$NON-NLS-1$
				getProp().getProperty("signaturePage").equals("append")) { //$NON-NLS-1$ //$NON-NLS-2$
			createImage();
		}
		if (this.image.getHeight() > PREFERRED_HEIGHT || this.image.getWidth() > PREFERRED_WIDTH) {
			resizeImage(panel);
		}
		this.viewLabel = new JLabel(new ImageIcon(this.image));
		this.viewLabel.getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPreview.9") //$NON-NLS-1$
		);
		this.viewLabel.setToolTipText(SignPdfUiMessages.getString("SignPdfUiPreview.26")); //$NON-NLS-1$
		this.viewLabel.addMouseListener(
			new MouseAdapter() {
	            @Override
				public void mouseClicked(final MouseEvent evt) {
	            	if (SwingUtilities.isRightMouseButton(evt)) {
	            		if (getSignImage() != null) {
		            		final RemoveImPopUpMenu menu = new RemoveImPopUpMenu();
		                    menu.show(evt.getComponent(), evt.getX(), evt.getY());
	            		}
	            		else {
	            			final AddImPopUpMenu menu = new AddImPopUpMenu();
		                    menu.show(evt.getComponent(), evt.getX(), evt.getY());
	            		}
	            	}
	            	else {
	                	try {
							final String imPath = AOUIFactory.getLoadFiles(
								SignPdfUiMessages.getString("SignPdfUiPreview.21"), //$NON-NLS-1$,
								null,
								null,
								IMAGE_EXT,
								SignPdfUiMessages.getString("SignPdfUiPreview.22"), //$NON-NLS-1$,,
								false,
								false,
								null,
								SignPdfUiPanelPreview.this
							)[0].getAbsolutePath();
							paintImage(imPath);
							showPreview();
						}
						catch(final AOCancelledOperationException ex) {
							// Operacion cancelada por el usuario
						}
	                	catch (final Exception e) {
							Logger.getLogger("es.gob.afirma").severe( //$NON-NLS-1$
								"No ha sido posible cargar la imagen: " + e //$NON-NLS-1$
							);
							AOUIFactory.showMessageDialog(
								SignPdfUiPanelPreview.this,
								SignPdfUiMessages.getString("SignPdfUiPreview.24"),  //$NON-NLS-1$
								SignPdfUiMessages.getString("SignPdfUiPreview.23"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE
							);
						}
	                }
		        }
			}
		);
		this.viewLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

		panel.add(this.viewLabel, c);

		return panel;

	}

	private static Component createPreviewHelpPanel() {
		final JEditorPane helpLabel = new JEditorPane();
		helpLabel.setContentType("text/html"); //$NON-NLS-1$
		helpLabel.setText(SignPdfUiMessages.getString("SignPdfUiPreview.29")); //$NON-NLS-1$
        helpLabel.setEditable(false);
        helpLabel.setHighlighter(null);
        helpLabel.setFocusable(false);
        helpLabel.setBackground(new Color(0,0,0,0));

        return helpLabel;
	}

	private JPanel createFontPanel() {

		final JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 1.0;
		c.gridy = 0;
		c.insets = new Insets(5, 5, 0, 5);

		panel.setBorder(BorderFactory.createTitledBorder(
			SignPdfUiMessages.getString("SignPdfUiPreview.1")) //$NON-NLS-1$
		);

		this.letterType = new JComboBox<>(FontResource.getAllFontresources());
		setViewFont(
			new Font(
				((FontResource)this.letterType.getSelectedItem()).getFont().getFontName(),
				getStyle(),
				getSelectedSize()
			)
		);

		this.letterType.setSelectedItem(FontResource.COURIER);
		this.letterType.setRenderer(new ComboRenderer());
		this.letterType.addKeyListener(this);
		this.letterType.setToolTipText(SignPdfUiMessages.getString("SignPdfUiPreview.20")); //$NON-NLS-1$

		final JComponent comp = this.sizeSpinner.getEditor();
	    final JFormattedTextField field = (JFormattedTextField) comp.getComponent(0);
	    final DefaultFormatter formatter = (DefaultFormatter) field.getFormatter();
	    formatter.setAllowsInvalid(false);
		this.sizeSpinner.addChangeListener(
			e -> {
				setViewFont(getViewFont().deriveFont(getStyle(), getSelectedSize()));
				showPreview();
			}
		);
		this.sizeSpinner.getEditor().getComponent(0).addKeyListener(this);
		this.sizeSpinner.setToolTipText(SignPdfUiMessages.getString("SignPdfUiPreview.18")); //$NON-NLS-1$

		this.boldButton.setToolTipText(SignPdfUiMessages.getString("SignPdfUiPreview.14")); //$NON-NLS-1$
		this.boldButton.getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPreview.10") //$NON-NLS-1$
		);
		this.boldButton.addActionListener(
			e -> {
				if (getBoldButton().isSelected()) {
					if(getUnderlineButton().isSelected()) {
						getUnderlineButton().doClick();
					}
					if(getStrikethroughButton().isSelected()) {
						getStrikethroughButton().doClick();
					}
					addStyle(Font.BOLD);
					setViewFont(getViewFont().deriveFont(getStyle()));
					showPreview();
				}
				else {
					deleteStyle(Font.BOLD);
					setViewFont(getViewFont().deriveFont(getStyle()));
					showPreview();
				}
			}
		);
		this.boldButton.addKeyListener(this);

		this.italicButton.setToolTipText(SignPdfUiMessages.getString("SignPdfUiPreview.15")); //$NON-NLS-1$
		this.italicButton.getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPreview.11") //$NON-NLS-1$
		);
		this.italicButton.addActionListener(
			e -> {
				if (getItalicButton().isSelected()) {
					if(getUnderlineButton().isSelected()) {
						getUnderlineButton().doClick();
					}
					if(getStrikethroughButton().isSelected()) {
						getStrikethroughButton().doClick();
					}
					addStyle(Font.ITALIC);
					setViewFont(getViewFont().deriveFont(getStyle()));
					showPreview();
				}
				else {
					deleteStyle(Font.ITALIC);
					setViewFont(getViewFont().deriveFont(getStyle()));
					showPreview();
				}
			}
		);
		this.italicButton.addKeyListener(this);

		this.underlineButton.setToolTipText(SignPdfUiMessages.getString("SignPdfUiPreview.16")); //$NON-NLS-1$
		this.underlineButton.getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPreview.12") //$NON-NLS-1$
		);
		this.underlineButton.addActionListener(
			e -> {
				if (getUnderlineButton().isSelected()) {
					if(getBoldButton().isSelected()) {
						getBoldButton().doClick();
					}
					if(getItalicButton().isSelected()) {
						getItalicButton().doClick();
					}
					if(getStrikethroughButton().isSelected()) {
						getStrikethroughButton().doClick();
					}
					final Map<TextAttribute, Integer> atr = new HashMap<>();
					atr.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
					setViewFont(getViewFont().deriveFont(atr));
					showPreview();
				}
				else {
					setViewFont(new Font(getViewFont().getFontName(), getStyle(), getSelectedSize()));
					showPreview();
				}
			}
		);
		this.underlineButton.addKeyListener(this);

		this.strikethroughButton.setToolTipText(SignPdfUiMessages.getString("SignPdfUiPreview.17")); //$NON-NLS-1$
		this.strikethroughButton.getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPreview.13") //$NON-NLS-1$
		);
		this.strikethroughButton.addActionListener(
			e -> {
				if (getStrikethroughButton().isSelected()) {
					if(getBoldButton().isSelected()) {
						getBoldButton().doClick();
					}
					if(getItalicButton().isSelected()) {
						getItalicButton().doClick();
					}
					if(getUnderlineButton().isSelected()) {
						getUnderlineButton().doClick();
					}
					final Map<TextAttribute, Boolean> atr = new HashMap<>();
					atr.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
					setViewFont(getViewFont().deriveFont(atr));
					showPreview();
				}
				else {
					setViewFont(new Font(getViewFont().getFontName(), getStyle(), getSelectedSize()));
					showPreview();
				}
			}
		);
		this.strikethroughButton.addKeyListener(this);

		this.colorCombobox = new CustomComboBox(ColorResource.getAllColorResources());
		this.colorCombobox.getColorList().addKeyListener(this);
		this.colorCombobox.getColorList().setToolTipText(SignPdfUiMessages.getString("SignPdfUiPreview.19")); //$NON-NLS-1$



		c.gridwidth = 4;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(this.letterType, c);
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		panel.add(this.sizeSpinner, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy++;
		panel.add(this.boldButton, c);
		panel.add(this.italicButton, c);
		panel.add(this.underlineButton, c);
		panel.add(this.strikethroughButton, c);
		panel.add(this.colorCombobox, c);

		return panel;
	}

	private JPanel createTextAreaPanel() {

		final JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.setMinimumSize(new Dimension(400, 100));

		panel.setBorder(BorderFactory.createTitledBorder(
			SignPdfUiMessages.getString("SignPdfUiPreview.2")) //$NON-NLS-1$
		);

		this.textArea.setEditable(true);
		this.textArea.setBackground(Color.WHITE);
		this.textArea.setText(SignPdfUiMessages.getString("SignPdfUiPreview.25")); //$NON-NLS-1$
		this.textArea.setLineWrap(true);
		this.textArea.setWrapStyleWord(true);
		this.textArea.addKeyListener(this);
		final JScrollPane scrollPane = new JScrollPane(
			this.textArea,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
		);
		scrollPane.setMinimumSize(new Dimension(300, 80));
		scrollPane.addKeyListener(this);

		this.textArea.getDocument().addDocumentListener(
			new DocumentListener() {
		        @Override
		        public void removeUpdate(final DocumentEvent e) {
		        	showPreview();
		        }

		        @Override
		        public void insertUpdate(final DocumentEvent e) {
		        	showPreview();
		        }

		        @Override
		        public void changedUpdate(final DocumentEvent e) {
		        	showPreview();
		        }
		    }
		);

		panel.add(scrollPane);

		return panel;
	}

	/** Crea el panel con los botones de aceptar y cancelar.
	 * @return Panel de botones. */
	private Component createTextAreaHelpPanel() {

		final JEditorPane helpLabel = new JEditorPane();
		helpLabel.setContentType("text/html"); //$NON-NLS-1$
		helpLabel.setText(SignPdfUiMessages.getString("SignPdfUiPreview.4")); //$NON-NLS-1$

        final EditorFocusManager editorFocusManager = new EditorFocusManager (
    		helpLabel,
    		(he, linkIndex) -> {
			    try {
			        if (he.getURL() != null) {
			            Desktop.getDesktop().browse(he.getURL().toURI());
			        }
			    }
			    catch (final Exception e) {
			    	AOUIFactory.showErrorMessage(
						SignPdfUiPanelPreview.this,
			            SimpleAfirmaMessages.getString("SignResultPanel.0") + he.getURL(), //$NON-NLS-1$
			            SimpleAfirmaMessages.getString("SimpleAfirma.7"), //$NON-NLS-1$
			            JOptionPane.ERROR_MESSAGE
			        );
			    }
			}
		);

        helpLabel.addHyperlinkListener(editorFocusManager);
        helpLabel.addKeyListener(editorFocusManager);
        helpLabel.setEditable(false);
        helpLabel.setFocusable(false);
        helpLabel.setBackground(new Color(0,0,0,0));

        return helpLabel;
	}


	/** Crea el panel con el botón de insertar sello.
	 * @return Panel de insertar sello. */
	private JPanel createStampPanel() {

		final JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.setBorder(BorderFactory.createTitledBorder(
				SignPdfUiMessages.getString("SignPdfUiPreview.35")) //$NON-NLS-1$
			);

		this.addStampButton.setMnemonic('I');
		this.addStampButton.getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPreview.30") //$NON-NLS-1$
		);
		this.addStampButton.addActionListener(
			e -> {
				final File[] files;
				try {
					files = AOUIFactory.getLoadFiles(
							SignPdfUiMessages.getString("SignPdfUiPreview.21"), //$NON-NLS-1$,
							null,
							null,
							IMAGE_EXT,
							SignPdfUiMessages.getString("SignPdfUiPreview.22"), //$NON-NLS-1$,,
							false,
							false,
							null,
							this
					);
				}
				catch(AOCancelledOperationException ex)
				{
					return;
				}
				
				stampImagePath = files[0].getAbsolutePath();

				final SignPdfUiStamp dialog = new SignPdfUiStamp(this.getParentDialog(), this);

				PdfLoader.loadPdf(
					isPdfSign,
					pdfData,
					dialog
				);
			}
		);
		
		this.addStampButton.addKeyListener(this);
		this.addStampButton.setEnabled(!isPdfSign);
		panel.add(this.addStampButton);

		this.removeStampButton.setMnemonic('E');
		this.removeStampButton.getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPreview.34") //$NON-NLS-1$
		);
		this.removeStampButton.addActionListener(
			e -> {
				this.stampImage = null;
				this.removeStampButton.setEnabled(false);
			}
		);
		this.removeStampButton.addKeyListener(this);
		this.removeStampButton.setEnabled(false);
		panel.add(this.removeStampButton);

		return panel;
	}

	/** Crea el panel con los botones de aceptar y cancelar.
	 * @return Panel de botones. */
	private JPanel createButtonsPanel() {

		final JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		final JButton defaultButton = new JButton(SignPdfUiMessages.getString("SignPdfUiPreview.31")); //$NON-NLS-1$
		defaultButton.setMnemonic('R');
		defaultButton.getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPreview.7") //$NON-NLS-1$
		);
		defaultButton.addActionListener(
			e -> {
				loadDefaultProperties();
			}
		);
		defaultButton.addKeyListener(this);

		this.okButton.setMnemonic('A');
		this.okButton.getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPanel.2") //$NON-NLS-1$
		);
		this.okButton.addActionListener(
			e -> {
				if (!getTextArea().getText().trim().isEmpty()) {
					getProp().put("layer2Text", getTextArea().getText().toString()); //$NON-NLS-1$
					getProp().put("layer2FontFamily", ((FontResource)getLetterType().getSelectedItem()).getPdfFontIndex()); //$NON-NLS-1$
					getProp().put("layer2FontSize", Integer.toString(getSelectedSize())); //$NON-NLS-1$
					getProp().put("layer2FontStyle", Integer.toString(getStyleIndex())); //$NON-NLS-1$
					getProp().put("layer2FontColor", getColorCombobox().getSelectedItem().getPdfColorKey()); //$NON-NLS-1$*/
				}
				if (getSignImage() != null ) {
					getProp().put("signatureRubricImage", getInsertImageBase64(getSignImage())); //$NON-NLS-1$
				}
				if (getStampImage() != null ) {
					getProp().put("image", getInsertImageBase64(getStampImage())); //$NON-NLS-1$
					getProp().put(
						"imagePositionOnPageLowerLeftX", //$NON-NLS-1$
						stampLocation.getProperty("signaturePositionOnPageLowerLeftX") //$NON-NLS-1$
					);
					getProp().put(
						"imagePositionOnPageLowerLeftY", //$NON-NLS-1$
						stampLocation.getProperty("signaturePositionOnPageLowerLeftY") //$NON-NLS-1$
					);
					getProp().put(
						"imagePositionOnPageUpperRightX", //$NON-NLS-1$
						stampLocation.getProperty("signaturePositionOnPageUpperRightX") //$NON-NLS-1$
					);
					getProp().put(
						"imagePositionOnPageUpperRightY", //$NON-NLS-1$
						stampLocation.getProperty("signaturePositionOnPageUpperRightY") //$NON-NLS-1$
					);
					getProp().put(
						"imagePage", //$NON-NLS-1$
						stampLocation.getProperty("imagePage") //$NON-NLS-1$
					);
				}
				if(this.saveConfig.isSelected()) {
					saveProperties(getProp());
				}
				getListener().positionSelected(getProp());
			}
		);
		this.okButton.addKeyListener(this);
		panel.add(this.okButton);

		final JButton cancelButton = new JButton(SignPdfUiMessages.getString("SignPdfUiPreview.6")); //$NON-NLS-1$
		cancelButton.setMnemonic('C');
		cancelButton.getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfUiPreview.7") //$NON-NLS-1$
		);
		cancelButton.addActionListener(
			e -> getListener().positionCancelled()
		);
		cancelButton.addKeyListener(this);

		// En Mac OS X el orden de los botones es distinto
		if (Platform.OS.MACOSX.equals(Platform.getOS())) {
			panel.add(defaultButton);
			panel.add(cancelButton);
			panel.add(this.okButton);
		}
		else {
			panel.add(defaultButton);
			panel.add(this.okButton);
			panel.add(cancelButton);
		}

		return panel;
	}

	/** Recupera las propiedades de la firma. */
	private void loadProperties() {
		getTextArea().setText(PreferencesManager.get(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2TEXT));

		final String pdfFontIndex = PreferencesManager.get(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTFAMILY);
		int comboFontIndex = 0;
		final FontResource[] fonts = FontResource.getAllFontresources();
		for (int i = 0; i < fonts.length; ++i) {
			if (fonts[i].getPdfFontIndex().equals(pdfFontIndex)) {
				comboFontIndex = i;
			}
		}

		getLetterType().setSelectedIndex(comboFontIndex);

		setSelectedSize(
				Integer.parseInt(PreferencesManager.get(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTSIZE)));

		final int fontStyle = Integer.parseInt(PreferencesManager.get(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTSTYLE));
		if (fontStyle == 8) {
			getStrikethroughButton().doClick();
		} else if (fontStyle == 4) {
			getUnderlineButton().doClick();
		} else if (fontStyle == 3) {
			getItalicButton().doClick();
			getBoldButton().doClick();
		} else if (fontStyle == 2) {
			getItalicButton().doClick();
		} else if (fontStyle == 1) {
			getBoldButton().doClick();
		}

		final String pdfColorIndex = PreferencesManager.get(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTCOLOR);
		int comboColorIndex = 0;
		final ColorResource[] colors = ColorResource.getAllColorResources();
		for (int i = 0; i < colors.length; ++i) {
			if (colors[i].getPdfColorKey().equals(pdfColorIndex)) {
				comboColorIndex = i;
			}
		}
		getColorCombobox().setSelectedItem(colors[comboColorIndex]);
		getColorCombobox().getColorList().setSelectedIndex(comboColorIndex);

		final Font fon = fonts[comboFontIndex].getFont();
		final Map<TextAttribute, Object> atr = (Map<TextAttribute, Object>) getViewFont().getAttributes();
		atr.put(TextAttribute.FAMILY, fon.getFontName());
		setViewFont(getViewFont().deriveFont(atr));

		setInsertImageBase64(PreferencesManager.get(PreferencesManager.PREFERENCE_PDF_SIGN_IMAGE));

		showPreview();
	}

	/** Restaura las propiedades por defecto de la firma. */
	private void loadDefaultProperties() {
		getTextArea().setText(
			PreferencesManager.getDefaultPreference(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2TEXT)
		);

		final String pdfFontIndex =
			PreferencesManager.getDefaultPreference(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTFAMILY);
		int comboFontIndex = 0;
		final FontResource[] fonts = FontResource.getAllFontresources();
		for (int i = 0; i < fonts.length; ++i) {
			if (fonts[i].getPdfFontIndex().equals(pdfFontIndex)) {
				comboFontIndex = i;
			}
		}

		getLetterType().setSelectedIndex(comboFontIndex);

		setSelectedSize(Integer.parseInt(
			PreferencesManager.getDefaultPreference(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTSIZE)
		));

		final int style = Integer.parseInt(
			PreferencesManager.getDefaultPreference(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTSTYLE));
		if (style == 8) {
			getStrikethroughButton().doClick();
		} else if (style == 4) {
			getUnderlineButton().doClick();
		} else if (style == 3) {
			getItalicButton().doClick();
			getBoldButton().doClick();
		} else if (style == 2) {
			getItalicButton().doClick();
		} else if (style == 1) {
			getBoldButton().doClick();
		}

		final String pdfColorIndex =
			PreferencesManager.getDefaultPreference(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTCOLOR);
		int comboColorIndex = 0;
		final ColorResource[] colors = ColorResource.getAllColorResources();
		for (int i = 0; i < colors.length; ++i) {
			if (colors[i].getPdfColorKey().equals(pdfColorIndex)) {
				comboColorIndex = i;
			}
		}
		getColorCombobox().setSelectedItem(colors[comboColorIndex]);
		getColorCombobox().getColorList().setSelectedIndex(comboColorIndex);

		final Font fon = fonts[comboFontIndex].getFont();
		final Map<TextAttribute, Object> atr = (Map<TextAttribute, Object>) getViewFont().getAttributes();
		atr.put(TextAttribute.FAMILY, fon.getFontName());
		setViewFont(getViewFont().deriveFont(atr));

		setInsertImageBase64(PreferencesManager.getDefaultPreference(PreferencesManager.PREFERENCE_PDF_SIGN_IMAGE));

		showPreview();
	}

	/** Almacena las propiedades de la firma.
	 * @param params Colección de propiedades de la firma. */
	private void saveProperties(final Properties params) {

		if (params.getProperty("layer2Text") != null) {
			PreferencesManager.put(
				PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2TEXT, params.getProperty("layer2Text")
			);
			PreferencesManager.put(
				PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTFAMILY, params.getProperty("layer2FontFamily")
			);
			PreferencesManager.put(
				PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTSIZE, params.getProperty("layer2FontSize")
			);
			PreferencesManager.put(
				PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTSTYLE, params.getProperty("layer2FontStyle")
			);
			PreferencesManager.put(
				PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTCOLOR, params.getProperty("layer2FontColor")
			);
		}
		else {
			PreferencesManager.remove(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2TEXT);			
		}
		
		if (params.getProperty("signatureRubricImage") != null) {
			PreferencesManager.put(
				PreferencesManager.PREFERENCE_PDF_SIGN_IMAGE, params.getProperty("signatureRubricImage")
			);
		}
		else {
			PreferencesManager.remove(PreferencesManager.PREFERENCE_PDF_SIGN_IMAGE);
		}
		try {
			PreferencesManager.flush();
		} catch (final Exception e) {
			Logger.getLogger("es.gob.afirma").severe("Error al guardar las preferencias de firma visible PDF: " + e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	 /** Carga la imagen para a&ntilde;adir a la firma.
     * @param file Imagen para a&ntilde;adir.
	 * @throws IOException Si hay problemas en el pintado de la imagen. */
	void loadFile(final File file) throws IOException {

        setCursor(new Cursor(Cursor.WAIT_CURSOR));

        String errorMessage = null;
        if (!file.exists()) {
            errorMessage = SimpleAfirmaMessages.getString("SignPanel.3"); //$NON-NLS-1$
        }
        else if (file.isDirectory()) {
        	errorMessage = SimpleAfirmaMessages.getString("SignPanel.21"); //$NON-NLS-1$
        }
        else if (!file.canRead()) {
            errorMessage = SimpleAfirmaMessages.getString("SignPanel.7"); //$NON-NLS-1$
        }
        else if (file.length() < 1) {
            errorMessage = SimpleAfirmaMessages.getString("SignPanel.5"); //$NON-NLS-1$
        }
        else if (!isValidImage(file.getName())) {
        	errorMessage = SimpleAfirmaMessages.getString("SignPanel.101"); //$NON-NLS-1$
        }
        if (errorMessage != null) {
        	AOUIFactory.showErrorMessage(
        		SignPdfUiPanelPreview.this,
                errorMessage,
                SimpleAfirmaMessages.getString("SimpleAfirma.7"), //$NON-NLS-1$
                JOptionPane.ERROR_MESSAGE
            );
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            return;
        }

        paintImage(file.getAbsolutePath());
        showPreview();
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

	private static boolean isValidImage(final String file) {
		boolean valid = false;
		for (final String ext : IMAGE_EXT) {
			if (file.endsWith(ext)) {
				valid = true;
			}
		}
		return valid;
	}

	String getInsertImageBase64(BufferedImage bi) {
		try (final ByteArrayOutputStream osImage = new ByteArrayOutputStream()) {
			ImageIO.write(bi, "jpg", osImage); //$NON-NLS-1$
			return Base64.encode(osImage.toByteArray());
		}
        catch (final Exception e1) {
        	Logger.getLogger("es.gob.afirma").severe( //$NON-NLS-1$
				"No ha sido posible pasar la imagen a JPG: " + e1 //$NON-NLS-1$
			);
		}
		return null;
	}

	void setInsertImageBase64(final String base64Image) {
		try (final ByteArrayInputStream isImage = new ByteArrayInputStream(Base64.decode(base64Image))) {
			final BufferedImage bi = ImageIO.read(isImage);

			if(bi == null) {
				this.signImage = null;
				return;
			}

			final BufferedImage newImage = new BufferedImage(
			    this.image.getWidth(), this.image.getHeight(), this.image.getType()
			);

			final Graphics2D g = newImage.createGraphics();
			g.drawImage(this.image, 0, 0, null);
			g.drawImage(
				bi.getScaledInstance(this.image.getWidth(), this.image.getHeight(), Image.SCALE_SMOOTH), 0, 0, null
			);
			g.dispose();
			this.signImage = newImage;
		}
		catch (final Exception e1) {
			Logger.getLogger("es.gob.afirma").severe( //$NON-NLS-1$
					"No ha sido posible recuperar la imagen guardada: " + e1 //$NON-NLS-1$
			);
		}
	}

	private void createImage() {
		final BufferedImage bi = new BufferedImage(
			this.image.getWidth(),
			this.image.getHeight(),
            BufferedImage.TYPE_INT_RGB
        );
		final Graphics2D ig2 = bi.createGraphics();
		ig2.setPaint (Color.WHITE);
		ig2.fillRect (0, 0, bi.getWidth(), bi.getHeight());
		ig2.dispose();
		this.image = bi;
	}

	void paintImage(final String path) throws IOException {

		final BufferedImage bi = ImageIO.read(new File(path));

		final BufferedImage newImage = new BufferedImage(
		    this.image.getWidth(), this.image.getHeight(), this.image.getType()
		);

		final Graphics2D g = newImage.createGraphics();
		g.drawImage(this.image, 0, 0, null);
		g.drawImage(bi.getScaledInstance(this.image.getWidth(), this.image.getHeight(), Image.SCALE_SMOOTH), 0, 0, null);
		g.dispose();
		this.signImage = newImage;
	}

	public static String breakLines(final String input, final int maxLineLength, final FontMetrics fm ) {
	    final String[] tokens = input.split(SPLIT_REGEXP);
	    final StringBuilder output = new StringBuilder(input.length());
	    int lineLen = 0;
	    for (int i = 0; i < tokens.length; i++) {
	        String word = tokens[i];

	        if (lineLen + fm.stringWidth(SPACE_SEPARATOR + word) > maxLineLength) {
	            if (i > 0) {
	                output.append(NEWLINE);
	            }
	            lineLen = 0;
	        }
	        if (i < tokens.length - 1 && lineLen + fm.stringWidth(word + SPACE_SEPARATOR + tokens[i + 1]) <= maxLineLength) {
	            word += SPACE_SEPARATOR;
	        }
	        output.append(word);
	        lineLen += fm.stringWidth(word);
	    }
	    return output.toString();
	}

	void showPreview() {
	    final BufferedImage bi = new BufferedImage(
	        this.image.getWidth(),
	        this.image.getHeight(),
	        this.image.getType()
        );
	    final Graphics2D g = bi.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	    if (this.signImage != null) {
		    g.drawImage(this.signImage, 0, 0, null);
	    }
	    else {
		    g.drawImage(this.image, 0, 0, null);
	    }

	    g.setColor(this.colorCombobox.getSelectedItem().getColor());
	    final int scaledSize = Math.max(1,Math.round(getViewFont().getSize() / this.scale) - 3);
	    g.setFont(getViewFont().deriveFont(getStyle(), scaledSize));
	    final int x = 5;
	    int y;
	    if (getProp().getProperty("signatureField") != null) { //$NON-NLS-1$
	    	y = 0;
	    }
	    else {
	    	y = 0;
	    }
	    int textLength;
	    for (final String line : getTextArea().getText().split("\n")) { //$NON-NLS-1$
	    	textLength = g.getFontMetrics().stringWidth(line);
	    	if (textLength > this.image.getWidth()) {
	    		final String lineWrapped = breakLines(line, this.image.getWidth(), g.getFontMetrics());
	    		for (final String s : lineWrapped.split("\n")) { //$NON-NLS-1$
	    			g.drawString(s, x, y += g.getFontMetrics().getHeight());
	    		}
	    	}
	    	else {
	    		g.drawString(line, x, y += g.getFontMetrics().getHeight());
	    	}
		}
	    g.dispose();

	    getViewLabel().setIcon(new ImageIcon(bi));
	}

	private void resizeImage(final JPanel panel) {
		int newWidth = this.image.getWidth();
		int newHeight = this.image.getHeight();

		final Dimension screen = panel.getPreferredSize();
		while (newWidth > screen.width || newHeight > screen.height) {
			newWidth = (int) Math.round(newWidth * 0.9);
			newHeight = (int) Math.round(newHeight * 0.9);
		}
		final float aspectRatio = (float) newWidth / this.image.getWidth();

		newWidth = Math.round(this.image.getWidth() * aspectRatio);
		newHeight = Math.round(this.image.getHeight() * aspectRatio);

		this.scale = this.image.getWidth() / newWidth;
		final Image newIm = this.image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
	    final BufferedImage newBi = new BufferedImage(
    		newWidth,
    		newHeight,
	        this.image.getType()
        );
	    final Graphics g = newBi.createGraphics();
	    g.drawImage(newIm, 0, 0, null);

	    g.dispose();
		this.image = newBi;
	}

	int getStyleIndex() {
		if (getStrikethroughButton().isSelected()) {
			return 8;
		}
		else if (getUnderlineButton().isSelected()) {
			return 4;
		}
		else if (getBoldButton().isSelected() && getItalicButton().isSelected()) {
			return 3;
		}
		else if (getItalicButton().isSelected()) {
			return 2;
		}
		else if (getBoldButton().isSelected()) {
			return 1;
		}
		else {
			return 0;
		}
	}

	void addStyle(final int s) {
		setStyle(getStyle() + s);
	}

	void deleteStyle(final int s) {
		setStyle(getStyle() - s);
	}

	@Override public void keyTyped(final KeyEvent e) { /* vacio */ }
	@Override public void keyReleased(final KeyEvent e) { /* vacio */ }

	@Override
	public void keyPressed(final KeyEvent ke) {
		if (ke != null) {
			if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
				getListener().positionCancelled();
			}
			else if (ke.getKeyCode() == KeyEvent.VK_N && ke.isControlDown()) {
				this.boldButton.doClick();
			}
			else if (ke.getKeyCode() == KeyEvent.VK_K && ke.isControlDown()) {
				this.italicButton.doClick();
			}
			else if (ke.getKeyCode() == KeyEvent.VK_S && ke.isControlDown()) {
				this.underlineButton.doClick();
			}
			else if (ke.getKeyCode() == KeyEvent.VK_T && ke.isControlDown()) {
				this.strikethroughButton.doClick();
			}
			else if (ke.getKeyCode() == KeyEvent.VK_P && ke.isControlDown()) {
				SwingUtilities.invokeLater(() ->  {
		                getViewLabel().dispatchEvent(
	                		new MouseEvent(
		                		 SignPdfUiPanelPreview.this,
			                     MouseEvent.MOUSE_CLICKED,
			                     1,
			                     MouseEvent.BUTTON1,
			                     0, 0,
			                     1,
			                     false
			                )
                		);
		            }
				);
			}
		}
	}

	private final class CustomComboBox extends JPanel {

		private static final long serialVersionUID = 6905237438341447834L;
		private ColorResource color = ColorResource.BLACK;
		final ColorResource[] colorStrings;
		private final JComboBox<Integer> colorList ;
		JComboBox<Integer> getColorList() {
			return this.colorList;
		}
		ImageIcon[] images;


	    ColorResource getSelectedItem() {
	    	return this.color;
	    }

	    void setSelectedItem(final ColorResource c) {
	    	this.color = c;
	    }

	    public CustomComboBox(final ColorResource[] colorResources) {
	        super(new BorderLayout());

	        this.colorStrings = colorResources;
	        this.images = new ImageIcon[this.colorStrings.length];
	        final Integer[] intArray = new Integer[this.colorStrings.length];
	        for (int i = 0; i < this.colorStrings.length; i++) {
	            intArray[i] = Integer.valueOf(i);
	            this.images[i] = new ImageIcon(this.colorStrings[i].getImage());
	            if (this.images[i] != null) {
	                this.images[i].setDescription(this.colorStrings[i].toString());
	            }
	        }

	        this.colorList = new JComboBox<>(intArray);
	        final ComboBoxRenderer renderer = new ComboBoxRenderer();
	        renderer.setPreferredSize(new Dimension(60, 20));
	        this.colorList.setRenderer(renderer);
	        this.colorList.setMaximumRowCount(3);

	        add(this.colorList, BorderLayout.PAGE_START);
	    }

	    private final class ComboBoxRenderer extends JLabel implements ListCellRenderer<Object> {

			private static final long serialVersionUID = -9197195477672319113L;
			private Font uhOhFont;

	        public ComboBoxRenderer() {
	            setOpaque(true);
	            setHorizontalAlignment(LEFT);
	            setVerticalAlignment(CENTER);
	        }


	        @Override
			public Component getListCellRendererComponent(final JList<?> list,
	                                                      final Object value,
	                                                      final int index,
	                                                      final boolean isSelected,
	                                                      final boolean cellHasFocus) {

	            final int selectedIndex = ((Integer)value).intValue();
	            final ImageIcon icon = CustomComboBox.this.images[selectedIndex];
	            final String col = CustomComboBox.this.colorStrings[selectedIndex].toString();
	            if (isSelected) {
	            	setSelectedItem(CustomComboBox.this.colorStrings[selectedIndex]);
	            	showPreview();
	                setBackground(list.getSelectionBackground());
	                setForeground(list.getSelectionForeground());
	            }
	            else {
	                setBackground(list.getBackground());
	                setForeground(list.getForeground());
	            }

	            setBorder(new EmptyBorder(1, 3, 1, 1));
	            setIcon(icon);
	            if (icon != null) {
	                setText(col);
	                setFont(list.getFont());
	            }
	            else {
	                setUhOhText(col + " (no image available)", list.getFont()); //$NON-NLS-1$
	            }

	            return this;
	        }

	        //Fuente cuando no encuentra la imagen
	        protected void setUhOhText(final String uhOhText, final Font normalFont) {
	            if (this.uhOhFont == null) {
	                this.uhOhFont = normalFont.deriveFont(Font.PLAIN);
	            }
	            setFont(this.uhOhFont);
	            setText(uhOhText);
	        }
	    }
	}

	private final class ComboRenderer extends BasicComboBoxRenderer {

		private static final long serialVersionUID = 3706919693882358747L;
		private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
        private int row;

        ComboRenderer() {
			// vacio
		}

		@Override
        public Component getListCellRendererComponent(final JList list,
        		                                      final Object value,
        		                                      final int index,
        		                                      final boolean isSelected,
        		                                      final boolean cellHasFocus) {

        	super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            final JLabel renderer =
            	(JLabel) this.defaultRenderer.getListCellRendererComponent(list, value, this.row, isSelected, cellHasFocus);
            final Object fntObj = value;
            final Font fon = ((FontResource) fntObj).getFont();
            final Font itemFont = new Font(fon.getFontName(), Font.PLAIN, 14);
            final Map<TextAttribute, Object> atr = (Map<TextAttribute, Object>) getViewFont().getAttributes();
			atr.put(TextAttribute.FAMILY, fon.getFontName());
			atr.put(TextAttribute.SIZE, Integer.valueOf(getSelectedSize()));
            final Font newFont = new Font(atr);
			setViewFont(getViewFont().deriveFont(atr));
            getLetterType().setFont(itemFont);
            renderer.setFont(itemFont);
            renderer.setText(fntObj.toString());
            if (isSelected) {
	            setViewFont(newFont);
	            showPreview();
            }
            return renderer;
        }
    }

	class RemoveImPopUpMenu extends JPopupMenu {

		private static final long serialVersionUID = 8491839704433747625L;
		JMenuItem removeImageItem;

		public RemoveImPopUpMenu(){
	        this.removeImageItem = new JMenuItem(SignPdfUiMessages.getString("SignPdfUiPreview.27")); //$NON-NLS-1$
	        this.removeImageItem.addActionListener(
    			e -> {
					setSignImage(null);
					showPreview();
				}
    		);
	        add(this.removeImageItem);
	    }
	}

	class AddImPopUpMenu extends JPopupMenu {

		private static final long serialVersionUID = 8491839704433747625L;
		JMenuItem addImageItem;

		public AddImPopUpMenu(){
	        this.addImageItem = new JMenuItem(SignPdfUiMessages.getString("SignPdfUiPreview.28")); //$NON-NLS-1$
	        this.addImageItem.addActionListener(
    			e -> SwingUtilities.invokeLater(() ->  {
				        getViewLabel().dispatchEvent(
				    		new MouseEvent(
				        		 SignPdfUiPanelPreview.this,
				                 MouseEvent.MOUSE_CLICKED,
				                 1,
				                 MouseEvent.BUTTON1,
				                 0, 0,
				                 1,
				                 false
				            )
						);
				    }
				)
    		);
	        add(this.addImageItem);
	    }
	}

	@Override
	public void positionSelected(Properties extraParams) {
		try {
			stampImage = ImageIO.read(new File(stampImagePath));
			
			if(stampImage != null) {
				this.stampLocation = extraParams;
				this.removeStampButton.setEnabled(true);
			}
		}
		catch (IOException ioe) {
			Logger.getLogger("es.gob.afirma").severe( //$NON-NLS-1$
					"No ha sido posible cargar la imagen: " + ioe //$NON-NLS-1$
			);
			AOUIFactory.showMessageDialog(
				SignPdfUiPanelPreview.this,
				SignPdfUiMessages.getString("SignPdfUiPreview.24"), //$NON-NLS-1$
				SignPdfUiMessages.getString("SignPdfUiPreview.23"), //$NON-NLS-1$
				JOptionPane.ERROR_MESSAGE
			);
		}
	}
	
	@Override
	public void positionCancelled() {/* vacio */}
}
