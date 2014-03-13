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
		
		methodIndex = new NodeIndex(DB_URI + "/index/node/methods/id");
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
			candidateNodesCache.put(className, candidateClassCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), className, end, start);
		return candidateClassCollection;
	}

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
			candidateMethodNodesCache.put(methodName, candidateMethodNodes);
		
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), methodName, end, start);
		return candidateMethodNodes;
	}

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
			if(tempArray.length()>0)
			{
				for(int i=0; i<tempArray.length(); i++)
				{
					JSONArray temptempArray = tempArray.getJSONArray(i);
					JSONObject classObj = temptempArray.getJSONObject(0);
					JSONObject methodObj = temptempArray.getJSONObject(1);
					JSONObject returnObj = temptempArray.getJSONObject(2);
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
			}
			shortClassShortMethodCache.put(classExactName+'.'+methodExactName, classMethodCollection);
			
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), classExactName+'.'+methodExactName, end, start);
		return classMethodCollection;
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
			String cypher = "START root=node({startName})MATCH (root)-[:HAS_METHOD]->(method)WHERE method.exactName = {exactName}RETURN method";
			JSONObject tempJSON = new JSONObject();
			tempJSON.put("startName", classNode.getNodeNumber());
			tempJSON.put("exactName", methodExactName);
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
					methodCollection.add(nodejson);
				}
			}
			else
			{
				//System.out.println("$$ "+tempArray);
			}
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
					JSONArray obj = tempArray.getJSONArray(i);
					JSONObject toInsert = obj.getJSONObject(0);
					NodeJSON nodejson = new NodeJSON(toInsert);
					classElementCollection.add(nodejson);
				}
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
			/*
			String cypher = "START root=node({startName}) MATCH (root)-[:RETURN_TYPE]->(container) RETURN container LIMIT 1";
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
			returnNode = new NodeJSON(temptempArray.getJSONObject(0));
			methodReturnCache.put(node, returnNode);
			*/
			///*
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
			//*/
		}
		long end = System.nanoTime();
		if(returnNode != null)
			logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString() + " - " + returnNode.getProperty("id").toString(), end, start);
		return returnNode;
	}

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

	public IndexHits<NodeJSON> getAllMethodsInClass(NodeJSON parentNode, HashMap<String, IndexHits<NodeJSON>> allMethodsInClass) 
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
			String cypher = "START root=node({startName})MATCH (root)-[:HAS_METHOD]->(method) RETURN method";
			JSONObject tempJSON = new JSONObject();
			tempJSON.put("startName", parentNode.getNodeNumber());
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
					methodCollection.add(nodejson);
				}
			}
			else
			{
				//System.out.println("$$ "+tempArray);
			}
			allMethodsInClass.put(className, methodCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), className + "."  , end, start);
		return methodCollection;
	}
	
}