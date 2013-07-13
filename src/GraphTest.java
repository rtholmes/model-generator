import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;

import ca.uwaterloo.cs.se.inconsistency.core.model2.ClassElement;
import ca.uwaterloo.cs.se.inconsistency.core.model2.FieldElement;
import ca.uwaterloo.cs.se.inconsistency.core.model2.MethodElement;

public class GraphTest
{
	private static GraphDatabaseService graphDb;
	private static final String DB_PATH = "neo4j-store";
	
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
	
	public static void main(String[] args)
	{
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
		Index<Node> index= graphDb.index().forNodes("classes");
		Index<Node> smindex= graphDb.index().forNodes("short_methods");
		registerShutdownHook();
		
		Transaction tx2 = graphDb.beginTx();
		try
		{
			//###################################################
			System.out.println("searching.....");
			String idToFind = "java.io.BufferedWriter";
			Node foundUser = index.get( "id", idToFind ).getSingle();
			
			String output = foundUser.getProperty("id") + "'s parents:\n";
			Traverser ParentTraverser = getParents( foundUser );
			int numberOfSuperTypes=0;
			for ( Path pathToParent : ParentTraverser )
			{
				output += "At depth " + pathToParent.length() + " => "
						+ pathToParent.endNode().getProperty( "id" ) + "\n";
				numberOfSuperTypes++;
			}
			output += "Number of friends found: " + numberOfSuperTypes + "\n";
			System.out.println(output);
			System.out.println( "The vis of node " + idToFind + " is "	+  foundUser.hasRelationship(RelTypes.PARENT));
			

			output = null;
			output = foundUser.getProperty("id") + "'s methods:\n";
			ParentTraverser = getMethods( foundUser );
			numberOfSuperTypes=0;
			for ( Path mehods : ParentTraverser )
			{
				output += "At depth " + mehods.length() + " => "
						+ mehods.endNode().getProperty( "id" ) + "\n";
				numberOfSuperTypes++;
			}
			output += "Number of methods found: " + numberOfSuperTypes + "\n";
			System.out.println(output);

			output = null;
			output = foundUser.getProperty("id") + "'s fields:\n";
			ParentTraverser = getFields( foundUser );
			numberOfSuperTypes=0;
			for ( Path fields : ParentTraverser )
			{
				output += "At depth " + fields.length() + " => "
						+ fields.endNode().getProperty( "id" ) + "\n";
				numberOfSuperTypes++;
			}
			output += "Number of fields found: " + numberOfSuperTypes + "\n";
			System.out.println(output);

			//###################################################
			System.out.println("**************************");
			IndexHits<Node> iter = smindex.get("short_name", "close");
			for(Node temp: iter)
			{
				Iterable<Relationship> parent = temp.getRelationships(RelTypes.PARENT);
				
				Traverser traverser = getParents(temp);
				for ( Path node : traverser )
				{
					output += "At depth " + node.length() + " => "
							+ node.endNode().getProperty( "id" ) + "\n";
					numberOfSuperTypes++;
				}
				System.out.println(temp.getProperty("id"));
				
			}
			System.out.println(iter.size()+" methods found");
			//###################################################
			tx2.success();
		}
		finally
		{
			tx2.finish();
		}

		shutdown();

	}
	private static void shutdown()
	{
		graphDb.shutdown();
	}
	private static Traverser getParents(final Node node )
	{
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.PARENT, Direction.OUTGOING )
				.evaluator( Evaluators.excludeStartPosition() );
		return td.traverse( node );
	}

	private static Traverser getMethods(final Node node )
	{
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.HAS_METHOD, Direction.OUTGOING )
				.evaluator( Evaluators.excludeStartPosition() );
		return td.traverse( node );
	}

	private static Traverser getFields(final Node node )
	{
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.HAS_FIELD, Direction.OUTGOING )
				.evaluator( Evaluators.excludeStartPosition() );
		return td.traverse( node );
	}
	private static void registerShutdownHook()
	{
		// Registers a shutdown hook for the Neo4j and index service instances
		// so that it shuts down nicely when the VM exits (even if you
		// "Ctrl-C" the running example before it's completed)
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				shutdown();
			}
		} );
	}
	
	private ClassElement getCE(Node node)
	{
		
		return null;
	}
	
	private MethodElement getME(Node node)
	{
		
		
		return null;
	}
	private FieldElement getFE(Node node)
	{
		
		
		return null;
	}
}