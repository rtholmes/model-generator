import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.StoreLockException;
import org.neo4j.kernel.Traversal;



public class GraphDatabase
{
	private static GraphDatabaseService graphDb;
	private static String DB_PATH;

	private Index<Node> methodIndex ;

	private Index<Node> allClassIndex;
	private Index<Node> shortClassIndex ;
	private Index<Node> shortMethodIndex ;
	private Index<Node> allParentsNodeIndex;
	private Index<Node> allMethodsIndex;
	
	private Index<Node> newParentsIndex;
	public Logger logger = new Logger();

	private ClusterEliminator cEliminator;
	
	private static enum RelTypes implements RelationshipType
	{
		PARENT,
		CHILD,		
		IS_METHOD, 
		HAS_METHOD,
		IS_FIELD,
		HAS_FIELD,
		RETURN_TYPE, 
		IS_RETURN_TYPE, 
		PARAMETER, 
		IS_PARAMETER, 
		IS_FIELD_TYPE,
		HAS_FIELD_TYPE
	}

	public GraphDatabase(String input_oracle) throws StoreLockException, IOException
	{
		DB_PATH = input_oracle;
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
		
		cEliminator = new ClusterEliminator("class-collisions_update.txt", "forReid.txt");
		
		logger.disableAccessTimes();
		logger.disableCacheHit();
		
		allClassIndex = graphDb.index().forNodes("classes");
		methodIndex = graphDb.index().forNodes("methods");
		shortClassIndex = graphDb.index().forNodes("short_classes");
		shortMethodIndex = graphDb.index().forNodes("short_methods");
		allParentsNodeIndex = graphDb.index().forNodes("allParentsString");
		allMethodsIndex = graphDb.index().forNodes("allMethodsIndex");
		
		newParentsIndex = graphDb.index().forNodes("parentNodes");
		
		/*//((LuceneIndex<Node>) classIndex).setCacheCapacity( "classes", 100000000 );
			//((LuceneIndex<Node>) methodIndex).setCacheCapacity( "methods", 100000000 );
			//((LuceneIndex<Node>) fieldIndex).setCacheCapacity( "fields", 1000000 );
			((LuceneIndex<Node>) shortClassIndex).setCacheCapacity( "short_fields", 500000000 );
			((LuceneIndex<Node>) shortMethodIndex).setCacheCapacity( "short_methods", 500000000 );
			//((LuceneIndex<Node>) shortFieldIndex).setCacheCapacity( "short_classes", 1000000 );
			((LuceneIndex<Node>) parentIndex).setCacheCapacity( "parents", 500000000);*/
	}

	private String getCurrentMethodName()
	{
		StackTraceElement stackTraceElements[] = (new Throwable()).getStackTrace();
		return stackTraceElements[1].toString();
	}

	public ArrayList<Node> getCandidateClassNodes(String className, HashMap<String, ArrayList<Node>> candidateNodesCache) 
	{
		long start = System.nanoTime();
		ArrayList<Node> candidateClassCollection = null;
		if(candidateNodesCache.containsKey(className))
		{
			candidateClassCollection = candidateNodesCache.get(className);
			logger.printIfCacheHit("cache hit class!");
		}
		else
		{
			IndexHits<Node> candidateNodes = shortClassIndex.get("short_name", className.replace(".", "$"));
			candidateClassCollection = new ArrayList<Node>();
			for(Node candidate : candidateNodes)
			{
				if(candidate!=null)
					if(((String)candidate.getProperty("vis")).equals("PUBLIC")==true || ((String)candidate.getProperty("vis")).equals("NOTSET")==true)
						candidateClassCollection.add(candidate);
			}
			candidateNodesCache.put(className, candidateClassCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), className, end, start);
		return candidateClassCollection;
	}

	public ArrayList<Node> getCandidateMethodNodes(String methodName, HashMap<String, ArrayList<Node>> candidateMethodNodesCache) 
	{
		long start = System.nanoTime(); 
		ArrayList<Node> candidateMethodNodes = null;
		if(candidateMethodNodesCache.containsKey(methodName))
		{
			candidateMethodNodes = candidateMethodNodesCache.get(methodName);
			logger.printIfCacheHit("cache hit method!");
		}
		else
		{
			IndexHits<Node> candidateNodes = shortMethodIndex.get("short_name", methodName);
			candidateMethodNodes = new ArrayList<Node>();
			for(Node candidate : candidateNodes)
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

	public Node returnRightNodeIfCluster(Set<Node> set)
	{
		if(set.isEmpty())
			return null;
		if(cEliminator.checkIfCluster(set))
		{
			Node rightClass = cEliminator.findRightClass(set);
			return rightClass;
		}
		else
		{
			//check if set has the originalClass from the hashmap. Maybe that is the right ans and the cluster set
			//I created has some missing entities.
			Node rightClass = cEliminator.findRightClass(set);
			//return rightClass;
		}
		return null;
	}

	public boolean checkIfParentNode(Node parentNode, String childId, HashMap<String, ArrayList<Node>> parentNodeCache)
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
			ArrayList<Node>parents = parentNodeCache.get(childId);
			for(Node parent : parents)
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
			IndexHits<Node> candidateNodes = newParentsIndex.get("childId", childId);
			ArrayList<Node> parentList = new ArrayList<Node>();
			for(Node candidate : candidateNodes)
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

	@Deprecated
	public ArrayList<String> getClassChildrenNodes(Node node)
	{
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.CHILD, Direction.OUTGOING )
				.evaluator( Evaluators.excludeStartPosition() );
		Traverser childTraverser = td.traverse( node );
		ArrayList<String> childCollection = new ArrayList<String>();;
		for ( Path child : childTraverser )
		{
			if(child.endNode()!=null)
				childCollection.add((String) child.endNode().getProperty("id"));
		}
		return childCollection;
	}

	public ArrayList<Node> getMethodNodes(Node node, HashMap<String, ArrayList<Node>> methodNodesInClassNode)
	{
		long start = System.nanoTime(); 
		ArrayList<Node> methodsCollection = null;
		String className = (String) node.getProperty("id");
		if(methodNodesInClassNode.containsKey(node))
		{
			methodsCollection = methodNodesInClassNode.get(className);
			logger.printIfCacheHit("Cache hit methods in class");
		}
		else
		{
			IndexHits<Node> methods = allMethodsIndex.get("classId", className);
			methodsCollection = new ArrayList<Node>();
			for(Node method : methods)
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

	@Deprecated
	public IndexHits<Node> getMethodNodesInClassNodeOld (Node classNode, String methodExactName)
	{
		long start = System.nanoTime(); 
		String name = classNode.getProperty("id") + "." + methodExactName + "\\(*";
		name = escapeForLucene(name);
		IndexHits<Node> hits = methodIndex.query("id", name);
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), classNode.getProperty("id")+"."+methodExactName, end, start);
		return hits;
	}

	public ArrayList<Node> getMethodNodesInClassNode (Node classNode, String methodExactName,  HashMap<String, ArrayList<Node>> methodNodesInClassNode)
	{
		long start = System.nanoTime(); 
		ArrayList<Node> methodCollection = new ArrayList<Node>();
		ArrayList<Node> completeMethodCollection = new ArrayList<Node>();
		String className = (String) classNode.getProperty("id");
		if(methodNodesInClassNode.containsKey(classNode))
		{
			ArrayList<Node> methods = methodNodesInClassNode.get(className);
			for(Node method: methods)
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
			IndexHits<Node> methods = allMethodsIndex.get("classId", classNode.getProperty("id"));
			for(Node method: methods)
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
	}

	public boolean checkIfClassHasMethod(Node classNode, String methodExactName)
	{
		String name = classNode.getProperty("id") + "." + methodExactName + "\\(*";
		name = escapeForLucene(name);
		IndexHits<Node> hits = methodIndex.query("id", name);
		if(hits.size()>0)
			return true;
		else
			return false;
	}

	public Node getMethodContainer(Node node, HashMap<Node, Node> methodContainerCache)
	{
		long start = System.nanoTime(); 
		Node container = null;
		if(methodContainerCache.containsKey(node))
		{
			container = methodContainerCache.get(node);
			logger.printIfCacheHit("Cache hit method container");
		}
		else
		{
			TraversalDescription td = Traversal.description()
					.breadthFirst()
					.relationships( RelTypes.IS_METHOD, Direction.OUTGOING )
					.evaluator( Evaluators.excludeStartPosition() );
			Traverser paths = td.traverse( node );
			for ( Path path : paths )
			{
				if(path.length() == 1)
				{
					if(path.endNode()!=null)
					{
						container = path.endNode();
						methodContainerCache.put(node, container);
						break;
					}
				}
				else
					break;
			}
		}

		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString(), end, start);
		return container;
	}

	public Node getMethodReturn(Node node, HashMap<Node, Node> methodReturnCache)
	{
		long start = System.nanoTime(); 
		Node returnNode = null;
		if(methodReturnCache.containsKey(node))
		{
			returnNode = methodReturnCache.get(node);
			logger.printIfCacheHit("Cache hit method return");
		}
		else
		{
			TraversalDescription td = Traversal.description()
					.breadthFirst()
					.relationships( RelTypes.RETURN_TYPE, Direction.OUTGOING )
					.evaluator( Evaluators.excludeStartPosition() );
			Traverser traverser = td.traverse( node );
			for ( Path containerNode : traverser )
			{
				if(containerNode.length()==1)
				{
					returnNode = containerNode.endNode();
					break;
				}
				else
					break;
			}
		}
		long end = System.nanoTime();
		if(returnNode!=null)
			logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString() + " - " + returnNode.getProperty("id").toString(), end, start);
		return returnNode;
	}

	public ArrayList<Node> getMethodParams(Node node, HashMap<Node, ArrayList<Node>> methodParameterCache) 
	{
		long start = System.nanoTime();

		ArrayList<Node> paramNodesCollection = null;

		if(methodParameterCache.containsKey(node))
		{
			paramNodesCollection = methodParameterCache.get(node);
			logger.printIfCacheHit("Cache hit method parameters");
		}
		else
		{
			paramNodesCollection = new ArrayList<Node>();
			
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
						Node parameter = allClassIndex.get("id", param.trim()).getSingle();
						paramNodesCollection.add(parameter);
					}
				}
			}
			methodParameterCache.put(node, paramNodesCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString(), end, start);
		return paramNodesCollection;
	}

	void shutdown()
	{
		graphDb.shutdown();
	}


	@Deprecated
	public Node getUltimateParent(final Node node )
	{
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.PARENT, Direction.OUTGOING )
				.evaluator( Evaluators.excludeStartPosition() );
		Traverser ultimateParent =  td.traverse( node );
		Node answer = null;
		for ( Path paramNode : ultimateParent )
		{
			if(paramNode.length()==1)
			{
				answer = paramNode.endNode();
			}
		}
		return answer;
	}

	public ArrayList<Node> getParentsOld(final Node node, HashMap<String, ArrayList<Node>> parentNodeCache )
	{
		long start = System.nanoTime(); 
		String childId = (String) node.getProperty("id");
		ArrayList<Node> classElementCollection = null;
		if(parentNodeCache.containsKey(childId))
		{
			classElementCollection = parentNodeCache.get(childId);
		}
		else
		{
			IndexHits<Node> candidateNodes = allParentsNodeIndex.get("parent", childId);
			classElementCollection = new ArrayList<Node>();
			for(Node candidate : candidateNodes)
			{
				classElementCollection.add(candidate);
			}
			Node object = allClassIndex.get("id", "java.lang.Object").getSingle();
			classElementCollection.add(object);
			parentNodeCache.put(childId, classElementCollection);
		}
		
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString(), end, start);
		return classElementCollection;
	}
	
	public ArrayList<Node> getParents(final Node node, HashMap<String, ArrayList<Node>> parentNodeCache )
	{
		long start = System.nanoTime(); 
		String childId = (String) node.getProperty("id");
		ArrayList<Node> classElementCollection = null;
		if(parentNodeCache.containsKey(childId))
		{
			classElementCollection = parentNodeCache.get(childId);
		}
		else
		{
			IndexHits<Node> candidateNodes = newParentsIndex.get("childId", childId); 
			classElementCollection = new ArrayList<Node>();
			for(Node candidate : candidateNodes)
			{
				classElementCollection.add(candidate);
			}
			parentNodeCache.put(childId, classElementCollection);
		}
		long end = System.nanoTime();
		logger.printAccessTime(getCurrentMethodName(), node.getProperty("id").toString(), end, start);
		return classElementCollection;
	}
	
	
	

}