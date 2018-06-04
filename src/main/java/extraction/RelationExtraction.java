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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.util.CoreMap;
import utils.Entity;
import utils.Relation;

public class RelationExtraction {
	
	private static final FoxWebservice SERVICE = new FoxWebservice();
	
	private static final NLPParser PARSER = new NLPParser();
		
	private static final int DELAYSECONDS = 10; 
	
	private static final int STARTLINE = 0;
	
	private static final int LINESPERWRITE = 10;
	
	private static final int CHARACTERLIMIT = 2000;
	
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
	
	private void spotlightTriples(Map<Integer, Collection<RelationTriple>> binaryRelations, Map<Integer, ArrayList<Entity>> entities) {
		for(int i: binaryRelations.keySet()) {
			for(RelationTriple triple: binaryRelations.get(i)) {
				for(Entity entity: entities.get(i)) {
					for(Entity entity2: entities.get(i)) {
						if(triple.subjectGloss().contains(entity.getSurfaceForm()) && triple.objectGloss().contains(entity2.getSurfaceForm())){
							
						}
					}
				}
			}
		}
	}
	
	public void prepareArticle(String article) throws InterruptedException {
		List<CoreMap> sentencesRaw = PARSER.getSentences(article);
		List<CoreMap> sentences = new ArrayList<CoreMap>();
		int countC = 0;
		for(CoreMap sentence: sentencesRaw) {
			if(sentence.size() == 600) continue;
			if(countC + sentence.toString().length() < CHARACTERLIMIT) {
				countC += sentence.toString().length();
				sentences.add(sentence);
			} else {
				break;
			}
		}
		
		Map<Integer, Collection<RelationTriple>> binaryRelations = PARSER.binaryRelation(sentences);	
		
		SpotlightWebservice service = new SpotlightWebservice();

		Map<Integer, ArrayList<Entity>> entities = new LinkedHashMap<>();

		try {	
			for(int i = 0; i<sentences.size(); i++) {
				Thread.sleep(1000);
				entities.put(i, (ArrayList<Entity>) service.getEntitiesProcessed(sentences.get(i).toString()));
			}
//			for(int i = 0; i<sentences.size(); i++) {
//				if(countC + sentences.get(i).toString().length() < 600) {
//					question += sentences.get(i).toString();
//				} else if(countC + sentences.get(i).toString().length() > 600 || i == sentences.size()-1){
//					Thread.sleep(1000);
//					temp.addAll(service.getEntitiesProcessed(question));
//					countC = 0;
//				}	
//			}
		
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		if(entities == null || entities.size() == 0) return;
		
		spotlightTriples(binaryRelations,entities);
	}
	
	public void retrieveRelations(File input, String model, String ontology) throws MalformedURLException, ProtocolException, IOException, ParseException, InterruptedException  {		
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
		    		//getRelations(nextLine);
		    		prepareArticle(nextLine);
		    	}
		    	if(linesLastWrite == LINESPERWRITE) {
		    		graph.write(writer, "TTL");
		    		linesLastWrite = 0;
		    	}
		    		
		    	currentLine++;
		    	linesLastWrite++;
		    }
		} catch (IOException  e) {
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
		
		SpotlightWebservice service = new SpotlightWebservice();
		System.out.println(service.getEntities("Barack Obama."));
		
//		NLPParser p = new NLPParser();
//		List<CoreMap> list = p.getSentences("Albert Einstein (14 March 1879 – 18 April 1955) was a German-born theoretical physicist[5] who developed the theory"
//				+ " of relativity, one of the two pillars of modern physics (alongside quantum mechanics).");
//		//SpotlightWebservice service = new SpotlightWebservice();
//		p.binary(list);
//		
//		RelationExtraction n = new RelationExtraction();	
//		n.parseProperties("resources/ontology_english.nt");
	
		
	}
}
