package wikicleaner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class Writer implements Runnable{
 
	private BlockingQueue<String> writeQueue;
	
	private File output;

	public Writer(BlockingQueue<String> writeQueue, File output){
		    this.writeQueue = writeQueue; 
		    this.output = output;
	}

	@Override
	public void run() {
		FileWriter writer = null;
		
		try {
			final long startTime = System.currentTimeMillis();
			writer = new FileWriter(output);

			int workerDone = 0;
			while(true) {
				String article = writeQueue.take();
				if(article.equals(WikiCleaner.END)) {
					workerDone++;
					if(workerDone == WikiCleaner.WORKERAMOUNT) break;
				} else {
					writer.write(article + "\r\n");
				}			
			}               
 	 			
			final long endTime = System.currentTimeMillis();
	 		System.out.println("Total execution time: " + (endTime - startTime) );
		} catch ( IOException | InterruptedException e) {
			e.printStackTrace();   
		} finally{
			try {
				writer.close();
				System.out.println("Writer done.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
	}
}