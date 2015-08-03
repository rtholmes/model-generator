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

public class ReIndex {
	private static enum RelTypes implements RelationshipType {
		PARENT, CHILD, IS_METHOD, HAS_METHOD, IS_FIELD, HAS_FIELD, RETURN_TYPE, IS_RETURN_TYPE, PARAMETER, IS_PARAMETER, IS_FIELD_TYPE, HAS_FIELD_TYPE
	}

	private static final String DB_PATH = "maven-graph-database";
	private static GraphDatabaseService graphDb;
	private static Index<Node> nodeParents;
	private static Index<Node> shortClassIndex;
	private static Index<Node> newParentsIndex;
	private static Index<Node> classIndex;

	public static void main(String[] args) {
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
		classIndex = graphDb.index().forNodes("classes");
		shortClassIndex = graphDb.index().forNodes("short_classes");
		nodeParents = graphDb.index().forNodes("parents");
		newParentsIndex = graphDb.index().forNodes("parentNodes");

		/*
		 * Transaction tx1 = graphDb.beginTx(); try { newParentsIndex.delete();
		 * tx1.success(); } finally { tx1.finish(); } newParentsIndex =
		 * graphDb.index().forNodes("parentNodes");
		 */

		registerShutdownHook();
		int count = 0;
		IndexHits<Node> classes = shortClassIndex.query("short_name", "*");
		System.out.println(classes.size());
		System.out.println(newParentsIndex.query("childId", "*").size());

		int size = 0;
		for (Node classnode : classes) {
			System.out.println(count++);
			if (count > 716566) {
				String classId = (String) classnode.getProperty("id");
				System.out.println(classId);
				HashSet<Node> parents = getAllParents(classId, new HashSet<String>());

				for (Node parent : parents) {
					Transaction tx0 = graphDb.beginTx();
					try {
						size++;
						System.out.println("-- " + parent.getProperty("id"));
						int flag = 0;
						/*
						 * IndexHits<Node> hits = newParentsIndex.get("childId",
						 * classId); for(Node hit : hits) {
						 * if(hit.equals(parent)) { flag = 1; break; } }
						 */
						if (flag == 0)
							newParentsIndex.add(parent, "childId", classId);
						else
							System.out.println("already indexed");
						// newParentsIndex.add(parent, "childId", classId);
						tx0.success();
					} finally {
						tx0.finish();
					}
				}
				System.out.println(classId + " no of parents: " + parents.size() + " countsofar: " + size);
			}
		}
		System.out.println(size);
	}

	public static ArrayList<Node> getMethodNodes(Node node) {
		TraversalDescription td = Traversal.description().breadthFirst()
				.relationships(RelTypes.HAS_METHOD, Direction.OUTGOING).evaluator(Evaluators.excludeStartPosition());
		Traverser methodTraverser = td.traverse(node);
		ArrayList<Node> methodsCollection = new ArrayList<Node>();
		;
		for (Path methods : methodTraverser) {
			if (methods.length() == 1) {
				if (methods.endNode() != null)
					methodsCollection.add(methods.endNode());
			} else
				break;
		}
		return methodsCollection;
	}

	private static ArrayList<String> getAllChildren(Node parent) {
		HashSet<String> visited = new HashSet<String>();
		ArrayList<String> childNodes = new ArrayList<String>();

		TraversalDescription td = Traversal.description().breadthFirst()
				.relationships(RelTypes.CHILD, Direction.OUTGOING).evaluator(Evaluators.excludeStartPosition());
		Traverser childTraverser = td.traverse(parent);
		for (Path child : childTraverser) {
			if (child.endNode() != null && visited.contains((String) child.endNode().getProperty("id")) == false) {
				String childId = (String) child.endNode().getProperty("id");
				visited.add(childId);
				childNodes.add(childId);
			}
		}
		return childNodes;
	}

	public static HashMap<String, HashSet<Node>> cache = new HashMap<String, HashSet<Node>>();

	public static HashSet<Node> getAllParents(String className, HashSet<String> parentNames) {
		if (cache.containsKey(className))
			return cache.get(className);
		IndexHits<Node> candidateNodes = nodeParents.get("parent", className);
		HashSet<Node> classElementCollection = new HashSet<Node>();
		for (Node candidate : candidateNodes) {
			if (((String) candidate.getProperty("vis")).equals("PUBLIC") == true
					|| ((String) candidate.getProperty("vis")).equals("NOTSET") == true) {
				String _cid = (String) candidate.getProperty("id");
				if (parentNames.contains(_cid) == false) {
					parentNames.add(_cid);
					classElementCollection.add(candidate);
					classElementCollection.addAll(getAllParents(_cid, parentNames));
				}
			}
		}
		Node objectNode = classIndex.get("id", "java.lang.Object").getSingle();
		classElementCollection.add(objectNode);
		cache.put(className, classElementCollection);
		return classElementCollection;
	}

	private static void shutdown() {
		graphDb.shutdown();
	}

	private static void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				shutdown();
			}
		});
	}
}