package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;
import java.util.regex.Pattern;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;


public class WikiParser {
	
	private static final Pattern URLS = Pattern.compile("http.*?\\s");
	
	private static final Pattern PARENTHESES = Pattern.compile("\\(.*?\\)");
	
	private static final Pattern SYMBOLS = Pattern.compile("[^a-zA-Z0-9. ]");
	
	private static final Pattern NULLCHAR = Pattern.compile("\0");
	
		
	private static CoreDocument getSentences(String input){		  
	    Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	   
	    CoreDocument document = new CoreDocument(input);	   
	    pipeline.annotate(document);
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
		    File file = new File("enwiki-20171103-pages.tsv");
		    rd = new BufferedReader(new FileReader(file));
		    
		    
		    File fout = new File("out.txt");
			FileOutputStream fos = new FileOutputStream(fout);		 
			bw = new BufferedWriter(new OutputStreamWriter(fos));
		    		    		    
		    String article = cleanArticle(rd.readLine());		        	       
//		    bw.write(line);
//			bw.newLine();
		    
		    CoreDocument document = getSentences(article);	
		    
		    for(CoreSentence s: document.sentences())
		    {
		    	if(/*(s.posTags().contains("VB") || s.posTags().contains("VBD") ||
		    			s.posTags().contains("VBG") || s.posTags().contains("VBN") ||
		    			s.posTags().contains("VBP") || s.posTags().contains("VBZ"))  &&*/ s.tokens().size() > 3 && !Character.isLowerCase(s.tokens().get(0).toString().toCharArray()[0]))
		    	{
		    		bw.write(s.text());
			    	bw.newLine();
		    	}
		    }
		    NamedEntityRecognizer r = new NamedEntityRecognizer();
		   // r.printEntities(document.sentences().get(25).text());
		    
		    
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
