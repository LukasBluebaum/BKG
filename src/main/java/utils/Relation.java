package utils;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class Relation {
	
	private String label;
	
	private ArrayList<String> keywords;
	
	private String range;
	
	private String domain;
	
	private int countRelation;
	
	private String propertyType;	
				
	public Relation() {
		range = "";
		domain = "";
		keywords = new ArrayList<String>();
	}
	
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain.substring(domain.lastIndexOf("/")+1, domain.length());
	}

	public String getRange() {
		return range;
	}

	public void setRange(String range) {
		if(range.contains("http://www.w3.org/2001/")) {
			this.range = range;			
		} else {			
			this.range = range.substring(range.lastIndexOf("/")+1, range.length());
		}
	}

	public ArrayList<String> getKeywords() {
		return keywords;
	}

	public void setKeys(String keywords) {
		String[] keyList = Pattern.compile("\\(.*?\\)").matcher(keywords).replaceAll("").split(" ");
		for(String key: keyList) {
			if(key.length() > 2) this.keywords.add(key);
		}
	}
	
    public void setKeywords(ArrayList<String> keywords) {
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
		return "Label: " + label + " Keywords: " + keywords  + " Range: " + range + " Domain: " + domain + "type:" + propertyType;
	}

	public int getCountRelation() {
		return countRelation;
	}

	public void setCountRelation(int countRelation) {
		this.countRelation = countRelation;
	}
	
	public String getPropertyType() {
		return propertyType;
	}
	
	public void setPropertyType(String propertyType) {
		if(propertyType.equals("http://www.w3.org/2002/07/owl#DatatypeProperty") || propertyType.equals("data") ) {
			this.propertyType = "data";
		}
		else {
			this.propertyType = "object";
		}	
	}
}
