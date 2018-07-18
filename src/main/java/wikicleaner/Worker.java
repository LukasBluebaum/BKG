package wikicleaner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;

/**
 * Cleans the lines from the dump and forwards them to the {@link Writer}.
 * @author Lukas Blübaum
 * @author Nick Düsterhus
 * @author Monika Werner
 *
 */
public class Worker implements Runnable{

	private static final Pattern URLS = Pattern.compile("http.*?\\s");
	
	private static final Pattern PARENTHESES = Pattern.compile("\\(.*?\\)");
	
	private static final Pattern PARENTHESESD = Pattern.compile("\\[.*?\\]");
	
	private static final Pattern SYMBOLS = Pattern.compile("[^a-zA-Z0-9.,:' ]");
	
	private static final Pattern NULLCHAR = Pattern.compile("\0");
	
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	
	private static final ArrayList<String> PRESIDENTS = new ArrayList<String>(Arrays.asList("John_Adams", "John_Quincy_Adams" ,"Chester_A._Arthur" ,
			"James_Buchanan", "George_H._W._Bush","George_W._Bush","Jimmy_Carter","Grover_Cleveland","Bill_Clinton","Calvin_Coolidge","Dwight_D._Eisenhower",
			"Millard_Fillmore","Gerald_Ford", "James_A._Garfield","Ulysses_S._Grant","Warren_G._Harding",
			"Benjamin_Harrison","William_Henry_Harrison", "Rutherford_B._Hayes","Herbert_Hoover","Andrew_Jackson","Thomas_Jefferson","Andrew_Johnson","Lyndon_B._Johnson",
			"John_F._Kennedy","Abraham_Lincoln","James_Madison", "William_McKinley","James_Monroe","Richard_Nixon","Barack_Obama","Franklin_Pierce","James_K._Polk",
			"Ronald_Reagan","Franklin_D._Roosevelt", "Theodore_Roosevelt","William_Howard_Taft","Zachary_Taylor","Harry_S._Truman","Donald_Trump","John_Tyler",
			"Martin_Van_Buren","George_Washington", "Woodrow_Wilson", "List_of_Presidents_of_the_United_States", "Lifespan_timeline_of_Presidents_of_the_United_States",
			"President_of_the_United_States"));
	
	private BlockingQueue<String> readQueue = null;
	
	private BlockingQueue<String> writeQueue = null;

	public Worker(BlockingQueue<String> readQueue, BlockingQueue<String> writeQueue){
		this.readQueue = readQueue; 
		this.writeQueue = writeQueue; 
	}

	@Override
	public void run() {
		try {      
			System.out.println(PRESIDENTS.size());
			while(true) {
				String article = readQueue.take();
				if(article.equals(WikiCleaner.END)){ 
					readQueue.put(WikiCleaner.END);
					writeQueue.put(WikiCleaner.END);
					break;
				}
				article = NULLCHAR.matcher(article).replaceAll("");
//				int i = article.indexOf("\t");
//				
//				if(i != -1) {
//					String s = article.substring(0, i);
//					s = s.substring(s.lastIndexOf("/")+1);
//					if(containsPresident(s)  && article.length() > 10000) {
//						System.out.println(s);
//						String clean = cleanArticle(article);
//						if(clean.length() > 20000) {
//							writeQueue.put(clean);
//						}
//					}				
//				}							
				writeQueue.put(cleanArticle(article));
			}          
		} catch(InterruptedException e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * Removes nullchars, urls, parantheses, special symbols and whitespace from the given string.
	 * @param article
	 * @return
	 */
	private String cleanArticle(String article) {
		//article = NULLCHAR.matcher(article).replaceAll("");
		article = URLS.matcher(article).replaceAll("");
		article = PARENTHESES.matcher(article).replaceAll("");
		article = PARENTHESESD.matcher(article).replaceAll("");
		article = SYMBOLS.matcher(article).replaceAll("");
		article = WHITESPACE.matcher(article).replaceAll(" ");
		return article;
	}	
	
	private boolean containsPresident(String object) {
		for(int i = 0; i<PRESIDENTS.size(); i++) {
			if(object.equals(PRESIDENTS.get(i))) return true;
		}
		return false;
	}
}