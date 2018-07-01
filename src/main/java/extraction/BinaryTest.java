package extraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class BinaryTest {
	private static final Pattern URLS = Pattern.compile("http.*?\\s");
	
	private static final Pattern PARENTHESES = Pattern.compile("\\(.*?\\)");
	
	private static final Pattern SYMBOLS = Pattern.compile("[^a-zA-Z0-9. ]");
	
	private static final Pattern NULLCHAR = Pattern.compile("\0");
	
	private static final Pattern WHITESPACE1 = Pattern.compile("\\s+");
	
	private static final StanfordCoreNLP pipeline;
	
	private static final String ANNOTATORS = "tokenize,ssplit,pos,lemma,depparse,natlog,openie";
	
	//private static final  String ANNOTATORS = "tokenize,ssplit,pos,lemma,ner,parse,coref";
	
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

	public static void main(String[] args) throws IOException {	
		
//			File input = new File("resources/enwiki-20171103-pages.tsv");
//			System.out.println("------");
//			
//			BufferedReader reader = new BufferedReader(new FileReader(input));
//			FileWriter writer = new FileWriter(new File("out2.txt"));
//	 		String article =null;
//	 		while((article=reader.readLine())!=null) { 
//	 			// only consider lines with valid articles
//	 			if(article.length() > 1 && article.charAt(1) == 'h') {  
//	 				
//	 				article = article.length() > 10000+1 ? article.substring(0, 10000+1) : article;
//	 				System.out.println(article.length());
//	 				Annotation annotation = new Annotation(cleanArticle(article));
//				    pipeline.annotate(annotation);
//				    System.out.println("----------------------------------");
//				    writer.write(annotation.toString() +"\r\n");
//	 			}
//	 		}
//	 		writer.close();
	 		
	 		
		 	Annotation doc = new Annotation("The philosopher and mathematician Leibniz was born in Leipzig in 1646 and attended the University of Leipzig from 1661-1666");
		 	
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
			        	
//			        	String s = COMMA.matcher(triple.objectLemmaGloss()).replaceAll("");
//			        	s = NUMBERS.matcher(s).replaceAll(" ");
//			        	if(!s.trim().isEmpty()) {
//			        		int month = containsMonth(triple.objectLemmaGloss());
//			        		s = WHITESPACE.matcher(s).replaceAll(" ");
//		        			String[] numbers = s.trim().split(" ");
//			        		if(month != 0) {
//			        			if(numbers.length != 2) {
//			        				System.out.println("shit");
//			        			} else {
//			        				
//			        				System.out.println(numbers[1] + "-" + (month < 10 ? "0" + month : month) + "-" +
//			        						(numbers[0].length() != 1 ? numbers[0] : "0" + numbers[0]));
//			        			}		        			
//			        		} else {
//			        			if(numbers.length > 1) {
//			        				System.out.println("shit2");
//			        			} else {
//			        				System.out.println(numbers[0]);
//			        			}
//			        		}
//			        		
//			        	}
			      }
			}
////			
//			
	}
		
	
	private static int containsMonth(String object) {
		for(int i = 0; i<MONTHS.size(); i++) {
			if(object.contains(" " + MONTHS.get(i) + " ") || object.contains(MONTHS.get(i) + " ") ||
					object.contains(" " + MONTHS.get(i))) return i+1;
		}
		return 0;
	}
	
	public static String getLemma(String input) {
 		Annotation noun = new Annotation(input);
 		pipeline.annotate(noun);
 		List<CoreMap> sentences = noun.get(SentencesAnnotation.class);
 		if(sentences.size() > 1) return null;
 		CoreMap sentence = sentences.get(0);
 		List<CoreLabel> token = sentence.get(TokensAnnotation.class);
 		return token.get(0).get(LemmaAnnotation.class);
 	}
	
	private static String cleanArticle(String article) {
		article = NULLCHAR.matcher(article).replaceAll("");
		article = URLS.matcher(article).replaceAll("");
		article = PARENTHESES.matcher(article).replaceAll("");
		article = SYMBOLS.matcher(article).replaceAll("");
		article = WHITESPACE1.matcher(article).replaceAll(" ");
		return article;
	}
}
