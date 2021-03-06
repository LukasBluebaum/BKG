package wikicleaner;

import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;

/**
 * Cleans the lines from the dump and forwards them to the {@link Writer}.
 * @author Lukas Blübaum
 * @author Nick Düsterhus
 * @author Monika Werner
 *
 */
public class Worker implements Runnable {

	private static final Pattern URLS = Pattern.compile("http.*?\\s");
	
	private static final Pattern PARENTHESES = Pattern.compile("\\(.*?\\)");
	
	private static final Pattern PARENTHESESD = Pattern.compile("\\[.*?\\]");
	
	private static final Pattern SYMBOLS = Pattern.compile("[^a-zA-Z0-9.,:' ]");
	
	private static final Pattern NULLCHAR = Pattern.compile("\0");
	
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	
	private BlockingQueue<String> readQueue = null;
	
	private BlockingQueue<String> writeQueue = null;

	public Worker(BlockingQueue<String> readQueue, BlockingQueue<String> writeQueue){
		this.readQueue = readQueue; 
		this.writeQueue = writeQueue; 
	}

	@Override
	public void run() {
		try {      
			while(true) {
				String article = readQueue.take();
				if(article.equals(WikiCleaner.END)){ 
					readQueue.put(WikiCleaner.END);
					writeQueue.put(WikiCleaner.END);
					break;
				}				
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
		article = NULLCHAR.matcher(article).replaceAll("");
		article = URLS.matcher(article).replaceAll("");
		article = PARENTHESES.matcher(article).replaceAll("");
		article = PARENTHESESD.matcher(article).replaceAll("");
		article = SYMBOLS.matcher(article).replaceAll("");
		article = WHITESPACE.matcher(article).replaceAll(" ");
		return article;
	}	
}