package RestAPI;

import java.util.ArrayList;
import java.util.HashMap;


import Node.IndexHits;
import Node.NodeJSON;

public class ThreadedMethodFetch implements Runnable 
{
	private GraphServerAccess model;
	private String methodExactName;
	private HashMap<String, IndexHits<NodeJSON>> candidateMethodNodesCache;
	private HashMap<NodeJSON, ArrayList<NodeJSON>> methodParameterCache;
	private HashMap<NodeJSON, NodeJSON> methodContainerCache;
	private HashMap<NodeJSON, NodeJSON> methodReturnCache;
	
	public ThreadedMethodFetch(String methodExactName, HashMap<String, IndexHits<NodeJSON>> candidateMethodNodesCache, HashMap<NodeJSON, NodeJSON> methodContainerCache, HashMap<NodeJSON, NodeJSON> methodReturnCache, HashMap<NodeJSON, ArrayList<NodeJSON>> methodParameterCache, GraphServerAccess graphModel)
	{
		this.methodExactName = methodExactName;
		this.candidateMethodNodesCache = candidateMethodNodesCache;
		this.methodContainerCache = methodContainerCache;
		this.methodReturnCache = methodReturnCache;
		this.methodParameterCache = methodParameterCache;
		this.model = graphModel;
	}
	



	@Override
	public void run()
	{
		model.getCandidateMethodNodes(methodExactName, candidateMethodNodesCache);
	}
}
