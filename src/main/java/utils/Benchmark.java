package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
	
	public void merge(List<String> dumps) throws IOException {
	
		FileWriter fw = new FileWriter(new File("resources/dump.txt"),true);	
		for(String path : dumps) {
		     System.out.println(path);		
			 String line;
			 BufferedReader br = new BufferedReader(new FileReader(path));
			       while ((line = br.readLine()) != null) { 
			    	   for(Resource r: subject) {
			    		   if(line.startsWith("<"+ r +">")) {
			    			   fw.write(line + "\r\n");
			    		   }
			    			   
			    	   }
			    	  
		       } 
		
			 br.close();
	
		}
		
		  fw.close();
		}
	
		
	public static void main(String[] args) {
		try {
	       
	   
		
		final PrintStream err = new PrintStream(System.err);
		try {
			System.setErr(new PrintStream("text"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Benchmark b = new Benchmark("resources/model.ttl", "Presidents_of_the_United_States" );
		List<String> dumps = Arrays.asList("resources/disambiguations_en.ttl", "resources/instance_types_en.ttl", "resources/instance_types_transitive_en.ttl",
										    "resources/labels_en.ttl","resources/long_abstracts_en.ttl", "resources/mappingbased_literals_en.ttl", 
											"resources/mappingbased_objects_en.ttl", "resources/persondata_en.ttl" , "resources/specific_mappingbased_properties_en.ttl" ,  "resources/transitive_redirects_en.ttl" );
		
	//	List<String> dumps = Arrays.asList("resources/instance_types_en.ttl");
		try {
			b.merge(dumps);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.setErr(err);
	
		
	 } catch (Throwable ex) {
	        System.err.println("Uncaught exception - " + ex.getMessage());
	        ex.printStackTrace(System.err);
	    }
	}
}