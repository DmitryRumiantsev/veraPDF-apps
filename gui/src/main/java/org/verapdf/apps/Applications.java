/**
 * 
 */
package org.verapdf.apps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import javax.xml.bind.JAXBException;

import org.verapdf.processor.FormatOption;

/**
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
 *         <a href="https://github.com/carlwilson">carlwilson AT github</a>
 * @version 0.1 Created 31 Oct 2016:13:15:23
 */

public final class Applications {
	public static final String APP_HOME_PROPERTY = "app.home";
	public static final String DEFAULT_CONFIG_ROOT_NAME = "config";
	private Applications() {
		assert (false);
	}

	public static ConfigManager createConfigManager(final File root) {
		if (root == null)
			throw new NullPointerException("Arg root cannot be null");
		if ((!root.isDirectory() && !root.mkdir()) || !root.canWrite()) {
			throw new IllegalArgumentException(
					"Arg root:" + root.getAbsolutePath() + ", must be a writable directory.");
		}
		return ConfigManagerImpl.create(root);
	}

	public static ConfigManager createAppConfigManager() {
		try {
			return createConfigManager(appHomeRoot());
		} catch (IOException excep) {
			throw new IllegalStateException("Can't find a writeable temp file system.", excep);
		}
	}

	public static ConfigManager createTmpConfigManager() {
		try {
			return createConfigManager(tempRoot());
		} catch (IOException excep) {
			throw new IllegalStateException("Can't find a writeable temp file system.", excep);
		}
	}

	public static VeraAppConfig defaultConfig() {
		return VeraAppConfigImpl.defaultInstance();
	}

	public static Applications.Builder defaultConfigBuilder() {
		return Applications.Builder.defaultBuilder();
	}

	public static Applications.Builder createConfigBuilder(VeraAppConfig base) {
		return Applications.Builder.fromConfig(base);
	}

	public static String toXml(final VeraAppConfig toConvert, Boolean prettyXml) throws JAXBException, IOException {
		return VeraAppConfigImpl.toXml(toConvert, prettyXml);
	}

	public static void toXml(final VeraAppConfig toConvert, final OutputStream stream, Boolean prettyXml)
			throws JAXBException {
		VeraAppConfigImpl.toXml(toConvert, stream, prettyXml);
	}

	public static VeraAppConfigImpl fromXml(final InputStream toConvert) throws JAXBException {
		return VeraAppConfigImpl.fromXml(toConvert);
	}

	public static void toXml(final VeraAppConfig toConvert, final Writer writer, Boolean prettyXml)
			throws JAXBException {
		VeraAppConfigImpl.toXml(toConvert, writer, prettyXml);
	}

	public static VeraAppConfigImpl fromXml(final Reader toConvert) throws JAXBException {
		return VeraAppConfigImpl.fromXml(toConvert);
	}

	public static VeraAppConfigImpl fromXml(final String toConvert) throws JAXBException {
		return VeraAppConfigImpl.fromXml(toConvert);
	}

	public static class Builder {
		private ProcessType _type = ProcessType.VALIDATE;
		private int _maxFails = 100;
		private boolean _isOverwrite = false;
		private String _fixerFolder = FileSystems.getDefault().getPath("").toString();
		private FormatOption _format = FormatOption.MRR;
		private String _wikiPath = "https://github.com/veraPDF/veraPDF-validation-profiles/wiki/";
		private String _reportFile = FileSystems.getDefault().getPath("").toString();
		private String _reportFolder = FileSystems.getDefault().getPath("").toString();
		private String _policyFile = FileSystems.getDefault().getPath("").toString();
		private String _pluginsFolder = FileSystems.getDefault().getPath("").toString();
		private boolean _isVerbose = false;

		private Builder() {
			super();
		}

		private Builder(VeraAppConfig config) {
			super();
			this._type = config.getProcessType();
			this._maxFails = config.getMaxFailsDisplayed();
			this._isOverwrite = config.isOverwriteReport();
			this._fixerFolder = config.getFixesFolder();
			this._format = config.getFormat();
			this._isVerbose = config.isVerbose();
			this._wikiPath = config.getWikiPath();
			this._reportFile = config.getReportFile();
			this._reportFolder = config.getReportFolder();
			this._policyFile = config.getPolicyFile();
			this._pluginsFolder = config.getPluginsFolder();
		}

		public Builder type(ProcessType type) {
			this._type = type;
			return this;
		}

		public Builder maxFails(int maxFails) {
			this._maxFails = maxFails;
			return this;
		}

		public Builder overwrite(boolean overwrite) {
			this._isOverwrite = overwrite;
			return this;
		}

		public Builder fixerFolder(String fixerFold) {
			this._fixerFolder = fixerFold;
			return this;
		}

		public Builder format(FormatOption format) {
			this._format = format;
			return this;
		}

		public Builder isVerbose(boolean isVerbose) {
			this._isVerbose = isVerbose;
			return this;
		}

		public Builder wikiPath(String path) {
			this._wikiPath = path;
			return this;
		}

		public Builder reportFile(String report) {
			this._reportFile = report;
			return this;
		}

		public Builder reportFolder(String reports) {
			this._reportFolder = reports;
			return this;
		}

		public Builder policyFile(String policy) {
			this._policyFile = policy;
			return this;
		}

		public Builder pluginsFolder(String plugins) {
			this._pluginsFolder = plugins;
			return this;
		}

		public static Builder fromConfig(VeraAppConfig config) {
			return new Builder(config);
		}

		public static Builder defaultBuilder() {
			return new Builder();
		}

		public VeraAppConfig build() {
			return new VeraAppConfigImpl(_type, _maxFails, _isOverwrite, _fixerFolder, _format, _isVerbose,
					_wikiPath, _reportFile, _reportFolder, _policyFile, _pluginsFolder);
		}
	}

	private static File appHomeRoot() throws IOException {
		String appHome = System.getProperty(APP_HOME_PROPERTY);
		if (appHome != null) {
			File user = new File(appHome);
			File f = new File(user, DEFAULT_CONFIG_ROOT_NAME);
			if (f.exists() || f.mkdir()) {
				return f;
			}
		}
		return tempRoot();
	}

	private static File tempRoot() throws IOException {
		File temp = Files.createTempDirectory("").toFile();
		temp.deleteOnExit();
		return temp;
	}
}
