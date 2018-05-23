package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.json.simple.parser.ParseException;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;


public class WikiParser {
	
	private static final Pattern URLS = Pattern.compile("http.*?\\s");
	
	private static final Pattern PARENTHESES = Pattern.compile("\\(.*?\\)");
	
	private static final Pattern SYMBOLS = Pattern.compile("[^a-zA-Z0-9. ]");
	
	private static final Pattern NULLCHAR = Pattern.compile("\0");
	
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
		
//	private static final String ANNOTATORS = "tokenize, ssplit";
//	
//	private static final StanfordCoreNLP PIPELINE;
//	
//	static {
//		 Properties props = new Properties();
//		 props.put("annotators", ANNOTATORS);
//		 PIPELINE = new StanfordCoreNLP(props);
//	}
////	
//	private CoreDocument getSentences(String input) {		  		 
//	    CoreDocument document = new CoreDocument(input);	   
//	    PIPELINE.annotate(document);
//	    return document;
//	}
//						
//	private void removeRefs(CoreDocument article) {
//		Iterator<CoreSentence> sentences = article.sentences().iterator();
//	    
//	    while(sentences.hasNext()) {
//	    	CoreSentence sentence = sentences.next();
//	    	if(/*!checkVerbs(sentence)*/ sentence.tokens().size() <= 3 || Character.isLowerCase(sentence.tokens().get(0).toString().charAt(0))) {
//	    		sentences.remove();
//	    	}
//	    }
//	}
	
	private String cleanArticle(String article) {
		article = NULLCHAR.matcher(article).replaceAll("");
		article = URLS.matcher(article).replaceAll("");
		article = PARENTHESES.matcher(article).replaceAll("");
		article = SYMBOLS.matcher(article).replaceAll("");
		article = WHITESPACE.matcher(article).replaceAll(" ");
		return article;
	}
	
	private void printSentences(CoreDocument article) throws IOException{
		File fout = new File("out2.txt");
		FileOutputStream fos = new FileOutputStream(fout);		 
		BufferedWriter	bw = new BufferedWriter(new OutputStreamWriter(fos));
		
		for(CoreSentence s: article.sentences()) {
			bw.write(s.text() + "\r\n");
		}
		bw.close();
	}
		
//	private String organizeArticle(String article) throws IOException, ParseException {
//		  	CoreDocument document = getSentences(cleanArticle(article));	
//		  	//printSentences(document);    		
//		 	return document.text();
//	}
	

	
//	public static void main(String[] args) throws ParseException {		
//		BufferedReader rd = null;
//		BufferedWriter bw = null;
//			
//		try {
//		    File file = new File("enwiki-20171103-pages.tsv");
//		    rd =  new BufferedReader(new FileReader(new File("enwiki-20171103-pages.tsv")));
//		    
//		    File fout = new File("out2.txt");
////			FileOutputStream fos = new FileOutputStream(fout);		 
////			bw = new BufferedWriter(new OutputStreamWriter(fos));
//		    FileWriter writer = new FileWriter(fout);
//		    
//			WikiParser wiki = new WikiParser();
//			
//		    String nextLine;
//		    int i = 1;
//		    while((nextLine = rd.readLine()) != null) {		
//		    	//if(nextLine.length() > 1 && nextLine.charAt(1) == 'h') {
//		    		 writer.write((nextLine + "\r\n"));
//		    	//}
//		    	//i++;	
//		    }
//		    writer.flush();
//		    writer.close();    
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {		   
//		    try {
//		    	rd.close();
//		    	//bw.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}		    		    
//		}
//	}
}
