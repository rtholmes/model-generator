package RestAPI;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import javax.ws.rs.core.MediaType;


import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.kernel.StoreLockException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import Node.IndexHits;
import Node.NodeIndex;
import Node.NodeJSON;



public class GraphServerAccess
{
	private static String DB_URI;

	private NodeIndex methodIndex ;
	//private NodeIndex allClassIndex;
	private NodeIndex allMethodsIndex;
	//private NodeIndex shortClassIndex ;
	private NodeIndex shortMethodIndex ;
	private NodeIndex newParentsIndex;
	
	
	public Logger logger = new Logger();

	private ClusterEliminator cEliminator;
	

	public GraphServerAccess(String input_oracle) throws StoreLockException, IOException
	{
		DB_URI = input_oracle;
		//DB_URI = "http://gadget.cs:7474/db/data";
		
		cEliminator = new ClusterEliminator("class-collisions_update.txt", "forReid.txt");
		
		//logger.disableAccessTimes();
		//logger.disableCacheHit();
		
		//allClassIndex = new NodeIndex(DB_URI + "/index/node/classes/id/");
		methodIndex = new NodeIndex(DB_URI + "/index/node/methods/id");
		//shortClassIndex = new NodeIndex(DB_URI + "/index/node/short_classes/short_name/");
		shortMethodIndex = new NodeIndex(DB_URI + "/index/node/short_methods/short_name/");
		allMethodsIndex = new NodeIndex(DB_URI + "/index/node/allMethodsIndex/classId/");
		
		newParentsIndex = new NodeIndex(DB_URI + "/index/node/parentNodes/childId/");
		
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

	
	/*CYPHER*/
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
			//System.out.println(className + "\n" + jsonString);
			JSONObject jsonArray = null;
			try 
			{
				jsonArray = new JSONObject(jsonString);
			}
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			//System.out.println(jsonArray);
			JSONArray tempArray = (JSONArray) jsonArray.get("data");
			if(tempArray.length()>0)
			{
				for(int i=0; i<tempArray.length(); i++)
				{
					JSONArray obj = tempArray.getJSONArray(i);
					JSONObject toInsert = obj.getJSONObject(0);
					NodeJSON nodejson = new NodeJSON(toInsert);
					candidateClassCollection.add(nodejson);
				}
			}
			else
			{
				//System.out.println("$$ "+tempArray);
			}
			System.out.println(className + " : " +candidateClassCollection.size());
			candidateNodesCache.put(className, candidateClassCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), className, end, start);
		return candidateClassCollection;
	}

	/*CYPHER*/
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
			//System.out.println(jsonArray);
			JSONArray tempArray = (JSONArray) jsonArray.get("data");
			if(tempArray.length()>0)
			{
				for(int i=0; i<tempArray.length(); i++)
				{
					JSONArray obj = tempArray.getJSONArray(i);
					JSONObject toInsert = obj.getJSONObject(0);
					NodeJSON nodejson = new NodeJSON(toInsert);
					candidateMethodNodes.add(nodejson);
				}
			}
			else
			{
				//System.out.println("$$ "+tempArray);
			}
			System.out.println(methodName + " : " +candidateMethodNodes.size());
			candidateMethodNodesCache.put(methodName, candidateMethodNodes);
		
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), methodName, end, start);
		return candidateMethodNodes;
	}

	/*public IndexHits<NodeJSON> getCandidateMethodNodes(String methodName, HashMap<String, IndexHits<NodeJSON>> candidateMethodNodesCache) 
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
			
			IndexHits<NodeJSON> candidateNodes = shortMethodIndex.get(methodName);
			candidateMethodNodes = new IndexHits<NodeJSON>();
			for(NodeJSON candidate : candidateNodes)
			{
				if(candidate!=null)
					if(((String)candidate.getProperty("vis")).equals("PUBLIC")==true || ((String)candidate.getProperty("vis")).equals("NOTSET")==true)
						candidateMethodNodes.add(candidate);
			}
			candidateMethodNodesCache.put(methodName, candidateMethodNodes);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), methodName, end, start);
		return candidateMethodNodes;
	}
*/
	
	public NodeJSON returnRightNodeIfCluster(Set<NodeJSON> set)
	{
		if(set.isEmpty())
			return null;
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
		
		if(((String)parentNode.getProperty("id")).equals("java.lang.Object"))
		{
			long end = System.nanoTime();
			logger.printAccessTime(getCurrentMethodName(), parentNode.getProperty("id") + " | " + childId, end, start);
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
					logger.printAccessTime(getCurrentMethodName(), parentNode.getProperty("id") + " | " + childId, end, start);
					return true;
				}
			}
			long end = System.nanoTime();
			logger.printAccessTime(getCurrentMethodName(), parentNode.getProperty("id") + " | " + childId, end, start);
			return false;
		}
		else
		{
			boolean ans = false;
			IndexHits<NodeJSON> candidateNodes = newParentsIndex.get(childId);
			ArrayList<NodeJSON> parentList = new ArrayList<NodeJSON>();
			for(NodeJSON candidate : candidateNodes)
			{
				parentList.add(candidate);
				if(((String)candidate.getProperty("id")).equals(parentId))
				{
					ans = true;
				}
			}
			//Node object = allClassIndex.get("id", "java.lang.Object").getSingle();
			//parentList.add(object);
			parentNodeCache.put(childId, parentList);
			long end = System.nanoTime();
			logger.printAccessTime(getCurrentMethodName(), parentNode.getProperty("id") + " | " + childId, end, start);
			return ans;
		}
	}


	private static String escapeForLucene(String input)
	{
		StringBuilder output = new StringBuilder();
		for(int i=0; i<input.length(); i++)
		{
			char x = input.charAt(i);
			if(x=='[' || x==']' || x=='+' || x=='^' || x=='"' || x=='?')
			{
				output.append("\\"+x);
			}
			else
			{
				output.append(x);
			}
		}
		return output.toString();
	}

	public ArrayList<NodeJSON> getMethodNodes(NodeJSON node, HashMap<String, ArrayList<NodeJSON>> methodNodesInClassNode)
	{
		long start = System.nanoTime(); 
		ArrayList<NodeJSON> methodsCollection = null;
		String className = (String) node.getProperty("id");
		if(methodNodesInClassNode.containsKey(node))
		{
			methodsCollection = methodNodesInClassNode.get(className);
			logger.printIfCacheHit("Cache hit methods in class");
		}
		else
		{
			IndexHits<NodeJSON> methods = allMethodsIndex.get(className);
			methodsCollection = new ArrayList<NodeJSON>();
			for(NodeJSON method : methods)
			{
				if(method!=null)
				{
					if(((String)method.getProperty("vis")).equals("PUBLIC")==true || ((String)method.getProperty("vis")).equals("NOTSET")==true)
					{
						methodsCollection.add(method);
					}
				}
			}
			methodNodesInClassNode.put(className, methodsCollection);
		}

		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString(), end, start);
		return methodsCollection;
	}

	/*CYPHER*/
	public ArrayList<NodeJSON> getMethodNodesInClassNode(NodeJSON classNode, String methodExactName,  HashMap<String, IndexHits<NodeJSON>> methodNodesInClassNode)
	{
		long start = System.nanoTime(); 
		IndexHits<NodeJSON> methodCollection = new IndexHits<NodeJSON>();
		String className = (String) classNode.getProperty("id");
		if(methodNodesInClassNode.containsKey(classNode))
		{
			ArrayList<NodeJSON> methods = methodNodesInClassNode.get(className);
			for(NodeJSON method: methods)
			{
				if(((String)method.getProperty("exactName")).equals(methodExactName))
				{
					methodCollection.add(method);
				}	
			}
			logger.printIfCacheHit("cache hit methods in class ++");
		}
		else
		{
			String cypher = "START root=node({startName})MATCH (root)-[:HAS_METHOD]->(container)WHERE container.exactName = {exactName}RETURN container";
			JSONObject tempJSON = new JSONObject();
			tempJSON.put("startName", classNode.getNodeNumber());
			tempJSON.put("exactName", methodExactName);
			JSONObject json = new JSONObject();
			json.put("query", cypher);
			json.put("params", tempJSON);
			System.out.println(classNode.getNodeNumber() + " : " + methodExactName);
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
				JSONArray temptempArray = (JSONArray)tempArray.get(0);
				for(int i=0; i<temptempArray.length(); i++)
				{
					JSONObject obj = temptempArray.getJSONObject(i);
					NodeJSON nodejson = new NodeJSON(obj);
					methodCollection.add(nodejson);
				}
			}
			else
			{
				//System.out.println("$$ "+tempArray);
			}
			System.out.println(className + " . " + methodExactName + " : " + methodCollection.size());
			methodNodesInClassNode.put(className, methodCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), classNode.getProperty("id")+"."+methodExactName, end, start);
		return methodCollection;
	}

	
	public boolean checkIfClassHasMethod(NodeJSON classNode, String methodExactName)
	{
		String name = classNode.getProperty("id") + "." + methodExactName + "\\(*";
		name = escapeForLucene(name);
		IndexHits<NodeJSON> hits = methodIndex.query("id", name);
		if(hits.size()>0)
			return true;
		else
			return false;
	}



	/*Cypher*/
	public ArrayList<NodeJSON> getMethodParams(NodeJSON node, HashMap<NodeJSON, ArrayList<NodeJSON>> methodParameterCache, HashMap<String, NodeJSON> candidateClassNodesCache) 
	{
		long start = System.nanoTime();

		ArrayList<NodeJSON> paramNodesCollection = null;

		if(methodParameterCache.containsKey(node))
		{
			paramNodesCollection = methodParameterCache.get(node);
			logger.printIfCacheHit("Cache hit method parameters");
		}
		else
		{
			
			paramNodesCollection = new ArrayList<NodeJSON>();
			String cypher = "START root=node({startName})MATCH (root)-[:PARAMETER]->(param)RETURN param";
			JSONObject tempJSON = new JSONObject();
			tempJSON.put("startName", node.getNodeNumber());
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
			if(tempArray.length()>0)
			{
				JSONArray temptempArray = (JSONArray)tempArray.get(0);
				for(int i=0; i<temptempArray.length(); i++)
				{
					JSONObject obj = temptempArray.getJSONObject(i);
					NodeJSON nodejson = new NodeJSON(obj);
					paramNodesCollection.add(nodejson);
				}
			}
			else
			{
				//System.out.println("$$ "+tempArray);
			}
			methodParameterCache.put(node, paramNodesCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString(), end, start);
		return paramNodesCollection;
	}

	
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
			IndexHits<NodeJSON> candidateNodes = newParentsIndex.get(childId); 
			classElementCollection = new ArrayList<NodeJSON>();
			for(NodeJSON candidate : candidateNodes)
			{
				classElementCollection.add(candidate);
			}
			parentNodeCache.put(childId, classElementCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString(), end, start);
		return classElementCollection;
	}

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
			String outgoingrel = node.getJSONObject().getString("outgoing_relationships");
			JSONArray relationshipsArray = GraphServerAccess.queryURI(outgoingrel);
			for(int i=0; i<relationshipsArray.length(); i++)
			{
				JSONObject relationship = relationshipsArray.getJSONObject(i);
				if(relationship.get("type").equals("RETURN_TYPE"))
				{
					returnNode = new NodeJSON(GraphServerAccess.queryURI((String)relationship.get("end")).getJSONObject(0));
					methodReturnCache.put(node, returnNode);
					break;
				}
			}
		}
		long end = System.nanoTime();
		if(returnNode!=null)
			logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString() + " - " + returnNode.getProperty("id").toString(), end, start);
		return returnNode;
	}

	/*CYPHER*/
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
			String cypher = "START root=node({startName}) MATCH (root)-[:IS_METHOD]->(container) RETURN container LIMIT 1";
			JSONObject tempJSON = new JSONObject();
			tempJSON.put("startName", node.getNodeNumber());
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
	
	/*public NodeJSON getMethodContainer(NodeJSON node,HashMap<NodeJSON, NodeJSON> methodContainerCache) 
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
			String outgoingrel = node.getJSONObject().getString("outgoing_relationships");
			JSONArray relationshipsArray = GraphServerAccess.queryURI(outgoingrel);
			for(int i=0; i<relationshipsArray.length(); i++)
			{
				JSONObject relationship = relationshipsArray.getJSONObject(i);
				if(relationship.get("type").equals("IS_METHOD"))
				{
					containerNode = new NodeJSON(GraphServerAccess.queryURI((String)relationship.get("end")).getJSONObject(0));
					methodContainerCache.put(node, containerNode);
					break;
				}
			}
		}
		long end = System.nanoTime();
		if(containerNode!=null)
			logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString() + " - " + containerNode.getProperty("id").toString(), end, start);
		return containerNode;
	}*/
	

/*	public ArrayList<NodeJSON> getMethodNodesInClassNode1(NodeJSON classNode, String methodExactName,  HashMap<String, IndexHits<NodeJSON>> methodNodesInClassNode)
	{
		long start = System.nanoTime(); 
		IndexHits<NodeJSON> methodCollection = new IndexHits<NodeJSON>();
		IndexHits<NodeJSON> completeMethodCollection = new IndexHits<NodeJSON>();
		String className = (String) classNode.getProperty("id");
		if(methodNodesInClassNode.containsKey(classNode))
		{
			ArrayList<NodeJSON> methods = methodNodesInClassNode.get(className);
			for(NodeJSON method: methods)
			{
				if(((String)method.getProperty("exactName")).equals(methodExactName))
				{
					methodCollection.add(method);
				}	
			}
			logger.printIfCacheHit("cache hit methods in class ++");
		}
		else
		{
			IndexHits<NodeJSON> methods = allMethodsIndex.get(classNode.getProperty("id"));
			for(NodeJSON method: methods)
			{
				if(method!=null)
				{
					if(((String)method.getProperty("vis")).equals("PUBLIC")==true || ((String)method.getProperty("vis")).equals("NOTSET")==true)
					{
						completeMethodCollection.add(method);
						if(((String)method.getProperty("exactName")).equals(methodExactName))
						{
							methodCollection.add(method);
						}
					}
				}
			}
			methodNodesInClassNode.put(className, completeMethodCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), classNode.getProperty("id")+"."+methodExactName, end, start);
		return methodCollection;
	}*/
	
	/*	public ArrayList<NodeJSON> getMethodParams1(NodeJSON node, HashMap<NodeJSON, ArrayList<NodeJSON>> methodParameterCache, HashMap<String, NodeJSON> candidateClassNodesCache) 
	{
		long start = System.nanoTime();

		ArrayList<NodeJSON> paramNodesCollection = null;

		if(methodParameterCache.containsKey(node))
		{
			paramNodesCollection = methodParameterCache.get(node);
			logger.printIfCacheHit("Cache hit method parameters");
		}
		else
		{
			paramNodesCollection = new ArrayList<NodeJSON>();
			
			String mname = (String) node.getProperty("id");
			mname = mname.substring(0, mname.length()-1);
			if(mname.lastIndexOf("(") != mname.length()-1)
			{	
				String[] array = mname.split("\\(");
				String paramList = array[array.length-1];
				//System.out.println(paramList + "--");
				if(!paramList.trim().equals(null))
				{	
					String[] params = paramList.split(",");
					for(String param : params)
					{
						NodeJSON parameter = null;
						if(candidateClassNodesCache.containsKey(param))
							parameter = candidateClassNodesCache.get(param);
						else
						{
							parameter = allClassIndex.get(param.trim()).getSingle();
							candidateClassNodesCache.put(param, parameter);
						}
						paramNodesCollection.add(parameter);
					}
				}
			}
			methodParameterCache.put(node, paramNodesCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString(), end, start);
		return paramNodesCollection;
	}*/
	
	
	/*public IndexHits<NodeJSON> getCandidateClassNodes(String className, HashMap<String, IndexHits<NodeJSON>> candidateNodesCache) 
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
			IndexHits<NodeJSON> candidateNodes = shortClassIndex.get(className.replace(".", "$"));
			candidateClassCollection = new IndexHits<NodeJSON>();
			for(NodeJSON candidate : candidateNodes)
			{
				if(candidate!=null)
					if(((String)candidate.getProperty("vis")).equals("PUBLIC")==true || ((String)candidate.getProperty("vis")).equals("NOTSET")==true)
						candidateClassCollection.add(candidate);
			}
			System.out.println(className + " : " +candidateClassCollection.size());
			candidateNodesCache.put(className, candidateClassCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), className, end, start);
		return candidateClassCollection;
	}*/
}