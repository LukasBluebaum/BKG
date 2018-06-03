package extraction;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.json.simple.parser.ParseException;

import edu.stanford.nlp.util.CoreMap;
import utils.Relation;

public class RelationExtraction {
	
	private static final FoxWebservice SERVICE = new FoxWebservice();
	
	private static final NLPParser PARSER = new NLPParser();
		
	private static final int DELAYSECONDS = 10; 
	
	private static final int STARTLINE = 0;
	
	private static final int LINESPERWRITE = 10;
	
	private static AtomicInteger count;
		
	private static Model graph = ModelFactory.createDefaultModel() ;
	
	private static ArrayList<Relation> properties;
	
	private void getTriple(Statement statement, StmtIterator iterator) {
		Resource subject = ResourceFactory.createResource(statement.getObject().toString());
		Statement next = iterator.next();
		Property predicate = ResourceFactory.createProperty(next.getObject().toString());
		next = iterator.next();
		
		RDFNode object = null;
		if(next.getObject().isResource()) {
			 object = ResourceFactory.createResource(next.getObject().toString());
		} else {
			object = ResourceFactory.createStringLiteral(next.getObject().toString());
		}	
		Statement triple = ResourceFactory.createStatement(subject, predicate, object);
		System.out.println(triple);		
		graph.add(triple);	
	}
	
	private void getRelations(String article) throws MalformedURLException, ProtocolException, IOException, ParseException {
		count = new AtomicInteger(0);
				
		final List<CoreMap> sentences = PARSER.getSentences(article);
		final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		
		Runnable askFox = new Runnable() {
		    public void run() {
		        try {
		        	
		        	Model model = ModelFactory.createDefaultModel() ;
		        	System.out.println(sentences.get(count.get()));
		    		model.read(new ByteArrayInputStream(SERVICE.extract(sentences.get(count.get()).toString(), "en" , "re").getBytes()),null, "TTL");
		    		StmtIterator iterator = model.listStatements();
		    					    		
		    		while(iterator.hasNext())
		    		{
		    			Statement s = iterator.next();
		    			if(s.getPredicate().toString().contains("subject")) {
		    				getTriple(s , iterator);
		    			}
		    		}
		    		System.out.println("------------------------");
		    		
					if(count.incrementAndGet() == sentences.size()) {
						executor.shutdownNow();
						
						StmtIterator foundTriples = graph.listStatements();
						
						while(foundTriples.hasNext())
						{
							System.out.println(foundTriples.next());
						}
					}
				} catch (Exception e) {
					executor.shutdownNow();
					e.printStackTrace();
				}
		    }
		};
		
		executor.scheduleWithFixedDelay(askFox, 0, DELAYSECONDS, TimeUnit.SECONDS);	
	}
	
	public void retrieveRelations(File input, String model, String ontology) throws MalformedURLException, ProtocolException, IOException, ParseException  {		
		parseProperties(ontology);
		
		BufferedReader reader = null;
		FileWriter writer = null;
		
		try {		   
		    reader =  new BufferedReader(new FileReader(input));
		    
		    File out = new File(model);
		    writer = new FileWriter(out);		

		    graph.read(model,null, "TTL");

		    String nextLine;
		    int currentLine = 0;
		    int linesLastWrite = 0;
		    while((nextLine = reader.readLine()) != null) {		
		    	if(currentLine >= STARTLINE) {
		    		getRelations(nextLine);	
		    	}
		    	if(linesLastWrite == LINESPERWRITE) {
		    		graph.write(writer, "TTL");
		    		linesLastWrite = 0;
		    	}
		    		
		    	currentLine++;
		    	linesLastWrite++;
		    }
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		} finally {		   
		    try {
		    	reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}		    		    
		}
	}
	
	
	private void parseProperties(String o) {
		properties = new ArrayList<Relation>();
		Model ontology = ModelFactory.createDefaultModel();
		ontology.read(o);
		
		ResIterator subjects = ontology.listSubjects();

		while(subjects.hasNext()) {
			Relation property = new Relation();
			properties.add(property);
			Resource resource = subjects.next();
			StmtIterator predicates = resource.listProperties();
			while(predicates.hasNext()) {
				Statement next = predicates.next();
				if(next.getPredicate().toString().equals("http://www.w3.org/2000/01/rdf-schema#label")) {
					property.setLabel(next.getSubject().toString());
					String temp = next.getObject().toString();
					property.setKeywords(temp.substring(0, temp.length()-3));
				} else if(next.getPredicate().toString().equals("http://www.w3.org/2000/01/rdf-schema#domain")) {
					property.setDomain(next.getObject().toString());
				} else if(next.getPredicate().toString().equals("http://www.w3.org/2000/01/rdf-schema#range")){
					property.setRange(next.getObject().toString());
				}
			}					
		}	
	}
	
	public static void main(String[] args) throws Exception  {
//		NamedEntityRecognizer n = new NamedEntityRecognizer();	
//		n.retrieveEntities(new File("resources/out2.txt","graph.ttl"));
//		FoxWebservice n = new FoxWebservice();
//		System.out.println(n.extract("The philosopher and mathematician Leibniz was born in Leipzig in 1646. He died in Hanover.", "en", "re"));
		
		NLPParser p = new NLPParser();
		List<CoreMap> list = p.getSentences("Austin is the capital of Texas and is a city in the USA.");
		//SpotlightWebservice service = new SpotlightWebservice();
		
		
//		RelationExtraction n = new RelationExtraction();	
//		n.parseProperties("resources/ontology_english.nt");
	
		
	}
}
