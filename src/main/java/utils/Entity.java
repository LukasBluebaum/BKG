package utils;

public class Entity {
	
	private String types;
	
	private String uri;
	
	public Entity(String types, String uri) {
		this.setType(types);
		this.setUri(uri);
	}

	public String getType() {
		return types;
	}

	public void setType(String type) {
		this.types = type;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public String toString() {
		return "URI: " + uri + "- Type:" + types;
	}
}
