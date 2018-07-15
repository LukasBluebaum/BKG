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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
	
	/**
	 * Constructor 
	 * @param parser An instance of the NLPParser class.
	 * @param graph An Apache Jena model this thread will write to.
	 * @param model The file this thread will write the model to.
	 * @param articles A BlockingQueue containing a List of CoreMaps (Stanford CoreNLP).
	 */
	public SpotlightThread(NLPParser parser, Model graph, File model, BlockingQueue<List<CoreMap>> articles) {
		this.parser = parser;
		this.graph = graph;
		this.model = model;
		this.articles = articles;
	}
	
	/**
	 * Takes the next line from the blocking queue, then calls {@link #getRelationsSpotlight()} on this line.
	 * Writes the graph to a file.
	 */
	@Override
	public void run() {
		FileWriter writer = null;		
		try {
			//writer = new FileWriter(model,false);
			
			int lastWrite = 0;
			while(true) {
				lastWrite++;
				List<CoreMap> nextLine = articles.take();
				if(nextLine.size() == 0) break;
				getRelationsSpotlight(nextLine);
								
				//if(lastWrite >= RelationExtraction.ARTICLESPERWRITE) {
					graph.enterCriticalSection(Lock.WRITE);
					try {
						//System.out.println("Spotlight Enter Critical. Write.");
						//System.out.println("Spotlight:" + lastWrite);
						writer = new FileWriter(model,false);
						graph.write(writer, "TTL");
					} finally {
						//System.out.println("Spotlight Leave Critical. Write.");
						graph.leaveCriticalSection();
					}
					lastWrite = 0;
				//}
			}               
 	 			
			graph.enterCriticalSection(Lock.WRITE);
			try {
				writer = new FileWriter(model,false);
				graph.write(writer, "TTL");
				graph.write(System.out,"TTL");
			} finally {
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
	
	/**
	 * Uses the SpotlightWebservice to get the entities for the current sentences.
	 * Then calls {@link #retrieveTriples()}.
	 * @param sentences Current sentences that are to be processed.
	 */
	private void getRelationsSpotlight(List<CoreMap> sentences)  {	
		Map<Integer, Collection<RelationTriple>> binaryRelations = parser.binaryRelation(sentences);	
		
		SpotlightWebservice service = new SpotlightWebservice();

		ArrayList<Entity> entityList = new ArrayList<Entity>();
		try {
			Thread.sleep(100);
			String result = service.getEntities(sentences.toString());
			entityList =  (ArrayList<Entity>) service.postProcessing(result);
			//writeTypes(result);
			
		} catch (IOException | ParseException | InterruptedException e) {
			e.printStackTrace();
			return;
		}
		Map<Integer, ArrayList<Entity>> entities = mapEntitiesToSentences(entityList,sentences);
		
		for(int i = 0; i<sentences.size(); i++) {
			retrieveTriples(i, binaryRelations, entities);
		}
	}
	
	/**
	 * Maps the entities back to the sentence their were found in.
	 * @param entities Entities which were found in the last call to the spotlight demo.
	 * @param sentences Sentences that were last send to the spotlight demo.
	 * @return A HashMap with the sentence index as key and the corresponding entities as values.
	 */
	private Map<Integer, ArrayList<Entity>> mapEntitiesToSentences(ArrayList<Entity> entities, List<CoreMap> sentences) {
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
	
	/**
	 * Writes the types for the entities into the graph.
	 * @param response Last spotlight demo response.
	 * @throws ParseException
	 */
	private void writeTypes(final String response) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = (JSONObject) parser.parse(response);

		JSONArray resources = (JSONArray) jsonObject.get("Resources");
		if (resources != null) {
			for (Object res : resources.toArray()) {
				JSONObject next = (JSONObject) res;
				String uri = ((String) next.get("@URI"));
				String type = ((String) next.get("@types"));
				
				String[] comma = type.split(",");
				String typeUri = "";
				for(String typeLink: comma) {
					String[] split = typeLink.split(":");
					switch(split[0]) {
					case("Wikidata") : typeUri =  "http://www.wikidata.org/entity/" + split[1]; break;
					case("DUL") : typeUri =  "http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#" + split[1]; break;
					case("DBpedia") : typeUri =  "http://dbpedia.org/ontology/" + split[1]; break;
					case("Schema") : typeUri =  "http://schema.org/" + split[1]; break;
					}
					if(split[0].toLowerCase().startsWith("http")) {
						typeUri = Character.toLowerCase(split[0].charAt(0)) + split[0].substring(1);
					}
					if(typeUri.length() != 0) {
						Resource subject = ResourceFactory.createResource(uri);
						Property predicate = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#/type");
						RDFNode object = ResourceFactory.createResource(typeUri);
						Statement statement = ResourceFactory.createStatement(subject, predicate, object);
						graph.enterCriticalSection(Lock.WRITE);
						try {
							graph.add(statement);	
						} finally {
							graph.leaveCriticalSection();
						}
					}
				}
			}
		}
	}
	
	/**
	 * Iterates through the binary relations of a sentence and tries to map the subject and object of these to entities which
	 * were found in the current sentence. Finally tries to map the predicate of the binary relation to an existing property.
	 * If there is an entity in the subject also calls {@link #literalRelation()} to search for literal relations.
	 * @param i Index of the current sentence.
	 * @param binaryRelations List of binary relations in the current sentence.
	 */
	private void retrieveTriples(int i, Map<Integer, Collection<RelationTriple>> binaryRelations,
		Map<Integer, ArrayList<Entity>> entities) {
		for(Entity entity: entities.get(i))		{
			if(entity == null) return;
			literalRelation(entity, i, binaryRelations);
			for(Entity entity2: entities.get(i)) {
				if(entity.getUri().equals(entity2.getUri())) continue;
				for(RelationTriple triple: binaryRelations.get(i)) {					
									
					if(triple.subjectGloss().trim().equals(entity.getSurfaceForm()) && triple.objectGloss().trim().equals(entity2.getSurfaceForm())
							/*|| triple.subjectGloss().trim().equals(entity2.getSurfaceForm()) && triple.objectGloss().contains(entity.getSurfaceForm())*/) {						
						String tripleRelation = triple.relationLemmaGloss() + " " + triple.objectLemmaGloss();
						for(Relation rel: RelationExtraction.properties) {
							
							if( (entity.getTypes().contains(rel.getDomain()) || rel.getDomain().equals("")) 
									&& (entity2.getTypes().contains(rel.getRange()) || rel.getRange().equals(""))) {
								String[] tripleR = tripleRelation.toLowerCase().split(" ");
								if(checkKeywords(rel,tripleR)) {	
									Resource subject = ResourceFactory.createResource(entity.getUri());
									Property predicate = ResourceFactory.createProperty(rel.getLabel());
									RDFNode object = ResourceFactory.createResource(entity2.getUri());
									Statement statement = ResourceFactory.createStatement(subject, predicate, object);
									graph.enterCriticalSection(Lock.WRITE);
									try {
										graph.add(statement);	
									} finally {
										graph.leaveCriticalSection();
									}
									setLabelAndName(entity);
									setLabelAndName(entity2);
								}
							}
						}					
					}
				}
			}
		}
	}
	
	/**
	 * Iterates through the binary relations of a sentence and determines if there is a literal in the 
	 * object of the binary relation. (only dates and numbers) Finally tries to map the predicate of the 
	 * binary relation to an existing property.
	 * @param entity An entity which was found in the current sentence.
	 * @param i Index of the current sentence.
	 * @param binaryRelations List of binary relations in the current sentence.
	 */
	private void literalRelation(Entity entity, int i, Map<Integer, Collection<RelationTriple>> binaryRelations) {
		for(RelationTriple triple: binaryRelations.get(i)) {
			String data = null;
			if(triple.subjectGloss().trim().equals(entity.getSurfaceForm())){
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
	        			}else if(numbers.length == 1) {
	        				data = numbers[0];
	        			}
	        		} else {
	        			if(numbers.length == 1) {
	        				int zeros = mapNumber(triple.objectLemmaGloss(), numbers[0]);
	        				data = numbers[0];
	        				if(data.contains(".")) {
	        					data = data.replace(".", "");
	        				}
	        				for(int j = 0; j<zeros; j++) {
	        					data = data + "0";
	        				}
	        			} 
	        		}	        		
	        	}
	        	if(data != null && data.length() > 0) {
	        		for(Relation rel: RelationExtraction.properties) {
						if((entity.getTypes().contains(rel.getDomain()) || rel.getDomain().equals("")) && rel.getPropertyType().equals("data") &&
								!rel.getRange().toLowerCase().contains("string")) {
							String tripleRelation = triple.relationLemmaGloss() + " " + triple.objectLemmaGloss();
							String[] tripleR = tripleRelation.toLowerCase().split(" ");	
							if(checkKeywords(rel,tripleR)) {	
								//if the number that was found is a date but the found property does not have the range date then break 
								if((data.contains("-") && !rel.getRange().contains("date")) || rel.getRange().contains("date") && !data.contains("-") ) {
									break;
								}
								//if the property has year as range but the found number does not consist of 4 digits break
								if(rel.getRange().contains("Year") && data.length() != 4) {
									break;
								}
								Resource subject = ResourceFactory.createResource(entity.getUri());
								Property predicate = ResourceFactory.createProperty(rel.getLabel());
								TypeMapper mapper = TypeMapper.getInstance();
								RDFDatatype type = mapper.getTypeByName(rel.getRange());
								RDFNode object = ResourceFactory.createTypedLiteral(data, type);
								Statement statement = ResourceFactory.createStatement(subject, predicate, object);									
								graph.enterCriticalSection(Lock.WRITE);
								try {
									graph.add(statement);	
								} finally {
									graph.leaveCriticalSection();
								}
								setLabelAndName(entity);
							}
						}
					}		
	        	}        	
			}		
		}		
	}
	
	/**
	 * Checks the words inside the predicate and object of the binary relation against the list of keywords  
	 * for the current property.
	 * @param rel 
	 * @param tripleR Predicate and object of the current binary relation. 
	 * @return True if the keywords for the property match words inside the predicate and object of the binary relation,
	 * false otherwise.
	 */
	private boolean checkKeywords(Relation rel, String[] tripleR) {
		ArrayList<String> keywords= rel.getKeywords();
		
		ArrayList<String> wordsFound = new ArrayList<String>(Arrays.asList(tripleR));
		for(String keyList: keywords) {
			// one or multiple keywords have to appear together
			String[] partKey = keyList.split(" ");
			boolean found = false;
			for(String key: partKey) {
				if(wordsFound.contains(key)) {
					found = true;
				} else {
					found = false;
					break;
				}
			}
			if(found) return true;
		}		
		return false;
	}
	
	/**
	 * Checks if there is a number keyword in the object of the relation. 
	 * @param objectLemmaGloss object of the current relation
	 * @param number the number which was found on the object of the current relation
	 * @return The amount of zeros that need to be added to the given number because of the occuring number keyword.
	 */
	private int mapNumber(String objectLemmaGloss, String number) {					
		int comma = number.lastIndexOf('.') != -1 ? ( number.length()-1) - number.lastIndexOf('.') : 0;
		if(objectLemmaGloss.contains("hundred"))
			return (2-comma);
		if(objectLemmaGloss.contains("thousand"))	
			return (3-comma);
		if(objectLemmaGloss.contains("million"))
			return (6-comma);
		if(objectLemmaGloss.contains("billion"))
			return (9-comma);
		if(objectLemmaGloss.contains("trillion"))
			return (12-comma);
				
		return 0;
	}
	
	/**
	 * Creates two triples for the rdfs:label and foaf:name relation and the given entity.
	 * @param entity
	 */
	private void setLabelAndName(Entity entity) {
		Resource subject = ResourceFactory.createResource(entity.getUri());
		Property predicate = ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#/label");		
		String uri = entity.getUri();
		String label = uri.substring(uri.lastIndexOf("/")+1).trim().replaceAll("_", " ");
		RDFNode object = ResourceFactory.createLangLiteral(label, "en");
		Statement statement = ResourceFactory.createStatement(subject, predicate, object);	
		
		Resource subjectName = ResourceFactory.createResource(entity.getUri());
		Property predicateName = ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/name");		
		RDFNode objectName = ResourceFactory.createLangLiteral(label, "en");
		Statement statementName = ResourceFactory.createStatement(subjectName, predicateName, objectName);	
		graph.enterCriticalSection(Lock.WRITE);
		try {
			graph.add(statement);	
			graph.add(statementName);	
		} finally {
			graph.leaveCriticalSection();
		}	
	}

	/**
	 * Checks if there is a month in the object of the current relation.
	 * @param object Object of the current relation.
	 * @return The number of the month in a year, 0 otherwise.
	 */
	private int containsMonth(String object) {
		for(int i = 0; i<MONTHS.size(); i++) {
			if(object.contains(" " + MONTHS.get(i) + " ") || object.contains(MONTHS.get(i) + " ") ||
					object.contains(" " + MONTHS.get(i))) return i+1;
		}
		return 0;
	}
}