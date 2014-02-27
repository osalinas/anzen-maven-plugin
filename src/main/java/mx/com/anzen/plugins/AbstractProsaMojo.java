package mx.com.anzen.plugins;

import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

public abstract class AbstractProsaMojo extends AbstractMojo{

	// ----------------------------------------------------------------------
	// Mojo components
	// ----------------------------------------------------------------------

	/**
	 * Used for resolving artifacts.
	 *
	 * @component
	 */
	protected ArtifactResolver resolver;

	/**
	 * Factory for creating artifact objects.
	 *
	 * @component
	 */
	protected ArtifactFactory factory;

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
	public MavenProject project;

	/**
	 * The local repository where the artifacts are located.
	 *
	 * @parameter default-value="${localRepository}"
	 * @required
	 * @readonly
	 */
	public ArtifactRepository localRepository;

	/**
	 * The remote repositories where artifacts are located.
	 *
	 * @parameter default-value="${project.remoteArtifactRepositories}"
	 * @readonly
	 */
	public List<String> remoteRepositories;

	/**
	 * The current user system settings for use in Maven.
	 *
	 * @parameter default-value="${settings}"
	 * @required
	 * @readonly
	 */
	public Settings settings;

	/**
	 * Whether or not to overwrite the <code>build.xml</code> file.
	 *
	 * @parameter property="overwrite" default-value="false"
	 */
	public boolean overwrite;

	/**
	 * The current Maven session.
	 *
	 * @parameter default-value="${session}"
	 * @readonly
	 */
	public MavenSession session;

}
