package mx.com.anzen.plugins;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which translates from Maven project to Ant project.
 *
 * @goal prosa
 * @requiresDependencyResolution test
 */
public class ProsaMojo extends AbstractProsaMojo {

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





	@Override
	public void execute() throws MojoExecutionException {

		ProsaFileStructureBuilder directoryBuilder = new ProsaFileStructureBuilder(project, filesMappings, rootDirectory, libDirectory);

		try {
			directoryBuilder.makeFileStructure();
			directoryBuilder.copyDependencies();
		} catch (IOException e) {
			getLog().error(e);
		}

		ArtifactResolverWrapper artifactResolverWrapper = ArtifactResolverWrapper.getInstance(resolver, factory, localRepository, remoteRepositories);

		Properties executionProperties = (session != null) ? session.getExecutionProperties() : null;

		AntBuildWriter antBuildWriter = new AntBuildWriter(project, artifactResolverWrapper, settings, overwrite, executionProperties, rootDirectory, libDirectory);

		try {
			antBuildWriter.writeBuildXmls(filesMappings, webappDirectory);
			antBuildWriter.writeBuildProperties();
		} catch (IOException e) {
			throw new MojoExecutionException("Error building Ant script: " + e.getMessage(), e);
		}

		getLog().info("Wrote Ant project for " + project.getArtifactId() + " to " + project.getBasedir().getAbsolutePath());
	}
}
