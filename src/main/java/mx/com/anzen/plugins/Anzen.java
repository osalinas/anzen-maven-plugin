package mx.com.anzen.plugins;


public enum Anzen {

	BuildFinalName("build.finalName"),
	BuildDir("build.dir"),
	BuildOutputDir("build.outputDir"),
	BuildSrcDir("build.srcDir."),
	BuildResourceDir("build.resourceDir."),
	BuildTestOutputDir("build.testOutputDir"),
	BuildTestDir("build.testDir."),
	BuildTestResourceDir("build.testResourceDir."),
	BuildModuleDir("build.module.dir"),
	BuildClasspathDir("build.classpath.dir"),
	BuildParentDir("parent.dir"),

	TestReports("test.reports"),
	ReportingOutputDirectory("reporting.outputDirectory"),
	SettingsOffline("settings.offline"),
	SettingsInteractiveMode("settings.interactiveMode"),
	TestSkip("test.skip"),
	JavadocDir("javadoc.dir"),
	BUILD("build"),
	BaseDir("base.dir");

	private final String property;

	Anzen(String property){
		this.property = property;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return this.property;
	}

}
