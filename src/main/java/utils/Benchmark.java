package utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;

public class Benchmark {
	private static final String service = "http://dbpedia.org/sparql";
	private List<Resource> subject = new ArrayList<Resource>(); 
	
	
	
	
	private void initializeSubjects(String category) {
		String q = "SELECT ?subject {?subject <http://purl.org/dc/terms/subject> <http://dbpedia.org/resource/Category:" + category + "> }";
		QueryExecution qe = QueryExecutionFactory.sparqlService(service, q);
		ResultSet rs = qe.execSelect();
		while(rs.hasNext()) {
			QuerySolution s = rs.nextSolution();
			subject.add(s.getResource("subject"));
		}	
		System.out.println(subject);
	}
	
	
	
	public static void main(String[] args) {
	
		Benchmark b = new Benchmark();
		b.initializeSubjects("Presidents_of_the_United_States");
	 
	
	}	
		
		
}
