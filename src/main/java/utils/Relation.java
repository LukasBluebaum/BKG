package utils;

import java.util.ArrayList;
import java.util.Arrays;

public class Relation {
	
	private String label;
	
	private ArrayList<String> keywords ;
	
	private String range;
	
	private String domain;
	
	private int countRelation;
			
	public Relation() {
		range = "";
		domain = "";
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
		this.range = range.substring(range.lastIndexOf("/")+1, range.length());
	}

	public ArrayList<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = new ArrayList<String>(Arrays.asList(keywords.split(" ")));
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

	public int getCountRelation() {
		return countRelation;
	}

	public void setCountRelation(int countRelation) {
		this.countRelation = countRelation;
	}

}
