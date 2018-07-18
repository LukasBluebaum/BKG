package extraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import edu.stanford.nlp.util.CoreMap;
import utils.Relation;

/**
 * Responsible for managing the relation extraction, calling {@link #retrieveRelations(String, String)}. And starts a 
 * {@link SpotlightThread} and {@link FoxThread} and hands them the lines of the text file.
 * @author Nick Düsterhus
 * @author Lukas Blübaum
 * @author Monika Werner
 */

public class RelationExtraction {
		
	private final static int QUEUESIZE = 1000;
	
	protected static final int ARTICLESPERWRITE = 10;
	
	/**
	 * List of DBpedia ontology properties {@link utils.Relation}
	 */
	protected static ArrayList<Relation> properties;
	
	/**
	 * Limit of characters we send to Spotlight at once. If we sent more than that, Spotlight might not answer.
	 */
	private static final int CHARACTERLIMIT = 4500;
		
	/**
	 * The graph the triples will be written to.
	 */
	private static Model graph;
	
	private static NLPParser parser;
		
	/**
	 * Initializes model and parser. If a properties.json file at the specified location exists, this file will be loaded.
	 * Otherwise the default ontology will be loaded.
	 */
	public RelationExtraction() {
		graph = ModelFactory.createDefaultModel() ;
		parser = new NLPParser();
		File out = new File("src/main/resources/properties.json");		    
	    if(out.exists()) {
	    	loadJson();
	    } else {
	    	parseProperties();
	    }
	}
	
	
	/**
	 * Reads the sentences/strings from the given file and puts them in two BlockingQueues.
	 * Creates a {@link SpotlightThread} and {@link FoxThread}, which then read the articles from their given BlockingQueue and try to extract triples.
	 * @param input Path to a text file like the wikipedia dump.
	 * @param model Path to a Jena Model, the model will be created if it does not exist yet.
	 * @throws MalformedURLException
	 * @throws ProtocolException
	 * @throws IOException
	 * @throws ParseException
	 * @throws InterruptedException
	 */
	private void retrieveRelations(String input, String model) throws InterruptedException  {		
		
		BufferedReader reader = null;	
		try {		
			File dump = new File(input);
		    reader =  new BufferedReader(new FileReader(dump));		    
		    File out = new File(model);		    
		    if(out.length() != 0) graph.read(new FileInputStream(out),null, "TTL");

		    String nextLine;
		    
		    BlockingQueue<List<CoreMap>> spotlightQueue = new ArrayBlockingQueue<List<CoreMap>>(QUEUESIZE);	    
		    BlockingQueue<List<CoreMap>> foxQueue = new ArrayBlockingQueue<List<CoreMap>>(QUEUESIZE);		    
		    SpotlightThread spotlightThread = new SpotlightThread(parser,graph,out,spotlightQueue);
//		    FoxThread foxThread = new FoxThread(graph,out,foxQueue);
		    
		    Thread spotlight = new Thread(spotlightThread);
		    spotlight.start();
		    
//		    Thread fox = new Thread(foxThread);
//		    fox.start();
		    while((nextLine = reader.readLine()) != null) {	
		    	final long startTime2 = System.currentTimeMillis();
		    	
//		    	System.out.println(nextLine.length());
		    	List<CoreMap> sentences = parser.getSentences(nextLine);	    	
		    	List<CoreMap> nextSentences = new ArrayList<CoreMap>();
		    	int currentLength = 0;
		    	for(CoreMap sentence: sentences) {
		    		if(currentLength + sentence.toString().length() > CHARACTERLIMIT) {
//		    			final long startTime1 = System.currentTimeMillis();
		    			String coRef = parser.coreferenceResolution(nextSentences);
//		    			final long endTime1 = System.currentTimeMillis();
//		    			final long test1 = endTime1-startTime1;
//		    			System.out.println("Relation:" + TimeUnit.MILLISECONDS.toSeconds(test1));
		    				    			
//		    			final long startTime = System.currentTimeMillis();
			    		List<CoreMap> sentencesRelations = parser.calculateRelations(coRef);
//			    		final long endTime = System.currentTimeMillis();
//			    		final long test = endTime-startTime;
//		    			System.out.println("Relation:" + TimeUnit.MILLISECONDS.toSeconds(test));
			    		spotlightQueue.put(sentencesRelations);
//			    		foxQueue.put(sentencesRelations);
			    		nextSentences = new ArrayList<CoreMap>();
			    		currentLength = 0;
		    		} else {
		    			nextSentences.add(sentence);
		    			currentLength += sentence.toString().length();
		    		}
		    	}
		    	if(currentLength > 0) {
		    		String coRef = parser.coreferenceResolution(nextSentences);
		    		List<CoreMap> sentencesRelations = parser.calculateRelations(coRef);
		    		spotlightQueue.put(sentencesRelations);
		    		
//		    		foxQueue.put(sentencesRelations);	    		
		    	}
		    	final long endTime2 = System.currentTimeMillis();
    			final long test2 = endTime2-startTime2;
    			double i = (double) TimeUnit.MILLISECONDS.toSeconds(test2);
    			System.out.println("Done with article: (" + nextLine.length() + "," + (i/60.0) + ")");
//		    	if(currentLine >= STARTLINE) {
//		    		String line = nextLine.length() > CHARACTERLIMIT+1 ? nextLine.substring(0, CHARACTERLIMIT+1) : nextLine;
//		    		line = PARSER.coreferenceResolution(line);
//		    		List<CoreMap> sentences = PARSER.getSentences(line);		    		
//		    		spotlightQueue.put(sentences);
//		    		foxQueue.put(sentences);
//		    	}	
		    }
		    
		    spotlightQueue.put(new ArrayList<CoreMap>());
		    spotlight.join();
//		    foxQueue.put(new ArrayList<CoreMap>());
//		    fox.join();
		    
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
	
	
	/**
	 * Writes the property List into a json file.
	 */
	@SuppressWarnings("unused")
	private void toJsonFile() {
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		try {
			String json = ow.writeValueAsString(properties);
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("properties.json"));
			writer.write(json);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Reads a Json-file containing a List of Relations and puts them into a property list.
	 */
	private void loadJson() {
		ObjectMapper mapper = new ObjectMapper();
		try 
		{  String json = Files.toString(new File("src/main/resources/properties.json"), Charsets.UTF_8);			
			properties = mapper.readValue(json , new TypeReference<ArrayList<Relation>>(){});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/** 
	 * Adds the number of properties with exactly the same range and domain to each property.
	 */
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
	
	/**
	 * Creates a List of properties from src/main/resources/ontology_english.nt .
	 */
	private void parseProperties() {
		properties = new ArrayList<Relation>(); 
		Model ontology = ModelFactory.createDefaultModel();
		ontology.read("src/main/resources/ontology_english.nt");
		
		ResIterator subjects = ontology.listSubjects();
		while(subjects.hasNext()) {	
			Relation property = new Relation();
			Resource resource = subjects.next();
			StmtIterator predicates = resource.listProperties();
			while(predicates.hasNext()) {
				Statement next = predicates.next();
				if(next.getPredicate().toString().equals("http://www.w3.org/2000/01/rdf-schema#label") &&
						!resource.toString().contains("wiki")) {
					property.setLabel(next.getSubject().toString());
					String temp = next.getObject().toString();
					property.setKeys(temp.substring(0, temp.length()-3));
					
					ArrayList<String> keys = property.getKeywords();
					if(keys.size() >= 2 && !parser.isNounOrVerb(keys.get(0))) {
						String newKey = "";
						for(String key: keys) {
							newKey += " " + key;
						}
						newKey = newKey.substring(1);						
						property.setKeywords(new ArrayList<>(Arrays.asList(newKey)));
					}
				} else if(next.getPredicate().toString().equals("http://www.w3.org/2000/01/rdf-schema#domain")) {
					property.setDomain(next.getObject().toString());
				} else if(next.getPredicate().toString().equals("http://www.w3.org/2000/01/rdf-schema#range")){
					property.setRange(next.getObject().toString());
				} else if (next.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")){
					property.setPropertyType(next.getObject().toString());
				}					
			}	
			if(!property.toString().contains("null")) properties.add(property);			
		}
			
		countOfRangeDomain();
	}
	
	
	public static void main(String[] args) throws Exception  {
//		RelationExtraction n = new RelationExtraction();
//		n.parseProperties();
//		n.toJsonFile();
//		properties = null;
//		n.readJson();
//		for(Relation r: properties) {
//			System.out.println(r);
//		}
		RelationExtraction n = new RelationExtraction();	
//		n.parseProperties();
//		n.toJsonFile();
		n.retrieveRelations("resources/o.txt", "src/main/resources/model.ttl");
//		SpotlightWebservice service = new SpotlightWebservice();
//		for(Entity e: service.getEntitiesProcessed("During his first two years in office, Obama signed many landmark bills into law. The main reforms were the Patient Protection and Affordable Care Act (often referred to as \"Obamacare\", shortened as the \"Affordable Care Act\"), the Dodd–Frank Wall Street Reform and Consumer Protection Act, and the Don't Ask, Don't Tell Repeal Act of 2010. The American Recovery and Reinvestment Act of 2009 and Tax Relief, Unemployment Insurance Reauthorization, and Job Creation Act of 2010 served as economic stimulus amidst the Great Recession. After a lengthy debate over the national debt limit, he signed the Budget Control and the American Taxpayer Relief Acts. In foreign policy, he increased U.S. troop levels in Afghanistan, reduced nuclear weapons with the United States–Russia New START treaty, and ended military involvement in the Iraq War. He ordered military involvement in Libya in opposition to Muammar Gaddafi; Gaddafi was killed by NATO-assisted forces, and he also ordered the military operation that resulted in the deaths of Osama bin Laden and suspected Yemeni Al-Qaeda operative Anwar al-Awlaki.")) {
//			System.out.println(e);
//		}
	
//		NLPParser p = new NLPParser();
//		List<CoreMap> list = p.getSentences("Linkin Park's genre is rock");
//		Map<Integer, Collection<RelationTriple>> binaryRelations = p.binaryRelation(list);
//		for(RelationTriple triple: binaryRelations.get(0)) {			
//			System.out.println(0 +": " + triple.subjectGloss() + " - " + triple.relationGloss() + " - " + triple.objectGloss());
//		}
//		p.binaryRelation2(null);
	}
}
