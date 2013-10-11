import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

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

import com.google.common.collect.HashMultimap;





public class GraphTest
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
	
	public static String getIdWithoutArgs(String name)	//to store class name and exact method name as ivars
	{
		String[] array = name.split("\\(");
		return array[0];
	}
	public static String getPackage(String name)	//to store class name and exact method name as ivars
	{
		String[] array = name.split("\\(");
		return array[0];
	}
	public static String getExactNameMethod(String id)	//to store class name and exact method name as ivars
	{
		if (id.endsWith("<clinit>")){
			//_exactName = "<clinit>";
			String array[] = id.split(".<clinit>");
			return getExactNameClass(array[0]);	
		}
		else{		
		String[] array = id.split("\\(");
		array = array[0].split("\\.");
		//_exactName = array[array.length-1];
		String className = array[0];		
		for (int i=1; i<array.length-1; i++) className += "." + array[i];
		return getExactNameClass(className);
	 	}
		
	}
	public static String getExactNameClass(String id) 
	{
		String name=id;
		int i;
		for(i = 0;i<name.length();i++)
		{
			if(Character.isUpperCase(name.charAt(i)))
			{
				return id.substring(i);
			}
		}
		return id;
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

	
	public static ArrayList<String> getCommonMethods(Set<Node> exactClassNameNodeList)
	{
		ArrayList<String> answerList = new ArrayList<String>();
		HashMap<String, TreeSet<String>> counter = new HashMap<String, TreeSet<String>>();
		String exactClassName = null;
		for(Node exactClassNameNode : exactClassNameNodeList)
		{
			exactClassName = (String) exactClassNameNode.getProperty("exactName");
			HashSet<Node> methodNodeList = getMethodNodes(exactClassNameNode);
			for(Node methodNode : methodNodeList)
			{
				String idWithArgs = (String) methodNode.getProperty("id");
				String exactMethodName = (String) methodNode.getProperty("exactName");
				if(exactMethodName.equals("<init>")==false)
				{
					String idWithoutArgs = getIdWithoutArgs(idWithArgs);
					//System.out.println(idWithoutArgs);
					if(counter.containsKey(exactMethodName))
					{
						TreeSet<String> temp = counter.get(exactMethodName);
						temp.add(idWithoutArgs);
						counter.put(exactMethodName, temp);
					}
					else
					{
						TreeSet<String> temp = new TreeSet<String>();
						temp.add(idWithoutArgs);
						counter.put(exactMethodName, temp);
					}
				}
			}
		}
		
		Set<String> keys = counter.keySet();
		for(String key:keys)
		{
			TreeSet<String> valSet = counter.get(key);
			if(valSet.size() > exactClassNameNodeList.size()/2)
			{
				//answerList.add(key);
				String blah =  exactClassName+" . "+key + " : \n";
				for(String val : valSet)
				{
					blah = blah + "  - " + val + "\n";
				}
				//System.out.println(blah);
				answerList.add(blah);
			}
		}
		return answerList;
	
	}	
	
	
	public static void main(String[] args) throws IOException
	{
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
		classIndex = graphDb.index().forNodes("classes");
		methodIndex = graphDb.index().forNodes("methods");
		fieldIndex = graphDb.index().forNodes("fields");
		
		shortClassIndex = graphDb.index().forNodes("short_classes");
		shortMethodIndex = graphDb.index().forNodes("short_methods");
		shortFieldIndex = graphDb.index().forNodes("short_fields");
		parentIndex = graphDb.index().forNodes("parents");
		//classIndex.
		
		registerShutdownHook();
		BufferedWriter br = new BufferedWriter(new FileWriter("collisions2.txt"));
		
		Transaction tx2 = graphDb.beginTx();
		try
		{	
			
			 /*
			    classIndex : 1646650
				shortClassIndex : 1121887
				methodIndex : 14206944
				shortMethodIndex : 1600053
				fieldIndex : 3149206
				shortFieldIndex : 1115099
			  */
			HashMultimap<String, Node> nodesWithSameExactName = HashMultimap.create();
			IndexHits<Node> indices = classIndex.query("id", "*");
			System.out.println("Number of distinct class names: "+indices.size());
			
			for(Node node : indices)
			{
				String shortName = (String)node.getProperty("exactName");
				nodesWithSameExactName.put(shortName, node);
			}
			Set<String> distinctExactNames = nodesWithSameExactName.keySet();
			System.out.println("Number of distinct short class names: "+distinctExactNames.size());
			int c=0;
			for(String name : distinctExactNames)
			{
				c++;
				
				Set<Node> list = nodesWithSameExactName.get(name);
				if(list.size()>=3)
				{
					System.out.println(c);
					ArrayList<String> methodnames = getCommonMethods(list);
					for(String methodname:methodnames)
					{
						System.out.println(methodname);
						br.write(methodname);
					}
				}
			}
			
			tx2.success();
			br.close();
		
			}
		finally
		{
			tx2.finish();
		}

		shutdown();
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
	}*/
	
}