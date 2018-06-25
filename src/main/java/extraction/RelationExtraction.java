package extraction;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.util.CoreMap;
import utils.Entity;
import utils.Relation;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

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
	

	
	private void getRelationsFox(String article) throws MalformedURLException, ProtocolException, IOException, ParseException {
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
						StmtIterator foundTriples = graph.listStatements();
						
						while(foundTriples.hasNext())
						{
							System.out.println(foundTriples.next());
						}
						executor.shutdownNow();	
					}
				} catch (Exception e) {
					executor.shutdownNow();
					e.printStackTrace();
				}
		    }
		};
		
		executor.scheduleWithFixedDelay(askFox, 0, DELAYSECONDS, TimeUnit.SECONDS);	
	}
	

	
	public void getRelationsSpotlight(String article) throws InterruptedException {
		List<CoreMap> sentences = PARSER.getSentences(article);
	
		Map<Integer, Collection<RelationTriple>> binaryRelations = PARSER.binaryRelation(sentences);	
		
		SpotlightWebservice service = new SpotlightWebservice();

		Map<Integer, ArrayList<Entity>> entities = new LinkedHashMap<>();

			for(int i = 0; i<sentences.size(); i++) {
				Thread.sleep(5000);
				System.out.println(sentences.get(i));
				try {
					entities.put(i, (ArrayList<Entity>) service.getEntitiesProcessed(sentences.get(i).toString()));
				} catch (IOException | ParseException  e) {
					entities.put(i, null);
					e.printStackTrace();
				}
				spotlightTriples(i, binaryRelations, entities);
			}
		
		if(entities == null || entities.size() == 0) return;
	}
	
	private void spotlightTriples(int i, Map<Integer, Collection<RelationTriple>> binaryRelations,
		Map<Integer, ArrayList<Entity>> entities) {
		for(Entity entity: entities.get(i))		{
			if(entity == null) return;
			for(Entity entity2: entities.get(i)) {
				for(RelationTriple triple: binaryRelations.get(i)) {					
								
					if(triple.subjectGloss().contains(entity.getSurfaceForm()) && triple.objectGloss().contains(entity2.getSurfaceForm())
							|| triple.subjectGloss().contains(entity2.getSurfaceForm()) && triple.objectGloss().contains(entity.getSurfaceForm())){
						System.out.println(i +": " + triple.subjectGloss() + " - " + binaryRelations.get(i) + " - " + triple.objectGloss());
						String tripleRelation = triple.relationGloss();
						for(Relation r: properties) {
							if( (entity.getTypes().contains(r.getDomain()) || r.getDomain().equals("")) && (entity2.getTypes().contains(r.getRange()) || r.getRange().equals(""))) {
								String[] tripleR = tripleRelation.split(" ");
								System.out.println("Entity1: " + entity + "entity2" + entity2 + " domain & range true for " + r);
								for(String s: tripleR) {
									if(r.getKeywords().contains(s)) {
										Resource subject = ResourceFactory.createResource(entity.getUri());
										Property predicate = ResourceFactory.createProperty(r.getLabel());
										RDFNode object = ResourceFactory.createResource(entity2.getUri());
										Statement t = ResourceFactory.createStatement(subject, predicate, object);
										System.out.println(t);		
										graph.add(t);	
									}
								}
							}
						}					
					}
				}
			}
		}
	}

	
//	private void spotlightTriples(Map<Integer, Collection<RelationTriple>> binaryRelations, Map<Integer, ArrayList<Entity>> entities) {
//	for(int i: binaryRelations.keySet()) {
//		for(RelationTriple triple: binaryRelations.get(i)) {
//			for(Entity entity: entities.get(i)) {
//				for(Entity entity2: entities.get(i)) {
//					if(triple.subjectGloss().contains(entity.getSurfaceForm()) && triple.objectGloss().contains(entity2.getSurfaceForm())){
//						
//					}
//				}
//			}
//		}
//	}
//}


	public void retrieveRelations(File input, String model) throws MalformedURLException, ProtocolException, IOException, ParseException, InterruptedException  {		
		parseProperties();
		
		BufferedReader reader = null;
		FileWriter writer = null;		
		try {		   
		    reader =  new BufferedReader(new FileReader(input));
		    
		    File out = new File(model);
		    writer = new FileWriter(out);			    
		    if(out.length() != 0) graph.read(model,null, "TTL");

		    String nextLine;
		    int currentLine = 0;
		    int linesLastWrite = 0;
		    while((nextLine = reader.readLine()) != null) {		
		    	if(currentLine >= STARTLINE) {
		    		String line = nextLine.length() > CHARACTERLIMIT+1 ? nextLine.substring(0, CHARACTERLIMIT+1) : nextLine;
		    		getRelationsSpotlight(PARSER.coreferenceResolution(line));
		    		//getRelationsFox(line);	    		
		    	}
		    	
		    	if(linesLastWrite == LINESPERWRITE) {
		    		graph.write(writer, "TTL");
		    		linesLastWrite = 0;
		    	}
		    		
		    	currentLine++;
		    	linesLastWrite++;
		    }
		    graph.write(writer, "TTL");
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
	
	private void toJsonFile() {

		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = null;

		try {
			json = ow.writeValueAsString(properties);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		 try {
				OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("properties.json"));
				writer.write(json);
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	
	private void readJson() {
		ObjectMapper mapper = new ObjectMapper();
		try 
		{  String json = Files.toString(new File("properties.json"), Charsets.UTF_8);
			
			properties =   mapper.readValue(json , new TypeReference<ArrayList<Relation>>(){});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void countOfRangeDomain() {
		for(Relation r: properties) {
			int count = 0;
			for(Relation r2: properties) {		
				if( r.getDomain().equals(r2.getDomain())  && r.getRange().equals(r2.getRange())) {
					count++;
				}
			}
			r.setCountRelation(count);
		}
	}
	
	private void parseProperties() {
		properties = new ArrayList<Relation>();
		Model ontology = ModelFactory.createDefaultModel();
		ontology.read("src/main/resources/ontology_english.nt");
		
		ResIterator subjects = ontology.listSubjects();
        int x = 0;
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
					property.setKeys(temp.substring(0, temp.length()-3));
				} else if(next.getPredicate().toString().equals("http://www.w3.org/2000/01/rdf-schema#domain")) {
					property.setDomain(next.getObject().toString());
				} else if(next.getPredicate().toString().equals("http://www.w3.org/2000/01/rdf-schema#range")){
					property.setRange(next.getObject().toString());
				} else if (next.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")){
					property.setPropertyType(next.getObject().toString());
				}
						
			}					
		}
		
		
		countOfRangeDomain();
	}
	
	
	public static void main(String[] args) throws Exception  {
		RelationExtraction n = new RelationExtraction();
		n.parseProperties();
		n.toJsonFile();
		properties = null;
		n.readJson();
		for(Relation r: properties) {
			System.out.println(r);
		}
//		RelationExtraction n = new RelationExtraction();	
//		n.retrieveRelations(new File("resources/out.txt"), "src/main/resources/model.ttl");
		
	
//		NLPParser p = new NLPParser();
//		List<CoreMap> list = p.getSentences("Linkin Park's genre is rock");
//		Map<Integer, Collection<RelationTriple>> binaryRelations = p.binaryRelation(list);
//		for(RelationTriple triple: binaryRelations.get(0)) {			
//			System.out.println(0 +": " + triple.subjectGloss() + " - " + triple.relationGloss() + " - " + triple.objectGloss());
//		}
//		p.binaryRelation2(null);
	}
}
