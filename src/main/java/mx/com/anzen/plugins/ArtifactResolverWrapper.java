package mx.com.anzen.plugins;

import java.io.IOException;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;

public class ArtifactResolverWrapper {
	/**
	 * Used for resolving artifacts
	 */
	private ArtifactResolver resolver;

	/**
	 * Factory for creating artifact objects
	 */
	private ArtifactFactory factory;

	/**
	 * The local repository where the artifacts are located
	 */
	private ArtifactRepository localRepository;

	/**
	 * The remote repositories where artifacts are located
	 */
	private List<String> remoteRepositories;

	/**
	 * @param resolver
	 * @param factory
	 * @param localRepository
	 * @param remoteRepositories
	 */
	private ArtifactResolverWrapper(ArtifactResolver resolver, ArtifactFactory factory, ArtifactRepository localRepository, List<String> remoteRepositories) {
		this.resolver = resolver;
		this.factory = factory;
		this.localRepository = localRepository;
		this.remoteRepositories = remoteRepositories;
	}

	/**
	 * @param resolver
	 * @param factory
	 * @param localRepository
	 * @param remoteRepositories
	 * @return an instance of ArtifactResolverWrapper
	 */
	public static ArtifactResolverWrapper getInstance(ArtifactResolver resolver, ArtifactFactory factory, ArtifactRepository localRepository, List<String> remoteRepositories) {
		return new ArtifactResolverWrapper(resolver, factory, localRepository, remoteRepositories);
	}

	protected ArtifactFactory getFactory() {
		return factory;
	}

	protected void setFactory(ArtifactFactory factory) {
		this.factory = factory;
	}

	protected ArtifactRepository getLocalRepository() {
		return localRepository;
	}

	protected void setLocalRepository(ArtifactRepository localRepository) {
		this.localRepository = localRepository;
	}

	protected List<String> getRemoteRepositories() {
		return remoteRepositories;
	}

	protected void setRemoteRepositories(List<String> remoteRepositories) {
		this.remoteRepositories = remoteRepositories;
	}

	protected ArtifactResolver getResolver() {
		return resolver;
	}

	protected void setResolver(ArtifactResolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * Return the artifact path in the local repository for an artifact defined
	 * by its <code>groupId</code>, its <code>artifactId</code> and its
	 * <code>version</code>.
	 * 
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @return the locale artifact path
	 * @throws IOException
	 */
	public String getArtifactAbsolutePath(String groupId, String artifactId, String version) throws IOException {
		Artifact artifact = factory.createArtifact(groupId, artifactId, version, "compile", "jar");
		try {
			resolver.resolve(artifact, remoteRepositories, localRepository);

			return artifact.getFile().getAbsolutePath();
		} catch (ArtifactResolutionException e) {
			throw new IOException("Unable to resolve artifact: " + groupId + ":" + artifactId + ":" + version);
		} catch (ArtifactNotFoundException e) {
			throw new IOException("Unable to find artifact: " + groupId + ":" + artifactId + ":" + version);
		}
	}

	/**
	 * Gets the path to the specified artifact relative to the local
	 * repository's base directory. Note that this method does not actually
	 * resolve the artifact, it merely calculates the path at which the artifact
	 * is or would be stored in the local repository.
	 * 
	 * @param artifact
	 *            The artifact whose path should be determined, must not be
	 *            <code>null</code>.
	 * @return The path to the artifact, never <code>null</code>.
	 */
	public String getLocalArtifactPath(Artifact artifact) {
		/*
		 * NOTE: Don't use Artifact.getFile() here because this method could
		 * return the path to a JAR from the build output, e.g.
		 * ".../target/some-0.1.jar". The other special case are system-scope
		 * artifacts that reside somewhere outside of the local repository.
		 */
		return localRepository.pathOf(artifact);
	}
}
