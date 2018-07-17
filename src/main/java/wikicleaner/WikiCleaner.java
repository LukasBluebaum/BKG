package wikicleaner;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Handles the cleaning of the wikipedia dump. Using {@link Reader}, {@link Reader} and {@link Writer}.
 * @author Lukas Blübaum
 * @author Nick Düsterhus
 * @author Monika Werner
 */
public class WikiCleaner {
	
	protected final static int WORKERAMOUNT = 4;
	
	protected final static String END = "END";
	
	private final static int QUEUESIZE = 10000;
	
	/**
	 * Instantiates and starts a {@link Reader} and a {@link Writer} thread and an appropriate amount of {@link Reader} threads to clean the given file.
	 * @param in Path to the input file.
	 * @param out Path to the output file.
	 */
	public void cleanWikiDump(String in, String out) {
		
		File input = new File(in);
		File output = new File(out);
		BlockingQueue<String> readQueue = new ArrayBlockingQueue<String>(QUEUESIZE);	    
	    BlockingQueue<String> writeQueue = new ArrayBlockingQueue<String>(QUEUESIZE);
	    
	    Reader reader = new Reader(readQueue, input);
	    Writer writer = new Writer(writeQueue,output);
	    
	    Worker[] workers = new Worker[WORKERAMOUNT];
	    for(int i = 0; i<workers.length; i++) {
	    	workers[i] = new Worker(readQueue,writeQueue);
	    }
	    
	    new Thread(reader).start();
	    for(int i = 0; i<workers.length; i++) {
	    	 new Thread(workers[i]).start();
	    }
	    new Thread(writer).start();
	}
	
	public static void main(String[] args) {
		WikiCleaner cleaner = new WikiCleaner();
		cleaner.cleanWikiDump("resources/enwiki-20171103-pages.tsv", "resources/out.txt");
	}	
}
