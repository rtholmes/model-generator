package publicAccess;

import RestAPI.*;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.kernel.StoreLockException;

import com.sun.jersey.server.impl.model.parameter.multivalued.ExtractorContainerException;

import Node.IndexHits;
import Node.NodeJSON;


public class GraphAccess
{
	private static GraphServerAccess graphDb;
	private static final String DB_URI = "http://localhost:7474/db/data";


	public static void main(String[] args) throws StoreLockException, IOException 
	{
		//(1) Given a package name, what classes does the Oracle know about? 
		//(2) Given a class (FQN), what methods does it contain? and 
		//(3) Given a method identifier, which classes (FQNs) declare a method with that name? 
		
		int queryType = Integer.valueOf(args[0]);
		String query = args[1];
		graphDb = new GraphServerAccess(DB_URI);

		long startTime = System.nanoTime();
		
		JSONArray jsonArray = new JSONArray();
		JSONObject returnValue = new JSONObject();
		
		switch(queryType)
		{
			case 1:
			{
				IndexHits<NodeJSON> classes = graphDb.getClassesInPackage_PUBLIC_ACCESS(query);
				for(final NodeJSON classNode : classes)
				{
					JSONObject value = classNode.getJSONObject().getJSONObject("data");
					jsonArray.put(value);
				}
				break;
			}
			case 2:
			{
				IndexHits<NodeJSON> methods = graphDb.getMethodsInClass_PUBLIC_ACCESS(query);
				for(final NodeJSON methodNode : methods)
				{
					JSONObject value = methodNode.getJSONObject().getJSONObject("data");
					jsonArray.put(value);
				}
				break;
			}
			case 3:
			{
				IndexHits<NodeJSON> methods = graphDb.getCandidateMethodNodes(query, new HashMap<String, IndexHits<NodeJSON>>());
				for(NodeJSON methodNode : methods)
				{
					JSONObject value = methodNode.getJSONObject().getJSONObject("data");
					String methodName = (String) value.get("id");
					value.put("className",extractClassName(methodName));
					jsonArray.put(value);
				}
				break;
			}
			default:
			{
				System.out.println("Invalid option");
			}
		}
		
		long endTime = System.nanoTime();
		double time = (double)(endTime-startTime)/(1000000000);
		//returnValue.put("time", time);
		returnValue.put("api_elements", jsonArray);
		//returnValue.put("count", jsonArray.length());
		System.out.println(returnValue.toString(3));
	}
	
	public static String extractClassName(String methodName)	//to store class name and exact method name as ivars
	{
		if (methodName.endsWith("<clinit>"))
		{
			String array[] = methodName.split(".<clinit>");
			return array[0];		
		}
		else
		{		
			String[] array = methodName.split("\\(");
			array = array[0].split("\\.");
			String className = array[0];		
			for (int i=1; i<array.length-1; i++) 
				className += "." + array[i];
			return className;
	 	}
		
	}
}