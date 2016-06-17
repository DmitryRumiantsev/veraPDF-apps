/**
 *
 */
package org.verapdf.cli;

import org.apache.log4j.Logger;
import org.verapdf.cli.commands.VeraCliArgParser;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.processor.Processor;
import org.verapdf.processor.ProcessorImpl;
import org.verapdf.processor.config.Config;
import org.verapdf.processor.config.ConfigIO;
import org.verapdf.processor.config.ProcessingType;
import org.verapdf.report.ItemDetails;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.List;
import java.lang.StringBuilder;

/**
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
 *
 */
final class VeraPdfCliProcessor {

    private static final Logger LOGGER = Logger.getLogger(VeraPdfCliProcessor.class);

    final boolean recurse;

    private Config config;
	
	private String baseDirectory = "";

    private VeraPdfCliProcessor() throws IOException {
        this(new VeraCliArgParser());
    }

    private VeraPdfCliProcessor(final VeraCliArgParser args)
            throws IOException {
        this.recurse = args.isRecurse();


        if (args.isLoadingConfig()) {
            try {
                config = ConfigIO.readConfig();
            } catch (IOException e) {
                LOGGER.error("Can not read config file. Using default config", e);
                this.config = new Config();
            } catch (JAXBException e) {
                LOGGER.error("Cannot parse config XML. Using default config", e);
                this.config = new Config();
            }
        } else {
            config = new Config();
            config.setShowPassedRules(args.logPassed());
            config.setMaxNumberOfFailedChecks(args.maxFailures());
            config.setMaxNumberOfDisplayedFailedChecks(args.maxFailuresDisplayed());
            config.setMetadataFixerPrefix(args.prefix());
            config.setFixMetadataPathFolder(FileSystems.getDefault().getPath(args.saveFolder()));
            config.setProfileWikiPath(args.getProfilesWikiPath());
            config.setFixMetadata(args.fixMetadata());
            config.setProcessingType(processingTypeFromArgs(args));
            config.setReportType(args.getFormat());
            config.setValidationProfilePath(
                    args.getProfileFile() == null ?
                            FileSystems.getDefault().getPath("") : args.getProfileFile().toPath());
            config.setFlavour(args.getFlavour());
            config.setVerboseCli(args.isVerbose());
            config.setPluginsEnabled(args.isPluginsEnabled());
            config.setReportFolderPath(args.getReportFolder());
            config.setReportFilePath(args.getReportFile());
        }
        
        if (!config.getReportFolder().isEmpty() && !config.getReportFile().isEmpty()) {
            LOGGER.error("Report folder and report file defined together, switching to STDOUT.");
            config.setReportFolderPath("");
            config.setReportFilePath("");
        }
    }

    public Config getConfig() {
        return config;
    }

    static ProcessingType processingTypeFromArgs(final VeraCliArgParser args) {
        boolean isValidating = args.getFlavour() != PDFAFlavour.NO_FLAVOUR;
        return ProcessingType.getType(isValidating, args.extractFeatures());
    }

    void processPaths(final List<String> pdfPaths) {
        // If the path list is empty then
        if (pdfPaths.isEmpty()) {
            ItemDetails item = ItemDetails.fromValues("STDIN");
            processStream(item, System.in);
        }

        for (String pdfPath : pdfPaths) {
			File file = new File(pdfPath);
            if (file.isDirectory()) {
				baseDirectory = pdfPath;
                processDir(file);
            } else {
                processFile(file);
            }
        }
    }

    static VeraPdfCliProcessor createProcessorFromArgs(
            final VeraCliArgParser args) throws FileNotFoundException,
            IOException {
        return new VeraPdfCliProcessor(args);
    }

    private void processDir(final File dir) {
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                int extIndex = file.getName().lastIndexOf(".");
                String ext = file.getName().substring(extIndex + 1);
                if ("pdf".equalsIgnoreCase(ext)) {
                    processFile(file);
                }
            } else if (file.isDirectory()) {
                if (this.recurse) {
                    processDir(file);
                }
            }
        }
    }

    private void processFile(final File pdfFile) {
        if (checkFileCanBeProcessed(pdfFile)) {
            try (InputStream toProcess = new FileInputStream(pdfFile)) {
                processStream(ItemDetails.fromFile(pdfFile), toProcess);
            } catch (IOException e) {
                System.err.println("Exception raised while processing "
                        + pdfFile.getAbsolutePath());
                e.printStackTrace();
            }
        }
    }

    private static boolean checkFileCanBeProcessed(final File file) {
        if (!file.isFile()) {
            System.err.println("Path " + file.getAbsolutePath()
                    + " is not an existing file.");
            return false;
        } else if (!file.canRead()) {
            System.err.println("Path " + file.getAbsolutePath()
                    + " is not a readable file.");
            return false;
        }
        return true;
    }

    private void processStream(final ItemDetails item,
                               final InputStream toProcess) {
        Processor processor = new ProcessorImpl();
        OutputStream outputReportStream = System.out;
        boolean stdOut = true;
        
        if (!config.getReportFolder().isEmpty()) {
            Path fileAbsolutePath = Paths.get(item.getName());
            String pdfFileName = fileAbsolutePath.getFileName().toString();
			String pdfFileDirectory = fileAbsolutePath.getParent().toString();
            String extension = "." + config.getReportType().toString();
            String outputFileName = pdfFileName.replace(".pdf", extension);
			String reportFolder = config.getReportFolder();
            
			if (pdfFileDirectory.length() > baseDirectory.length()) {
				StringBuilder reportFolderBuilder = new StringBuilder();
				reportFolderBuilder.append(reportFolder);
			
				String subDirectory = pdfFileDirectory.substring(baseDirectory.length());
				reportFolderBuilder.append(File.separator);
				reportFolderBuilder.append(subDirectory);
				
				reportFolder = reportFolderBuilder.toString();
				
				File dir = new File(reportFolder);
				
				if (!dir.exists()) {
					try {
						dir.mkdir();
					}
					catch (SecurityException ex) {
						LOGGER.error("Cannot create subdirectories the: " + ex.toString() + "\n");
						reportFolder = config.getReportFolder();
					}
				}
			}
			
            File outputFile = new File(reportFolder, outputFileName);
            try {
                outputReportStream = new FileOutputStream(outputFile);
                stdOut = false;
            }
            catch (FileNotFoundException ex) {
                outputReportStream = System.out;
                stdOut = true;
            }
        }
        
        if (!config.getReportFile().isEmpty()) {
            try {
                outputReportStream = new FileOutputStream(config.getReportFile(), true);
                stdOut = false;
            }
            catch (FileNotFoundException ex) {
                outputReportStream = System.out;
                stdOut = true;
            }
        }
        
        processor.validate(toProcess, item, this.config, outputReportStream);
        
        if (stdOut == false) {
            try {
                outputReportStream.close();
            }
            catch (IOException ex) {
                LOGGER.error("Cannot close the report file: " + ex.toString() + "\n");
            }
        }
    }
}
