import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

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

import ca.uwaterloo.cs.se.inconsistency.core.model2.ClassElement;
import ca.uwaterloo.cs.se.inconsistency.core.model2.FieldElement;
import ca.uwaterloo.cs.se.inconsistency.core.model2.MethodElement;
import ca.uwaterloo.cs.se.inconsistency.core.model2.MethodParamElement;
import ca.uwaterloo.cs.se.inconsistency.core.model2.MethodReturnElement;

public class GraphTest
{
	private static GraphDatabaseService graphDb;
	private static final String DB_PATH = "neo4j-store-new_indices";
	public static Index<Node> classIndex ;
	public static Index<Node> methodIndex ;
	public static Index<Node> fieldIndex ;
	
	public static Index<Node> shortClassIndex ;
	public static Index<Node> shortMethodIndex ;
	public static Index<Node> shortFieldIndex ;
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
	
/*	public ClassElement convertByteArrayToClassElement(byte[] yourBytes) throws IOException, ClassNotFoundException
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(yourBytes);
		ObjectInput in = null;
		ClassElement ce = null;
		try 
		{
		  in = new ObjectInputStream(bis);
		  ce = (ClassElement) in.readObject(); 
		} 
		finally 
		{
		  bis.close();
		  in.close();
		}
		return ce;
	}
	
	public FieldElement convertByteArrayToFieldElement(byte[] yourBytes) throws IOException, ClassNotFoundException
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(yourBytes);
		ObjectInput in = null;
		FieldElement fe = null;
		try 
		{
		  in = new ObjectInputStream(bis);
		  fe = (FieldElement) in.readObject(); 
		} 
		finally 
		{
		  bis.close();
		  in.close();
		}
		return fe;
	}
	
	public MethodElement convertByteArrayToMethodElement(byte[] yourBytes) throws IOException, ClassNotFoundException
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(yourBytes);
		ObjectInput in = null;
		MethodElement me = null;
		try 
		{
		  in = new ObjectInputStream(bis);
		  me = (MethodElement) in.readObject(); 
		} 
		finally 
		{
		  bis.close();
		  in.close();
		}
		return me;
	}
	
*/	
	public static void main(String[] args)
	{
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
		classIndex = graphDb.index().forNodes("classes");
		methodIndex = graphDb.index().forNodes("methods");
		fieldIndex = graphDb.index().forNodes("fields");
		
		shortClassIndex = graphDb.index().forNodes("short_classess");
		shortMethodIndex = graphDb.index().forNodes("short_methods");
		shortFieldIndex = graphDb.index().forNodes("short_fields");
		registerShutdownHook();
		
		Transaction tx2 = graphDb.beginTx();
		try
		{
			System.out.println("searching.....");
			String idToFind = "java.lang.Integer";
			Node foundUser = classIndex.get( "id", idToFind ).getSingle();
			
			String output = foundUser.getProperty("id") + "'s parents:\n";
			Traverser ParentTraverser = getParents( foundUser );
			int numberOfSuperTypes=0;
			for ( Path pathToParent : ParentTraverser )
			{
				output += "At depth " + pathToParent.length() + " => "+ pathToParent.endNode().getProperty( "id" ) + "\n";
				numberOfSuperTypes++;
			}
			output += "Number of friends found: " + numberOfSuperTypes + "\n";
			System.out.println(output);

			output = null;
			output = foundUser.getProperty("id") + "'s methods:\n";
			ParentTraverser = getMethods( foundUser );
			numberOfSuperTypes=0;
			for ( Path methods : ParentTraverser )
			{
				output += "At depth " + methods.length() + " => "+ methods.endNode().getProperty( "id" ) + "\n";
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
				output += "At depth " + fields.length() + " => "+ fields.endNode().getProperty( "id" ) + "\n";
				numberOfSuperTypes++;
			}
			output += "Number of fields found: " + numberOfSuperTypes + "\n";
			System.out.println(output);

			System.out.println("**************************");
			
			IndexHits<Node> iter = shortClassIndex.get("short_name", "String");
			for(Node temp: iter)
			{
				System.out.println(temp.getProperty("id"));
				Traverser traverser = getParentClass(temp);
				for ( Path node : traverser )
				{
					output = "At depth " + node.length() + " => "+ node.endNode().getProperty( "id" ) + "\n";
					numberOfSuperTypes++;
					System.out.println(output);
				}
			}
			System.out.println(iter.size()+" methods found");
			
			
			tx2.success();
		}
		finally
		{
			tx2.finish();
		}

		shutdown();
	}
	
	public static Collection<ClassElement> getCandidateClasses(String className) 
	{
		IndexHits<Node> candidateNodes = shortClassIndex.get("short_name", className);
		Collection<ClassElement> classElementCollection = new HashSet<ClassElement>();
		for(Node candidate : candidateNodes)
		{
			ClassElement ce = getClassElementFromNode(candidate);
			classElementCollection.add(ce);
		}
		return classElementCollection;
	}
	
	public static Collection<MethodElement> getCandidateMethods(String methodName) 
	{
		IndexHits<Node> candidateNodes = shortMethodIndex.get("short_name", methodName);
		Collection<MethodElement> methodElementCollection = new HashSet<MethodElement>();
		for(Node candidate : candidateNodes)
		{
			MethodElement me = getMethodElementFromNode(candidate);
			methodElementCollection.add(me);
		}
		return methodElementCollection;
	}
	
	public static ClassElement getClassElementForMethod(String id) 
	{
		Node node = methodIndex.get("id", id).getSingle();
		Node containerNode = getMethodContainer(node);
		ClassElement container = getClassElementFromNode(containerNode);
		return container;
	}
	
	private static ClassElement getClassElementFromNode(Node node)
	{
		System.out.println(node.getProperty("id"));
		String id = (String) node.getProperty("id");
		boolean isExternal = (Boolean) node.getProperty("isExternal");
		boolean isInterface = (Boolean) node.getProperty("isInterface");
		boolean isAbstract = (Boolean) node.getProperty("isAbstract");
		String vis = (String) node.getProperty("vis");
		ClassElement ce = new ClassElement(id, isExternal, isInterface, isAbstract);
		ce.setVisibilty(vis);
		HashSet<Node> methods = getMethodNodes(node);
		Collection<MethodElement>methodElementCollection = new HashSet<MethodElement>();
		for(Node methodNode : methods)
		{
			MethodElement me = getMethodElementFromNode(methodNode);
			methodElementCollection.add(me);
		}
		return ce;
	}
	
	private static MethodElement addMethodElementToNode(Node node)
	{
		System.out.println(node.getProperty("id"));
		String id = (String) node.getProperty("id");
		String vis = (String) node.getProperty("vis");
		String exactName = (String) node.getProperty("exactName");
		MethodElement me = new MethodElement(id);
		me.setVisibilty(vis);
		me.extractNames();
		me.setCallsNull();
		me.setReferencesNull();
		Node returnNode = getMethodReturn(node);
		ClassElement returnType = getClassElementFromNode(returnNode);
		MethodReturnElement mre = new MethodReturnElement(returnType);
		me.setReturn(mre);
		Collection<Node> paramNodes = new HashSet<Node>();
		paramNodes=getMethodParams(node);
		List<MethodParamElement> paramElementsList = new Vector<MethodParamElement>();
		for(Node paramNode : paramNodes)
		{
			System.out.println(paramNode.getProperty("id"));
			MethodParamElement mpe = new MethodParamElement(getClassElementFromNode(paramNode));
			paramElementsList.add(mpe);
		}
		me.setParams(paramElementsList);
		return me;
	}
	
	private static MethodElement getMethodElementFromNode(Node node)
	{
		System.out.println(node.getProperty("id"));
		String id = (String) node.getProperty("id");
		String vis = (String) node.getProperty("vis");
		String exactName = (String) node.getProperty("exactName");
		MethodElement me = new MethodElement(id);
		me.setVisibilty(vis);
		me.extractNames();
		me.setCallsNull();
		me.setReferencesNull();
		Node returnNode = getMethodReturn(node);
		ClassElement returnType = getClassElementFromNode(returnNode);
		MethodReturnElement mre = new MethodReturnElement(returnType);
		me.setReturn(mre);
		Collection<Node> paramNodes = new HashSet<Node>();
		paramNodes=getMethodParams(node);
		List<MethodParamElement> paramElementsList = new Vector<MethodParamElement>();
		for(Node paramNode : paramNodes)
		{
			System.out.println(paramNode.getProperty("id"));
			MethodParamElement mpe = new MethodParamElement(getClassElementFromNode(paramNode));
			paramElementsList.add(mpe);
		}
		me.setParams(paramElementsList);
		return me;
	}
	
	private static HashSet<Node> getMethodNodes(Node node)
	{
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.HAS_METHOD, Direction.OUTGOING )
				.evaluator( Evaluators.excludeStartPosition() );
		Traverser methodTraverser = td.traverse( node );
		HashSet<Node> methodsCollection = new HashSet<Node>();;
		for ( Path methods : methodTraverser )
		{
			if(methods.length()==1)
			{
				methodsCollection.add(methods.endNode());
			}
		}
		return methodsCollection;
	}
	
	private static Node getMethodContainer(Node node)
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
				container = containerNode.endNode();
			}
		}
		return container;
	}
	private static Node getMethodReturn(Node node)
	{
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
			}
		}
		return container;
	}
	private static Collection<Node> getMethodParams(Node node) 
	{
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.PARAMETER, Direction.OUTGOING )
				.evaluator( Evaluators.excludeStartPosition() );
		Traverser traverser = td.traverse( node );
		Collection<Node> paramNodesCollection = new HashSet<Node>();
		for ( Path paramNode : traverser )
		{
			if(paramNode.length()==1)
			{
				paramNodesCollection.add(paramNode.endNode());
			}
		}
		return paramNodesCollection;
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
	
	private static Traverser getParentClass(final Node node )
	{
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.IS_METHOD, Direction.OUTGOING )
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
	
}