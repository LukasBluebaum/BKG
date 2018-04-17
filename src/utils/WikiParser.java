package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;


public class WikiParser {
	
	private static final Pattern URLS = Pattern.compile("http.*?\\s");
	
	private static final Pattern PARENTHESES = Pattern.compile("\\(.*?\\)");
	
	private static final Pattern SYMBOLS = Pattern.compile("[^a-zA-Z0-9. ]");
	
	private static final Pattern NULLCHAR = Pattern.compile("\0");
	
		
	private static List<String> getSentences(String input){
		
	    List<String> sentenceList = new ArrayList<>();	   
	    Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	   
	    Annotation document = new Annotation(input);	
	    pipeline.annotate(document);
	    // these are all the sentences in this document
	    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
	    List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
	  
	    for(CoreMap sentence:sentences){	    	    	
	    	sentenceList.add(sentence.toString());    	
	    }
	    return sentenceList;
	}
	
	private static String cleanArticle(String article) {
		article = NULLCHAR.matcher(article).replaceAll("");
		article = URLS.matcher(article).replaceAll("");
		article = PARENTHESES.matcher(article).replaceAll("");
		article = SYMBOLS.matcher(article).replaceAll("");
		return article;
	}
	
	
	public static void main(String[] args)	{		
		BufferedReader rd = null;
		BufferedWriter bw = null;
		
		try {
		    File file = new File("testfile.tsv");
		    rd = new BufferedReader(new FileReader(file));
		    
		    
		    File fout = new File("out.txt");
			FileOutputStream fos = new FileOutputStream(fout);		 
			bw = new BufferedWriter(new OutputStreamWriter(fos));
		    		    		    
		    String line = cleanArticle(rd.readLine());		        	       
//		    bw.write(line);
//			bw.newLine();
		            
		    ArrayList<String> s = (ArrayList<String>) getSentences(line);		    
//		    s.remove(s.size()-1);
		    for(int j = 0; j<s.size(); j++)
		    {
		    	bw.write(s.get(j));
		    	bw.newLine();
		    }
		     
		    bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {		   
		    try {
		    	rd.close();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}		    		    
		}
	}
}
