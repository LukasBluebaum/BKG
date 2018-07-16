package wikicleaner;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class WikiCleaner {
	
	protected final static int WORKERAMOUNT = 4;
	
	protected final static String END = "END";
	
	private final static int QUEUESIZE = 10000;
	
	/**
	 * Instantiates and starts a Reader and a Writer thread and an appropriate amount of Worker threads to clean the given file.
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
		cleaner.cleanWikiDump("resources/out2.txt", "resources/o.txt");
	}	
}
