package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
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

/**
 * Benchmarks the given model. Calculates all the triples from the given model {@link #model} that are contained inside the dumps that were specified.
 * @author Lukas Blübaum
 * @author Nick Düsterhus
 * @author Monika Werner
 */
public class Benchmark {
	private static final String service = "http://dbpedia.org/sparql";
	private List<Resource> subject = new ArrayList<Resource>(); 
	
	/**
	 * The model that should be evaluated.
	 */
	private Model model = ModelFactory.createDefaultModel();
	private int totalStatements = 0;
	private int valid = 0;
	private int sizeModel = 0;
	private int sizeWithoutTypes = 0;
	private int validWithoutTypes = 0;
	private int sizeWithoutTypesDump = 0;
	
	
	/**
	 * Initializes the given models and the given category.
	 * @param modelPath 
	 * @param category
	 */
	public Benchmark(String modelPath, String category) {
		 initializeSubjects(category);
		 File m = new File(modelPath);
		 try {
			model.read(new FileInputStream(m),null, "TTL");
			StmtIterator it =  model.listStatements();
			while (it.hasNext()) {
			     Statement stmt = it.next();
			     if(subject.contains(stmt.getSubject())) {
			    	 sizeModel++;	    	 
			    	if(! stmt.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
			    		sizeWithoutTypes ++;
			    	}
			     }
			}			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Queries all subjects of the given category from DBpedia.
	 * @param category
	 */
	private void initializeSubjects(String category) {
		String q = "SELECT ?subject {?subject <http://purl.org/dc/terms/subject> <http://dbpedia.org/resource/Category:" + category + "> }";
		QueryExecution qe = QueryExecutionFactory.sparqlService(service, q);
		ResultSet rs = qe.execSelect();
		while(rs.hasNext()) {
			QuerySolution s = rs.nextSolution();
			subject.add(s.getResource("subject"));
		}	
	}
	
	
	/**
	 * Calls {@link #benchmarkForDump(String)} for every dump that is contained in the list. So overall it calculates
	 * all the triples from the given model {@link #model} that are contained inside the dumps.
	 * @param dumpPaths List of paths for the given dumps.
	 */
	private void benchmark(List<String> dumpPaths) {
		for(String path : dumpPaths) {
			benchmarkForDump(path);
		}
		System.out.println("Number of statements in the dumps not considering the types:" + sizeWithoutTypesDump);
		System.out.println("The model contains " + sizeModel + " statements of given category. " + valid + " are classified as valid by the dumps. All dumps combined contain " + totalStatements + " statements of given category"  );
		System.out.println("The model contains " + sizeWithoutTypes + " statements of given category that are not types. " + validWithoutTypes + " are classified as valid by the dumps. All dumps combined contain " + totalStatements + " statements of given category"  );
		
	}
	
	/**
	 * Calculates the amount of triple from the model {@link #model} given to the benchmark class that are contained in the given dump.
	 * @param file Path to dump.
	 */
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
			    	 if(!stmt.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
				    		sizeWithoutTypesDump++;
				     } 
			    	 if(model.contains(stmt)) {
			    		 validDump++;
			    		 if(! stmt.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
					    		validWithoutTypes++;
					     } 		    		
			    		 valid++;
			    	 }
			     }
			     
			}
			System.out.println("The given dump " + dump.toString() + " contains " + totalDump + " statements of given category and the given model contains " + validDump + " of them." );
			writeNotFound(dumpModel);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes all triples from the given model that are not contained in the dump to a file. 
	 * @param dump
	 */
	private void writeNotFound(Model dump) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(new File("resources/notFound.txt"),true);
		
			StmtIterator it =  model.listStatements();
			while(it.hasNext()) {
				 Statement stmt = it.next();
			     if(subject.contains(stmt.getSubject())) {
			    	 if(!dump.contains(stmt)) {
			    		 fw.write(stmt + "\r\n");
			    	 }
			     }
			} 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Merging all triples from the given categories over multiple N-Triple dumps into one dump located at evaluation/dump
	 * @param dumps
	 * @throws IOException
	 */
	public void merge(List<String> dumps) throws IOException {	
		FileWriter fw = new FileWriter(new File("evaluation/dump.txt"),true);	
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
			@SuppressWarnings("unused")
			final PrintStream err = new PrintStream(System.err);
			try {
				System.setErr(new PrintStream("text"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			Benchmark b = new Benchmark("evaluation/model.ttl", "Presidents_of_the_United_States" );
//			List<String> dumps = Arrays.asList("resources/disambiguations_en.ttl", "resources/instance_types_en.ttl", "resources/instance_types_transitive_en.ttl",
//											    "resources/labels_en.ttl","resources/long_abstracts_en.ttl", "resources/mappingbased_literals_en.ttl", 
//												"resources/mappingbased_objects_en.ttl", "resources/persondata_en.ttl" , "resources/specific_mappingbased_properties_en.ttl" ,  "resources/transitive_redirects_en.ttl" );
//			
//			List<String> dumps = Arrays.asList("resources/instance_types_en.ttl");
//			try {
//					b.merge(dumps);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
	
			List<String> dump = Arrays.asList("evaluation/dump.ttl");
			b.benchmark(dump);
		
		 } catch (Throwable ex) {
		        System.err.println("Uncaught exception - " + ex.getMessage());
		        ex.printStackTrace(System.err);
		    }
	}
}