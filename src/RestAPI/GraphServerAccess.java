package RestAPI;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.MediaType;


import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.kernel.StoreLockException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import Node.IndexHits;
import Node.NodeJSON;



public class GraphServerAccess
{
	private static String DB_URI;



	public Logger logger = new Logger();

	private ClusterEliminator cEliminator;


	public GraphServerAccess(String input_oracle) throws StoreLockException, IOException
	{
		DB_URI = input_oracle;

		cEliminator = new ClusterEliminator("class-collisions_update.txt", "forReid.txt");

		//logger.disableAccessTimes();
		//logger.disableCacheHit();

	}

	public static JSONArray queryURI(String URI)
	{
		WebResource resource = Client.create().resource(URI);
		ClientResponse response = resource.accept("application/json").get( ClientResponse.class );
		String jsonString = response.getEntity( String.class);
		JSONArray jsonArray = null;
		if(!jsonString.startsWith("{"))
		{
			try 
			{
				jsonArray = new JSONArray(jsonString);
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
		}
		else
		{
			try 
			{
				JSONObject jsonObj = new JSONObject(jsonString);
				jsonArray = new JSONArray();
				jsonArray.put(jsonObj);
			}
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
		}
		response.close();
		return jsonArray;
	}

	private String getCurrentMethodName()
	{
		StackTraceElement stackTraceElements[] = (new Throwable()).getStackTrace();
		return stackTraceElements[1].toString();
	}

	//json works
	public IndexHits<NodeJSON> getCandidateClassNodes(String className, HashMap<String, IndexHits<NodeJSON>> candidateNodesCache) 
	{
		long start = System.nanoTime();
		IndexHits<NodeJSON> candidateClassCollection = null;
		if(candidateNodesCache.containsKey(className))
		{
			candidateClassCollection = candidateNodesCache.get(className);
			logger.printIfCacheHit("cache hit class!");
		}
		else
		{
			candidateClassCollection = new IndexHits<NodeJSON>();
			String cypher = "START root=node:short_classes(short_name={startName}) WHERE root.vis = {public} OR root.vis = {notset} RETURN root";
			JSONObject tempJSON = new JSONObject();
			tempJSON.put("startName", className.replace(".", "$"));
			tempJSON.put("public", "PUBLIC");
			tempJSON.put("notset", "NOTSET");
			JSONObject json = new JSONObject();
			json.put("query", cypher);
			json.put("params", tempJSON);

			String jsonString = postQuery(DB_URI+ "/cypher", json.toString());
			JSONObject jsonArray = null;
			try 
			{
				jsonArray = new JSONObject(jsonString);
			}
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			JSONArray tempArray = (JSONArray) jsonArray.get("data");
			for(int i=0; i<tempArray.length(); i++)
			{
				JSONArray arr = tempArray.getJSONArray(i);
				JSONObject obj = arr.getJSONObject(0);
				NodeJSON nodejson = new NodeJSON(obj);
				candidateClassCollection.add(nodejson);
			}
			candidateNodesCache.put(className, candidateClassCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), className + " "+ candidateClassCollection.size(), end, start);
		return candidateClassCollection;
	}
	
	//json works
	public IndexHits<NodeJSON> getCandidateMethodNodes(String methodName, HashMap<String, IndexHits<NodeJSON>> candidateMethodNodesCache) 
	{
		long start = System.nanoTime(); 
		IndexHits<NodeJSON> candidateMethodNodes = null;
		if(candidateMethodNodesCache.containsKey(methodName))
		{
			candidateMethodNodes = candidateMethodNodesCache.get(methodName);
			logger.printIfCacheHit("cache hit method!");
		}
		else
		{
			candidateMethodNodes = new IndexHits<NodeJSON>();
			String cypher = "START root=node:short_methods(short_name={startName}) WHERE root.vis = {public} OR root.vis = {notset} RETURN root";
			JSONObject tempJSON = new JSONObject();
			tempJSON.put("startName", methodName.replace(".", "$"));
			tempJSON.put("public", "PUBLIC");
			tempJSON.put("notset", "NOTSET");
			JSONObject json = new JSONObject();
			json.put("query", cypher);
			json.put("params", tempJSON);

			String jsonString = postQuery(DB_URI+ "/cypher", json.toString());
			JSONObject jsonArray = null;
			try 
			{
				jsonArray = new JSONObject(jsonString);
			}
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			JSONArray tempArray = (JSONArray) jsonArray.get("data");
			for(int i=0; i<tempArray.length(); i++)
			{
				JSONArray arr = tempArray.getJSONArray(i);
				JSONObject obj = arr.getJSONObject(0);
				NodeJSON nodejson = new NodeJSON(obj);
				candidateMethodNodes.add(nodejson);
			}
			candidateMethodNodesCache.put(methodName, candidateMethodNodes);

		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), methodName + candidateMethodNodes.size(), end, start);
		return candidateMethodNodes;
	}

	public NodeJSON returnRightNodeIfCluster(Set<NodeJSON> set)
	{
		if(set.isEmpty())
			return null;
		HashSet<NodeJSON> javaSet = new HashSet<NodeJSON>();
		for(NodeJSON node : set)
		{
			if(node.getProperty("id").startsWith("java."))
				javaSet.add(node);
		}
		
		/*if(javaSet.size() == 1)
			return javaSet.iterator().next();*/
		
		if(cEliminator.checkIfCluster(set))
		{
			NodeJSON rightClass = cEliminator.findRightClass(set);
			return rightClass;
		}
		else
		{
			//check if set has the originalClass from the hashmap. Maybe that is the right ans and the cluster set
			//I created has some missing entities.
			//Node rightClass = cEliminator.findRightClass(set);
			//return rightClass;
		}
		return null;
	}

	
	public boolean checkIfParentNode(NodeJSON parentNode, String childId, HashMap<String, ArrayList<NodeJSON>> parentNodeCache)
	{
		long start = System.nanoTime();
		String parentId = (String) parentNode.getProperty("id");

		if(parentId.equals("java.lang.Object"))
		{
			long end = System.nanoTime();
			logger.printAccessTime(getCurrentMethodName(), parentNode.getProperty("id") + " | " + childId + ": true", end, start);
			return true;
		}

		if(parentNodeCache.containsKey(childId))
		{
			logger.printIfCacheHit("parent list found in cache");
			ArrayList<NodeJSON>parents = parentNodeCache.get(childId);
			for(NodeJSON parent : parents)
			{
				if(((String)parent.getProperty("id")).equals(parentId))
				{
					long end = System.nanoTime();
					logger.printAccessTime(getCurrentMethodName(), parentNode.getProperty("id") + " | " + childId +": true", end, start);
					return true;
				}
			}
			long end = System.nanoTime();
			logger.printAccessTime(getCurrentMethodName(), parentNode.getProperty("id") + " | " + childId +": false", end, start);
			return false;
		}
		else
		{
			String cypher = "START root=node:parentNodes(childId={childId}) RETURN root";
			JSONObject tempJSON = new JSONObject();
			tempJSON.put("childId", childId);
			JSONObject json = new JSONObject();
			json.put("query", cypher);
			json.put("params", tempJSON);
			ArrayList<NodeJSON> parentElementCollection = new ArrayList<NodeJSON>();
			String jsonString = postQuery(DB_URI+ "/cypher", json.toString());
			JSONObject jsonArray = null;
			try 
			{
				jsonArray = new JSONObject(jsonString);
			}
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			JSONArray tempArray = (JSONArray) jsonArray.get("data");
			for(int i=0; i<tempArray.length(); i++)
			{
				JSONArray arr = tempArray.getJSONArray(i);
				JSONObject obj = arr.getJSONObject(0);
				NodeJSON nodejson = new NodeJSON(obj);
				parentElementCollection.add(nodejson);
			}
			parentNodeCache.put(childId, parentElementCollection);
			
			//
			boolean ans = false;
			ArrayList<NodeJSON> parentList = new ArrayList<NodeJSON>();
			for(NodeJSON candidate : parentElementCollection)
			{
				parentList.add(candidate);
				if(((String)candidate.getProperty("id")).equals(parentId))
				{
					ans = true;
				}
			}
			long end = System.nanoTime();
			logger.printAccessTime(getCurrentMethodName(), parentNode.getProperty("id") + " | " + childId +": " + ans, end, start);
			return ans;
		}
	}
	
	//json works
	public boolean checkIfParentNodeOld(NodeJSON parentNode, String childId, HashMap<String, ArrayList<NodeJSON>> parentNodeCache)
	{
		long start = System.nanoTime();
		Integer x = 5;
		
		if(((String)parentNode.getProperty("id")).equals("java.lang.Object"))
		{
			long end = System.nanoTime();
			logger.printAccessTime(getCurrentMethodName(), parentNode.getProperty("id") + " | " + childId + ": true", end, start);
			return true;
		}
		String parentId = (String) parentNode.getProperty("id");
		if(parentNodeCache.containsKey(childId))
		{
			logger.printIfCacheHit("parent list found in cache");
			ArrayList<NodeJSON>parents = parentNodeCache.get(childId);
			for(NodeJSON parent : parents)
			{
				if(((String)parent.getProperty("id")).equals(parentId))
				{
					long end = System.nanoTime();
					logger.printAccessTime(getCurrentMethodName(), parentNode.getProperty("id") + " | " + childId +": true", end, start);
					return true;
				}
			}
			long end = System.nanoTime();
			logger.printAccessTime(getCurrentMethodName(), parentNode.getProperty("id") + " | " + childId +": false", end, start);
			return false;
		}
		else
		{
			String cypher = "START root=node:parentNodes(childId={childId}) RETURN root";
			JSONObject tempJSON = new JSONObject();
			tempJSON.put("childId", childId);
			JSONObject json = new JSONObject();
			json.put("query", cypher);
			json.put("params", tempJSON);
			ArrayList<NodeJSON> parentElementCollection = new ArrayList<NodeJSON>();
			String jsonString = postQuery(DB_URI+ "/cypher", json.toString());
			JSONObject jsonArray = null;
			try 
			{
				jsonArray = new JSONObject(jsonString);
			}
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			JSONArray tempArray = (JSONArray) jsonArray.get("data");
			for(int i=0; i<tempArray.length(); i++)
			{
				JSONArray arr = tempArray.getJSONArray(i);
				JSONObject obj = arr.getJSONObject(0);
				NodeJSON nodejson = new NodeJSON(obj);
				parentElementCollection.add(nodejson);
			}
			parentNodeCache.put(childId, parentElementCollection);
			
			//
			boolean ans = false;
			ArrayList<NodeJSON> parentList = new ArrayList<NodeJSON>();
			for(NodeJSON candidate : parentElementCollection)
			{
				parentList.add(candidate);
				if(((String)candidate.getProperty("id")).equals(parentId))
				{
					ans = true;
				}
			}
			long end = System.nanoTime();
			logger.printAccessTime(getCurrentMethodName(), parentNode.getProperty("id") + " | " + childId +": " + ans, end, start);
			return ans;
		}
	}

	//json works i think
	public ArrayList<ArrayList<NodeJSON>> getMethodNodeWithShortClass(String methodExactName, String classExactName, HashMap<String, ArrayList<ArrayList<NodeJSON>>> shortClassShortMethodCache, HashMap<NodeJSON, NodeJSON> methodReturnCache, HashMap<NodeJSON, NodeJSON> methodContainerCache)
	{
		long start = System.nanoTime(); 
		ArrayList<ArrayList<NodeJSON>> classMethodCollection = new ArrayList<ArrayList<NodeJSON>>();
		ArrayList<NodeJSON> classCollection = new ArrayList<NodeJSON>();
		ArrayList<NodeJSON> methodCollection = new ArrayList<NodeJSON>();
		ArrayList<NodeJSON> returnCollection = new ArrayList<NodeJSON>();
		if(shortClassShortMethodCache.containsKey(classExactName + '.' + methodExactName))
		{
			classMethodCollection = shortClassShortMethodCache.get(classExactName + '.' + methodExactName);
			logger.printIfCacheHit("cache hit methods in class ++");
		}
		else
		{
			String cypher = "START class=node:short_classes(short_name = {classexactname}), method = node:short_methods(short_name = {methodexactname}) MATCH class-[:HAS_METHOD]->method, method-[:RETURN_TYPE]->returnNode WHERE (class.vis = {notset} or class.vis = {public}) and (method.vis = {notset} or method.vis = {public}) RETURN class, method, returnNode";
			JSONObject tempJSON = new JSONObject();
			tempJSON.put("classexactname", classExactName);
			tempJSON.put("methodexactname", methodExactName);
			tempJSON.put("notset", "NOTSET");
			tempJSON.put("public", "PUBLIC");
			JSONObject json = new JSONObject();
			json.put("query", cypher);
			json.put("params", tempJSON);
			String jsonString = postQuery(DB_URI+ "/cypher", json.toString());
			JSONObject jsonArray = null;
			try 
			{
				jsonArray = new JSONObject(jsonString);
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			JSONArray tempArray = (JSONArray) jsonArray.get("data");
			for(int i=0; i<tempArray.length(); i++)
			{
				JSONArray arr = tempArray.getJSONArray(i);
				JSONObject classObj = arr.getJSONObject(0);
				JSONObject methodObj = arr.getJSONObject(1);
				JSONObject returnObj = arr.getJSONObject(2);
				NodeJSON classNodejson = new NodeJSON(classObj);
				NodeJSON methodNodejson = new NodeJSON(methodObj);
				NodeJSON returnNodejson = new NodeJSON(returnObj);
				classCollection.add(classNodejson);
				methodCollection.add(methodNodejson);
				returnCollection.add(returnNodejson);
				methodReturnCache.put(methodNodejson, returnNodejson);
				methodContainerCache.put(methodNodejson, classNodejson);
			}
			classMethodCollection.add(classCollection);
			classMethodCollection.add(methodCollection);
			classMethodCollection.add(returnCollection);
			shortClassShortMethodCache.put(classExactName+'.'+methodExactName, classMethodCollection);

		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), classExactName+'.'+methodExactName, end, start);
		return classMethodCollection;
	}

	//json works
	public ArrayList<NodeJSON> getMethodNodesInClassNode(NodeJSON classNode, String methodExactName,  HashMap<String, IndexHits<NodeJSON>> methodNodesInClassNode)
	{
		long start = System.nanoTime(); 
		String fqn = classNode.getProperty("id") + "." + methodExactName;
		IndexHits<NodeJSON> methodCollection = new IndexHits<NodeJSON>();
		if(methodNodesInClassNode.containsKey(fqn))
		{
			logger.printIfCacheHit("cache hit methods in class ++");
			methodCollection = methodNodesInClassNode.get(fqn);
		}
		else
		{
			//String cypher = "START root=node:classes(id = {classname}) MATCH (root)-[:HAS_METHOD]->(method) WHERE method.exactName = {exactName} RETURN method";
			String cypher = "START n=node:methods('id:"+ fqn +"*') RETURN n";
			
			JSONObject tempJSON = new JSONObject();
			
			//tempJSON.put("classname", classNode.getProperty("id"));
			//tempJSON.put("exactName", methodExactName);
			//tempJSON.put("fqn", fqn);
			JSONObject json = new JSONObject();
			json.put("query", cypher);
			json.put("params", tempJSON);
			String jsonString = postQuery(DB_URI+ "/cypher", json.toString());
			JSONObject jsonArray = null;
			try 
			{
				jsonArray = new JSONObject(jsonString);
			}
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			JSONArray tempArray = (JSONArray) jsonArray.get("data");
			for(int i=0; i<tempArray.length(); i++)
			{
				JSONArray arr = tempArray.getJSONArray(i);
				JSONObject obj = arr.getJSONObject(0);
				NodeJSON nodejson = new NodeJSON(obj);
				methodCollection.add(nodejson);
			}
			methodNodesInClassNode.put(fqn, methodCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), classNode.getProperty("id")+"."+methodExactName + ":" + methodCollection.size(), end, start);
		return methodCollection;
	}

	//json works
	public ArrayList<NodeJSON> getMethodParams(NodeJSON node, HashMap<NodeJSON, ArrayList<NodeJSON>> methodParameterCache) 
	{
		long start = System.nanoTime();

		ArrayList<NodeJSON> paramNodesCollection = new ArrayList<NodeJSON>();

		if(methodParameterCache.containsKey(node))
		{
			paramNodesCollection = methodParameterCache.get(node);
			logger.printIfCacheHit("Cache hit method parameters");
		}
		else
		{
			String methodId = node.getProperty("id");
			String[] params = methodId.substring(methodId.indexOf("(")+1, methodId.indexOf(")")).split(",");
			for(String param : params)
			{
				if(!param.equals(""))
				{
					String exactName = getExactName(param);
					JSONObject data = new JSONObject();
					data.put("id", param.trim());
					data.put("exactName",exactName);
					JSONObject temp = new JSONObject();
					temp.put("data", data);
					NodeJSON nodeParameter = new NodeJSON(temp); 
					paramNodesCollection.add(nodeParameter);
				}
			}
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString() + " : " + paramNodesCollection.size(), end, start);
		return paramNodesCollection;
	}

	public String getExactName(String _id) 
	{
		String name=_id;
		int i;
		for(i = 0;i<name.length();i++)
		{
			if(Character.isUpperCase(name.charAt(i)))
			{
				return _id.substring(i);
			}
		}
		return _id;
	}
	
	//json works
	public ArrayList<NodeJSON> getParents(final NodeJSON node, HashMap<String, ArrayList<NodeJSON>> parentNodeCache )
	{
		long start = System.nanoTime(); 
		String childId = (String) node.getProperty("id");
		ArrayList<NodeJSON> classElementCollection = null;
		if(parentNodeCache.containsKey(childId))
		{
			classElementCollection = parentNodeCache.get(childId);
		}
		else
		{
			String cypher = "START root=node:parentNodes(childId={childId}) RETURN root";
			JSONObject tempJSON = new JSONObject();
			tempJSON.put("childId", childId);
			JSONObject json = new JSONObject();
			json.put("query", cypher);
			json.put("params", tempJSON);
			classElementCollection = new ArrayList<NodeJSON>();
			String jsonString = postQuery(DB_URI+ "/cypher", json.toString());
			JSONObject jsonArray = null;
			try 
			{
				jsonArray = new JSONObject(jsonString);
			}
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			JSONArray tempArray = (JSONArray) jsonArray.get("data");
			if(tempArray.length()>0)
			{
				for(int i=0; i<tempArray.length(); i++)
				{
					JSONArray arr = tempArray.getJSONArray(i);
					JSONObject obj = arr.getJSONObject(0);
					if(obj.has("isInterface") && obj.has("isAbstract") && obj.has("isPrimitive") )
					{
						if(obj.get("isInterface").equals("false") && obj.get("isAbstract").equals("false") && obj.get("isPrimitive").equals("false") && (obj.get("vis").equals("PUBLIC") || obj.get("vis").equals("NOTSET")))
						{
							NodeJSON nodejson = new NodeJSON(obj);
							classElementCollection.add(nodejson);
						}
					}
				}
			}
			parentNodeCache.put(childId, classElementCollection);
		}
		//s.getParents(GraphServerAccess.java:493) - org.pietschy.wizard.PanelWizardStep : 0.0563
		//ess.getParents(GraphServerAccess.java:493) - com.extjs.gxt.desktop.client.StartButton : 24.190832406
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString(), end, start);
		return classElementCollection;
	}
	
	//json works
	public NodeJSON getMethodReturn(NodeJSON node, HashMap<NodeJSON, NodeJSON> methodReturnCache) 
	{

		long start = System.nanoTime(); 
		NodeJSON returnNode = null;
		if(methodReturnCache.containsKey(node))
		{
			returnNode = methodReturnCache.get(node);
			logger.printIfCacheHit("Cache hit method return");
		}
		else
		{
			String cypher = "START root=node:methods(id = {methodName}) MATCH (root)-[:RETURN_TYPE]->(container) RETURN container LIMIT 1";
			JSONObject tempJSON = new JSONObject();
			tempJSON.put("methodName", node.getProperty("id"));
			JSONObject json = new JSONObject();
			json.put("query", cypher);
			json.put("params", tempJSON);

			String jsonString = postQuery(DB_URI+ "/cypher", json.toString());
			JSONObject jsonArray = null;
			try 
			{
				jsonArray = new JSONObject(jsonString);
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			JSONArray tempArray = (JSONArray) jsonArray.get("data");
			JSONArray temptempArray = (JSONArray)tempArray.get(0);
			returnNode = new NodeJSON(temptempArray.getJSONObject(0));
			methodReturnCache.put(node, returnNode);
		}
		long end = System.nanoTime();
		if(returnNode != null)
			logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString() + " - " + returnNode.getProperty("id").toString(), end, start);
		return returnNode;
	}

	//json works
	public NodeJSON getMethodContainer(NodeJSON node,HashMap<NodeJSON, NodeJSON> methodContainerCache) 
	{
		long start = System.nanoTime(); 
		NodeJSON containerNode = null;
		if(methodContainerCache.containsKey(node))
		{
			containerNode = methodContainerCache.get(node);
			logger.printIfCacheHit("Cache hit method return");
		}
		else
		{
			String cypher = "START root=node:methods(id = {methodName}) MATCH (root)-[:IS_METHOD]->(container) RETURN container LIMIT 1";

			JSONObject tempJSON = new JSONObject();
			tempJSON.put("methodName", node.getProperty("id"));
			JSONObject json = new JSONObject();
			json.put("query", cypher);
			json.put("params", tempJSON);

			String jsonString = postQuery(DB_URI+ "/cypher", json.toString());
			JSONObject jsonArray = null;
			try 
			{
				jsonArray = new JSONObject(jsonString);
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			JSONArray tempArray = (JSONArray) jsonArray.get("data");
			JSONArray temptempArray = (JSONArray)tempArray.get(0);
			containerNode = new NodeJSON(temptempArray.getJSONObject(0));
			methodContainerCache.put(node, containerNode);
		}
		long end = System.nanoTime();
		if(containerNode!=null)
			logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString() + " - " + containerNode.getProperty("id").toString(), end, start);
		return containerNode;
	}

	private String postQuery(String queryuri, String payload) 
	{
		WebResource resource = Client.create().resource( queryuri);
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).entity(payload).post(ClientResponse.class);
		String jsonString = response.getEntity(String.class);
		response.close();
		return jsonString;
	}

	/*public IndexHits<NodeJSON> getAllMethodsInClass(NodeJSON parentNode, HashMap<String, IndexHits<NodeJSON>> allMethodsInClass) 
	{

		long start = System.nanoTime(); 
		IndexHits<NodeJSON> methodCollection = new IndexHits<NodeJSON>();
		String className = (String) parentNode.getProperty("id");
		if(allMethodsInClass.containsKey(className))
		{
			methodCollection = allMethodsInClass.get(className);
			logger.printIfCacheHit("cache hit methods in class ++");
		}
		else
		{
			//String cypher = "START root=node({startName})MATCH (root)-[:HAS_METHOD]->(method) RETURN method";
			String cypher = "START node:classes(id = {className}) MATCH (root)-[:HAS_METHOD]->(method) RETURN method";
			JSONObject tempJSON = new JSONObject();
			tempJSON.put("className", parentNode.getProperty("id"));
			//tempJSON.put("startName", parentNode.getNodeNumber());
			JSONObject json = new JSONObject();
			json.put("query", cypher);
			json.put("params", tempJSON);
			String jsonString = postQuery(DB_URI+ "/cypher", json.toString());
			JSONObject jsonArray = null;
			try 
			{
				jsonArray = new JSONObject(jsonString);
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			JSONArray tempArray = (JSONArray) jsonArray.get("data");
				JSONArray temptempArray = (JSONArray)tempArray.get(0);
				for(int i=0; i<temptempArray.length(); i++)
				{
					JSONObject obj = temptempArray.getJSONObject(i);
					NodeJSON nodejson = new NodeJSON(obj);
					methodCollection.add(nodejson);
				}
			allMethodsInClass.put(className, methodCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), className + "."  , end, start);
		return methodCollection;
	}*/

}