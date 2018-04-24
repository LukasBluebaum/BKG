package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Pattern;

import org.json.simple.parser.ParseException;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;


public class WikiParser {
	
	private static final Pattern URLS = Pattern.compile("http.*?\\s");
	
	private static final Pattern PARENTHESES = Pattern.compile("\\(.*?\\)");
	
	private static final Pattern SYMBOLS = Pattern.compile("[^a-zA-Z0-9. ]");
	
	private static final Pattern NULLCHAR = Pattern.compile("\0");
	
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	
	private static final String ANNOTATORS = "tokenize, ssplit, pos";
	
	private static final StanfordCoreNLP PIPELINE;
	
	static {
		 Properties props = new Properties();
		 props.put("annotators", ANNOTATORS);
		 PIPELINE = new StanfordCoreNLP(props);
	}
	
	private CoreDocument getSentences(String input) {		  		 
	    CoreDocument document = new CoreDocument(input);	   
	    PIPELINE.annotate(document);
	    return document;
		    
//	    Properties props2 = new Properties();
//	    props2.put("annotators", "tokenize, ssplit, pos, lemma,ner, parse, dcoref, sentiment, relation");
//	    //props2.setProperty("ner.useSUTime", "false");
//	    StanfordCoreNLP pipeline2 = new StanfordCoreNLP(props2);
//	    String s = "The capital of Germany is Berlin.";
//	    //CoreDocument document3 = new CoreDocument(document2.sentences().get(0).text());
//	    CoreDocument document3 = new CoreDocument(s);
//	    pipeline2.annotate(document3);
//	    
//	    //System.out.println(document3.sentences().get(0).dependencyParse());
//	    System.out.println(document3.sentences().get(0).dependencyParse().toDotFormat());
//	   
	}
			
	private boolean checkVerbs(CoreSentence sentence) {
		for(String tag: sentence.posTags()) {
			if(tag.startsWith("VB")) {
    			return true;
    		}
		}
		return false;
	}
		
	private void removeRefs(CoreDocument article) {
		Iterator<CoreSentence> sentences = article.sentences().iterator();
	    
	    while(sentences.hasNext()) {
	    	CoreSentence sentence = sentences.next();
	    	if(!checkVerbs(sentence) || Character.isLowerCase(sentence.tokens().get(0).toString().charAt(0))) {
	    		sentences.remove();
	    	}
	    }
	}
	
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
	
	private String organizeArticle(String article) throws IOException, ParseException {
		  	CoreDocument document = getSentences(cleanArticle(article));	
		  	removeRefs(document);
		  	printSentences(document);    		
		 	return document.text();
	}
	
	public static void main(String[] args) throws ParseException {		
		BufferedReader rd = null;
		BufferedWriter bw = null;
		
		try {
		    File file = new File("enwiki-20171103-pages.tsv");
		    Reader reader = new InputStreamReader(new FileInputStream(file), "utf-8");
		    rd = new BufferedReader(reader);
		    
		    File fout = new File("out.txt");
			FileOutputStream fos = new FileOutputStream(fout);		 
			bw = new BufferedWriter(new OutputStreamWriter(fos));
		    
			WikiParser wiki = new WikiParser();
			
		    String nextLine;
		    int i = 1;
		    while((nextLine = rd.readLine()) != null && i<=1) {		
		    	//if(nextLine.length() > 1 && nextLine.charAt(1) == 'h') {
		    		bw.write(wiki.organizeArticle(nextLine) + "\r\n");
		    	//}
		    	i++;	
		    }
		    	    
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
