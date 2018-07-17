package utils;

import java.util.ArrayList;

/**
 * Each instance represents a entity we received from Spotlight.
 * @author Lukas Blübaum
 * @author Nick Düsterhus
 * @author Monika Werner
 */
public class Entity {
	
	private ArrayList<String> types = new ArrayList<String>();
	
	private String uri;
	
	private String surfaceForm;
	
	private int offset;
	
	public Entity(String types, String uri, String surfaceForm, int offset) {
		this.toList(types);
		this.setUri(uri);
		this.setSurfaceForm(surfaceForm);
		this.setOffset(offset);
	}

	private void toList(String t) {
		String[] comma = t.split(",");
		for(String typeLink: comma) {
			String[] type = typeLink.split(":");
			if(type.length == 2) types.add(type[1]);
		}
	}
	
	public ArrayList<String> getTypes() {
		return types;
	}
	
	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public String toString() {
		return "URI: " + uri + "- Type:" + types + "- Surface Form: " + surfaceForm + "- Offset: " + offset;
	}

	public String getSurfaceForm() {
		return surfaceForm;
	}

	public void setSurfaceForm(String surfaceForm) {
		this.surfaceForm = surfaceForm;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}
}
