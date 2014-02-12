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

	public List<FilesMapping> makeFileStructure() throws IOException {

		if (project.hasParent()) {
			return null;
		}

		makeDefauldFileStructure();
		File base = new File(project.getBasedir(), rootDirectory);
		List<FilesMapping> restoringMap = new ArrayList<FilesMapping>();
		log.info("Base directory: " + base.getAbsolutePath());
		for (FilesMapping fm : filesMapping) {

			File target = new File(base, fm.getDestinationDirectory());

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
			restoringMap.add(fm);
			log.info("Copy: " + fm.getSourceDirectory() + " => " + fm.getDestinationDirectory());
		}
		return restoringMap;
	}

	private void makeDefauldFileStructure() {
		log.info("Making file system");
		File base = new File(project.getBasedir(), rootDirectory);
		for (String path : prosaFileStructure) {
			File f = new File(base, path);
			f.mkdirs();
		}
	}

	public void getDependencies() throws IOException {
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

	// @SuppressWarnings({ "unused", "unchecked" })
	// private void printArtifactsMap() {
	// Map<Object, Object> map = project.getArtifactMap();
	// Set<Entry<Object, Object>> set = map.entrySet();
	// for (Entry<Object, Object> entry : set) {
	// log.debug(entry.getKey() + "   ======>   " + entry.getValue());
	// }
	// }
}
