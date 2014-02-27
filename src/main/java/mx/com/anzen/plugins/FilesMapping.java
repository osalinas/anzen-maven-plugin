package mx.com.anzen.plugins;

public class FilesMapping {

	private boolean isConfigFile = false;
	private boolean isRestorable = true;
	private String sourceDirectory;
	private String destinationDirectory;

	public boolean isRestorable() {
		return isRestorable;
	}
	public void setRestorable(boolean isRestorable) {
		this.isRestorable = isRestorable;
	}
	public boolean isConfigFile() {
		return isConfigFile;
	}
	public void setConfigFile(boolean isConfigFile) {
		this.isConfigFile = isConfigFile;
	}
	public String getSourceDirectory() {
		return sourceDirectory;
	}
	public void setSourceDirectory(String sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}
	public String getDestinationDirectory() {
		return destinationDirectory;
	}
	public void setDestinationDirectory(String destinationDirectory) {
		this.destinationDirectory = destinationDirectory;
	}

}
