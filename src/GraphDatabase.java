import java.util.Collection;
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

public class GraphDatabase
{
	private static GraphDatabaseService graphDb;
	private static String DB_PATH;
	
	public Index<Node> classIndex ;
	public Index<Node> methodIndex ;
	public Index<Node> fieldIndex ;
	
	public Index<Node> shortClassIndex ;
	public Index<Node> shortMethodIndex ;
	public Index<Node> shortFieldIndex ;
	
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
	
	public GraphDatabase(String input_oracle) 
	{
		DB_PATH = input_oracle;
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
		
		classIndex = graphDb.index().forNodes("classes");
		methodIndex = graphDb.index().forNodes("methods");
		fieldIndex = graphDb.index().forNodes("fields");
		
		shortClassIndex = graphDb.index().forNodes("short_classess");
		shortMethodIndex = graphDb.index().forNodes("short_methods");
		shortFieldIndex = graphDb.index().forNodes("short_fields");
		
		registerShutdownHook();
	}
	
	
	public Collection<Node> getCandidateClassNodes(String className) 
	{
		IndexHits<Node> candidateNodes = shortClassIndex.get("short_name", className);
		Collection<Node> classElementCollection = new HashSet<Node>();
		for(Node candidate : candidateNodes)
		{
			//System.out.println(candidate.getProperty("vis"));
			if(candidate!=null)
				if(((String)candidate.getProperty("vis")).equals("PUBLIC")==true || ((String)candidate.getProperty("vis")).equals("NOTSET")==true)
					classElementCollection.add(candidate);
		}
		return classElementCollection;
	}
	
	public Collection<Node> getCandidateMethodNodes(String methodName) 
	{
		IndexHits<Node> candidateNodes = shortMethodIndex.get("short_name", methodName);
		Collection<Node> classElementCollection = new HashSet<Node>();
		for(Node candidate : candidateNodes)
		{
			if(candidate!=null)
				if(((String)candidate.getProperty("vis")).equals("PUBLIC")==true || ((String)candidate.getProperty("vis")).equals("NOTSET")==true)
					classElementCollection.add(candidate);
		}
		return classElementCollection;
	}
	
	public void test()
	{
		Collection<Node> test = new HashSet<Node>();
		test= getCandidateClassNodes("Comparable");
		for(Node n : test)
		{
			System.out.println(n.getProperty("id"));
		}
	}
	
	/*public Collection<ClassElement> getCandidateClasses(String className) 
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
	
	public Collection<MethodElement> getCandidateMethods(String methodName) 
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
	
	public ClassElement getClassElementForMethod(String id) 
	{
		Node node = methodIndex.get("id", id).getSingle();
		Node containerNode = getMethodContainer(node);
		ClassElement container = getClassElementFromNode(containerNode);
		return container;
	}
	
	private ClassElement getClassElementFromNode(Node node)
	{
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
	
	private MethodElement getMethodElementFromNode(Node node)
	{
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
			MethodParamElement mpe = new MethodParamElement(getClassElementFromNode(paramNode));
			paramElementsList.add(mpe);
		}
		me.setParams(paramElementsList);
		return me;
	}
	*/
	
	public HashSet<String> getClassChildernNodes(Node node)
	{
		TraversalDescription td = Traversal.description()
				.breadthFirst()
				.relationships( RelTypes.CHILD, Direction.OUTGOING )
				.evaluator( Evaluators.excludeStartPosition() );
		Traverser childTraverser = td.traverse( node );
		HashSet<String> childCollection = new HashSet<String>();;
		for ( Path child : childTraverser )
		{
				if(child.endNode()!=null)
					childCollection.add((String) child.endNode().getProperty("id"));
		}
		return childCollection;
	}
	
	public HashSet<Node> getMethodNodes(Node node)
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
				if(methods.endNode()!=null)
					methodsCollection.add(methods.endNode());
			}
			else
				break;
		}
		return methodsCollection;
	}
	
	public Node getMethodContainer(Node node)
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
					container = containerNode.endNode();
			}
			else
				break;
		}
		return container;
	}
	public Node getMethodReturn(Node node)
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
			else
				break;
		}
		return container;
	}
	public Collection<Node> getMethodParams(Node node) 
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
			else
				break;
		}
		return paramNodesCollection;
	}
	
	private static void shutdown()
	{
		graphDb.shutdown();
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
	private static Node getUltimateParent(final Node node )
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

}