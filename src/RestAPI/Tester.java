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

	public static NodeIndex classIndex ;
	public static NodeIndex methodIndex ;

	public static NodeIndex shortClassIndex ;
	public static NodeIndex shortMethodIndex ;
	
	public static NodeIndex allMethodsIndex;
	private static NodeIndex newParentsIndex;

	public static void main(String[] args) throws StoreLockException, IOException 
	{
		classIndex = new NodeIndex(DB_URI + "/index/node/classes/id/");
		methodIndex = new NodeIndex(DB_URI + "/index/node/methods/id/");
		shortClassIndex = new NodeIndex(DB_URI + "/index/node/short_classes/short_name/");
		shortMethodIndex = new NodeIndex(DB_URI + "/index/node/short_methods/short_name/");
		allMethodsIndex = new NodeIndex(DB_URI + "/index/node/allMethodsIndex/classId/");
		newParentsIndex = new NodeIndex(DB_URI + "/index/node/parentNodes/childId/");
		graphDb = new GraphServerAccess(DB_URI);
		IndexHits<NodeJSON> classes = graphDb.getCandidateClassNodes("AutoCompleteTextView", new HashMap<String, IndexHits<NodeJSON>>());
		for(NodeJSON classNode : classes)
		{
			ArrayList<NodeJSON> methods = graphDb.getMethodNodes(classNode, new HashMap<String, ArrayList<NodeJSON>>());
			for(NodeJSON method : methods)
			{
				System.out.println(method.getProperty("id"));
			}
			
		}
		
	}
}