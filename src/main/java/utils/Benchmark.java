package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

public class Benchmark {
	private static final String service = "http://dbpedia.org/sparql";
	private List<Resource> subject = new ArrayList<Resource>(); 
	private Model model = ModelFactory.createDefaultModel();
	private int totalStatements = 0;
	private int valid = 0;
	private int sizeModel = 0;
	
	
	
	public Benchmark(String modelPath, String category) {
		 initializeSubjects("Presidents_of_the_United_States");
		 File m = new File(modelPath);
		 try {
			model.read(new FileInputStream(m),null, "TTL");
			StmtIterator it =  model.listStatements();
			while (it.hasNext()) {
			     Statement stmt = it.next();
			     if(subject.contains(stmt.getSubject())) {
			    	 System.out.println(stmt);
			    	 sizeModel++;	    	 
			     }
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void initializeSubjects(String category) {
		String q = "SELECT ?subject {?subject <http://purl.org/dc/terms/subject> <http://dbpedia.org/resource/Category:" + category + "> }";
		QueryExecution qe = QueryExecutionFactory.sparqlService(service, q);
		ResultSet rs = qe.execSelect();
		while(rs.hasNext()) {
			QuerySolution s = rs.nextSolution();
			subject.add(s.getResource("subject"));
		}	
	}
	
	
	private void benchmark(List<String> dumpPaths) {
		for(String path : dumpPaths) {
			benchmarkForDump(path);
		}
		
		System.out.println("The model contains " + sizeModel + "statements of given category. " + valid + " are classified as valid by the dumps. All dumps combined contain " + totalStatements + "statements of given category"  );
	}
	
	private void benchmarkForDump(String file) {
		 File dump = new File(file);
		 Model dumpModel = ModelFactory.createDefaultModel() ;
		 int validDump = 0;
		 int totalDump = 0;
		 try {

			dumpModel.read(new FileInputStream(dump),null, "TTL");
			StmtIterator it =  dumpModel.listStatements();
			while (it.hasNext()) {
			     Statement stmt = it.next();
			     if(subject.contains(stmt.getSubject())) {
			    	 totalStatements++;
			    	 totalDump++;
			    	 if(model.contains(stmt)) {
			    		 validDump++;
			    		 valid++;
			    	 }
			    		 
			     }
			     
			}
			System.out.println(dump.toString() + "contains " + totalDump + "statements of given category and the given model contains " + validDump + " of them." );
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	public static void main(String[] args) {
		final PrintStream err = new PrintStream(System.err);
		try {
			System.setErr(new PrintStream("text"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Benchmark b = new Benchmark("resources/model.ttl", "Presidents_of_the_United_States" );
//		List<String> dumps = Arrays.asList("src/main/resources/disambiguations_en.ttl", "src/main/resources/instance_types_en.ttl", "src/main/resources/instance_types_transitive_en.ttl",
//										    "src/main/resources/labels_en.ttl","src/main/resources/long_abstracts_en.ttl", "src/main/resources/mappingbased_literals_en.ttl", 
//											"src/main/resources/mappingbased_objects_en.ttl", "src/main/resources/persondata_en.ttl", "src/main/resources/specific_mappingbased_properties_en.ttl", "src/main/resources/transitive_redirects_en.ttl");
		
		List<String> dumps = Arrays.asList("resources/instance_types_en.ttl");
		b.benchmark(dumps);
		System.setErr(err);
	}	
		
		
}
