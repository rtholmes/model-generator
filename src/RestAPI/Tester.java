package RestAPI;

import org.json.JSONArray;
import org.json.JSONObject;

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

	public static void main(String[] args) 
	{
		classIndex = new NodeIndex(DB_URI + "/index/node/classes/id/");
		methodIndex = new NodeIndex(DB_URI + "/index/node/methods/id/");
		shortClassIndex = new NodeIndex(DB_URI + "/index/node/short_classes/short_name/");
		shortMethodIndex = new NodeIndex(DB_URI + "/index/node/short_methods/short_name/");
		allMethodsIndex = new NodeIndex(DB_URI + "/index/node/allMethodsIndex/classId/");
		newParentsIndex = new NodeIndex(DB_URI + "/index/node/parentNodes/childId/");
		
		IndexHits<NodeJSON> hits = shortClassIndex.get("Log");	
		for(NodeJSON hit: hits)
		{
			String outgoingrel = hit.getJSONObject().getString("outgoing_relationships");
			JSONArray arr = GraphServerAccess.queryURI(outgoingrel);
			for(int i=0; i<arr.length(); i++)
			{
				JSONObject obj = arr.getJSONObject(i);
				System.out.println(obj.toString(2));
				
			}
		}
	}
}