package extraction;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.json.simple.JSONObject;

/**
 * Requests the FOX demo and processes the output.
 * @author Lukas Blübaum
 * @author Nick Düsterhus
 * @author Monika Werner
 *
 */
public class FoxWebservice {
	
	private final String REQUESTURL = "http://fox-demo.aksw.org/fox";
	
	private final String FORMAT = "Turtle";
	
	private final String INPUT = "text";
	
	private final int TIMEOUT = 10000;
	
	
	/**
	 * Sets the parameters for the next FOX request and calls {@link #requestPost()}.
	 * @param inputText
	 * @param lang
	 * @param taskType
	 * @return
	 * @throws IOException 
	 * @throws ProtocolException 
	 * @throws MalformedURLException 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	protected String extract(String inputText, String lang, String taskType) throws IOException  {
		JSONObject urlParameters = new JSONObject();

		urlParameters.put("type", INPUT);
		urlParameters.put("task", taskType);
		urlParameters.put("lang", lang);

		urlParameters.put("output", FORMAT);
		urlParameters.put("input", inputText);
		return requestPOST(urlParameters, REQUESTURL);

	}

	/**
	 * Requests the FOX Demo.
	 * @param urlParameters
	 * @param requestURL
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ProtocolException
	 */
	private String requestPOST(final JSONObject urlParameters, final String requestURL) throws IOException {
		try {	
			URL url = new URL(requestURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			connection.setConnectTimeout(TIMEOUT);
	
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(urlParameters.toString());
			wr.flush();
	
			InputStream inputStream = connection.getInputStream();
			InputStreamReader in = new InputStreamReader(inputStream);
			BufferedReader reader = new BufferedReader(in);
	
			StringBuilder sb = new StringBuilder();
			while (reader.ready()) {
				sb.append(reader.readLine());
			}
	
			wr.close();
			reader.close();
			connection.disconnect();
			return sb.toString();
		} catch(SocketTimeoutException e) {
			System.out.println("TimeOut");	
			return null;
		} 
	}
	
}
