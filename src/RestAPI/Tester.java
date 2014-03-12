package RestAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.kernel.StoreLockException;

import Node.IndexHits;
import Node.NodeIndex;
import Node.NodeJSON;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;







public class Tester
{
	private static GraphServerAccess graphDb;
	private static final String DB_URI = "http://gadget.cs:7474/db/data";


	public static void main(String[] args) throws StoreLockException, IOException 
	{
		graphDb = new GraphServerAccess(DB_URI);
		long startTime = System.nanoTime();
		IndexHits<NodeJSON> classes = graphDb.getCandidateClassNodes("View", new HashMap<String, IndexHits<NodeJSON>>());
		for(final NodeJSON classNode : classes)
		{
		}
		
		long endTime = System.nanoTime();
		double time = (double)(endTime-startTime)/(1000000000);
		System.out.println(time);
		
	}


	public void run() 
	{
		
	}
}