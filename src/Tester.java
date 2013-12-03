import java.io.IOException;
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
	public static String getClassId(String id)	//to store class name and exact method name as ivars
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
			System.out.println("here0");
			try
			{
				graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
			}
			catch(Exception e)
			{
				System.out.println("Database Locked");
				//e.printStackTrace();
			}
			System.out.println("here1");
			classIndex = graphDb.index().forNodes("classes");
			methodIndex = graphDb.index().forNodes("methods");
			fieldIndex = graphDb.index().forNodes("fields");
			
			
			
			shortClassIndex = graphDb.index().forNodes("short_classes");
			shortMethodIndex = graphDb.index().forNodes("short_methods");
			shortFieldIndex = graphDb.index().forNodes("short_fields");
			parentIndex = graphDb.index().forNodes("parents");
			allParentsNodeStringIndex = graphDb.index().forNodes("allParentsString");
			
			/*IndexHits<Node> hits = shortClassIndex.get("short_name", "HashMap");
			for(Node hit : hits)
			{
				System.out.println(hit.getProperty("id"));
				HashSet<Node> methods = getMethodNodes(hit);
				for(Node method : methods)
				{
					System.out.println("-- "+method.getProperty("id"));
				}
			}
			System.out.println(hits.size());*/
			IndexHits<Node> methods = shortClassIndex.get("short_name", "Chronometer");
			HashSet<String> test = new HashSet<String>();
			for(Node method : methods)
			{
				String s = (String)method.getProperty("id");
				test.add(s);
				System.out.println(s);
			}
			System.out.println(methods.size());
			System.out.println(test.size());
		registerShutdownHook();

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
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
