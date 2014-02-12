package mx.com.anzen.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.types.AntFilterReader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.XmlWriterUtil;

/**
 * Write Ant build files from <code>Maven Project</code> for <a
 * href="http://ant.apache.org">Ant</a> 1.6.2 or above:
 * <ul>
 * <li>build.xml</li>
 * <li>build.properties</li>
 * </ul>
 **/

public class AntBuildWriter {
	/**
	 * Logger from SystemStreamLog.
	 */
	private static Log log = new SystemStreamLog();
	/**
	 * The default line indenter
	 */
	protected static final int DEFAULT_INDENTATION_SIZE = XmlWriterUtil.DEFAULT_INDENTATION_SIZE;
	/**
	 * The default build file name (build.xml)
	 */
	protected static final String DEFAULT_BUILD_FILENAME = Main.DEFAULT_BUILD_FILENAME;

	/**
	 * The default build properties file name
	 */
	protected static final String DEFAULT_MAVEN_PROPERTIES_FILENAME = "build.properties";

	/**
	 * Final name for the package.
	 */
	protected static final String antBuildFinalName = "build.finalName";
	protected static final String antBuildDir = "build.dir";
	protected static final String antBuildOutputDir = "build.outputDir";
	protected static final String antBuildSrcDir = "build.srcDir.";
	protected static final String antBuildResourceDir = "build.resourceDir.";
	protected static final String antBuildTestOutputDir = "build.testOutputDir";
	protected static final String antBuildTestDir = "build.testDir.";
	protected static final String antBuildTestResourceDir = "build.testResourceDir.";
	protected static final String antTestReports = "test.reports";
	protected static final String antReportingOutputDirectory = "reporting.outputDirectory";
	protected static final String antSettingsOffline = "settings.offline";
	protected static final String antSettingsInteractiveMode = "settings.interactiveMode";
	protected static final String antTestSkip = "test.skip";
	protected static final String antJavadocDir = "javadoc.dir";
	protected static final String AntBuildDir = "build";
	protected static final String antParentDir = "parent.dir";
	protected static final String antBuildModuleDir = "build.module.dir";
	protected static final String antBuildClasspathDir = "build.classpath.dir";
	protected static final String antBaseDir = "base.dir";
	protected static final String FS = File.separator;
	/**
	 * The pom.xml descriptor.
	 */
	private MavenProject project;

	private ArtifactResolverWrapper artifactResolverWrapper;

	private File localRepository;

	private Settings settings;
	/**
	 * Root directory of ant distribution.
	 */
	private String rootDirectory;
	/**
	 * Library directory.
	 */
	private String libDirectory;

	/**
	 * Web App directory
	 */
	private String webappDirectory;
	/**
	 * Mapping for restoring files to original strucure.
	 */
	private List<FilesMapping> restoreMapping;

	private boolean overwrite;

	private Properties executionProperties;

	/**
	 * @param project
	 * @param artifactResolverWrapper
	 * @param settings
	 * @param overwrite
	 */
	public AntBuildWriter(MavenProject project, ArtifactResolverWrapper artifactResolverWrapper, Settings settings, boolean overwrite, Properties executionProperties, String rootDirectory,
			String libDirectory) {
		this.project = project;
		this.artifactResolverWrapper = artifactResolverWrapper;
		this.localRepository = new File(artifactResolverWrapper.getLocalRepository().getBasedir());
		this.settings = settings;
		this.overwrite = overwrite;
		this.executionProperties = (executionProperties != null) ? executionProperties : new Properties();
		this.rootDirectory = rootDirectory;
		this.libDirectory = libDirectory;
	}

	/**
	 * Generate Ant build XML files
	 * 
	 * @throws IOException
	 */
	protected void writeBuildXmls(List<FilesMapping> restoreMapping, String webappDirectory) throws IOException {
		this.restoreMapping = restoreMapping;
		this.webappDirectory = webappDirectory;
		writeGeneratedBuildXml();
	}

	/**
	 * Generate <code>build.properties</code> only for a non-POM project
	 * 
	 * @see #DEFAULT_MAVEN_PROPERTIES_FILENAME
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	protected void writeBuildProperties() throws IOException {
		if (AntBuildWriterUtil.isPomPackaging(project)) {
			return;
		}

		Properties properties = new Properties();

		// ----------------------------------------------------------------------
		// Build properties
		// ----------------------------------------------------------------------

		// build.finalName
		addProperty(properties, antBuildFinalName, AntBuildWriterUtil.toRelative(project.getBasedir(), project.getBuild().getFinalName()));

		// build
		addProperty(properties, antBuildDir, AntBuildWriterUtil.toRelative(project.getBasedir(), AntBuildDir));

		// parent.dir
		addProperty(properties, antParentDir, getParentSubPath());

		// build.module.dir
		addProperty(properties, antBuildModuleDir, getModuleDir());

		// base.dir
		addProperty(properties, antBaseDir, getBaseDir());

		// build.outputDir
		addProperty(properties, antBuildOutputDir, "${" + antBaseDir + "}" + FS + AntBuildWriterUtil.toRelative(project.getBasedir(), project.getBuild().getOutputDirectory()));

		// build.srcDir.
		if (!project.getCompileSourceRoots().isEmpty()) {
			String[] compileSourceRoots = (String[]) project.getCompileSourceRoots().toArray(new String[0]);
			for (int i = 0; i < compileSourceRoots.length; i++) {
				addProperty(properties, antBuildSrcDir + i, "${" + antBaseDir + "}" + FS + AntBuildWriterUtil.toRelative(project.getBasedir(), compileSourceRoots[i]));
			}
		}

		// build.resourceDir.
		if (project.getBuild().getResources() != null) {
			Resource[] array = (Resource[]) project.getBuild().getResources().toArray(new Resource[0]);
			for (int i = 0; i < array.length; i++) {
				addProperty(properties, antBuildResourceDir + i, "${" + antBaseDir + "}" + FS + AntBuildWriterUtil.toRelative(project.getBasedir(), array[i].getDirectory()));
			}
		}

		// ${build.dir}/test-classes
		addProperty(properties, antBuildTestOutputDir, "${" + antBaseDir + "}" + FS + AntBuildWriterUtil.toRelative(project.getBasedir(), project.getBuild().getTestOutputDirectory()));

		// src/test/java
		if (!project.getTestCompileSourceRoots().isEmpty()) {
			String[] compileSourceRoots = (String[]) project.getTestCompileSourceRoots().toArray(new String[0]);
			for (int i = 0; i < compileSourceRoots.length; i++) {
				addProperty(properties, antBuildTestDir + i, "${" + antBaseDir + "}" + FS + AntBuildWriterUtil.toRelative(project.getBasedir(), compileSourceRoots[i]));
			}
		}

		// build.testResourceDir.
		if (project.getBuild().getTestResources() != null) {
			Resource[] array = (Resource[]) project.getBuild().getTestResources().toArray(new Resource[0]);
			for (int i = 0; i < array.length; i++) {
				addProperty(properties, antBuildTestResourceDir + i, "${" + antBaseDir + "}" + FS + AntBuildWriterUtil.toRelative(project.getBasedir(), array[i].getDirectory()));
			}
		}

		// test.reports
		addProperty(properties, antTestReports, "${" + antParentDir + "}javadocs${" + antBuildModuleDir + "}"+FS+"test-reports");

		// javadoc.dir
		addProperty(properties, antJavadocDir, "${" + antParentDir + "}javadocs${" + antBuildModuleDir + "}"+FS+"javadoc");

		// reporting.outputDirectory
		String reportingOutputDir = project.getReporting().getOutputDirectory();
		if (!new File(reportingOutputDir).isAbsolute()) {
			reportingOutputDir = new File(project.getBasedir(), reportingOutputDir).getAbsolutePath();
		}
		addProperty(properties, antReportingOutputDirectory,
				"${" + antParentDir + "}${" + antBuildModuleDir + "}" + AntBuildWriterUtil.toRelative(new File(project.getBuild().getDirectory()), reportingOutputDir));

		// build.classpath.dir
		addProperty(properties, antBuildClasspathDir, "${" + antParentDir + "}" + libDirectory+ "${" + antBuildModuleDir + "}");

		// ----------------------------------------------------------------------
		// Settings properties
		// ----------------------------------------------------------------------
		addProperty(properties, antSettingsOffline, String.valueOf(settings.isOffline()));
		addProperty(properties, antSettingsInteractiveMode, String.valueOf(settings.isInteractiveMode()));

		// ----------------------------------------------------------------------
		// Project properties
		// ----------------------------------------------------------------------

		if (project.getProperties() != null) {
			Set<Entry<Object, Object>> set = project.getProperties().entrySet();
			for (Entry<Object, Object> entry : set) {
				addProperty(properties, entry.getKey().toString(), entry.getValue().toString());
			}
		}

		FileOutputStream os = new FileOutputStream(getBulidFile(DEFAULT_MAVEN_PROPERTIES_FILENAME));
		try {
			properties.store(os, "Generated by Anzen Ant Plugin - DO NOT EDIT THIS FILE!");
		} finally {
			IOUtil.close(os);
		}
	}

	private File getBulidFile(String nameFile) {
		File f = null;
		if (project.hasParent()) {
			String aux = project.getParent().getFile().getParent();
			aux += File.separator + rootDirectory + File.separator + "configFiles";
			aux += File.separator + project.getFile().getParentFile().getName();
			f = new File(aux);
		} else {
			f = new File(project.getBasedir(), rootDirectory);
		}
		f.mkdirs();
		return new File(f, nameFile);
	}

	/**
	 * Generate an <code>build.xml</code>
	 * 
	 * @see #DEFAULT_MAVEN_BUILD_FILENAME
	 * @throws IOException
	 */
	private void writeGeneratedBuildXml() throws IOException {

		String encoding = "UTF-8";
		String docType = null;// "project [<ENTITY include SYSTEM \"../common/commonProsa.xml\">]";
		File outputFile = getBulidFile(DEFAULT_BUILD_FILENAME);
		OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(outputFile), encoding);
		XMLWriter writer = new PrettyPrintXMLWriter(w, StringUtils.repeat(" ", DEFAULT_INDENTATION_SIZE), encoding, docType);

		// <project/>
		writeAntProject(writer);

		// <property/>
		writeProperties(writer);

		// <path/>
		writeBuildPathDefinition(writer);

		// <target name="clean" />
		writeCleanTarget(writer);

		// -----------------------------------------------------------------------
		// <target name="setup"/>
		// -----------------------------------------------------------------------
		writeSetupTarget(writer);

		// <target name="compile" />
		List compileSourceRoots = AntBuildWriterUtil.removeEmptyCompileSourceRoots(project.getCompileSourceRoots());
		writeCompileTarget(writer, compileSourceRoots);

		// ----------------------------------------------------------------------
		// <target name="compile-tests" />
		// ----------------------------------------------------------------------

		List testCompileSourceRoots = AntBuildWriterUtil.removeEmptyCompileSourceRoots(project.getTestCompileSourceRoots());
		writeCompileTestsTarget(writer, testCompileSourceRoots);

		// ----------------------------------------------------------------------
		// <target name="test" />
		// ----------------------------------------------------------------------

		writeTestTargets(writer, testCompileSourceRoots);

		// ----------------------------------------------------------------------
		// <target name="javadoc" />
		// ----------------------------------------------------------------------
		writeJavadocTarget(writer);

		// ----------------------------------------------------------------------
		// <target name="package" />
		// ----------------------------------------------------------------------
		writePackageTarget(writer);

		// ----------------------------------------------------------------------
		// <target name="get-deps" />
		// ----------------------------------------------------------------------
		// writeGetDepsTarget(writer);

		XmlWriterUtil.writeLineBreak(writer);

		writer.endElement(); // project

		XmlWriterUtil.writeLineBreak(writer);

		IOUtil.close(w);
	}

	private void writeAntProject(XMLWriter writer) {
		writer.startElement("project");
		writer.addAttribute("name", project.getArtifactId());
		writer.addAttribute("default", "package");
		writer.addAttribute("basedir", ".");
		XmlWriterUtil.writeLineBreak(writer);
	}

	/**
	 * Write properties in the writer only for a non-POM project.
	 * 
	 * @param writer
	 */
	@SuppressWarnings("unchecked")
	private void writeProperties(XMLWriter writer) {

		XmlWriterUtil.writeCommentText(writer, "Build environment properties", 1);

		// build.finalName
		writer.startElement("property");
		writer.addAttribute("name", antBuildFinalName);
		writer.addAttribute("value", project.getBuild().getFinalName());
		writer.endElement(); // property

		// build.dir
		writer.startElement("property");
		writer.addAttribute("name", antBuildDir);
		writer.addAttribute("value", AntBuildWriterUtil.toRelative(project.getBasedir(), AntBuildDir));
		writer.endElement(); // property

		XmlWriterUtil.writeLineBreak(writer, 2, 1);

		if (AntBuildWriterUtil.isPomPackaging(project)) {
			return;
		}

		// parent.dir
		writer.startElement("property");
		writer.addAttribute("name", "build.parentName");
		writer.addAttribute("value", getParentName());
		writer.endElement(); // property

		// File properties to override local properties.
		// include build.properties file
		writer.startElement("property");
		writer.addAttribute("file", DEFAULT_MAVEN_PROPERTIES_FILENAME);
		writer.endElement(); // property

		// parent.dir
		writer.startElement("property");
		writer.addAttribute("name", antParentDir);
		writer.addAttribute("value", getParentSubPath());
		writer.endElement(); // property

		// build.module.dir
		writer.startElement("property");
		writer.addAttribute("name", antBuildModuleDir);
		writer.addAttribute("value", getModuleDir());
		writer.endElement(); // property

		// base.dir
		writer.startElement("property");
		writer.addAttribute("name", "base.dir");
		writer.addAttribute("value", getBaseDir());
		writer.endElement(); // property

		// build.outputDir
		writer.startElement("property");
		writer.addAttribute("name", antBuildOutputDir);
		writer.addAttribute("value", "${" + antBaseDir + "}" + FS + AntBuildWriterUtil.toRelative(project.getBasedir(), project.getBuild().getOutputDirectory()));
		writer.endElement(); // property

		// build.srcDir.
		if (!project.getCompileSourceRoots().isEmpty()) {
			String[] compileSourceRoots = (String[]) project.getCompileSourceRoots().toArray(new String[0]);
			for (int i = 0; i < compileSourceRoots.length; i++) {
				writer.startElement("property");
				writer.addAttribute("name", antBuildSrcDir + i);
				writer.addAttribute("value", "${" + antBaseDir + "}" + FS + AntBuildWriterUtil.toRelative(project.getBasedir(), compileSourceRoots[i]));
				writer.endElement(); // property
			}
		}

		// build.resourceDir.
		if (project.getBuild().getResources() != null) {
			Resource[] array = (Resource[]) project.getBuild().getResources().toArray(new Resource[0]);
			for (int i = 0; i < array.length; i++) {
				writer.startElement("property");
				writer.addAttribute("name", antBuildResourceDir + i);
				writer.addAttribute("value", "${" + antBaseDir + "}" + FS + AntBuildWriterUtil.toRelative(project.getBasedir(), array[i].getDirectory()));
				writer.endElement(); // property
			}
		}

		// ${build.dir}/target/test-clases
		writer.startElement("property");
		writer.addAttribute("name", antBuildTestOutputDir);
		writer.addAttribute("value", "${" + antBaseDir + "}" + FS + AntBuildWriterUtil.toRelative(project.getBasedir(), project.getBuild().getTestOutputDirectory()));
		writer.endElement(); // property

		// build.testDir.
		if (!project.getTestCompileSourceRoots().isEmpty()) {
			String[] compileSourceRoots = (String[]) project.getTestCompileSourceRoots().toArray(new String[0]);
			for (int i = 0; i < compileSourceRoots.length; i++) {
				writer.startElement("property");
				writer.addAttribute("name", antBuildTestDir + i);
				writer.addAttribute("value", "${" + antBaseDir + "}" + FS + AntBuildWriterUtil.toRelative(project.getBasedir(), compileSourceRoots[i]));
				writer.endElement(); // property
			}
		}

		// build.testResourceDir.
		if (project.getBuild().getTestResources() != null) {
			Resource[] array = (Resource[]) project.getBuild().getTestResources().toArray(new Resource[0]);
			for (int i = 0; i < array.length; i++) {
				writer.startElement("property");
				writer.addAttribute("name", antBuildTestResourceDir + i);
				writer.addAttribute("value", "${" + antBaseDir + "}" + FS + AntBuildWriterUtil.toRelative(project.getBasedir(), array[i].getDirectory()));
				writer.endElement(); // property
			}
		}

		// test.reports
		writer.startElement("property");
		writer.addAttribute("name", antTestReports);
		writer.addAttribute("value", "${" + antParentDir + "}javadocs${" + antBuildModuleDir + "}" + FS + "test-reports");
		writer.endElement(); // property

		// javadoc.dir
		writer.startElement("property");
		writer.addAttribute("name", antJavadocDir);
		writer.addAttribute("value", "${" + antParentDir + "}javadocs${" + antBuildModuleDir + "}" + FS + "javadoc");
		writer.endElement(); // property

		// reporting.outputDirectory
		String reportingOutputDir = project.getReporting().getOutputDirectory();
		if (!new File(reportingOutputDir).isAbsolute()) {
			reportingOutputDir = new File(project.getBasedir(), reportingOutputDir).getAbsolutePath();
		}
		writer.startElement("property");
		writer.addAttribute("name", antReportingOutputDirectory);
		writer.addAttribute("value", "${" + antParentDir + "}${" + antBuildModuleDir + "}" + AntBuildWriterUtil.toRelative(new File(project.getBuild().getDirectory()), reportingOutputDir));
		writer.endElement(); // property

		// build.classpath.dir
		writer.startElement("property");
		writer.addAttribute("name", antBuildClasspathDir);
		writer.addAttribute("value", "${" + antParentDir + "}" + libDirectory + "${" + antBuildModuleDir + "}");
		writer.endElement(); // property

		// ----------------------------------------------------------------------
		// Setting properties
		// ----------------------------------------------------------------------

		XmlWriterUtil.writeLineBreak(writer, 2, 1);

		writer.startElement("property");
		writer.addAttribute("name", antSettingsOffline);
		writer.addAttribute("value", String.valueOf(settings.isOffline()));
		writer.endElement(); // property

		writer.startElement("property");
		writer.addAttribute("name", antSettingsInteractiveMode);
		writer.addAttribute("value", String.valueOf(settings.isInteractiveMode()));
		writer.endElement(); // property

		XmlWriterUtil.writeLineBreak(writer);
	}

	private String getParentName(){
		if(project.hasParent()){
			return project.getParent().getBuild().getFinalName();
		}
		return project.getBuild().getFinalName();
	}
	
	private String getParentSubPath() {
		if (project.hasParent()) {
			return ".." + FS + ".." + FS + ".." + FS;
		}
		return "." + FS;
	}

	private String getBaseDir() {
		// if (!project.hasParent()) {
		return "${" + antParentDir + "}${" + antBuildDir + "}" + FS + "${build.parentName}${" + antBuildModuleDir + "}";
		// }
		// return ".." + FS + ".." + FS + ".." + FS;
	}

	private String getModuleDir() {
		if (project.hasParent()) {
			return FS + project.getFile().getParentFile().getName();
		}
		return "";
	}

	/**
	 * Write path definition in the writer only for a non-POM project.
	 * 
	 * @param writer
	 */
	private void writeBuildPathDefinition(XMLWriter writer) {
		if (AntBuildWriterUtil.isPomPackaging(project)) {
			return;
		}

		XmlWriterUtil.writeCommentText(writer, "Defining classpaths", 1);

		writeBuildPathDefinition(writer, "build.classpath", project.getCompileArtifacts());

		writeBuildPathDefinition(writer, "build.test.classpath", project.getTestArtifacts());

		XmlWriterUtil.writeLineBreak(writer);
	}

	private void writeBuildPathDefinition(XMLWriter writer, String id, List artifacts) {

		writer.startElement("path");
		writer.addAttribute("id", id);
		writer.startElement("fileset");
		writer.addAttribute("dir", "${" + antBuildClasspathDir + "}");
		writer.startElement("include");
		writer.addAttribute("name", "*.jar");
		writer.endElement();// include
		writer.endElement();// fileset
		writer.endElement();// path
	}

	private String getUninterpolatedSystemPath(Artifact artifact) {
		String managementKey = artifact.getDependencyConflictId();

		for (Iterator it = project.getOriginalModel().getDependencies().iterator(); it.hasNext();) {
			Dependency dependency = (Dependency) it.next();
			if (managementKey.equals(dependency.getManagementKey())) {
				return dependency.getSystemPath();
			}
		}

		for (Iterator itp = project.getOriginalModel().getProfiles().iterator(); itp.hasNext();) {
			Profile profile = (Profile) itp.next();
			for (Iterator it = profile.getDependencies().iterator(); it.hasNext();) {
				Dependency dependency = (Dependency) it.next();
				if (managementKey.equals(dependency.getManagementKey())) {
					return dependency.getSystemPath();
				}
			}
		}

		String path = artifact.getFile().getAbsolutePath();

		Properties props = new Properties();
		props.putAll(project.getProperties());
		props.putAll(executionProperties);
		props.remove("user.dir");
		props.put("basedir", project.getBasedir().getAbsolutePath());

		SortedMap candidateProperties = new TreeMap();
		for (Iterator it = props.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			String value = new File(props.getProperty(key)).getPath();
			if (path.startsWith(value) && value.length() > 0) {
				candidateProperties.put(value, key);
			}
		}
		if (!candidateProperties.isEmpty()) {
			String value = candidateProperties.lastKey().toString();
			String key = candidateProperties.get(value).toString();
			path = path.substring(value.length());
			path = path.replace('\\', '/');
			return "${" + key + "}" + path;
		}

		return path;
	}

	/**
	 * Write clean target in the writer depending the packaging of the project.
	 * 
	 * @param writer
	 */
	private void writeCleanTarget(XMLWriter writer) {
		XmlWriterUtil.writeCommentText(writer, "Cleaning up target", 1);

		writer.startElement("target");
		writer.addAttribute("name", "clean");
		writer.addAttribute("description", "Clean the output directory");

		if (AntBuildWriterUtil.isPomPackaging(project)) {
			if (project.getModules() != null) {
				List<String> modules = project.getModules();
				for (String moduleSubPath : modules) {
					AntBuildWriterUtil.writeAntTask(writer, project, moduleSubPath, "clean");
				}
			}
		} else {
			writer.startElement("delete");
			writer.addAttribute("dir", "${" + antBuildDir + "}");
			writer.endElement(); // delete
		}

		writer.endElement(); // target

		XmlWriterUtil.writeLineBreak(writer);
	}

	/**
	 * Write compile target in the writer depending the packaging of the
	 * project.
	 * 
	 * @param writer
	 * @param compileSourceRoots
	 * @throws IOException
	 */
	private void writeCompileTarget(XMLWriter writer, List compileSourceRoots) throws IOException {
		XmlWriterUtil.writeCommentText(writer, "Compilation target", 1);

		if (AntBuildWriterUtil.isPomPackaging(project)) {
			writer.startElement("target");
			writer.addAttribute("name", "compile");
			writer.addAttribute("description", "Compile the code");
			if (project.getModules() != null) {
				for (Iterator it = project.getModules().iterator(); it.hasNext();) {
					String moduleSubPath = (String) it.next();
					AntBuildWriterUtil.writeAntTask(writer, project, moduleSubPath, "compile");
				}
			}
			writer.endElement(); // target
		} else {
			writer.startElement("target");
			writer.addAttribute("name", "compile");
			writer.addAttribute("description", "Compile the code");

			writeCompileTasks(writer, "${" + antBuildOutputDir + "}", compileSourceRoots, project.getBuild().getResources(), null, false);

			writer.endElement(); // target
		}

		XmlWriterUtil.writeLineBreak(writer);
	}

	/**
	 * Write compile-test target in the writer depending the packaging of the
	 * project.
	 * 
	 * @param writer
	 * @param testCompileSourceRoots
	 * @throws IOException
	 */
	private void writeCompileTestsTarget(XMLWriter writer, List testCompileSourceRoots) throws IOException {
		XmlWriterUtil.writeCommentText(writer, "Test-compilation target", 1);

		if (AntBuildWriterUtil.isPomPackaging(project)) {
			writer.startElement("target");
			writer.addAttribute("name", "compile-tests");
			writer.addAttribute("description", "Compile the test code");
			if (project.getModules() != null) {
				List<String> modules = project.getModules();
				for (String moduleSubPath : modules) {
					AntBuildWriterUtil.writeAntTask(writer, project, moduleSubPath, "compile-tests");
				}
			}
			writer.endElement(); // target
		} else {
			writer.startElement("target");
			writer.addAttribute("name", "compile-tests");
			AntBuildWriterUtil.addWrapAttribute(writer, "target", "depends", "compile", 2);
			AntBuildWriterUtil.addWrapAttribute(writer, "target", "description", "Compile the test code", 2);
			AntBuildWriterUtil.addWrapAttribute(writer, "target", "unless", antTestSkip, 2);

			writeCompileTasks(writer, "${" + antBuildTestOutputDir + "}", testCompileSourceRoots, project.getBuild().getTestResources(), "${" + antBuildOutputDir + "}", true);

			writer.endElement(); // target
		}

		XmlWriterUtil.writeLineBreak(writer);
	}

	/**
	 * Write test target in the writer depending the packaging of the project.
	 * 
	 * @param writer
	 * @param testCompileSourceRoots
	 */
	private void writeTestTargets(XMLWriter writer, List testCompileSourceRoots) throws IOException {
		XmlWriterUtil.writeCommentText(writer, "Run all tests", 1);

		if (AntBuildWriterUtil.isPomPackaging(project)) {
			writer.startElement("target");
			writer.addAttribute("name", "test");
			writer.addAttribute("description", "Run the test cases");
			if (project.getModules() != null) {
				for (Iterator it = project.getModules().iterator(); it.hasNext();) {
					String moduleSubPath = (String) it.next();
					AntBuildWriterUtil.writeAntTask(writer, project, moduleSubPath, "test");
				}
			}
			writer.endElement(); // target
		} else {
			writer.startElement("target");
			writer.addAttribute("name", "test");
			AntBuildWriterUtil.addWrapAttribute(writer, "target", "depends", "compile-tests, junit-missing", 2);
			AntBuildWriterUtil.addWrapAttribute(writer, "target", "unless", "junit.skipped", 2);
			AntBuildWriterUtil.addWrapAttribute(writer, "target", "description", "Run the test cases", 2);

			if (!testCompileSourceRoots.isEmpty()) {
				writer.startElement("mkdir");
				writer.addAttribute("dir", "${" + antTestReports + "}");
				writer.endElement(); // mkdir

				writer.startElement("junit");
				writer.addAttribute("printSummary", "yes");
				writer.addAttribute("haltonerror", "true");
				writer.addAttribute("haltonfailure", "true");
				writer.addAttribute("fork", "true");
				writer.addAttribute("dir", ".");

				writer.startElement("sysproperty");
				writer.addAttribute("key", "basedir");
				writer.addAttribute("value", ".");
				writer.endElement(); // sysproperty

				writer.startElement("formatter");
				writer.addAttribute("type", "xml");
				writer.endElement(); // formatter

				writer.startElement("formatter");
				writer.addAttribute("type", "plain");
				writer.addAttribute("usefile", "false");
				writer.endElement(); // formatter

				writer.startElement("classpath");
				writer.startElement("path");
				writer.addAttribute("refid", "build.test.classpath");
				writer.endElement(); // path
				writer.startElement("pathelement");
				writer.addAttribute("location", "${" + antBuildOutputDir + "}");
				writer.endElement(); // pathelement
				writer.startElement("pathelement");
				writer.addAttribute("location", "${" + antBuildTestOutputDir + "}");
				writer.endElement(); // pathelement
				writer.endElement(); // classpath

				writer.startElement("batchtest");
				writer.addAttribute("todir", "${" + antTestReports + "}");
				writer.addAttribute("unless", "test");

				List includes = getTestIncludes();
				List excludes = getTestExcludes();

				writeTestFilesets(writer, testCompileSourceRoots, includes, excludes);

				writer.endElement(); // batchtest

				writer.startElement("batchtest");
				writer.addAttribute("todir", "${" + antTestReports + "}");
				writer.addAttribute("if", "test");

				includes = Arrays.asList(new String[] { "**/${test}.java" });

				writeTestFilesets(writer, testCompileSourceRoots, includes, excludes);

				writer.endElement(); // batchtest

				writer.endElement(); // junit
			}
			writer.endElement(); // target

			XmlWriterUtil.writeLineBreak(writer, 2, 1);

			writer.startElement("target");
			writer.addAttribute("name", "test-junit-present");

			writer.startElement("available");
			writer.addAttribute("classname", "junit.framework.Test");
			writer.addAttribute("property", "junit.present");
			writer.endElement(); // available

			writer.endElement(); // target

			XmlWriterUtil.writeLineBreak(writer, 2, 1);

			writer.startElement("target");
			writer.addAttribute("name", "test-junit-status");
			AntBuildWriterUtil.addWrapAttribute(writer, "target", "depends", "test-junit-present", 2);
			writer.startElement("condition");
			writer.addAttribute("property", "junit.missing");
			writer.startElement("and");
			writer.startElement("isfalse");
			writer.addAttribute("value", "${junit.present}");
			writer.endElement(); // isfalse
			writer.startElement("isfalse");
			writer.addAttribute("value", "${" + antTestSkip + "}");
			writer.endElement(); // isfalse
			writer.endElement(); // and
			writer.endElement(); // condition
			writer.startElement("condition");
			writer.addAttribute("property", "junit.skipped");
			writer.startElement("or");
			writer.startElement("isfalse");
			writer.addAttribute("value", "${junit.present}");
			writer.endElement(); // isfalse
			writer.startElement("istrue");
			writer.addAttribute("value", "${" + antTestSkip + "}");
			writer.endElement(); // istrue
			writer.endElement(); // or
			writer.endElement(); // condition
			writer.endElement(); // target

			XmlWriterUtil.writeLineBreak(writer, 2, 1);

			writer.startElement("target");
			writer.addAttribute("name", "junit-missing");
			AntBuildWriterUtil.addWrapAttribute(writer, "target", "depends", "test-junit-status", 2);
			AntBuildWriterUtil.addWrapAttribute(writer, "target", "if", "junit.missing", 2);

			writer.startElement("echo");
			writer.writeText(StringUtils.repeat("=", 35) + " WARNING " + StringUtils.repeat("=", 35));
			writer.endElement(); // echo

			writer.startElement("echo");
			writer.writeText(" JUnit is not present in your $ANT_HOME/lib directory. Tests not executed.");
			writer.endElement(); // echo

			writer.startElement("echo");
			writer.writeText(StringUtils.repeat("=", 79));
			writer.endElement(); // echo

			writer.endElement(); // target
		}

		XmlWriterUtil.writeLineBreak(writer);
	}

	/**
	 * Gets the include patterns for the unit tests.
	 * 
	 * @return A list of strings with include patterns, might be empty but never
	 *         <code>null</code>.
	 */
	private List getTestIncludes() throws IOException {
		List includes = getSelectorList(AntBuildWriterUtil.getMavenSurefirePluginOptions(project, "includes", null));
		if (includes == null || includes.isEmpty()) {
			includes = Arrays.asList(new String[] { "**/Test*.java", "**/*Test.java", "**/*TestCase.java" });
		}
		return includes;
	}

	/**
	 * Gets the exclude patterns for the unit tests.
	 * 
	 * @return A list of strings with exclude patterns, might be empty but never
	 *         <code>null</code>.
	 */
	private List getTestExcludes() throws IOException {
		List excludes = getSelectorList(AntBuildWriterUtil.getMavenSurefirePluginOptions(project, "excludes", null));
		if (excludes == null || excludes.isEmpty()) {
			excludes = Arrays.asList(new String[] { "**/*Abstract*Test.java" });
		}
		return excludes;
	}

	/**
	 * Write the <code>&lt;fileset&gt;</code> elements for the test compile
	 * source roots.
	 * 
	 * @param writer
	 * @param testCompileSourceRoots
	 * @param includes
	 * @param excludes
	 */
	private void writeTestFilesets(XMLWriter writer, List testCompileSourceRoots, List includes, List excludes) {
		for (int i = 0; i < testCompileSourceRoots.size(); i++) {
			writer.startElement("fileset");
			writer.addAttribute("dir", "${" + antBuildTestDir + i + "}");
			// TODO: m1 allows additional test exclusions via
			// maven.ant.excludeTests
			AntBuildWriterUtil.writeIncludesExcludes(writer, includes, excludes);
			writer.endElement(); // fileset
		}
	}

	private void writeSetupTarget(XMLWriter writer) throws IOException {

		XmlWriterUtil.writeCommentText(writer, "Setup target", 1);

		writer.startElement("target");
		writer.addAttribute("name", "setup");
		writer.addAttribute("description", "Setup file system.");

		// If there isn't nothing to setup set an ant target dummy.
		if (restoreMapping == null) {
			writer.startElement("echo");
			writer.writeText("Nothing to setup.");
			writer.endElement();// echo
			writer.endElement();// target
			return;
		}

		for (FilesMapping fm : restoreMapping) {
			File target = new File(new File(project.getBasedir(), rootDirectory), fm.getDestinationDirectory());
			File origin = new File(project.getBasedir(), fm.getSourceDirectory());
			writer.startElement("copy");
			if (target.isDirectory() && origin.isDirectory()) {
				writer.addAttribute("todir", "${" + antBuildDir + "}" + File.separator + "${" + antBuildFinalName + "}" + File.separator + fm.getSourceDirectory());
				writer.startElement("fileset");
				writer.addAttribute("dir", fm.getDestinationDirectory());
				writer.startElement("include");
				writer.addAttribute("name", "**");
				writer.endElement();// include
				writer.endElement();// fileset
			} else if (target.isDirectory() && origin.isFile()) {
				writer.addAttribute("file", fm.getDestinationDirectory() + File.separator + origin.getName());
				writer.addAttribute("toFile", "${" + antBuildDir + "}" + File.separator + "${" + antBuildFinalName + "}" + File.separator + fm.getSourceDirectory());
				writer.addAttribute("overwrite", "true");
			} else if (target.isFile() && origin.isFile()) {
				writer.addAttribute("file", fm.getDestinationDirectory());
				writer.addAttribute("toFile", "${" + antBuildDir + "}" + File.separator + "${" + antBuildFinalName + "}" + File.separator + fm.getSourceDirectory());
				writer.addAttribute("overwrite", "true");
			}
			writer.endElement();// copy
		}

		if (AntBuildWriterUtil.isPomPackaging(project)) {
			String fp = File.separator;
			if (project.getModules() != null) {
				List<String> modules = project.getModules();
				for (String moduleSubPath : modules) {
					writer.startElement("copy");
					writer.addAttribute("file", "configFiles" + fp + moduleSubPath + fp + "build.xml");
					writer.addAttribute("toFile", "${" + antBuildDir + "}" + fp + "${" + antBuildFinalName + "}" + fp + moduleSubPath + fp + "build.xml");
					writer.endElement();
					writer.startElement("copy");
					writer.addAttribute("file", "configFiles" + fp + moduleSubPath + fp + "build.properties");
					writer.addAttribute("toFile", "${" + antBuildDir + "}" + fp + "${" + antBuildFinalName + "}" + fp + moduleSubPath + fp + "build.properties");
					writer.endElement();
				}
			}
		}
		writer.endElement();// target
	}

	/**
	 * Write javadoc target in the writer depending the packaging of the
	 * project.
	 * 
	 * @param writer
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void writeJavadocTarget(XMLWriter writer) throws IOException {
		XmlWriterUtil.writeCommentText(writer, "Javadoc target", 1);

		writer.startElement("target");
		writer.addAttribute("name", "javadoc");
		writer.addAttribute("description", "Generates the Javadoc of the application");

		if (AntBuildWriterUtil.isPomPackaging(project)) {
			if (project.getModules() != null) {
				List<String> modules = project.getModules();
				for (String moduleSubPath : modules) {
					AntBuildWriterUtil.writeAntTask(writer, project, moduleSubPath, "javadoc");
				}
			}
		} else {
			AntBuildWriterUtil.writeJavadocTask(writer, project, artifactResolverWrapper);
		}

		writer.endElement(); // target

		XmlWriterUtil.writeLineBreak(writer);
	}

	/**
	 * Write package target in the writer depending the packaging of the
	 * project.
	 * 
	 * @param writer
	 * @throws IOException .- if any
	 */
	private void writePackageTarget(XMLWriter writer) throws IOException {

		String synonym = null;
		// type of the package we are creating (for example jar)
		XmlWriterUtil.writeCommentText(writer, "Package target", 1);

		writer.startElement("target");
		writer.addAttribute("name", "package");

		if (!AntBuildWriterUtil.isPomPackaging(project)) {
			writer.addAttribute("depends", "compile,setup");
		}
		writer.addAttribute("description", "Package the application");

		if (AntBuildWriterUtil.isPomPackaging(project)) {
			if (project.getModules() != null) {
				List<String> modules = project.getModules();
				for (String moduleSubPath : modules) {
					AntBuildWriterUtil.writeAntTask(writer, project, moduleSubPath, "package");
				}
			}
		} else {
			if (AntBuildWriterUtil.isJarPackaging(project)) {
				AntBuildWriterUtil.writeJarTask(writer, project);
				synonym = "jar";
			} else if (AntBuildWriterUtil.isEarPackaging(project)) {
				AntBuildWriterUtil.writeEarTask(writer, project, artifactResolverWrapper);
				synonym = "ear";
			} else if (AntBuildWriterUtil.isWarPackaging(project)) {
				AntBuildWriterUtil.writeWarTask(writer, project, artifactResolverWrapper, webappDirectory);
				synonym = "war";
			} else {
				writer.startElement("echo");
				writer.addAttribute("message", "No Ant task exists for the packaging '" + project.getPackaging() + "'. " + "You could overrided the Ant package target in your build.xml.");
				writer.endElement(); // echo
			}
		}

		writer.endElement(); // target

		XmlWriterUtil.writeLineBreak(writer);

		if (synonym != null) {
			XmlWriterUtil.writeCommentText(writer, "A dummy target for the package named after the type it creates", 1);
			writer.startElement("target");
			writer.addAttribute("name", synonym);
			writer.addAttribute("depends", "package");
			writer.addAttribute("description", "Builds the " + synonym + " for the application");
			writer.endElement(); // target

			XmlWriterUtil.writeLineBreak(writer);
		}
	}

	private void writeCompileTasks(XMLWriter writer, String outputDirectory, List compileSourceRoots, List resources, String additionalClassesDirectory, boolean isTest) throws IOException {
		writer.startElement("mkdir");
		writer.addAttribute("dir", outputDirectory);
		writer.endElement(); // mkdir

		if (!compileSourceRoots.isEmpty()) {
			writer.startElement("javac");
			writer.addAttribute("destdir", outputDirectory);
			Map[] includes = AntBuildWriterUtil.getMavenCompilerPluginOptions(project, "includes", null);
			AntBuildWriterUtil.addWrapAttribute(writer, "javac", "includes", getCommaSeparatedList(includes, "include"), 3);
			Map[] excludes = AntBuildWriterUtil.getMavenCompilerPluginOptions(project, "excludes", null);
			AntBuildWriterUtil.addWrapAttribute(writer, "javac", "excludes", getCommaSeparatedList(excludes, "exclude"), 3);
			AntBuildWriterUtil.addWrapAttribute(writer, "javac", "encoding", AntBuildWriterUtil.getMavenCompilerPluginBasicOption(project, "encoding", null), 3);
			AntBuildWriterUtil.addWrapAttribute(writer, "javac", "nowarn", AntBuildWriterUtil.getMavenCompilerPluginBasicOption(project, "showWarnings", "false"), 3);
			AntBuildWriterUtil.addWrapAttribute(writer, "javac", "debug", AntBuildWriterUtil.getMavenCompilerPluginBasicOption(project, "debug", "true"), 3);
			AntBuildWriterUtil.addWrapAttribute(writer, "javac", "optimize", AntBuildWriterUtil.getMavenCompilerPluginBasicOption(project, "optimize", "false"), 3);
			AntBuildWriterUtil.addWrapAttribute(writer, "javac", "deprecation", AntBuildWriterUtil.getMavenCompilerPluginBasicOption(project, "showDeprecation", "true"), 3);
			AntBuildWriterUtil.addWrapAttribute(writer, "javac", "target", AntBuildWriterUtil.getMavenCompilerPluginBasicOption(project, "target", "1.1"), 3);
			AntBuildWriterUtil.addWrapAttribute(writer, "javac", "verbose", AntBuildWriterUtil.getMavenCompilerPluginBasicOption(project, "verbose", "false"), 3);
			AntBuildWriterUtil.addWrapAttribute(writer, "javac", "fork", AntBuildWriterUtil.getMavenCompilerPluginBasicOption(project, "fork", "false"), 3);
			AntBuildWriterUtil.addWrapAttribute(writer, "javac", "memoryMaximumSize", AntBuildWriterUtil.getMavenCompilerPluginBasicOption(project, "meminitial", null), 3);
			AntBuildWriterUtil.addWrapAttribute(writer, "javac", "memoryInitialSize", AntBuildWriterUtil.getMavenCompilerPluginBasicOption(project, "maxmem", null), 3);
			AntBuildWriterUtil.addWrapAttribute(writer, "javac", "source", AntBuildWriterUtil.getMavenCompilerPluginBasicOption(project, "source", "1.3"), 3);

			String[] compileSourceRootsArray = (String[]) compileSourceRoots.toArray(new String[0]);
			for (int i = 0; i < compileSourceRootsArray.length; i++) {
				writer.startElement("src");
				writer.startElement("pathelement");
				if (isTest) {
					writer.addAttribute("location", "${" + antBuildTestDir + i + "}");
				} else {
					writer.addAttribute("location", "${" + antBuildSrcDir + i + "}");
				}
				writer.endElement(); // pathelement
				writer.endElement(); // src
			}

			if (additionalClassesDirectory == null) {
				writer.startElement("classpath");
				if (isTest) {
					writer.addAttribute("refid", "build.test.classpath");
				} else {
					writer.addAttribute("refid", "build.classpath");
				}
				writer.endElement(); // classpath
			} else {
				writer.startElement("classpath");
				writer.startElement("path");
				if (isTest) {
					writer.addAttribute("refid", "build.test.classpath");
				} else {
					writer.addAttribute("refid", "build.classpath");
				}
				writer.endElement(); // path
				writer.startElement("pathelement");
				writer.addAttribute("location", additionalClassesDirectory);
				writer.endElement(); // pathelement
				writer.endElement(); // classpath
			}

			writer.endElement(); // javac
		}

		Resource[] array = (Resource[]) resources.toArray(new Resource[0]);
		for (int i = 0; i < array.length; i++) {
			Resource resource = array[i];

			if (new File(resource.getDirectory()).exists()) {
				String outputDir = outputDirectory;
				if (resource.getTargetPath() != null && resource.getTargetPath().length() > 0) {
					outputDir = outputDir + FS + resource.getTargetPath();

					writer.startElement("mkdir");
					writer.addAttribute("dir", outputDir);
					writer.endElement(); // mkdir
				}

				writer.startElement("copy");
				writer.addAttribute("todir", outputDir);

				writer.startElement("fileset");
				if (isTest) {
					writer.addAttribute("dir", "${" + antBuildTestResourceDir + i + "}");
				} else {
					writer.addAttribute("dir", "${" + antBuildResourceDir + i + "}");
				}

				AntBuildWriterUtil.writeIncludesExcludes(writer, resource.getIncludes(), resource.getExcludes());

				writer.endElement(); // fileset

				writer.endElement(); // copy
			}
		}
	}

	/**
	 * Gets the relative path to a repository that is rooted in the project. The
	 * returned path (if any) will always use the forward slash ('/') as the
	 * directory separator. For example, the path "target/it-repo" will be
	 * returned for a repository constructed from the URL
	 * "file://${basedir}/target/it-repo".
	 * 
	 * @param repoUrl
	 *            The URL to the repository, must not be <code>null</code>.
	 * @param projectDir
	 *            The absolute path to the base directory of the project, must
	 *            not be <code>null</code>
	 * @return The path to the repository (relative to the project base
	 *         directory) or <code>null</code> if the repository is not rooted
	 *         in the project.
	 */
	static String getProjectRepoDirectory(String repoUrl, String projectDir) {
		try {
			/*
			 * NOTE: The usual way of constructing repo URLs rooted in the
			 * project is "file://${basedir}" or "file:/${basedir}". None of
			 * these forms delivers a valid URL on both Unix and Windows (even
			 * ignoring URL encoding), one platform will end up with the first
			 * directory of the path being interpreted as the host name...
			 */
			if (repoUrl.regionMatches(true, 0, "file://", 0, 7)) {
				String temp = repoUrl.substring(7);
				if (!temp.startsWith("/") && !temp.regionMatches(true, 0, "localhost/", 0, 10)) {
					repoUrl = "file:///" + temp;
				}
			}
			String path = FileUtils.toFile(new URL(repoUrl)).getPath();
			if (path.startsWith(projectDir)) {
				path = path.substring(projectDir.length()).replace('\\', '/');
				if (path.startsWith("/")) {
					path = path.substring(1);
				}
				if (path.endsWith("/")) {
					path = path.substring(0, path.length() - 1);
				}
				return path;
			}
		} catch (Exception e) {
			// not a "file:" URL or simply malformed
		}
		return null;
	}

	// ----------------------------------------------------------------------
	// Convenience methods
	// ----------------------------------------------------------------------

	/**
	 * Put a property in properties defined by a name and a value
	 * 
	 * @param properties
	 *            not null
	 * @param name
	 * @param value
	 *            not null
	 */
	private static void addProperty(Properties properties, String name, String value) {
		properties.put(name, StringUtils.isNotEmpty(value) ? value : "");
	}

	/**
	 * @param includes
	 *            an array of includes or exludes map
	 * @param key
	 *            a key wanted in the map, like <code>include</code> or
	 *            <code>exclude</code>
	 * @return a String with comma-separated value of a key in each map
	 */
	private static String getCommaSeparatedList(Map[] includes, String key) {
		if ((includes == null) || (includes.length == 0)) {
			return null;
		}

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < includes.length; i++) {
			String s = (String) includes[i].get(key);
			if (StringUtils.isEmpty(s)) {
				continue;
			}

			sb.append(s);

			if (i < (includes.length - 1)) {
				sb.append(",");
			}
		}

		if (sb.length() == 0) {
			return null;
		}

		return sb.toString();
	}

	/**
	 * Flattens the specified file selector options into a simple string list.
	 * For instance, the input
	 * 
	 * <pre>
	 * [ {include=&quot;*Test.java&quot;}, {include=&quot;*TestCase.java&quot;} ]
	 * </pre>
	 * 
	 * is converted to
	 * 
	 * <pre>
	 * [ &quot;*Test.java&quot;, &quot;*TestCase.java&quot; ]
	 * </pre>
	 * 
	 * @param options
	 *            The file selector options to flatten, may be <code>null</code>
	 *            .
	 * @return The string list, might be empty but never <code>null</code>.
	 */
	private static List getSelectorList(Map[] options) {
		List list = new ArrayList();
		if (options != null && options.length > 0) {
			for (int i = 0; i < options.length; i++) {
				Map option = options[i];
				list.addAll(option.values());
			}
		}
		return list;
	}
}
