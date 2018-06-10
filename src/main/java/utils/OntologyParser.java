package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class OntologyParser {
	
	private static final String DOMAIN = "http://www.w3.org/2000/01/rdf-schema#domain";
	
	private static final String RANGE = "http://www.w3.org/2000/01/rdf-schema#range";
	
	private static final String COMMENT = "http://www.w3.org/2000/01/rdf-schema#comment";
	
	private static final int CLASSESENDLINE = 8388;
	
	private static final int DATATYPESTARTLINE = 29324;
	
	public static void main(String[] args) {
		BufferedReader reader = null;
		FileWriter writer = null;
		try {
    	 		reader = new BufferedReader(new FileReader("dbpedia_2016-10 (1).nt"));
    	 		writer = new FileWriter("ontology_english.nt");
    	 		
    	 		String nextLine =null;
    	 		int i = 0;
    	 		while((nextLine=reader.readLine())!=null) { 
    	 				if(i>CLASSESENDLINE && !(i>DATATYPESTARTLINE && i<30089) && (nextLine.contains("@en") && !nextLine.contains(COMMENT)) || nextLine.contains(RANGE) || nextLine.contains(DOMAIN)) {
    	 					writer.write(nextLine + "\r\n");
    	 			}
    	 			i++;
    	 		} 
		} catch ( IOException  e) {
			e.printStackTrace();
		} finally{
			try {
				reader.close();
				writer.close();
			} catch (IOException  e) {
				e.printStackTrace();
			}
		}
	}
}
