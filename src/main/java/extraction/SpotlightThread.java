package extraction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.Lock;
import org.json.simple.parser.ParseException;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.util.CoreMap;
import utils.Entity;
import utils.Relation;

public class SpotlightThread implements Runnable {
	
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private static final Pattern COMMA = Pattern.compile(",");
	
	private static final Pattern NUMBERS = Pattern.compile("[^0-9.]");
	
	private static final ArrayList<String> MONTHS = new ArrayList<String>(Arrays.asList("January", "February" ,"March" ,
			"April", "May","June","July","August","September","October","November","December"));
	
	private NLPParser parser;
	
	private Model graph;
	
	private BlockingQueue<List<CoreMap>> articles;
	
	private File model;
	
	public SpotlightThread(NLPParser parser, Model graph, File model, BlockingQueue<List<CoreMap>> articles) {
		this.parser = parser;
		this.graph = graph;
		this.model = model;
		this.articles = articles;
	}
	
	@Override
	public void run() {
		FileWriter writer = null;		
		try {
			writer = new FileWriter(model);
			
			int currentLine = 0;
			while(true) {
				currentLine++;
				List<CoreMap> article = articles.take();
				if(article.size() == 0) break;
				
				getRelationsSpotlight(article);
								
				if(currentLine >= RelationExtraction.ARTICLESPERWRITE) {
					graph.enterCriticalSection(Lock.WRITE);
					try {
						System.out.println("Spotlight Enter Critical. Write.");
						graph.write(writer, "TTL");
					} finally {
						System.out.println("Spotlight Leave Critical. Write.");
						graph.leaveCriticalSection();
					}
					currentLine = 0;
				}
			}               
 	 			
			graph.enterCriticalSection(Lock.WRITE);
			try {
				System.out.println("Spotlight Enter Critical. Write.");
				graph.write(writer, "TTL");
			} finally {
				System.out.println("Spotlight Leave Critical. Write.");
				graph.leaveCriticalSection();
			}
		} catch ( IOException | InterruptedException e) {
			e.printStackTrace();   
		} finally{
			try {
				writer.close();				
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
		
	}
	
	public void getRelationsSpotlight(List<CoreMap> sentences) throws InterruptedException {	
		Thread.sleep(100);
		Map<Integer, Collection<RelationTriple>> binaryRelations = parser.binaryRelation(sentences);	
		
		SpotlightWebservice service = new SpotlightWebservice();

		ArrayList<Entity> entityList = new ArrayList<Entity>();
		try {
			entityList =  (ArrayList<Entity>) service.getEntitiesProcessed(sentences.toString());
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		Map<Integer, ArrayList<Entity>> entities = mapEntitiesSentences(entityList,sentences);
		
		for(int i = 0; i<sentences.size(); i++) {
			retrieveTriples(i, binaryRelations, entities);
		}
	}
	
	private Map<Integer, ArrayList<Entity>> mapEntitiesSentences(ArrayList<Entity> entities, List<CoreMap> sentences) {
		Map<Integer, ArrayList<Entity>> entityMap = new LinkedHashMap<>();	
		int count = 0;
		for(int i = 0; i<sentences.size(); i++) {
			count += sentences.get(i).toString().length();
			ArrayList<Entity> entitySentence = new ArrayList<Entity>();
			for(Entity en: entities) {
				if(en.getOffset() <= count) entitySentence.add(en);
			}
			entityMap.put(i, entitySentence);
		}
		return entityMap;
	}
	
	private void retrieveTriples(int i, Map<Integer, Collection<RelationTriple>> binaryRelations,
		Map<Integer, ArrayList<Entity>> entities) {
		for(Entity entity: entities.get(i))		{
			if(entity == null) return;
			literalRelation(entity, i, binaryRelations);
			for(Entity entity2: entities.get(i)) {
				for(RelationTriple triple: binaryRelations.get(i)) {					
									
					if(triple.subjectGloss().contains(entity.getSurfaceForm()) && triple.objectGloss().contains(entity2.getSurfaceForm())
							|| triple.subjectGloss().contains(entity2.getSurfaceForm()) && triple.objectGloss().contains(entity.getSurfaceForm())){
						//System.out.println(i +": " + triple.subjectGloss() + " - " + binaryRelations.get(i) + " - " + triple.objectGloss());
						String tripleRelation = triple.relationLemmaGloss();
						for(Relation r: RelationExtraction.properties) {
							if( (entity.getTypes().contains(r.getDomain()) || r.getDomain().equals("")) && (entity2.getTypes().contains(r.getRange()) || r.getRange().equals(""))) {
								String[] tripleR = tripleRelation.split(" ");
								//System.out.println("Entity1: " + entity + "entity2" + entity2 + " domain & range true for " + r);
								for(String s: tripleR) {
									if(r.getKeywords().contains(s)) {
										Resource subject = ResourceFactory.createResource(entity.getUri());
										Property predicate = ResourceFactory.createProperty(r.getLabel());
										RDFNode object = ResourceFactory.createResource(entity2.getUri());
										Statement t = ResourceFactory.createStatement(subject, predicate, object);
										//System.out.println(t);	
										graph.enterCriticalSection(Lock.WRITE);
										try {
											System.out.println("Spotlight Enter Critical. Add triple.");
											graph.add(t);	
											graph.write(System.out, "TTL");
										} finally {
											System.out.println("Spotlight Leave Critical. Add triple.");
											graph.leaveCriticalSection();
										}
									}
								}
							}
						}					
					}
				}
			}
		}
	}
	
	private void literalRelation(Entity entity, int i, Map<Integer, Collection<RelationTriple>> binaryRelations) {
		for(RelationTriple triple: binaryRelations.get(i)) {
			String data = null;
			if(triple.subjectGloss().contains(entity.getSurfaceForm())){
				String value = COMMA.matcher(triple.objectLemmaGloss()).replaceAll("");
				value = NUMBERS.matcher(value).replaceAll(" ");
	        	if(!value.trim().isEmpty()) {
	        		int month = containsMonth(triple.objectLemmaGloss());
	        		value = WHITESPACE.matcher(value).replaceAll(" ");
	    			String[] numbers = value.trim().split(" ");
	        		if(month != 0) {	        			
	        			if(numbers.length == 2) {
	        				data = numbers[1] + "-" + (month < 10 ? "0" + month : month) + "-" +
	        						(numbers[0].length() != 1 ? numbers[0] : "0" + numbers[0]);
	        			}         			
	        		} else {
	        			if(numbers.length == 1) {
	        				data = numbers[0];
	        			} 
	        		}
	        		
	        	}
	        	if(data != null) {
	        		for(Relation r: RelationExtraction.properties) {
						if((entity.getTypes().contains(r.getDomain()) || r.getDomain().equals("")) && r.getPropertyType().equals("data")) {
							String tripleRelation = triple.relationLemmaGloss();
							String[] tripleR = tripleRelation.split(" ");
							//System.out.println("Entity1: " + entity + " domain & range true for " + r);
							for(String s: tripleR) {
								if(r.getKeywords().contains(s)) {
									if((data.contains("-") && !r.getRange().contains("date")) ||
											r.getRange().contains("date") && !data.contains("-")) {
										break;
									}									
									Resource subject = ResourceFactory.createResource(entity.getUri());
									Property predicate = ResourceFactory.createProperty(r.getLabel());
									TypeMapper mapper = TypeMapper.getInstance();
									RDFDatatype type = mapper.getSafeTypeByName(r.getRange());
									RDFNode object = ResourceFactory.createTypedLiteral(data, type);
									Statement t = ResourceFactory.createStatement(subject, predicate, object);
									System.out.println(t);	
									graph.enterCriticalSection(Lock.WRITE);
									try {
										System.out.println("Spotlight Enter Critical. Add triple.");
										graph.add(t);	
										graph.write(System.out, "TTL");
									} finally {
										System.out.println("Spotlight Leave Critical. Add triple.");
										graph.leaveCriticalSection();
									}
								}
							}
						}
					}		
	        	}        	
			}		
		}		
	}
	

	private int containsMonth(String object) {
		for(int i = 0; i<MONTHS.size(); i++) {
			if(object.contains(" " + MONTHS.get(i) + " ") || object.contains(MONTHS.get(i) + " ") ||
					object.contains(" " + MONTHS.get(i))) return i+1;
		}
		return 0;
	}
	
}