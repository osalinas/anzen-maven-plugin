package mx.com.anzen.plugins;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

/**
 * Goal which translates from Maven project to Ant project.
 * 
 * @goal prosa
 * @requiresDependencyResolution test
 * 
 */
public class ProsaMojo extends AbstractMojo {

	// ----------------------------------------------------------------------
	// Mojo components
	// ----------------------------------------------------------------------

	/**
	 * Used for resolving artifacts.
	 * 
	 * @component
	 */
	private ArtifactResolver resolver;

	/**
	 * Factory for creating artifact objects.
	 * 
	 * @component
	 */
	private ArtifactFactory factory;

	// ----------------------------------------------------------------------
	// Mojo parameters
	// ----------------------------------------------------------------------

	/**
	 * The project to create a build for.
	 * 
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * The local repository where the artifacts are located.
	 * 
	 * @parameter default-value="${localRepository}"
	 * @required
	 * @readonly
	 */
	private ArtifactRepository localRepository;

	/**
	 * The remote repositories where artifacts are located.
	 * 
	 * @parameter default-value="${project.remoteArtifactRepositories}"
	 * @readonly
	 */
	private List<String> remoteRepositories;

	/**
	 * The current user system settings for use in Maven.
	 * 
	 * @parameter default-value="${settings}"
	 * @required
	 * @readonly
	 */
	private Settings settings;

	/**
	 * Whether or not to overwrite the <code>build.xml</code> file.
	 * 
	 * @parameter property="overwrite" default-value="false"
	 */
	private boolean overwrite;

	/**
	 * The current Maven session.
	 * 
	 * @parameter default-value="${session}"
	 * @readonly
	 */
	private MavenSession session;

	/**
	 * A files mapping, meaning the origin file to target directory.
	 * 
	 * @parameter
	 */
	private List<FilesMapping> filesMappings;

	/**
	 * Directory name for the Ant project.
	 * 
	 * @parameter
	 */
	private String rootDirectory;
	/**
	 * Relative path for dependencies.
	 * 
	 * @parameter default-value="webapp/WEB-INF/lib"
	 */
	private String libDirectory;
	/**
	 * In web applications, set relative path to webapp directory.
	 * 
	 * @parameter default-value="src/main/webapp"
	 */
	private String webappDirectory;
	
	public void execute() throws MojoExecutionException {

		List<FilesMapping> restoreMapping = null;

		ProsaFileStructureBuilder directoryBuilder = new ProsaFileStructureBuilder(project, filesMappings, rootDirectory, libDirectory);

		try {
			restoreMapping = directoryBuilder.makeFileStructure();
			directoryBuilder.getDependencies();
		} catch (IOException e) {
			getLog().error(e);
		}

		ArtifactResolverWrapper artifactResolverWrapper = ArtifactResolverWrapper.getInstance(resolver, factory, localRepository, remoteRepositories);

		Properties executionProperties = (session != null) ? session.getExecutionProperties() : null;

		AntBuildWriter antBuildWriter = new AntBuildWriter(project, artifactResolverWrapper, settings, overwrite, executionProperties, rootDirectory, libDirectory);

		try {
			antBuildWriter.writeBuildXmls(restoreMapping, webappDirectory);
			antBuildWriter.writeBuildProperties();
		} catch (IOException e) {
			throw new MojoExecutionException("Error building Ant script: " + e.getMessage(), e);
		}

		getLog().info("Wrote Ant project for " + project.getArtifactId() + " to " + project.getBasedir().getAbsolutePath());

	}
}
