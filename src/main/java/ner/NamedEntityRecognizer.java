package ner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.json.simple.parser.ParseException;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class NamedEntityRecognizer {

	private static final String ANNOTATORS = "tokenize, ssplit";
	
	private static final StanfordCoreNLP PIPELINE;
	
	private static final FoxWebservice SERVICE = new FoxWebservice();
		
	private static final int DELAYSECONDS = 10; 
	
	private static AtomicInteger count = new AtomicInteger(0);
	
	private List<CoreMap> sentences;
	
	private static Model graph = ModelFactory.createDefaultModel() ;
	
	static {
		 Properties props = new Properties();
		 props.put("annotators", ANNOTATORS);
		 PIPELINE = new StanfordCoreNLP(props);
	}
	
	private void getSentences(String article) {		
		
		Annotation annotation = new Annotation(article);
		PIPELINE.annotate(annotation);
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);	
		this.sentences = sentences;
	}
	
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
	
	private void getEntities(String article) throws MalformedURLException, ProtocolException, IOException, ParseException {
		getSentences(article);
	
		final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		
		Runnable askSpotlight = new Runnable() {
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
						
						StmtIterator iterator2 = graph.listStatements();
						
						while(iterator2.hasNext())
						{
							Statement s = iterator2.next();
							System.out.println(s);
						}
					}
				} catch (Exception e) {
					executor.shutdownNow();
					e.printStackTrace();
				}
		    }
		};
		
		executor.scheduleAtFixedRate(askSpotlight, 0, DELAYSECONDS, TimeUnit.SECONDS);	
	}
	
	public void retrieveEntities(File input) throws MalformedURLException, ProtocolException, IOException, ParseException  {		
		BufferedReader reader = null;
		try {		   
		    reader =  new BufferedReader(new FileReader(input));
		    		
		    String nextLine;
		    while((nextLine = reader.readLine()) != null) {				    	
		    	getEntities(nextLine);		    	
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
	
	public static void main(String[] args) throws MalformedURLException, ProtocolException, IOException, ParseException  {
		NamedEntityRecognizer n = new NamedEntityRecognizer();	
		n.retrieveEntities(new File("resources/out2.txt"));
	}
}
