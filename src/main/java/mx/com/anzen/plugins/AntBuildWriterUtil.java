package mx.com.anzen.plugins;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;
import org.apache.xpath.XPathAPI;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.XmlWriterUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AntBuildWriterUtil {

	private static final String FS = File.separator;

	/**
	 * @param compileSourceRoots
	 * @return not null list
	 */
	public static List<String> removeEmptyCompileSourceRoots(List<String> compileSourceRoots) {
		List<String> newCompileSourceRootsList = new ArrayList<String>();
		if (compileSourceRoots != null) {
			for (String srcDir : compileSourceRoots) {
				if ((new File(srcDir)).exists()) {
					newCompileSourceRootsList.add(srcDir);
				}
			}
		}
		return newCompileSourceRootsList;
	}

	/**
	 * Convenience method to write <code>&lt;include/&gt;</code> and
	 * <code>&lt;exclude/&gt;</code>
	 * 
	 * @param writer
	 * @param includes
	 * @param excludes
	 */
	public static void writeIncludesExcludes(XMLWriter writer, List<String> includes, List<String> excludes) {
		if (includes != null) {
			Iterator<String> i = includes.iterator();
			while (i.hasNext()) {
				String include = (String) i.next();
				writer.startElement("include");
				writer.addAttribute("name", include);
				writer.endElement(); // include
			}
		}
		if (excludes != null) {
			Iterator<String> i = excludes.iterator();
			while (i.hasNext()) {
				String exclude = (String) i.next();
				writer.startElement("exclude");
				writer.addAttribute("name", exclude);
				writer.endElement(); // exclude
			}
		}
	}

	/**
	 * Write comment for the Ant supported version
	 * 
	 * @param writer
	 */
	public static void writeAntVersionHeader(XMLWriter writer) {
		XmlWriterUtil.writeCommentText(writer, "Ant build file (http://ant.apache.org/) for Ant 1.6.2 or above.", 0);
	}

	/**
	 * Convenience method to write XML ant task
	 * 
	 * @param writer
	 * @param project
	 * @param moduleSubPath
	 * @param tasks
	 */
	public static void writeAntTask(XMLWriter writer, MavenProject project, String moduleSubPath, String tasks) {
		String dir = "${" + AntBuildWriter.antBuildDir + "}";
		dir += FS + "${" + AntBuildWriter.antBuildFinalName + "}";
		dir += FS + toRelative(project.getBasedir(), moduleSubPath);

		writer.startElement("ant");
		writer.addAttribute("antfile", "build.xml");
		writer.addAttribute("dir", dir);
		writer.addAttribute("target", tasks);
		writer.endElement(); // ant
	}

	/**
	 * Convenience method to write XML Ant javadoc task
	 * 
	 * @param writer
	 * @param project
	 * @param wrapper
	 * @throws IOException
	 */
	public static void writeJavadocTask(XMLWriter writer, MavenProject project, ArtifactResolverWrapper wrapper) throws IOException {
		List<String> sources = new ArrayList<String>();
		List<String> sourceRoots = project.getCompileSourceRoots();
		for (String source : sourceRoots) {
			if (new File(source).exists()) {
				sources.add(source);
			}
		}

		// No sources
		if (sources.size() == 0) {
			return;
		}

		writer.startElement("javadoc");
		String sourcepath = getMavenJavadocPluginBasicOption(project, "sourcepath", null);
		if (sourcepath == null) {
			StringBuffer sb = new StringBuffer();
			String[] compileSourceRoots = (String[]) sources.toArray(new String[0]);
			for (int i = 0; i < compileSourceRoots.length; i++) {
				sb.append("${" + AntBuildWriter.antBuildSrcDir).append(i).append("}");

				if (i < (compileSourceRoots.length - 1)) {
					sb.append(File.pathSeparatorChar);
				}
			}
			writer.addAttribute("sourcepath", sb.toString());
			addWrapAttribute(writer, "javadoc", "packagenames", "*", 3);
		} else {
			writer.addAttribute("sourcepath", sourcepath);
		}
		addWrapAttribute(writer, "javadoc", "destdir", getMavenJavadocPluginBasicOption(project, "destdir", "${" + AntBuildWriter.antJavadocDir + "}"), 3);
		addWrapAttribute(writer, "javadoc", "extdirs", getMavenJavadocPluginBasicOption(project, "extdirs", null), 3);

		addWrapAttribute(writer, "javadoc", "overview", getMavenJavadocPluginBasicOption(project, "overview", null), 3);
		addWrapAttribute(writer, "javadoc", "access", getMavenJavadocPluginBasicOption(project, "show", "protected"), 3);
		addWrapAttribute(writer, "javadoc", "old", getMavenJavadocPluginBasicOption(project, "old", "false"), 3);
		addWrapAttribute(writer, "javadoc", "verbose", getMavenJavadocPluginBasicOption(project, "verbose", "false"), 3);
		addWrapAttribute(writer, "javadoc", "locale", getMavenJavadocPluginBasicOption(project, "locale", null), 3);
		addWrapAttribute(writer, "javadoc", "encoding", getMavenJavadocPluginBasicOption(project, "encoding", null), 3);
		addWrapAttribute(writer, "javadoc", "version", getMavenJavadocPluginBasicOption(project, "version", "true"), 3);
		addWrapAttribute(writer, "javadoc", "use", getMavenJavadocPluginBasicOption(project, "use", "true"), 3);
		addWrapAttribute(writer, "javadoc", "author", getMavenJavadocPluginBasicOption(project, "author", "true"), 3);
		addWrapAttribute(writer, "javadoc", "splitindex", getMavenJavadocPluginBasicOption(project, "splitindex", "false"), 3);
		addWrapAttribute(writer, "javadoc", "windowtitle", getMavenJavadocPluginBasicOption(project, "windowtitle", null), 3);
		addWrapAttribute(writer, "javadoc", "nodeprecated", getMavenJavadocPluginBasicOption(project, "nodeprecated", "false"), 3);
		addWrapAttribute(writer, "javadoc", "nodeprecatedlist", getMavenJavadocPluginBasicOption(project, "nodeprecatedlist", "false"), 3);
		addWrapAttribute(writer, "javadoc", "notree", getMavenJavadocPluginBasicOption(project, "notree", "false"), 3);
		addWrapAttribute(writer, "javadoc", "noindex", getMavenJavadocPluginBasicOption(project, "noindex", "false"), 3);
		addWrapAttribute(writer, "javadoc", "nohelp", getMavenJavadocPluginBasicOption(project, "nohelp", "false"), 3);
		addWrapAttribute(writer, "javadoc", "nonavbar", getMavenJavadocPluginBasicOption(project, "nonavbar", "false"), 3);
		addWrapAttribute(writer, "javadoc", "serialwarn", getMavenJavadocPluginBasicOption(project, "serialwarn", "false"), 3);
		addWrapAttribute(writer, "javadoc", "helpfile", getMavenJavadocPluginBasicOption(project, "helpfile", null), 3);
		addWrapAttribute(writer, "javadoc", "stylesheetfile", getMavenJavadocPluginBasicOption(project, "stylesheetfile", null), 3);
		addWrapAttribute(writer, "javadoc", "charset", getMavenJavadocPluginBasicOption(project, "charset", "ISO-8859-1"), 3);
		addWrapAttribute(writer, "javadoc", "docencoding", getMavenJavadocPluginBasicOption(project, "docencoding", null), 3);
		addWrapAttribute(writer, "javadoc", "excludepackagenames", getMavenJavadocPluginBasicOption(project, "excludepackagenames", null), 3);
		addWrapAttribute(writer, "javadoc", "source", getMavenJavadocPluginBasicOption(project, "source", null), 3);
		addWrapAttribute(writer, "javadoc", "linksource", getMavenJavadocPluginBasicOption(project, "linksource", "false"), 3);
		addWrapAttribute(writer, "javadoc", "breakiterator", getMavenJavadocPluginBasicOption(project, "breakiterator", "false"), 3);
		addWrapAttribute(writer, "javadoc", "noqualifier", getMavenJavadocPluginBasicOption(project, "noqualifier", null), 3);
		// miscellaneous
		addWrapAttribute(writer, "javadoc", "maxmemory", getMavenJavadocPluginBasicOption(project, "maxmemory", null), 3);
		addWrapAttribute(writer, "javadoc", "additionalparam", getMavenJavadocPluginBasicOption(project, "additionalparam", null), 3);

		// Nested arg
		String doctitle = getMavenJavadocPluginBasicOption(project, "doctitle", null);
		if (doctitle != null) {
			writer.startElement("doctitle");
			writer.writeText("<![CDATA[" + doctitle + "]]>");
			writer.endElement(); // doctitle
		}
		String header = getMavenJavadocPluginBasicOption(project, "header", null);
		if (header != null) {
			writer.startElement("header");
			writer.writeText("<![CDATA[" + header + "]]>");
			writer.endElement(); // header
		}
		String footer = getMavenJavadocPluginBasicOption(project, "footer", null);
		if (footer != null) {
			writer.startElement("footer");
			writer.writeText("<![CDATA[" + footer + "]]>");
			writer.endElement(); // footer
		}
		String bottom = getMavenJavadocPluginBasicOption(project, "bottom", null);
		if (bottom != null) {
			writer.startElement("bottom");
			writer.writeText("<![CDATA[" + bottom + "]]>");
			writer.endElement(); // bottom
		}

		Map[] links = getMavenJavadocPluginOptions(project, "links", null);
		if (links != null) {
			for (int i = 0; i < links.length; i++) {
				writer.startElement("link");
				writer.addAttribute("href", (String) links[i].get("link"));
				writer.endElement(); // link
			}
		}

		Map[] offlineLinks = getMavenJavadocPluginOptions(project, "offlineLinks", null);
		if (offlineLinks != null) {
			for (int i = 0; i < offlineLinks.length; i++) {
				writer.startElement("link");
				writer.addAttribute("href", (String) offlineLinks[i].get("url"));
				addWrapAttribute(writer, "javadoc", "offline", "true", 4);
				writer.endElement(); // link
			}
		}

		Map[] groups = getMavenJavadocPluginOptions(project, "groups", null);
		if (groups != null) {
			for (int i = 0; i < groups.length; i++) {
				Map<String, String> group = (Map<String, String>) groups[i].get("group");
				writer.startElement("group");
				writer.addAttribute("title", group.get("title"));
				addWrapAttribute(writer, "javadoc", "package", group.get("package"), 4);
				writer.endElement(); // group
			}
		}

		// TODO Handle docletArtifacts
		String doclet = getMavenJavadocPluginBasicOption(project, "doclet", null);
		if (doclet != null) {
			String docletpath = getMavenJavadocPluginBasicOption(project, "docletpath", null);
			if (StringUtils.isNotEmpty(docletpath)) {
				writer.startElement("doclet");
				writer.addAttribute("name", doclet);
				addWrapAttribute(writer, "javadoc", "path", docletpath, 4);
				writer.endElement(); // doclet
			} else {
				Map<String, String> docletArtifact = getMavenJavadocPluginOption(project, "docletArtifact", null);
				String path = wrapper.getArtifactAbsolutePath(docletArtifact.get("groupId"), docletArtifact.get("artifactId"), (String) docletArtifact.get("version"));
				path = StringUtils.replace(path, wrapper.getLocalRepository().getBasedir(), "${maven.repo.local}");

				writer.startElement("doclet");
				writer.addAttribute("name", doclet);
				addWrapAttribute(writer, "javadoc", "path", path, 4);
				writer.endElement(); // doclet
			}
		}

		// TODO Handle taglets
		String taglet = getMavenJavadocPluginBasicOption(project, "taglet", null);
		if (taglet != null) {
			String tagletpath = getMavenJavadocPluginBasicOption(project, "tagletpath", null);
			if (StringUtils.isNotEmpty(tagletpath)) {
				writer.startElement("taglet");
				writer.addAttribute("name", taglet);
				addWrapAttribute(writer, "javadoc", "path", tagletpath, 4);
				writer.endElement(); // taglet
			} else {
				Map<String, String> tagletArtifact = getMavenJavadocPluginOption(project, "tagletArtifact", null);
				String path = wrapper.getArtifactAbsolutePath(tagletArtifact.get("groupId"), tagletArtifact.get("artifactId"), (String) tagletArtifact.get("version"));
				path = StringUtils.replace(path, wrapper.getLocalRepository().getBasedir(), "${maven.repo.local}");

				writer.startElement("taglet");
				writer.addAttribute("name", taglet);
				addWrapAttribute(writer, "javadoc", "path", path, 4);
				writer.endElement(); // taglet
			}
		}

		Map[] tags = getMavenJavadocPluginOptions(project, "tags", null);
		if (tags != null) {
			for (int i = 0; i < tags.length; i++) {
				Map<String, String> props = (Map<String, String>) tags[i].get("tag");
				writer.startElement("tag");
				writer.addAttribute("name", props.get("name"));
				addWrapAttribute(writer, "javadoc", "scope", props.get("placement"), 4);
				addWrapAttribute(writer, "javadoc", "description", props.get("head"), 4);
				writer.endElement(); // tag
			}
		}

		writer.endElement(); // javadoc
	}

	/**
	 * Convenience method to write XML Ant jar task
	 * 
	 * @param writer
	 * @param project
	 * @throws IOException
	 */
	public static void writeJarTask(XMLWriter writer, MavenProject project) throws IOException {

		String inputLIbDir = "${" + AntBuildWriter.antBuildClasspathDir + "}";
		String outputLibDir = "${" + AntBuildWriter.antBuildOutputDir + "}";
		writeCopyLib(writer, project, inputLIbDir, outputLibDir);

		String jarfile = "${" + AntBuildWriter.antParentDir + "}dist" + FS + "${" + AntBuildWriter.antBuildFinalName + "}.jar";

		writer.startElement("jar");
		writer.addAttribute("jarfile", jarfile);
		addWrapAttribute(writer, "jar", "compress", getMavenJarPluginBasicOption(project, "archive//compress", "true"), 3);
		addWrapAttribute(writer, "jar", "index", getMavenJarPluginBasicOption(project, "archive//index", "false"), 3);
		if (getMavenJarPluginBasicOption(project, "archive//manifestFile", null) != null) {
			addWrapAttribute(writer, "jar", "manifest", getMavenJarPluginBasicOption(project, "archive//manifestFile", null), 3);
		}
		addWrapAttribute(writer, "jar", "basedir", "${" + AntBuildWriter.antBuildOutputDir + "}", 3);
		addWrapAttribute(writer, "jar", "excludes", "**" + FS + "package.html", 3);
		if (getMavenPluginOption(project, "maven-jar-plugin", "archive//manifest", null) != null) {
			writer.startElement("manifest");
			writer.startElement("attribute");
			writer.addAttribute("name", "Main-Class");
			addWrapAttribute(writer, "attribute", "value", getMavenJarPluginBasicOption(project, "archive//manifest//mainClass", null), 5);
			writer.endElement(); // attribute
			writer.endElement(); // manifest
		}
		writer.endElement(); // jar
	}

	/**
	 * Convenience method to write XML Ant ear task
	 * 
	 * @param writer
	 * @param project
	 * @param artifactResolverWrapper
	 * @throws IOException
	 */
	public static void writeEarTask(XMLWriter writer, MavenProject project, ArtifactResolverWrapper artifactResolverWrapper) throws IOException {

		String outputDir = "${" + AntBuildWriter.antBuildOutputDir + "}";
		String modulesDir = "${" + AntBuildWriter.antParentDir + "}dist";
		String inputLibdir = "${" + AntBuildWriter.antBuildClasspathDir + "}";
		String outputLibDir = outputDir + FS + "lib";
		String destfile = modulesDir + FS + "${" + AntBuildWriter.antBuildFinalName + "}.ear";
		String appDir = "${" + AntBuildWriter.antBaseDir + "}" + FS + "src" + FS + "main" + FS + "application";
		String appxml = appDir + FS + "META-INF" + FS + "application.xml";

		writer.startElement("mkdir");
		writer.addAttribute("dir", outputDir);
		writer.endElement();

		writer.startElement("copy");
		writer.addAttribute("todir", outputDir);
		writer.startElement("fileset");
		writer.addAttribute("dir", modulesDir);
		writer.endElement();// fileset
		writer.startElement("fileset");
		writer.addAttribute("dir", appDir);
		writer.endElement();// fileset
		writer.endElement();// copy

		writer.startElement("copy");
		writer.addAttribute("todir", outputLibDir);
		writer.startElement("fileset");
		writer.addAttribute("dir", inputLibdir);
		writer.endElement();// fileset
		writer.endElement();// copy

		writer.startElement("ear");
		writer.addAttribute("destfile", destfile);
		addWrapAttribute(writer, "ear", "basedir", outputDir, 3);
		addWrapAttribute(writer, "ear", "compress", getMavenEarPluginBasicOption(project, "archive//compress", "true"), 3);
		addWrapAttribute(writer, "ear", "includes ", getMavenEarPluginBasicOption(project, "includes", null), 3);
		addWrapAttribute(writer, "ear", "excludes", getMavenEarPluginBasicOption(project, "excludes", null), 3);
		if (getMavenEarPluginBasicOption(project, "applicationXml", null) != null) {
			addWrapAttribute(writer, "ear", "appxml", getMavenEarPluginBasicOption(project, "applicationXml", null), 3);
		} else {
			// Generated appxml
			addWrapAttribute(writer, "ear", "appxml", appxml, 3);
		}
		if (getMavenEarPluginBasicOption(project, "manifestFile", null) != null) {
			addWrapAttribute(writer, "ear", "manifest", getMavenEarPluginBasicOption(project, "manifestFile", null), 3);
		}
		writer.endElement(); // ear
	}

	/**
	 * Convenience method to write XML Ant war task
	 * 
	 * @param writer
	 * @param project
	 * @param artifactResolverWrapper
	 * @throws IOException
	 */
	public static void writeWarTask(XMLWriter writer, MavenProject project, ArtifactResolverWrapper artifactResolverWrapper, String webappDirectory) throws IOException {

		String webXml = "${basedir}" + FS + "${" + AntBuildWriter.antBaseDir + "}" + FS + webappDirectory + FS + "WEB-INF" + FS + "web.xml";
		webXml = getMavenWarPluginBasicOption(project, "webXml", webXml);
		if (webXml.startsWith("${basedir}" + FS)) {
			webXml = webXml.substring(("${basedir}".length() + 1));
		}

		String destfile = "${" + AntBuildWriter.antParentDir + "}dist" + FS + "${" + AntBuildWriter.antBuildFinalName + "}.war";
		writer.startElement("war");
		writer.addAttribute("destfile", destfile);
		addWrapAttribute(writer, "war", "compress", getMavenWarPluginBasicOption(project, "archive//compress", "true"), 3);

		String needxmlfile = getMavenWarPluginBasicOption(project, "failOnMissingWebXml", "true");
		addWrapAttribute(writer, "war", "needxmlfile", needxmlfile, 3);

		if (needxmlfile.equals("true")) {
			addWrapAttribute(writer, "war", "webxml", webXml, 3);
		}

		if (getMavenWarPluginBasicOption(project, "manifestFile", null) != null) {
			addWrapAttribute(writer, "war", "manifest", getMavenWarPluginBasicOption(project, "manifestFile", null), 3);
		}

		writer.startElement("lib");
		writer.addAttribute("dir", "${" + AntBuildWriter.antBuildClasspathDir + "}");
		writer.endElement(); // lib

		writer.startElement("classes");
		writer.addAttribute("dir", "${" + AntBuildWriter.antBuildOutputDir + "}");
		writer.endElement(); // classes

		writer.startElement("fileset");
		writer.addAttribute("dir", "${" + AntBuildWriter.antBaseDir + "}" + FS + webappDirectory);
		addWrapAttribute(writer, "fileset", "excludes", webXml, 4);
		writer.endElement(); // fileset

		writer.endElement(); // war
	}

	/**
	 * Convenience method to wrap long element tags for a given attribute.
	 * 
	 * @param writer
	 * @param tag
	 * @param name
	 * @param value
	 * @param indent
	 */
	public static void addWrapAttribute(XMLWriter writer, String tag, String name, String value, int indent) {
		if (StringUtils.isEmpty(value)) {
			return;
		}

		if (indent < 0) {
			writer.addAttribute(name, value);
		} else {
			writer.addAttribute("\n" + StringUtils.repeat(" ", (StringUtils.isEmpty(tag) ? 0 : tag.length()) + indent * XmlWriterUtil.DEFAULT_INDENTATION_SIZE) + name, value);
		}
	}

	/**
	 * @param mavenProject
	 * @return true if project packaging equals <code>pom</code>
	 */
	public static boolean isPomPackaging(MavenProject mavenProject) {
		return "pom".equals(mavenProject.getPackaging());
	}

	/**
	 * @param mavenProject
	 * @return true if project packaging equals one of several packaging types
	 *         including <code>jar</code>, <code>maven-plugin</code>,
	 *         <code>ejb</code>, or <code>bundle</code>
	 */
	public static boolean isJarPackaging(MavenProject mavenProject) {
		return "jar".equals(mavenProject.getPackaging()) || isEjbPackaging(mavenProject) || isMavenPluginPackaging(mavenProject) || isBundlePackaging(mavenProject);
	}

	/**
	 * @param mavenProject
	 * @return true if project packaging equals <code>bundle</code>
	 */
	public static boolean isBundlePackaging(MavenProject mavenProject) {
		return "bundle".equals(mavenProject.getPackaging());
	}

	/**
	 * @param mavenProject
	 * @return true if project packaging equals <code>ejb</code>
	 */
	public static boolean isEjbPackaging(MavenProject mavenProject) {
		return "ejb".equals(mavenProject.getPackaging());
	}

	/**
	 * @param mavenProject
	 * @return true if project packaging equals <code>maven-plugin</code>
	 */
	public static boolean isMavenPluginPackaging(MavenProject mavenProject) {
		return "maven-plugin".equals(mavenProject.getPackaging());
	}

	/**
	 * @param mavenProject
	 * @return true if project packaging equals <code>ear</code>
	 */
	public static boolean isEarPackaging(MavenProject mavenProject) {
		return "ear".equals(mavenProject.getPackaging());
	}

	/**
	 * @param mavenProject
	 *            not null
	 * @return true if project packaging equals <code>war</code>
	 */
	public static boolean isWarPackaging(MavenProject mavenProject) {
		return "war".equals(mavenProject.getPackaging());
	}

	/**
	 * Return the <code>optionName</code> value defined in a project for the
	 * "maven-compiler-plugin" plugin.
	 * 
	 * @param project
	 *            not null
	 * @param optionName
	 *            the option name wanted
	 * @param defaultValue
	 *            a default value
	 * @return the value for the option name or the default value. Could be null
	 *         if not found.
	 * @throws IOException
	 *             if any
	 */
	public static String getMavenCompilerPluginBasicOption(MavenProject project, String optionName, String defaultValue) throws IOException {
		return getMavenPluginBasicOption(project, "maven-compiler-plugin", optionName, defaultValue);
	}

	/**
	 * Return the map of <code>optionName</code> value defined in a project for
	 * the "maven-compiler-plugin" plugin.
	 * 
	 * @param project
	 *            not null
	 * @param optionName
	 *            the option name wanted
	 * @param defaultValue
	 *            a default value
	 * @return the map for the option name or the default value. Could be null
	 *         if not found.
	 * @throws IOException
	 *             if any
	 */
	public static Map getMavenCompilerPluginOption(MavenProject project, String optionName, String defaultValue) throws IOException {
		return getMavenPluginOption(project, "maven-compiler-plugin", optionName, defaultValue);
	}

	/**
	 * Return an array of map of <code>optionName</code> value defined in a
	 * project for the "maven-compiler-plugin" plugin.
	 * 
	 * @param project
	 *            not null
	 * @param optionName
	 *            the option name wanted
	 * @param defaultValue
	 *            a default value
	 * @return the array of option name or the default value. Could be null if
	 *         not found.
	 * @throws IOException
	 *             if any
	 */
	public static Map[] getMavenCompilerPluginOptions(MavenProject project, String optionName, String defaultValue) throws IOException {
		return getMavenPluginOptions(project, "maven-compiler-plugin", optionName, defaultValue);
	}

	/**
	 * Return the <code>optionName</code> value defined in a project for the
	 * "maven-surefire-plugin" plugin.
	 * 
	 * @param project
	 *            not null
	 * @param optionName
	 *            the option name wanted
	 * @param defaultValue
	 *            a default value
	 * @return the value for the option name or the default value. Could be null
	 *         if not found.
	 * @throws IOException
	 *             if any
	 */
	public static String getMavenSurefirePluginBasicOption(MavenProject project, String optionName, String defaultValue) throws IOException {
		return getMavenPluginBasicOption(project, "maven-surefire-plugin", optionName, defaultValue);
	}

	/**
	 * Return the map of <code>optionName</code> value defined in a project for
	 * the "maven-surefire-plugin" plugin.
	 * 
	 * @param project
	 *            not null
	 * @param optionName
	 *            the option name wanted
	 * @param defaultValue
	 *            a default value
	 * @return the map for the option name or the default value. Could be null
	 *         if not found.
	 * @throws IOException
	 *             if any
	 */
	public static Map getMavenSurefirePluginOption(MavenProject project, String optionName, String defaultValue) throws IOException {
		return getMavenPluginOption(project, "maven-surefire-plugin", optionName, defaultValue);
	}

	/**
	 * Return an array of map of <code>optionName</code> value defined in a
	 * project for the "maven-surefire-plugin" plugin.
	 * 
	 * @param project
	 *            not null
	 * @param optionName
	 *            the option name wanted
	 * @param defaultValue
	 *            a default value
	 * @return the array of option name or the default value. Could be null if
	 *         not found.
	 * @throws IOException
	 *             if any
	 */
	public static Map[] getMavenSurefirePluginOptions(MavenProject project, String optionName, String defaultValue) throws IOException {
		return getMavenPluginOptions(project, "maven-surefire-plugin", optionName, defaultValue);
	}

	/**
	 * Return the <code>optionName</code> value defined in a project for the
	 * "maven-javadoc-plugin" plugin.
	 * 
	 * @param project
	 *            not null
	 * @param optionName
	 *            the option name wanted
	 * @param defaultValue
	 *            a default value
	 * @return the value for the option name or the default value. Could be null
	 *         if not found.
	 * @throws IOException
	 *             if any
	 */
	public static String getMavenJavadocPluginBasicOption(MavenProject project, String optionName, String defaultValue) throws IOException {
		return getMavenPluginBasicOption(project, "maven-javadoc-plugin", optionName, defaultValue);
	}

	/**
	 * Return a map of <code>optionName</code> value defined in a project for
	 * the "maven-javadoc-plugin" plugin.
	 * 
	 * @param project
	 *            not null
	 * @param optionName
	 *            the option name wanted
	 * @param defaultValue
	 *            a default value
	 * @return the map for the option name or the default value. Could be null
	 *         if not found.
	 * @throws IOException
	 *             if any
	 */
	public static Map getMavenJavadocPluginOption(MavenProject project, String optionName, String defaultValue) throws IOException {
		return getMavenPluginOption(project, "maven-javadoc-plugin", optionName, defaultValue);
	}

	/**
	 * Return an array of map of <code>optionName</code> value defined in a
	 * project for the "maven-javadoc-plugin" plugin.
	 * 
	 * @param project
	 *            not null
	 * @param optionName
	 *            the option name wanted
	 * @param defaultValue
	 *            a default value
	 * @return an array of option name. Could be null if not found.
	 * @throws IOException
	 *             if any
	 */
	public static Map[] getMavenJavadocPluginOptions(MavenProject project, String optionName, String defaultValue) throws IOException {
		return getMavenPluginOptions(project, "maven-javadoc-plugin", optionName, defaultValue);
	}

	/**
	 * Return the <code>optionName</code> value defined in a project for the
	 * "maven-jar-plugin" plugin.
	 * 
	 * @param project
	 *            not null
	 * @param optionName
	 *            the option name wanted
	 * @param defaultValue
	 *            a default value
	 * @return the value for the option name or the default value. Could be null
	 *         if not found.
	 * @throws IOException
	 *             if any
	 */
	public static String getMavenJarPluginBasicOption(MavenProject project, String optionName, String defaultValue) throws IOException {
		return getMavenPluginBasicOption(project, "maven-jar-plugin", optionName, defaultValue);
	}

	/**
	 * Return the <code>optionName</code> value defined in a project for the
	 * "maven-ear-plugin" plugin.
	 * 
	 * @param project
	 *            not null
	 * @param optionName
	 *            the option name wanted
	 * @param defaultValue
	 *            a default value
	 * @return the value for the option name or the default value. Could be null
	 *         if not found.
	 * @throws IOException
	 *             if any
	 */
	public static String getMavenEarPluginBasicOption(MavenProject project, String optionName, String defaultValue) throws IOException {
		return getMavenPluginBasicOption(project, "maven-ear-plugin", optionName, defaultValue);
	}

	/**
	 * Return the <code>optionName</code> value defined in a project for the
	 * "maven-war-plugin" plugin.
	 * 
	 * @param project
	 *            not null
	 * @param optionName
	 *            the option name wanted
	 * @param defaultValue
	 *            a default value
	 * @return the value for the option name or the default value. Could be null
	 *         if not found.
	 * @throws IOException
	 *             if any
	 */
	public static String getMavenWarPluginBasicOption(MavenProject project, String optionName, String defaultValue) throws IOException {
		return getMavenPluginBasicOption(project, "maven-war-plugin", optionName, defaultValue);
	}

	// ----------------------------------------------------------------------
	// Convenience methods
	// ----------------------------------------------------------------------

	/**
	 * Return the value for the option <code>optionName</code> defined in a
	 * project with the given <code>artifactId</code> plugin. <br/>
	 * Example:
	 * <table>
	 * <tr>
	 * <td>Configuration</td>
	 * <td>Result</td>
	 * </tr>
	 * <tr>
	 * <td>
	 * 
	 * <pre>
	 * &lt;option&gt;value&lt;/option&gt;
	 * </pre>
	 * 
	 * </td>
	 * <td>
	 * 
	 * <pre>
	 * value
	 * </pre>
	 * 
	 * </td>
	 * </tr>
	 * </table>
	 * 
	 * @param project
	 *            not null
	 * @param pluginArtifactId
	 *            not null
	 * @param optionName
	 *            an <code>Xpath</code> expression from the plugin
	 *            <code>&lt;configuration/&gt;</code>
	 * @param defaultValue
	 *            could be null
	 * @return the value for the option name or null if not found
	 * @throws IOException .- if any
	 */
	private static String getMavenPluginBasicOption(MavenProject project, String pluginArtifactId, String optionName, String defaultValue) throws IOException {
		return (String) getMavenPluginConfigurationsImpl(project, pluginArtifactId, optionName, defaultValue).get(optionName);
	}

	/**
	 * Return a Map for the option <code>optionName</code> defined in a project
	 * with the given <code>artifactId</code> plugin. <br/>
	 * Example:
	 * <table>
	 * <tr>
	 * <td>Configuration</td>
	 * <td>Result</td>
	 * </tr>
	 * <tr>
	 * <td>
	 * 
	 * <pre>
	 * &lt;option&gt;
	 *  &lt;param1&gt;value1&lt;/param1&gt;
	 *  &lt;param2&gt;value2&lt;/param2&gt;
	 * &lt;/option&gt;
	 * </pre>
	 * 
	 * </td>
	 * <td>
	 * 
	 * <pre>{param1=value1, param2=value2}
	 * 
	 * <pre></td>
	 * </tr>
	 * </table>
	 * 
	 * @param project
	 *            not null
	 * @param pluginArtifactId
	 *            not null
	 * @param optionName
	 *            an <code>Xpath</code> expression from the plugin
	 *            <code>&lt;configuration/&gt;</code>
	 * @param defaultValue
	 *            could be null
	 * @return the value for the option name or null if not found
	 * @throws IOException
	 *             if any
	 */
	private static Map getMavenPluginOption(MavenProject project, String pluginArtifactId, String optionName, String defaultValue) throws IOException {
		return (Map) getMavenPluginConfigurationsImpl(project, pluginArtifactId, optionName, defaultValue).get(optionName);
	}

	/**
	 * Return an array of Map for the option <code>optionName</code> defined in
	 * a project with the given <code>artifactId</code> plugin. <br/>
	 * Example:
	 * <table>
	 * <tr>
	 * <td>Configuration</td>
	 * <td>Result</td>
	 * </tr>
	 * <tr>
	 * <td>
	 * 
	 * <pre>
	 * &lt;options&gt;
	 *   &lt;option&gt;
	 *    &lt;param1&gt;value1&lt;/param1&gt;
	 *    &lt;param2&gt;value2&lt;/param2&gt;
	 *   &lt;/option&gt;
	 *   &lt;option&gt;
	 *    &lt;param1&gt;value1&lt;/param1&gt;
	 *    &lt;param2&gt;value2&lt;/param2&gt;
	 *   &lt;/option&gt;
	 * &lt;/options&gt;
	 * </pre>
	 * 
	 * </td>
	 * <td>
	 * 
	 * <pre>[{option=[{param1=value1, param2=value2}]}, {option=[{param1=value1, param2=value2}]
	 * 
	 * <pre></td>
	 * </tr>
	 * </table>
	 * 
	 * @param project
	 *            not null
	 * @param pluginArtifactId
	 *            not null
	 * @param optionName
	 *            an <code>Xpath</code> expression from the plugin
	 *            <code>&lt;configuration/&gt;</code>
	 * @param defaultValue
	 *            could be null
	 * @return the value for the option name or null if not found
	 * @throws IOException
	 *             if any
	 */
	private static Map[] getMavenPluginOptions(MavenProject project, String pluginArtifactId, String optionName, String defaultValue) throws IOException {
		return (Map[]) getMavenPluginConfigurationsImpl(project, pluginArtifactId, optionName, defaultValue).get(optionName);
	}

	/**
	 * Return a Map for the option <code>optionName</code> defined in a project
	 * with the given <code>artifactId</code> plugin. <br/>
	 * Example:
	 * <table>
	 * <tr>
	 * <td>Configuration</td>
	 * <td>Result</td>
	 * </tr>
	 * <tr>
	 * <td>
	 * 
	 * <pre>
	 * &lt;option&gt;value&lt;/option&gt;
	 * </pre>
	 * 
	 * </td>
	 * <td>
	 * 
	 * <pre>
	 * { option = value }
	 * </pre>
	 * 
	 * </td>
	 * </tr>
	 * <tr>
	 * <td>
	 * 
	 * <pre>
	 * &lt;option&gt;
	 *  &lt;param1&gt;value1&lt;/param1&gt;
	 *  &lt;param2&gt;value2&lt;/param2&gt;
	 * &lt;/option&gt;
	 * </pre>
	 * 
	 * </td>
	 * <td>
	 * 
	 * <pre>{option={param1=value1, param2=value2}}
	 * 
	 * <pre></td>
	 * </tr>
	 * <tr>
	 * <td>
	 * 
	 * <pre>
	 * &lt;options&gt;
	 *   &lt;option&gt;
	 *    &lt;param1&gt;value1&lt;/param1&gt;
	 *    &lt;param2&gt;value2&lt;/param2&gt;
	 *   &lt;/option&gt;
	 *   &lt;option&gt;
	 *    &lt;param1&gt;value1&lt;/param1&gt;
	 *    &lt;param2&gt;value2&lt;/param2&gt;
	 *   &lt;/option&gt;
	 * &lt;/options&gt;
	 * </pre>
	 * 
	 * </td>
	 * <td>
	 * 
	 * <pre>{options=[{option=[{param1=value1, param2=value2}]}, {option=[{param1=value1, param2=value2}]}]
	 * 
	 * <pre></td>
	 * </tr>
	 * </table>
	 * 
	 * @param project
	 *            not null
	 * @param pluginArtifactId
	 *            not null
	 * @param optionName
	 *            an <code>Xpath</code> expression from the plugin
	 *            <code>&lt;configuration/&gt;</code>
	 * @param defaultValue
	 *            could be null
	 * @return a map with the options found
	 * @throws IOException
	 *             if any
	 */
	private static Map getMavenPluginConfigurationsImpl(MavenProject project, String pluginArtifactId, String optionName, String defaultValue) throws IOException {
		List<Object> plugins = new ArrayList<Object>();

		for (Object obj : project.getModel().getReporting().getPlugins()) {
			plugins.add(obj);
		}

		for (Object obj : project.getModel().getBuild().getPlugins()) {
			plugins.add(obj);
		}

		if (project.getBuild().getPluginManagement() != null) {
			for (Object obj : project.getBuild().getPluginManagement().getPlugins()) {
				plugins.add(obj);
			}
		}

		for (Object next : plugins) {

			Object pluginConf = null;

			if (next instanceof Plugin) {
				Plugin plugin = (Plugin) next;

				// using out-of-box Maven plugins
				if (!((plugin.getGroupId().equals("org.apache.maven.plugins")) && (plugin.getArtifactId().equals(pluginArtifactId)))) {
					continue;
				}

				pluginConf = plugin.getConfiguration();
			}

			if (next instanceof ReportPlugin) {
				ReportPlugin reportPlugin = (ReportPlugin) next;

				// using out-of-box Maven plugins
				if (!((reportPlugin.getGroupId().equals("org.apache.maven.plugins")) && (reportPlugin.getArtifactId().equals(pluginArtifactId)))) {
					continue;
				}

				pluginConf = reportPlugin.getConfiguration();
			}

			if (pluginConf == null) {
				continue;
			}

			try {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(pluginConf.toString().getBytes("UTF-8")));

				NodeList nodeList = XPathAPI.eval(doc, "//configuration/" + optionName).nodelist();
				if (nodeList.getLength() > 0) {
					Node optionNode = nodeList.item(0);

					if (isList(optionNode)) {
						/*
						 * <optionNames> <optionName> <param1>value1</param1>
						 * <param2>value2</param2> </optionName> </optionNames>
						 */
						Map options = new HashMap();

						List optionNames = new ArrayList();
						NodeList childs = optionNode.getChildNodes();
						for (int i = 0; i < childs.getLength(); i++) {
							Node child = childs.item(i);
							if (child.getNodeType() == Node.ELEMENT_NODE) {
								Map option = new HashMap();

								if (isElementContent(child)) {
									Map properties = new HashMap();
									NodeList childs2 = child.getChildNodes();
									if (childs2.getLength() > 0) {
										for (int j = 0; j < childs2.getLength(); j++) {
											Node child2 = childs2.item(j);
											if (child2.getNodeType() == Node.ELEMENT_NODE) {
												properties.put(child2.getNodeName(), getTextContent(child2));
											}
										}
										option.put(child.getNodeName(), properties);
									}
								} else {
									option.put(child.getNodeName(), getTextContent(child));
								}

								optionNames.add(option);
							}
						}

						options.put(optionName, optionNames.toArray(new Map[0]));

						return options;
					}

					if (isElementContent(optionNode)) {
						/*
						 * <optionName> <param1>value1</param1>
						 * <param2>value2</param2> </optionName>
						 */
						Map option = new HashMap();

						NodeList childs = optionNode.getChildNodes();
						if (childs.getLength() > 1) {
							Map parameters = new HashMap();

							for (int i = 0; i < childs.getLength(); i++) {
								Node child = childs.item(i);
								if (child.getNodeType() == Node.ELEMENT_NODE) {
									parameters.put(child.getNodeName(), getTextContent(child));
								}
							}

							option.put(optionName, parameters);
						}
						return option;
					} else {
						/*
						 * <optionName>value1</optionName>
						 */
						Map option = new HashMap();

						option.put(optionName, getTextContent(optionNode));

						return option;
					}
				}
			} catch (Exception e) {
				throw new IOException("Exception occured: " + e.getMessage());
			}
		}

		Map properties = new HashMap();
		properties.put(optionName, defaultValue);

		return properties;
	}

	/**
	 * Write copy tasks in an outputDir for EAR and WAR targets for project
	 * depencies without <code>provided</code> or <code>test</code> as scope
	 * 
	 * @param writer
	 * @param project
	 * @param artifactResolverWrapper
	 * @param outputDir
	 */
	private static void writeCopyLib(XMLWriter writer, MavenProject project, String inputLibDir, String outputLibDir) {
		writer.startElement("mkdir");
		writer.addAttribute("dir", outputLibDir);
		writer.endElement(); // mkdir

		writer.startElement("copy");
		writer.addAttribute("todir", outputLibDir);
		writer.startElement("fileset");
		writer.addAttribute("dir", inputLibDir);//
		writer.addAttribute("includes", "**");
		writer.endElement(); // fileset
		writer.endElement(); // copy
	}

	/**
	 * Check if a given <code>node</code> is a list of nodes or not. <br/>
	 * For instance, the node <code>options</code> is a list of
	 * <code>option</code> in the following case:
	 * 
	 * <pre>
	 * &lt;options&gt;
	 *   &lt;option&gt;
	 *    &lt;param1&gt;value1&lt;/param1&gt;
	 *    &lt;param2&gt;value2&lt;/param2&gt;
	 *   &lt;/option&gt;
	 *   &lt;option&gt;
	 *    &lt;param1&gt;value1&lt;/param1&gt;
	 *    &lt;param2&gt;value2&lt;/param2&gt;
	 *   &lt;/option&gt;
	 * &lt;/options&gt;
	 * </pre>
	 * 
	 * @param node
	 *            a given node, may be <code>null</code>.
	 * @return true if the node is a list, false otherwise.
	 */
	private static boolean isList(Node node) {
		if (node == null) {
			return false;
		}

		NodeList children = node.getChildNodes();

		boolean isList = false;
		String lastNodeName = null;
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				isList = isList || (child.getNodeName().equals(lastNodeName));
				lastNodeName = child.getNodeName();
			}
		}
		if (StringUtils.isNotEmpty(lastNodeName)) {
			isList = isList || lastNodeName.equals(getSingularForm(node.getNodeName()));
		}

		return isList;
	}

	/**
	 * Checks whether the specified node has element content or consists only of
	 * character data.
	 * 
	 * @param node
	 *            The node to test, may be <code>null</code>.
	 * @return <code>true</code> if any child node is an element,
	 *         <code>false</code> otherwise.
	 */
	private static boolean isElementContent(Node node) {
		if (node == null) {
			return false;
		}
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the text content of the specified node.
	 * 
	 * @param node
	 *            The node whose text contents should be retrieved, may be
	 *            <code>null</code>.
	 * @return The text content of the node, can be empty but never
	 *         <code>null</code>.
	 */
	private static String getTextContent(Node node) {
		StringBuffer buffer = new StringBuffer();
		if (node != null) {
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
					buffer.append(child.getNodeValue());
				}
			}
		}
		return buffer.toString();
	}

	/**
	 * Gets the singular form of the specified (English) plural form. For
	 * example:
	 * 
	 * <pre>
	 * properties -&gt; property
	 * branches   -&gt; branch
	 * reports    -&gt; report
	 * </pre>
	 * 
	 * @param pluralForm
	 *            The plural form for which to derive the singular form, may be
	 *            <code>null</code>.
	 * @return The corresponding singular form or an empty string if the input
	 *         string was not recognized as a plural form.
	 */
	static String getSingularForm(String pluralForm) {
		String singularForm = "";
		if (StringUtils.isNotEmpty(pluralForm)) {
			if (pluralForm.endsWith("ies")) {
				singularForm = pluralForm.substring(0, pluralForm.length() - 3) + 'y';
			} else if (pluralForm.endsWith("ches")) {
				singularForm = pluralForm.substring(0, pluralForm.length() - 2);
			} else if (pluralForm.endsWith("s") && pluralForm.length() > 1) {
				singularForm = pluralForm.substring(0, pluralForm.length() - 1);
			}
		}
		return singularForm;
	}

	/**
	 * Relativizes the specified path against the given base directory (if
	 * possible). If the specified path is a subdirectory of the base directory,
	 * the base directory prefix will be chopped off. If the specified path is
	 * equal to the base directory, the path "." is returned. Otherwise, the
	 * path is returned as is. Examples:
	 * <table border="1">
	 * <tr>
	 * <td>basedir</td>
	 * <td>path</td>
	 * <td>result</td>
	 * </tr>
	 * <tr>
	 * <td>/home</td>
	 * <td>/home/dir</td>
	 * <td>dir</td>
	 * </tr>
	 * <tr>
	 * <td>/home</td>
	 * <td>/home/dir/</td>
	 * <td>dir/</td>
	 * </tr>
	 * <tr>
	 * <td>/home</td>
	 * <td>/home</td>
	 * <td>.</td>
	 * </tr>
	 * <tr>
	 * <td>/home</td>
	 * <td>/home/</td>
	 * <td>./</td>
	 * </tr>
	 * <tr>
	 * <td>/home</td>
	 * <td>dir</td>
	 * <td>dir</td>
	 * </tr>
	 * </table>
	 * The returned path will always use the forward slash ('/') as the file
	 * separator regardless of the current platform. Also, the result path will
	 * have a trailing slash if the input path has a trailing file separator.
	 * 
	 * @param basedir
	 *            The base directory to relativize the path against, must not be
	 *            <code>null</code>.
	 * @param path
	 *            The path to relativize, must not be <code>null</code>.
	 * @return The relativized path, never <code>null</code>.
	 */
	static String toRelative(File basedir, String path) {
		String result = null;
		if (new File(path).isAbsolute()) {
			String pathNormalized = path.replace('/', File.separatorChar).replace('\\', File.separatorChar);
			result = PathTool.getRelativeFilePath(basedir.getAbsolutePath(), pathNormalized);
		}
		if (result == null) {
			result = path;
		}
		result = result.replace('\\', '/');
		if (result.length() <= 0 || "/".equals(result)) {
			result = '.' + result;
		}
		return result;
	}
}
