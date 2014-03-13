package RestAPI;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jdt.core.dom.MethodInvocation;

import com.google.common.collect.HashMultimap;



import Node.IndexHits;
import Node.NodeJSON;

public class ThreadedClassFetchHelper implements Runnable 
{
	
	private NodeJSON candidateClassNode;
	private MethodInvocation treeNode;
	private HashMultimap<Integer, NodeJSON> printmethods;
	private HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator;
	private ArrayList<NodeJSON> replacementClassNodesList;
	private HashMap<String, IndexHits<NodeJSON>> candidateClassNodesCache;
	private HashMap<NodeJSON, NodeJSON> methodReturnCache;
	private HashMap<String, ArrayList<NodeJSON>> parentNodeCache;
	private GraphServerAccess model;
	private HashMap<String, IndexHits<NodeJSON>> candidateMethodNodesCache;




	public ThreadedClassFetchHelper(NodeJSON candidateClassNode,
			MethodInvocation treeNode,
			HashMultimap<Integer, NodeJSON> printmethods,
			HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator,
			ArrayList<NodeJSON> replacementClassNodesList,
			HashMap<String, IndexHits<NodeJSON>> candidateClassNodesCache2,
			HashMap<String, IndexHits<NodeJSON>> candidateMethodNodesCache, 
			HashMap<NodeJSON, NodeJSON> methodReturnCache,
			HashMap<String, ArrayList<NodeJSON>> parentNodeCache,
			GraphServerAccess model2) 
	{
		this.candidateClassNode = candidateClassNode;
		this.treeNode = treeNode;
		this.printmethods = printmethods;
		this.candidateAccumulator = candidateAccumulator;
		this.replacementClassNodesList = replacementClassNodesList;
		this.candidateClassNodesCache = candidateClassNodesCache2;
		this.candidateMethodNodesCache = candidateMethodNodesCache;
		this.methodReturnCache = methodReturnCache;
		this.parentNodeCache = parentNodeCache;
		this.model = model2;
		
	}




	@Override
	public void run() 
	{}
}
