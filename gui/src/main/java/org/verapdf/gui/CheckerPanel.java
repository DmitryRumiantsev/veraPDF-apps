package org.verapdf.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.verapdf.apps.Applications;
import org.verapdf.apps.Applications.Builder;
import org.verapdf.apps.ConfigManager;
import org.verapdf.apps.ProcessType;
import org.verapdf.apps.VeraAppConfig;
import org.verapdf.gui.tools.GUIConstants;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.validation.profiles.Profiles;
import org.verapdf.pdfa.validation.profiles.ValidationProfile;
import org.verapdf.pdfa.validation.validators.ValidatorConfig;
import org.verapdf.pdfa.validation.validators.ValidatorFactory;
import org.verapdf.processor.ProcessorConfig;
import org.verapdf.processor.TaskType;
import org.verapdf.processor.reports.BatchSummary;

/**
 * Panel with functionality for checker.
 *
 * @author Maksim Bezrukov
 */
class CheckerPanel extends JPanel {
	private static transient ConfigManager config;
	/**
	 * ID for serialisation
	 */
	private static final long serialVersionUID = 1290058869994329766L;

	static final Logger LOGGER = Logger.getLogger(CheckerPanel.class);

	private static final Map<String, PDFAFlavour> FLAVOURS_MAP = new HashMap<>();

	private JFileChooser pdfChooser;
	private JFileChooser xmlChooser;
	private JFileChooser htmlChooser;
	private File pdfFile;
	private JTextField chosenPDF;
	private JTextField chosenProfile;
	private JLabel resultLabel;
	private File xmlReport;
	private File htmlReport;
	private File pdfReport;

	private JComboBox<ProcessType> ProcessTypes;
	private JCheckBox fixMetadata;
	private JComboBox<String> chooseFlavour;

	private boolean isValidationErrorOccurred;

	private JButton execute;
	private JButton saveXML;
	private JButton viewXML;
	private JButton saveHTML;
	private JButton viewHTML;
	private JButton savePDF;
	private JButton viewPDF;


	private transient Path profilePath;

	JProgressBar progressBar;
	transient ValidateWorker validateWorker;

	CheckerPanel(final ConfigManager config) throws IOException {
		CheckerPanel.config = config;
		this.profilePath = FileSystems.getDefault().getPath("");
		setPreferredSize(new Dimension(GUIConstants.PREFERRED_SIZE_WIDTH, GUIConstants.PREFERRED_SIZE_HEIGHT));

		GridBagLayout gbl = new GridBagLayout();
		this.setLayout(gbl);
		GridBagConstraints gbc = new GridBagConstraints();

		this.chosenPDF = new JTextField(GUIConstants.PDF_NOT_CHOSEN_TEXT);
		this.chosenPDF.setEditable(false);
		setGridBagConstraintsParameters(gbc, GUIConstants.CHOSEN_PDF_LABEL_CONSTRAINT_GRID_X,
				GUIConstants.CHOSEN_PDF_LABEL_CONSTRAINT_GRID_Y, GUIConstants.CHOSEN_PDF_LABEL_CONSTRAINT_WEIGHT_X,
				GUIConstants.CHOSEN_PDF_LABEL_CONSTRAINT_WEIGHT_Y, GUIConstants.CHOSEN_PDF_LABEL_CONSTRAINT_GRID_WIDTH,
				GUIConstants.CHOSEN_PDF_LABEL_CONSTRAINT_GRID_HEIGHT, GridBagConstraints.HORIZONTAL);
		gbl.setConstraints(this.chosenPDF, gbc);
		this.add(this.chosenPDF);

		JButton choosePDF = new JButton(GUIConstants.CHOOSE_PDF_BUTTON_TEXT);
		setGridBagConstraintsParameters(gbc, GUIConstants.CHOOSE_PDF_BUTTON_CONSTRAINT_GRID_X,
				GUIConstants.CHOOSE_PDF_BUTTON_CONSTRAINT_GRID_Y, GUIConstants.CHOOSE_PDF_BUTTON_CONSTRAINT_WEIGHT_X,
				GUIConstants.CHOOSE_PDF_BUTTON_CONSTRAINT_WEIGHT_Y,
				GUIConstants.CHOOSE_PDF_BUTTON_CONSTRAINT_GRID_WIDTH,
				GUIConstants.CHOOSE_PDF_BUTTON_CONSTRAINT_GRID_HEIGHT, GridBagConstraints.HORIZONTAL);
		gbl.setConstraints(choosePDF, gbc);
		this.add(choosePDF);

		final JLabel processType = new JLabel(GUIConstants.PROCESSING_TYPE);
		setGridBagConstraintsParameters(gbc, GUIConstants.PROCESS_TYPE_LABEL_CONSTRAINT_GRID_X,
				GUIConstants.PROCESS_TYPE_LABEL_CONSTRAINT_GRID_Y, GUIConstants.PROCESS_TYPE_LABEL_CONSTRAINT_WEIGHT_X,
				GUIConstants.PROCESS_TYPE_LABEL_CONSTRAINT_WEIGHT_Y,
				GUIConstants.PROCESS_TYPE_LABEL_CONSTRAINT_GRID_WIDTH,
				GUIConstants.PROCESS_TYPE_LABEL_CONSTRAINT_GRID_HEIGHT, GridBagConstraints.HORIZONTAL);
		gbl.setConstraints(processType, gbc);
		this.add(processType);

		this.ProcessTypes = new JComboBox<>(ProcessType.getOptionValues());
		this.ProcessTypes.setSelectedItem(config.getApplicationConfig().getProcessType());
		ProcessingTypeRenderer processingTypeRenderer = new ProcessingTypeRenderer();
		this.ProcessTypes.setRenderer(processingTypeRenderer);
		setGridBagConstraintsParameters(gbc, GUIConstants.PROCESSING_TYPE_COMBOBOX_CONSTRAINT_GRID_X,
				GUIConstants.PROCESSING_TYPE_COMBOBOX_CONSTRAINT_GRID_Y,
				GUIConstants.PROCESSING_TYPE_COMBOBOX_CONSTRAINT_WEIGHT_X,
				GUIConstants.PROCESSING_TYPE_COMBOBOX_CONSTRAINT_WEIGHT_Y,
				GUIConstants.PROCESSING_TYPE_COMBOBOX_CONSTRAINT_GRID_WIDTH,
				GUIConstants.PROCESSING_TYPE_COMBOBOX_CONSTRAINT_GRID_HEIGHT, GridBagConstraints.HORIZONTAL);
		gbl.setConstraints(this.ProcessTypes, gbc);
		this.add(this.ProcessTypes);

		this.fixMetadata = new JCheckBox(GUIConstants.FIX_METADATA_LABEL_TEXT);
		this.fixMetadata.setSelected(config.createProcessorConfig().getTasks().contains(TaskType.FIX_METADATA));
		setGridBagConstraintsParameters(gbc, GUIConstants.FIX_METADATA_CHECKBOX_CONSTRAINT_GRID_X,
				GUIConstants.FIX_METADATA_CHECKBOX_CONSTRAINT_GRID_Y,
				GUIConstants.FIX_METADATA_CHECKBOX_CONSTRAINT_WEIGHT_X,
				GUIConstants.FIX_METADATA_CHECKBOX_CONSTRAINT_WEIGHT_Y,
				GUIConstants.FIX_METADATA_CHECKBOX_CONSTRAINT_GRID_WIDTH,
				GUIConstants.FIX_METADATA_CHECKBOX_CONSTRAINT_GRID_HEIGHT, GridBagConstraints.HORIZONTAL);
		gbl.setConstraints(this.fixMetadata, gbc);
		this.add(this.fixMetadata);
		if (config.getApplicationConfig().getProcessType() == ProcessType.EXTRACT) {
			this.fixMetadata.setEnabled(false);
		}

		Vector<String> availableFlavours = new Vector<>();
		availableFlavours.add(GUIConstants.CUSTOM_PROFILE_COMBOBOX_TEXT);
		availableFlavours.add(GUIConstants.AUTO_FLAVOUR_COMBOBOX_TEXT);
		for (PDFAFlavour flavour : PDFAFlavour.values()) {
			Set<PDFAFlavour> currentFlavours = Profiles.getVeraProfileDirectory().getPDFAFlavours();
			if (currentFlavours.contains(flavour)) {
				String flavourReadableText = getFlavourReadableText(flavour);
				availableFlavours.add(flavourReadableText);
				FLAVOURS_MAP.put(flavourReadableText, flavour);
			}
		}
		this.chooseFlavour = new JComboBox<>(availableFlavours);
		this.chooseFlavour.setOpaque(true);
		ChooseFlavourRenderer renderer = new ChooseFlavourRenderer();
		this.chooseFlavour.setRenderer(renderer);
		PDFAFlavour fromConfig = config.createProcessorConfig().getValidatorConfig().getFlavour();
		String fromConfigFlavourText = getFlavourReadableText(fromConfig);
		if (availableFlavours.contains(fromConfigFlavourText)) {
			this.chooseFlavour.setSelectedItem(fromConfigFlavourText);
		} else {
			this.chooseFlavour.setSelectedItem(GUIConstants.AUTO_FLAVOUR_COMBOBOX_TEXT);
		}
		setGridBagConstraintsParameters(gbc, GUIConstants.CHOOSE_FLAVOUR_COMBOBOX_CONSTRAINT_GRID_X,
				GUIConstants.CHOOSE_FLAVOUR_COMBOBOX_CONSTRAINT_GRID_Y,
				GUIConstants.CHOOSE_FLAVOUR_COMBOBOX_CONSTRAINT_WEIGHT_X,
				GUIConstants.CHOOSE_FLAVOUR_COMBOBOX_CONSTRAINT_WEIGHT_Y,
				GUIConstants.CHOOSE_FLAVOUR_COMBOBOX_CONSTRAINT_GRID_WIDTH,
				GUIConstants.CHOOSE_FLAVOUR_COMBOBOX_CONSTRAINT_GRID_HEIGHT, GridBagConstraints.HORIZONTAL);
		gbl.setConstraints(this.chooseFlavour, gbc);
		this.add(this.chooseFlavour);

		this.chosenProfile = new JTextField(GUIConstants.VALIDATION_PROFILE_NOT_CHOSEN);
		this.chosenProfile.setEditable(false);
		this.chosenProfile.setEnabled(false);
		setGridBagConstraintsParameters(gbc, GUIConstants.CHOSEN_PROFILE_LABEL_CONSTRAINT_GRID_X,
				GUIConstants.CHOSEN_PROFILE_LABEL_CONSTRAINT_GRID_Y,
				GUIConstants.CHOSEN_PROFILE_LABEL_CONSTRAINT_WEIGHT_X,
				GUIConstants.CHOSEN_PROFILE_LABEL_CONSTRAINT_WEIGHT_Y,
				GUIConstants.CHOSEN_PROFILE_LABEL_CONSTRAINT_GRID_WIDTH,
				GUIConstants.CHOSEN_PROFILE_LABEL_CONSTRAINT_GRID_HEIGHT, GridBagConstraints.HORIZONTAL);
		gbl.setConstraints(this.chosenProfile, gbc);
		this.add(this.chosenProfile);
		if (!this.profilePath.toString().equals("")) {
			this.chosenProfile.setText(this.profilePath.toString());
		} else {
			this.chosenProfile.setText(GUIConstants.CHOOSEN_PROFILE_TEXTFIELD_DEFAULT_TEXT);
		}

		final JButton chooseProfile = new JButton(GUIConstants.CHOOSE_PROFILE_BUTTON_TEXT);
		chooseProfile.setEnabled(false);
		setGridBagConstraintsParameters(gbc, GUIConstants.CHOOSE_PROFILE_BUTTON_CONSTRAINT_GRID_X,
				GUIConstants.CHOOSE_PROFILE_BUTTON_CONSTRAINT_GRID_Y,
				GUIConstants.CHOOSE_PROFILE_BUTTON_CONSTRAINT_WEIGHT_X,
				GUIConstants.CHOOSE_PROFILE_BUTTON_CONSTRAINT_WEIGHT_Y,
				GUIConstants.CHOOSE_PROFILE_BUTTON_CONSTRAINT_GRID_WIDTH,
				GUIConstants.CHOOSE_PROFILE_BUTTON_CONSTRAINT_GRID_HEIGHT, GridBagConstraints.HORIZONTAL);
		gbl.setConstraints(chooseProfile, gbc);
		this.add(chooseProfile);

		this.resultLabel = new JLabel();
		this.resultLabel.setForeground(GUIConstants.BEFORE_VALIDATION_COLOR);
		this.resultLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		setGridBagConstraintsParameters(gbc, GUIConstants.RESULT_LABEL_CONSTRAINT_GRID_X,
				GUIConstants.RESULT_LABEL_CONSTRAINT_GRID_Y, GUIConstants.RESULT_LABEL_CONSTRAINT_WEIGHT_X,
				GUIConstants.RESULT_LABEL_CONSTRAINT_WEIGHT_Y, GUIConstants.RESULT_LABEL_CONSTRAINT_GRID_WIDTH,
				GUIConstants.RESULT_LABEL_CONSTRAINT_GRID_HEIGHT, GridBagConstraints.CENTER);
		gbl.setConstraints(this.resultLabel, gbc);
		this.add(this.resultLabel);

		this.progressBar = new JProgressBar();
		this.progressBar.setIndeterminate(true);
		this.progressBar.setVisible(false);
		setGridBagConstraintsParameters(gbc, GUIConstants.PROGRESSBAR_CONSTRAINT_GRID_X,
				GUIConstants.PROGRESSBAR_CONSTRAINT_GRID_Y, GUIConstants.PROGRESSBAR_CONSTRAINT_WEIGHT_X,
				GUIConstants.PROGRESSBAR_CONSTRAINT_WEIGHT_Y, GUIConstants.PROGRESSBAR_CONSTRAINT_GRID_WIDTH,
				GUIConstants.PROGRESSBAR_CONSTRAINT_GRID_HEIGHT, GridBagConstraints.HORIZONTAL);
		gbl.setConstraints(this.progressBar, gbc);
		this.add(this.progressBar);

		this.execute = new JButton(GUIConstants.VALIDATE_BUTTON_TEXT);
		this.execute.setEnabled(false);
		setGridBagConstraintsParameters(gbc, GUIConstants.VALIDATE_BUTTON_CONSTRAINT_GRID_X,
				GUIConstants.VALIDATE_BUTTON_CONSTRAINT_GRID_Y, GUIConstants.VALIDATE_BUTTON_CONSTRAINT_WEIGHT_X,
				GUIConstants.VALIDATE_BUTTON_CONSTRAINT_WEIGHT_Y, GUIConstants.VALIDATE_BUTTON_CONSTRAINT_GRID_WIDTH,
				GUIConstants.VALIDATE_BUTTON_CONSTRAINT_GRID_HEIGHT, GridBagConstraints.HORIZONTAL);
		gbl.setConstraints(this.execute, gbc);
		this.add(this.execute);

		JPanel reports = new JPanel();
		reports.setBorder(BorderFactory.createTitledBorder(GUIConstants.REPORT));
		reports.setLayout(
				new GridLayout(GUIConstants.REPORT_PANEL_LINES_NUMBER, GUIConstants.REPORT_PANEL_COLUMNS_NUMBER));
		setGridBagConstraintsParameters(gbc, GUIConstants.REPORT_PANEL_CONSTRAINT_GRID_X,
				GUIConstants.REPORT_PANEL_CONSTRAINT_GRID_Y, GUIConstants.REPORT_PANEL_CONSTRAINT_WEIGHT_X,
				GUIConstants.REPORT_PANEL_CONSTRAINT_WEIGHT_Y, GUIConstants.REPORT_PANEL_CONSTRAINT_GRID_WIDTH,
				GUIConstants.REPORT_PANEL_CONSTRAINT_GRID_HEIGHT, GridBagConstraints.HORIZONTAL);
		gbl.setConstraints(reports, gbc);
		this.add(reports);

		LogoPanel xmlLogo = new LogoPanel(GUIConstants.XML_LOGO_NAME, reports.getBackground(),
				GUIConstants.XML_LOGO_BORDER_WIDTH);
		reports.add(xmlLogo);

		this.saveXML = new JButton(GUIConstants.SAVE_REPORT_BUTTON_TEXT);
		this.saveXML.setEnabled(false);
		reports.add(this.saveXML);

		this.viewXML = new JButton(GUIConstants.VIEW_REPORT_BUTTON_TEXT);
		this.viewXML.setEnabled(false);
		reports.add(this.viewXML);

		LogoPanel htmlLogo = new LogoPanel(GUIConstants.HTML_LOGO_NAME, reports.getBackground(),
				GUIConstants.HTML_LOGO_BORDER_WIDTH);
		reports.add(htmlLogo);

		this.saveHTML = new JButton(GUIConstants.SAVE_HTML_REPORT_BUTTON_TEXT);
		this.saveHTML.setEnabled(false);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		reports.add(this.saveHTML);

		this.viewHTML = new JButton(GUIConstants.VIEW_HTML_REPORT_BUTTON_TEXT);
		this.viewHTML.setEnabled(false);
		reports.add(this.viewHTML);

		LogoPanel pdfLogo = new LogoPanel(GUIConstants.PDF_LOGO_NAME, reports.getBackground(),
				GUIConstants.PDF_LOGO_BORDER_WIDTH);
		reports.add(pdfLogo);

		this.savePDF = new JButton(GUIConstants.SAVE_PDF_REPORT_BUTTON_TEXT);
		this.savePDF.setEnabled(false);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		reports.add(this.savePDF);

		this.viewPDF = new JButton(GUIConstants.VIEW_PDF_REPORT_BUTTON_TEXT);
		this.viewPDF.setEnabled(false);
		reports.add(this.viewPDF);

		this.pdfChooser = getChooser(GUIConstants.PDF);
		this.pdfChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		this.xmlChooser = getChooser(GUIConstants.XML);
		this.htmlChooser = getChooser(GUIConstants.HTML);

		choosePDF.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CheckerPanel.this.chooseFile(CheckerPanel.this.pdfChooser, GUIConstants.PDF);
			}
		});

		this.ProcessTypes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ProcessType item = (ProcessType) CheckerPanel.this.ProcessTypes.getSelectedItem();
				switch (item) {
					case VALIDATE:
						CheckerPanel.this.fixMetadata.setEnabled(true);
						break;
					case EXTRACT:
						CheckerPanel.this.fixMetadata.setSelected(false);
						CheckerPanel.this.fixMetadata.setEnabled(false);
						break;
					case VALIDATE_EXTRACT:
						CheckerPanel.this.fixMetadata.setEnabled(true);
						break;
					default:
						break;
				}
			}
		});

		this.chooseFlavour.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (CheckerPanel.this.chooseFlavour.getSelectedItem().equals(GUIConstants.CUSTOM_PROFILE_COMBOBOX_TEXT)) {
					chooseProfile.setEnabled(true);
					CheckerPanel.this.chosenProfile.setEnabled(true);
				} else {
					chooseProfile.setEnabled(false);
					CheckerPanel.this.chosenProfile.setEnabled(false);
				}
				CheckerPanel.this.execute.setEnabled(isExecute());
			}
		});

		chooseProfile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CheckerPanel.this.chooseFile(CheckerPanel.this.xmlChooser, GUIConstants.XML);
			}
		});

		this.execute.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					changeConfig();
					ValidationProfile customProfile = null;
					if (CheckerPanel.this.chooseFlavour.getSelectedItem().equals(GUIConstants.CUSTOM_PROFILE_COMBOBOX_TEXT)) {
						customProfile = Profiles.profileFromXml(new FileInputStream(CheckerPanel.this.profilePath.toFile()));
					}
					CheckerPanel.this.validateWorker = new ValidateWorker(CheckerPanel.this, CheckerPanel.this.pdfFile,
							CheckerPanel.config, customProfile);
					CheckerPanel.this.progressBar.setVisible(true);
					CheckerPanel.this.resultLabel.setVisible(false);
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					CheckerPanel.this.execute.setEnabled(false);
					CheckerPanel.this.isValidationErrorOccurred = false;
					CheckerPanel.this.viewXML.setEnabled(false);
					CheckerPanel.this.saveXML.setEnabled(false);
					CheckerPanel.this.viewHTML.setEnabled(false);
					CheckerPanel.this.saveHTML.setEnabled(false);
					CheckerPanel.this.viewPDF.setEnabled(false);
					CheckerPanel.this.savePDF.setEnabled(false);
					CheckerPanel.this.validateWorker.execute();
				} catch (IllegalArgumentException | JAXBException | IOException exep) {
					JOptionPane.showMessageDialog(CheckerPanel.this, exep.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
					LOGGER.error(exep);
				}
			}
		});

		this.saveXML.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveReport(CheckerPanel.this.xmlChooser, GUIConstants.XML, CheckerPanel.this.xmlReport);
			}
		});

		this.saveHTML.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
			saveReport(CheckerPanel.this.htmlChooser, GUIConstants.HTML, CheckerPanel.this.htmlReport);
			}
		});
		this.savePDF.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

			saveReport(CheckerPanel.this.pdfChooser, GUIConstants.PDF, CheckerPanel.this.pdfReport);
			}
		});
		this.viewXML.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (CheckerPanel.this.xmlReport == null) {
					JOptionPane.showMessageDialog(CheckerPanel.this, "XML report hasn't been saved.",
							GUIConstants.ERROR, JOptionPane.ERROR_MESSAGE);
				} else {
					this.openXMLReport();
				}
			}

			private void openXMLReport() {
				try {
					Desktop.getDesktop().open(CheckerPanel.this.xmlReport);
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(CheckerPanel.this, "Some error in opening the XML report.",
							GUIConstants.ERROR, JOptionPane.ERROR_MESSAGE);
					LOGGER.error("Exception in opening the XML report", e1);
				}
			}
		});

		this.viewHTML.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (CheckerPanel.this.htmlReport == null) {
					JOptionPane.showMessageDialog(CheckerPanel.this, "HTML report hasn't been saved.",
							GUIConstants.ERROR, JOptionPane.ERROR_MESSAGE);
				} else {
					try {
						Desktop.getDesktop().open(CheckerPanel.this.htmlReport);
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(CheckerPanel.this, "Some error in opening the HTML report.",
								GUIConstants.ERROR, JOptionPane.ERROR_MESSAGE);
						LOGGER.error("Exception in opening the HTML report", e1);
					}
				}
			}
		});
		this.viewPDF.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (CheckerPanel.this.pdfReport == null) {
					JOptionPane.showMessageDialog(CheckerPanel.this, "PDF report hasn't been saved.",
							GUIConstants.ERROR, JOptionPane.ERROR_MESSAGE);
				} else {
					try {
						Desktop.getDesktop().open(CheckerPanel.this.pdfReport);
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(CheckerPanel.this, "Some error in opening the PDF report.",
								GUIConstants.ERROR, JOptionPane.ERROR_MESSAGE);
						LOGGER.error("Exception in opening the PDF report", e1);
					}
				}
			}
		});

	}


	void validationEnded(File xmlReport, File htmlReport,File pdfReport) {

		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		this.progressBar.setVisible(false);
		this.execute.setEnabled(true);

		if (!this.isValidationErrorOccurred) {
			try {
				BatchSummary result = this.validateWorker.get();
				if (result.getJobs() == 1) {
					if (result.getValidPdfaCount() > 0) {
						this.resultLabel.setForeground(GUIConstants.VALIDATION_SUCCESS_COLOR);
						this.resultLabel.setText(GUIConstants.VALIDATION_OK);
					} else if (result.getInvalidPdfaCount() > 0) {
								this.resultLabel.setForeground(GUIConstants.VALIDATION_FAILED_COLOR);
								this.resultLabel.setText(GUIConstants.VALIDATION_FALSE);
					} else if (result.getValidationExceptionCount() == 1) {
							this.resultLabel.setForeground(GUIConstants.VALIDATION_FAILED_COLOR);
							this.resultLabel.setText(GUIConstants.ERROR_IN_VALIDATING);
					} else if (result.getFeatureCount() > 0) {
							this.resultLabel.setForeground(GUIConstants.BEFORE_VALIDATION_COLOR);
							this.resultLabel.setText(GUIConstants.FEATURES_GENERATED_CORRECT);
					} else {
							this.resultLabel.setForeground(GUIConstants.VALIDATION_FAILED_COLOR);
							this.resultLabel.setText(GUIConstants.ERROR_IN_FEATURES);
					}
				} else {
					this.resultLabel.setForeground(GUIConstants.BEFORE_VALIDATION_COLOR);
					this.resultLabel.setText("Items processed: " + result.getJobs()
							+ ",   Succeed: " + (result.getJobs() - result.getFailedJobs())
							+ ",   Failed: " + result.getFailedJobs());
				}
				this.resultLabel.setVisible(true);

				this.xmlReport = xmlReport;
				this.htmlReport = htmlReport;
				this.pdfReport = pdfReport;
				if (xmlReport != null) {
					this.saveXML.setEnabled(true);
					this.viewXML.setEnabled(true);
				}

				if (htmlReport != null) {
					this.saveHTML.setEnabled(true);
					this.viewHTML.setEnabled(true);
				}
				if(pdfReport != null){
					this.savePDF.setEnabled(true);
					this.viewPDF.setEnabled(true);
				}
			} catch (InterruptedException e) {
				errorInValidatingOccur("Process has been interrupted: ", e);
			} catch (ExecutionException e) {
				errorInValidatingOccur("Execution exception in processing: ", e);
			}
		}

	}

	void errorInValidatingOccur(String message, Throwable e) {
		LOGGER.error(e);
		e.printStackTrace();
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		this.progressBar.setVisible(false);
		this.isValidationErrorOccurred = true;
		JOptionPane.showMessageDialog(CheckerPanel.this, message + e.getMessage(), GUIConstants.ERROR,
				JOptionPane.ERROR_MESSAGE);

		this.resultLabel.setForeground(GUIConstants.VALIDATION_FAILED_COLOR);
		this.resultLabel.setText(message + e.getMessage());
		this.resultLabel.setVisible(true);
	}

	private static JFileChooser getChooser(String type) throws IOException {
		JFileChooser res = new JFileChooser();
		File currentDir = new File(new File(GUIConstants.DOT).getCanonicalPath());
		res.setCurrentDirectory(currentDir);
		res.setAcceptAllFileFilterUsed(false);
		res.setFileFilter(new FileNameExtensionFilter(type, type));
		return res;
	}

	private static void setGridBagConstraintsParameters(GridBagConstraints gbc, int gridx, int gridy, int weightx,
														int weighty, int gridwidth, int gridheight, int fill) {
		gbc.gridx = gridx;
		gbc.gridy = gridy;
		gbc.weightx = weightx;
		gbc.weighty = weighty;
		gbc.gridwidth = gridwidth;
		gbc.gridheight = gridheight;
		gbc.fill = fill;
	}

	private void chooseFile(JFileChooser chooser, String extension) {
		int resultChoose = chooser.showOpenDialog(CheckerPanel.this);
		if (resultChoose == JFileChooser.APPROVE_OPTION) {

			File selectedFile = chooser.getSelectedFile();
			if (!selectedFile.exists()) {
				JOptionPane.showMessageDialog(CheckerPanel.this, "Error. Selected file doesn't exist.",
						GUIConstants.ERROR, JOptionPane.ERROR_MESSAGE);
			} else if (selectedFile.isFile() && !selectedFile.getName().toLowerCase()
					.endsWith(GUIConstants.DOT + extension.toLowerCase())) {
				JOptionPane.showMessageDialog(CheckerPanel.this,
						"Error. Selected file is not in " + extension.toUpperCase() + " format.", GUIConstants.ERROR,
						JOptionPane.ERROR_MESSAGE);
			} else {

				this.resultLabel.setForeground(GUIConstants.BEFORE_VALIDATION_COLOR);
				this.resultLabel.setText("");
				this.xmlReport = null;
				this.htmlReport = null;
				this.pdfReport = null;
				this.saveXML.setEnabled(false);
				this.viewXML.setEnabled(false);
				this.saveHTML.setEnabled(false);
				this.viewHTML.setEnabled(false);
				this.savePDF.setEnabled(false);
				this.viewPDF.setEnabled(false);

				switch (extension) {
					case GUIConstants.PDF:
						this.pdfFile = selectedFile;
						this.chosenPDF.setText(this.pdfFile.getAbsolutePath());
						break;
					case GUIConstants.XML:
						this.profilePath = selectedFile.toPath().toAbsolutePath();
						this.chosenProfile.setText(this.profilePath.toString());
						break;
					default:
						// This method used only for previous two cases.
						// So do nothing.
				}
				this.execute.setEnabled(isExecute());
			}
		}
	}

	private void saveReport(JFileChooser chooser, String extension, File report) {
		if (report == null) {
			JOptionPane.showMessageDialog(CheckerPanel.this, "Validation hasn't been run.", GUIConstants.ERROR,
					JOptionPane.ERROR_MESSAGE);
		} else {
			chooser.setSelectedFile(new File(extension.toLowerCase() + "Report." + extension.toLowerCase()));
			int resultChoose = chooser.showSaveDialog(CheckerPanel.this);
			if (resultChoose == JFileChooser.APPROVE_OPTION) {
				File temp = chooser.getSelectedFile();

				if (!(temp.getName().toLowerCase().endsWith(GUIConstants.DOT + extension.toLowerCase()))) {
					temp = new File(temp.getPath() + GUIConstants.DOT + extension.toLowerCase());
				}

				try {
					try {
						Files.copy(report.toPath(), temp.toPath());
					} catch (FileAlreadyExistsException e1) {
						LOGGER.debug("File already exists, conform overwrite with user", e1);
						int resultOption = JOptionPane.showConfirmDialog(CheckerPanel.this,
								extension.toUpperCase()
										+ " file with the same name already exists. Do you want to overwrite it?",
								"", JOptionPane.YES_NO_OPTION);
						if (resultOption == JOptionPane.YES_OPTION) {
							Files.copy(report.toPath(), temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
					}
				} catch (IOException e) {
					JOptionPane.showMessageDialog(CheckerPanel.this,
							GUIConstants.ERROR_IN_SAVING_HTML_REPORT + e.getMessage(), GUIConstants.ERROR,
							JOptionPane.ERROR_MESSAGE);
					LOGGER.error("Exception saving " + extension.toUpperCase() + " report", e);
				}
			}
		}
	}

	private void changeConfig() throws JAXBException, IOException {
		if (!this.chooseFlavour.getSelectedItem().equals(GUIConstants.CUSTOM_PROFILE_COMBOBOX_TEXT)) {
			this.profilePath = FileSystems.getDefault().getPath("");
		}
		PDFAFlavour flavour = getCurrentFlavour();
		ValidatorConfig validatorConfig = config.getValidatorConfig();
		ValidatorConfig currentConfig = ValidatorFactory.createConfig(flavour, validatorConfig.isRecordPasses(), validatorConfig.getMaxFails());
		config.updateValidatorConfig(currentConfig);
		config.updateAppConfig(appConfigFromState());
	}

	VeraAppConfig appConfigFromState() {
		Builder builder = Applications
				.createConfigBuilder(CheckerPanel.config.getApplicationConfig());
		ProcessType selectedItem = (ProcessType) this.ProcessTypes.getSelectedItem();
		if (isFixMetadata()) {
			selectedItem = ProcessType.addProcess(selectedItem, ProcessType.FIX);
		}
		builder.type(selectedItem);
		return builder.build();
	}
	
	private PDFAFlavour getCurrentFlavour() {
		String selectedItem = (String) this.chooseFlavour.getSelectedItem();
		PDFAFlavour flavour = FLAVOURS_MAP.get(selectedItem);
		return flavour == null ? PDFAFlavour.NO_FLAVOUR : flavour;
	}

	private boolean isExecute() {
		return (this.pdfFile != null && (!this.profilePath.toString().equals("")
				|| !this.chooseFlavour.getSelectedItem().equals(GUIConstants.CUSTOM_PROFILE_COMBOBOX_TEXT)));
	}

	boolean isFixMetadata() {
		return this.fixMetadata.isSelected();
	}

	ProcessType getProcessingType() {
		return (ProcessType) this.ProcessTypes.getSelectedItem();
	}

	private String getFlavourReadableText(PDFAFlavour flavour) {
		if (flavour.toString().matches("\\d\\w")) {
			String valueString = flavour.toString();
			String parsedFlavour = "PDF/A-";
			parsedFlavour += valueString.charAt(0);
			parsedFlavour += valueString.substring(1, 2).toUpperCase();
			return parsedFlavour;
		} else {
			return "Error in parsing flavour";
		}
	}

	private class ChooseFlavourRenderer extends JLabel implements ListCellRenderer<String> {

		/**
		 *
		 */
		private static final long serialVersionUID = 3740801661593829099L;

		public ChooseFlavourRenderer() {
			setOpaque(true);
			setHorizontalAlignment(CENTER);
			setVerticalAlignment(CENTER);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
													  boolean isSelected, boolean cellHasFocus) {
			this.setText(value);
			return this;
		}
	}

	private class ProcessingTypeRenderer extends JLabel implements ListCellRenderer<ProcessType> {

		/**
		 *
		 */
		private static final long serialVersionUID = -2841316639915833315L;

		public ProcessingTypeRenderer() {
			setOpaque(true);
			setHorizontalAlignment(CENTER);
			setVerticalAlignment(CENTER);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends ProcessType> list, ProcessType value, int index,
													  boolean isSelected, boolean cellHasFocus) {
			this.setText(value.getValue());
			return this;
		}
	}
}
