package utils;

public class Relation {
	
	private String label;
	
	private String keywords;
	
	private String range;
	
	private String domain;
	
	private String uri;
	
	
	public Relation() {
		
	}
	
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getRange() {
		return range;
	}

	public void setRange(String range) {
		this.range = range;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	@Override
	public String toString() {
		return "Label: " + label + " Keywords: " + keywords  + " Range: " + range + " Domain: " + domain;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
}
