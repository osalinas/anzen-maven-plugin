package mx.com.anzen.plugins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

public class ProsaFileStructureBuilder {

	private MavenProject project;
	private List<FilesMapping> filesMapping;
	private String rootDirectory;
	private String libDirectory;
	private static Log log = new SystemStreamLog();
	private static final List<String> prosaFileStructure = new ArrayList<String>(Arrays.asList("build", "configFiles", "dist", "javadocs", "ldap", "src", "webapp/jsp", "webapp/css", "webapp/html",
			"webapp/imagenes", "webapp/js", "webapp/jasper", "webapp/META-INF", "webapp/WEB-INF/cfg", "webapp/WEB-INF/lib"));

	ProsaFileStructureBuilder(MavenProject project, List<FilesMapping> filesMapping, String targetDirectory, String libDirectory) {
		this.project = project;
		this.filesMapping = filesMapping;
		this.rootDirectory = targetDirectory;
		this.libDirectory = libDirectory;
	}

	/**
	 * Makes the folders hierarchy and copy the mapping.
	 *
	 * @return {@link List}&lt;{@link FilesMaping}&gt; a file List has had been
	 *         copied.
	 * @throws IOException
	 */
	public void makeFileStructure() throws IOException {

		if (project.hasParent()) {
			return;
		}

		makeDefaultFileStructure();

		File base = new File(project.getBasedir(), rootDirectory);
		log.info("Base directory: " + base.getAbsolutePath());
		for (FilesMapping fm : filesMapping) {

			File target = new File(base, fm.getDestinationDirectory());

			// just creates a directory and this directory is not considered for
			// restoring.
			if (fm.getSourceDirectory() == null || fm.getSourceDirectory().trim().isEmpty()) {
				target.mkdirs();
				log.info("mkdir(s): " + fm.getDestinationDirectory());
				continue;
			}

			File origin = new File(project.getBasedir(), fm.getSourceDirectory());
			if (origin.isDirectory()) {
				target.mkdirs();
				FileUtils.copyDirectoryStructure(origin, target);
			} else if (target.isDirectory()) {
				FileUtils.copyFileToDirectory(origin, target);
			} else {
				FileUtils.copyFile(origin, target);
			}

			log.info("Copy: " + fm.getSourceDirectory() + " => " + fm.getDestinationDirectory());
		}
	}

	/**
	 * Build the default file structure indicated by PROSA.
	 */
	private void makeDefaultFileStructure() {
		log.info("Making file system");
		File base = new File(project.getBasedir(), rootDirectory);
		for (String path : prosaFileStructure) {
			File f = new File(base, path);
			f.mkdirs();
		}
	}

	/**
	 * Copy from Maven local repository all dependencies and paste these on lib
	 * directory.
	 *
	 * @throws IOException
	 */
	public void copyDependencies() throws IOException {
		@SuppressWarnings("unchecked")
		List<Artifact> artifactsList = new ArrayList<Artifact>(project.getArtifacts());
		File f = getLibFile();
		log.info("Coping dependencies (" + artifactsList.size() + "):");
		for (Artifact artifact : artifactsList) {
			FileUtils.copyFileToDirectory(artifact.getFile(), f);
			log.info("Copied: " + artifact.getFile().getAbsolutePath());
		}
		log.info("Ok");
	}

	/**
	 * Build the lib directory. The File object returned is builded based on the
	 * modules of Maven project (project) to achieve a minimal organization for
	 * the dependencies for each modules.
	 *
	 * @return {@link File} .- A File object representing the absolute path of
	 *         lib directory.
	 */
	public File getLibFile() {
		File f = null;
		if (project.hasParent()) {
			String aux = project.getParent().getFile().getParent();
			aux += File.separator + rootDirectory + File.separator + libDirectory;
			aux += File.separator + project.getFile().getParentFile().getName();
			f = new File(aux);
		} else {
			f = new File(new File(project.getBasedir(), rootDirectory), libDirectory);
		}
		return f;
	}
}
