import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;



public class ReIndex
{
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
	
	private static final String DB_PATH = "maven-graph-database";
	private static GraphDatabaseService graphDb;
	private static Index<Node> nodeParents;
	private static Index<Node> allMethodsIndex;
	private static Index<Node> shortClassIndex;
	private static Index<Node> allParentsNodeIndex;
	
	public static void main(String[] args)
	{
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );

		allMethodsIndex = graphDb.index().forNodes("allMethodsIndex");
		shortClassIndex = graphDb.index().forNodes("short_classes");
		allParentsNodeIndex = graphDb.index().forNodes("allParentsString");
		registerShutdownHook();
		int count = 0;

		IndexHits<Node> classes = shortClassIndex.query("short_name", "*");
		
		IndexHits<Node> test = allMethodsIndex.query("classId", "*");
		System.out.println(test.size());
		
		for(Node classnode : classes)
		{
			System.out.println(count++);
			if(count > 0)
			{
				String classId = (String) classnode.getProperty("id");
				ArrayList<Node> methods = getMethodNodes(classnode);
				for(Node method : methods)
				{
					Transaction tx0 = graphDb.beginTx();
					try
					{
						allMethodsIndex.putIfAbsent(method, "classId", classId);
						tx0.success();
					}
					finally
					{
						tx0.finish();
					}
				}
				System.out.println(classId + " : " + methods);
			}
		}
	}

	public static ArrayList<Node> getMethodNodes(Node node)
	{
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.HAS_METHOD, Direction.OUTGOING )
				.evaluator( Evaluators.excludeStartPosition() );
		Traverser methodTraverser = td.traverse( node );
		ArrayList<Node> methodsCollection = new ArrayList<Node>();;
		for ( Path methods : methodTraverser )
		{
			if(methods.length()==1)
			{
				if(methods.endNode()!=null)
					methodsCollection.add(methods.endNode());
			}
			else
				break;
		}
		return methodsCollection;
	}



	private static ArrayList<String> getAllChildren(Node parent) 
	{
		HashSet<String> visited = new HashSet<String>();
		ArrayList<String> childNodes = new ArrayList<String>();

		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.CHILD, Direction.OUTGOING )
				.evaluator( Evaluators.excludeStartPosition() );
		Traverser childTraverser = td.traverse( parent );
		for ( Path child : childTraverser )
		{
			if(child.endNode()!=null && visited.contains((String) child.endNode().getProperty("id"))==false)
			{
				String childId = (String) child.endNode().getProperty("id");
				visited.add(childId);
				childNodes.add(childId);
			}
		}
		return childNodes;
	}


	public static HashMap<String, ArrayList<Node>> cache = new HashMap<String, ArrayList<Node>>();
	public static ArrayList<Node> getAllParents(String className, HashSet<String> parentNames )
	{
		if(cache.containsKey(className))
			return cache.get(className);
		IndexHits<Node> candidateNodes = nodeParents.get("parent", className);
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
		cache.put(className, classElementCollection);
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