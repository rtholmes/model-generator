import java.io.File;
import java.util.Collection;

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
import ca.uwaterloo.cs.se.inconsistency.core.model2.Model;
import ca.uwaterloo.cs.se.inconsistency.core.model2.io.Model2XMLReader;

public class Graph
{
	static Model _model;
	private static final String DB_PATH = "neo4j-store";
	private static GraphDatabaseService graphDb;
	private static Index<Node> nodeIndexClass;
	private static Index<Node> nodeIndexMethod;
	private static Index<Node> nodeIndexField;

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

	public static void populate(String fName)
	{
		Model2XMLReader xmlrdf = new Model2XMLReader(fName);
		Model knownModel = xmlrdf.read();
		_model=knownModel;
		
		for ( ClassElement ce : knownModel.getClasses() )
		{
			createAndIndexClassElement(ce);
		}
		for(MethodElement me : knownModel.getMethods())
		{
			createAndIndexMethodElement(me);
		}
		for(FieldElement fe : knownModel.getFields())
		{
			createAndIndexFieldElement(fe);
		}
	}
	
	public static void main( final String[] args )
	{
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
		nodeIndexClass = graphDb.index().forNodes( "classes" );
		nodeIndexMethod = graphDb.index().forNodes( "methods" );
		nodeIndexField = graphDb.index().forNodes( "fields" );
		registerShutdownHook();

		Transaction tx = graphDb.beginTx();
		try
		{
			String fName = "/home/s23subra/workspace/Java Snippet Parser/rt.xml";
			populate(fName);
			File xmlPath = new File("/home/s23subra/maven_data/xml/");
			File[] fileList = xmlPath.listFiles();
			int i=0;
			for(File file : fileList)
			{
				i++;
				String fname = file.getAbsolutePath();
				System.out.println(fname + " : " + i);
				populate(fname);
				//if(i==3)
					//break;
			}
			
			//###################################################
			System.out.println("searching.....");
			String idToFind = "java.io.BufferedWriter";
			Node foundUser = nodeIndexClass.get( "id", idToFind ).getSingle();
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



			tx.success();
		}
		finally
		{
			tx.finish();
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

	private static Node createAndIndexClassElement( ClassElement ce )
	{
		IndexHits<Node> userNodes  = nodeIndexClass.get("id", ce.getId());
		if(userNodes.hasNext()==false)
		{
			//System.out.println(ce.getId());
			Node node = graphDb.createNode();
			node.setProperty( "id", ce.getId() );
			node.setProperty("exactName", ce.getExactName());
			node.setProperty( "vis", ce.getVisiblity().toString() );
			node.setProperty( "isAbstract", ce.isAbstract() );
			node.setProperty( "isPrimitive", "false" );
			node.setProperty( "isInterface", ce.isInterface() );
			node.setProperty( "isExternal", ce.isExternal() );
			//node.setProperty("ce", ce);
			Collection<ClassElement> parentsList = ce.getParents();
			if(parentsList!=null)
			{
				for(ClassElement parent : parentsList)
				{
					Node parentNode = createAndIndexClassElement(parent);
					node.createRelationshipTo(parentNode, RelTypes.PARENT);
					parentNode.createRelationshipTo(node, RelTypes.CHILD);
				}

			}
			nodeIndexClass.add( node, "id", ce.getId() );
			return node;
		}
		else
			return userNodes.getSingle();
	}

	private static Node createAndIndexMethodElement( MethodElement me )
	{
		IndexHits<Node> methodNodes  = nodeIndexMethod.get("id", me.getId());
		if(methodNodes.hasNext()==false)
		{
			//System.out.println(me.getId());
			Node node = graphDb.createNode();
			node.setProperty( "id", me.getId() );
			node.setProperty("exactName", me.getExactName());
			node.setProperty( "vis", me.getVisiblity().toString());
			
			ClassElement parentClass = _model.getClass(me.extractClassName());
			insertParameterAndReturn(RelTypes.IS_METHOD,RelTypes.HAS_METHOD, parentClass, node);
			
			ClassElement returnType = me.getReturnElement().getType();
			insertParameterAndReturn(RelTypes.RETURN_TYPE, RelTypes.IS_RETURN_TYPE, returnType, node);
			
			Collection<MethodParamElement> params = me.getParameters();
			for(MethodParamElement param : params)
			{
				ClassElement paramtype = param.getType();
				insertParameterAndReturn(RelTypes.PARAMETER, RelTypes.IS_PARAMETER, paramtype, node);
			}
			
			nodeIndexMethod.add( node, "id", me.getId() );
			return node;
		}
		else
			return methodNodes.getSingle();
	}

	private static Node createAndIndexFieldElement( FieldElement fe )
	{
		IndexHits<Node> fieldNodes  = nodeIndexField.get("id", fe.getId());
		if(fieldNodes.hasNext()==false)
		{
			//System.out.println(ce.getId());
			Node node = graphDb.createNode();
			node.setProperty( "id", fe.getId() );
			node.setProperty("exactName", fe.getExactName());
			node.setProperty( "vis", fe.getVisiblity().toString() );
			node.setProperty( "isPrimitive", "false" );
			node.setProperty( "isExternal", fe.isExternal() );
			ClassElement fieldType = fe.getType();
			
			insertParameterAndReturn(RelTypes.IS_FIELD_TYPE, RelTypes.HAS_FIELD_TYPE, fieldType, node);
			ClassElement parentClass = _model.getClass(fe.getExactClassName());
			insertParameterAndReturn(RelTypes.IS_FIELD, RelTypes.HAS_FIELD, parentClass, node);
			
			nodeIndexField.add( node, "id", fe.getId() );
			return node;
		}
		else
			return fieldNodes.getSingle();
	}

	
	private static void insertParameterAndReturn(RelationshipType outgoing, RelationshipType incoming, ClassElement type, Node node)
	{
		if(type==null)
			return; 
		//System.out.println(type.getId());
		IndexHits<Node> returnNode = nodeIndexClass.get("id", type.getId());
		if(returnNode.hasNext())
		{
			Node returnTypeNode = returnNode.getSingle();
			node.createRelationshipTo(returnTypeNode, outgoing);
			returnTypeNode.createRelationshipTo(node, incoming);
		}
		else if(Convert.isPrimitive(type.getId()))
		{
			Node primitiveNode = graphDb.createNode();
			primitiveNode.setProperty( "id", type.getId() );
			primitiveNode.setProperty("exactName", type.getExactName());
			primitiveNode.setProperty( "vis", type.getVisiblity().toString() );
			primitiveNode.setProperty( "isAbstract", type.isAbstract() );
			primitiveNode.setProperty( "isInterface", type.isInterface() );
			primitiveNode.setProperty( "isExternal", type.isExternal() );
			primitiveNode.setProperty( "isPrimitive", "true" );
			node.createRelationshipTo(primitiveNode, outgoing);
			primitiveNode.createRelationshipTo(node, incoming);
		}
		else
		{
			Node newReturnNode = graphDb.createNode();
			newReturnNode.setProperty( "id", type.getId() );
			newReturnNode.setProperty("exactName", type.getExactName());
			newReturnNode.setProperty( "vis", type.getVisiblity().toString() );
			newReturnNode.setProperty( "isAbstract", type.isAbstract() );
			newReturnNode.setProperty( "isInterface", type.isInterface() );
			newReturnNode.setProperty( "isExternal", type.isExternal() );
			newReturnNode.setProperty( "isPrimitive", "false" );
			node.createRelationshipTo(newReturnNode, outgoing);
			newReturnNode.createRelationshipTo(node, incoming);
		}
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