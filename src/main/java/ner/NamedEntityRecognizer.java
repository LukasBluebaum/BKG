package ner;

import java.io.BufferedReader;
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

import org.json.simple.parser.ParseException;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class NamedEntityRecognizer {

	private static final String ANNOTATORS = "tokenize, ssplit";
	
	private static final StanfordCoreNLP PIPELINE;
	
	private static final EntityWebservice SERVICE = new EntityWebservice();
		
	private static final int DELAYSECONDS = 10; 
	
	private static AtomicInteger count = new AtomicInteger(0);
	
	private List<CoreMap> sentences;
	
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
	
	private void getEntities(String article) throws MalformedURLException, ProtocolException, IOException, ParseException {
		getSentences(article);
	
		final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		
		Runnable askSpotlight = new Runnable() {
		    public void run() {
		        try {
					System.out.println(SERVICE.getEntitiesProcessed(sentences.get(count.get()).toString()));
					if(count.incrementAndGet() == sentences.size()) executor.shutdownNow();
				} catch (IOException | ParseException e) {
					executor.shutdownNow();
					e.printStackTrace();
				}
		    }
		};
		
		executor.scheduleAtFixedRate(askSpotlight, 0, DELAYSECONDS, TimeUnit.SECONDS);		
	}
	
	public void retrieveEntities(File input)  {		
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
	
	public static void main(String[] args)  {
		NamedEntityRecognizer n = new NamedEntityRecognizer();
		n.retrieveEntities(new File("resources/out.txt"));
	}
}
