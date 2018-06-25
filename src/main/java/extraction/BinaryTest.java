package extraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.regex.Pattern;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class BinaryTest {
	private static final StanfordCoreNLP pipeline;
	
	private static final String ANNOTATORS = "tokenize,ssplit,pos,lemma,depparse,natlog,openie";
	
	private static final ArrayList<String> MONTHS = new ArrayList<String>(Arrays.asList("January", "February" ,"March" ,
			"April", "May","June","July","August","September","October","November","December"));
	
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	
	private static final Pattern COMMA = Pattern.compile(",");
	
	private static final Pattern NUMBERS = Pattern.compile("[^0-9.]");
	
	static {
		Properties props = new Properties();
	    props.setProperty("annotators",ANNOTATORS);
	    pipeline = new StanfordCoreNLP(props);
	}

	public static void main(String[] args) {	
			System.out.println("------");
		 	Annotation doc = new Annotation("The western sectors, controlled by France, the United Kingdom, and the United States, were merged on 23 May 1949 to form the Federal Republic of Germany on 7 October 1949, the Soviet Zone became the German Democratic Republic. ");
		 	
			pipeline.annotate(doc);
			for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
			      // Get the OpenIE triples for the sentence
			      Collection<RelationTriple> triples =
				          sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
			      // Print the triples
			      for (RelationTriple triple : triples) {
			        System.out.println(triple.confidence + "\t" +
			            triple.subjectLemmaGloss() + "\t" +
			            triple.relationLemmaGloss() + "\t" +
			            triple.objectLemmaGloss());
			        	
			        	String s = COMMA.matcher(triple.objectLemmaGloss()).replaceAll("");
			        	s = NUMBERS.matcher(s).replaceAll(" ");
			        	if(!s.trim().isEmpty()) {
			        		int month = containsMonth(triple.objectLemmaGloss());
			        		s = WHITESPACE.matcher(s).replaceAll(" ");
		        			String[] numbers = s.trim().split(" ");
			        		if(month != 0) {
			        			if(numbers.length != 2) {
			        				System.out.println("shit");
			        			} else {
			        				
			        				System.out.println(numbers[1] + "-" + (month < 10 ? "0" + month : month) + "-" +
			        						(numbers[0].length() != 1 ? numbers[0] : "0" + numbers[0]));
			        			}		        			
			        		} else {
			        			if(numbers.length > 1) {
			        				System.out.println("shit2");
			        			} else {
			        				System.out.println(numbers[0]);
			        			}
			        		}
			        		
			        	}
			      }
			}
			
			
	}
		
	
	private static int containsMonth(String object) {
		for(int i = 0; i<MONTHS.size(); i++) {
			if(object.contains(" " + MONTHS.get(i) + " ")) return i+1;
		}
		return 0;
	}
}
