import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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
import org.neo4j.kernel.Traversal;






public class Tester
{
	private static GraphDatabaseService graphDb;
	private static final String DB_PATH = "maven-graph-database";
	public static Index<Node> classIndex ;
	public static Index<Node> methodIndex ;
	public static Index<Node> fieldIndex ;

	public static Index<Node> shortClassIndex ;
	public static Index<Node> shortMethodIndex ;
	public static Index<Node> shortFieldIndex ;
	public static Index<Node> parentIndex ;
	public static Index<Node> allParentsNodeStringIndex;
	public static Index<Node> allMethodsIndex;
	private static Index<Node> newParentsIndex;
	
	public static String getClassId(String id)
	{
		String _className = null;
		if (id.endsWith("<clinit>"))
		{
			String array[] = id.split(".<clinit>");
			_className = array[0];		
		}
		else
		{
			String[] array = id.split("\\(");
			array = array[0].split("\\.");
			String className = array[0];		
			for (int i=1; i<array.length-1; i++) className += "." + array[i];
			_className = className;
		}
		return _className;
	}
	
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

	public static void main(String[] args) throws IOException
	{
		try
		{
			try
			{
				graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
			}
			catch(Exception e)
			{
				System.out.println("Database Locked");
			}
			classIndex = graphDb.index().forNodes("classes");
			methodIndex = graphDb.index().forNodes("methods");
			fieldIndex = graphDb.index().forNodes("fields");



			shortClassIndex = graphDb.index().forNodes("short_classes");
			shortMethodIndex = graphDb.index().forNodes("short_methods");
			shortFieldIndex = graphDb.index().forNodes("short_fields");
			parentIndex = graphDb.index().forNodes("parents");
			allParentsNodeStringIndex = graphDb.index().forNodes("allParentsString");
			allMethodsIndex = graphDb.index().forNodes("allMethodsIndex");
			
			/*newParentsIndex = graphDb.index().forNodes("parentNodes");
			System.out.println(newParentsIndex.query("childId", "*").size());

			Node tempNode = classIndex.get("id", "java.awt.List$AccessibleAWTList$AccessibleAWTListChild").getSingle();
			System.out.println(tempNode.getProperty("exactName"));
			ArrayList<Node> methods2 = getParents(tempNode, new HashMap<String, ArrayList<Node>>()); 
			for(Node method: methods2)
			{
				System.out.println((String)method.getProperty("id") + " : " + method.getId());
			}*/
			
			IndexHits<Node> test = shortClassIndex.get("short_name", "TaggedComponent");
			for(Node n : test)
			{
				System.out.println(n.getProperty("id"));
			}
			registerShutdownHook();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static ArrayList<Node> getParentsOld(final Node node, HashMap<String, ArrayList<Node>> parentNodeCache )
	{
		String childId = (String) node.getProperty("id");
		ArrayList<Node> classElementCollection = null;
		if(parentNodeCache.containsKey(childId))
		{
			classElementCollection = parentNodeCache.get(childId);
		}
		else
		{
			IndexHits<Node> candidateNodes = allParentsNodeStringIndex.get("parent", childId);
			System.out.println(candidateNodes.size());
			classElementCollection = new ArrayList<Node>();
			for(Node candidate : candidateNodes)
			{
				classElementCollection.add(candidate);
			}
			Node object = classIndex.get("id", "java.lang.Object").getSingle();
			classElementCollection.add(object);
			parentNodeCache.put(childId, classElementCollection);
		}
		
		return classElementCollection;
	}
	
	public static ArrayList<Node> getParents(final Node node, HashMap<String, ArrayList<Node>> parentNodeCache )
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
		return classElementCollection;
	}
	
	public static ArrayList<String> getClassParentNodes(Node node)
	{
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.PARENT, Direction.OUTGOING )
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
	
	public static boolean checkIfClassHasMethod(Node classNode, String methodExactName)
	{
		String name = classNode.getProperty("id") + methodExactName + "*";
		IndexHits<Node> hits = methodIndex.get("id", name);
		for(Node node : hits)
		{
			System.out.println(node.getProperty("id"));
		}
		return false;
	}

	public static Node getMethodContainer(Node node)
	{
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.IS_METHOD, Direction.OUTGOING )
				.evaluator( Evaluators.excludeStartPosition() );
		Traverser traverser = td.traverse( node );
		Node container = null;
		for ( Path containerNode : traverser )
		{
			if(containerNode.length()==1)
			{
				if(containerNode.endNode()!=null)
				{
					container = containerNode.endNode();
					break;
				}
			}
			else
				break;
		}
		return container;
	}

	private static HashSet<Node> getMethodNodes(Node node)
	{
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.HAS_METHOD, Direction.OUTGOING )
				.evaluator(Evaluators.excludeStartPosition());
		Traverser methodTraverser = td.traverse( node );
		HashSet<Node> methodsCollection = new HashSet<Node>();;
		for ( Path methods : methodTraverser )
		{
			if(methods.length()==1)
			{
				methodsCollection.add(methods.endNode());
			}
			else if(methods.length()>=1)
			{
				break;
			}
		}
		return methodsCollection;
	}

	public static Node getMethodReturn(Node node)
	{
		long start = System.nanoTime(); 
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.RETURN_TYPE, Direction.OUTGOING )
				.evaluator( Evaluators.excludeStartPosition() );
		Traverser traverser = td.traverse( node );
		Node container = null;
		for ( Path containerNode : traverser )
		{
			if(containerNode.length()==1)
			{
				container = containerNode.endNode();
				System.out.println(container.getProperty("id"));
				break;
			}
			else
				break;
		}
		long end = System.nanoTime();
		System.out.println("getMethodReturn" + " - " + node.getProperty("id") + " : " + String.valueOf((double)(end-start)/(1000000000)));
		return container;
	}
	public static ArrayList<Node> getAllParents(String className, HashSet<String> parentNames )
	{
		IndexHits<Node> candidateNodes = parentIndex.get("parent", className);
		ArrayList<Node> classElementCollection = new ArrayList<Node>();
		for(Node candidate : candidateNodes)
		{
			if(((String)candidate.getProperty("vis")).equals("PUBLIC")==true || ((String)candidate.getProperty("vis")).equals("NOTSET")==true)
			{
				String _cid = (String) candidate.getProperty("id");
				if(parentNames.contains(_cid) == false)
				{
					parentNames.add(_cid);
					classElementCollection.add(candidate);
					classElementCollection.addAll(getAllParents(_cid, parentNames));
				}
			}
		}
		return classElementCollection;
	}
	private static void shutdown()
	{
		graphDb.shutdown();
	}

	private static void registerShutdownHook()
	{
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			public void run()
			{
				shutdown();
			}
		} );
	}
}
