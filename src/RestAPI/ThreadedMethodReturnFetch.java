package RestAPI;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.common.collect.HashMultimap;


import Node.NodeJSON;

public class ThreadedMethodReturnFetch implements Runnable 
{
	private GraphServerAccess model;
	private NodeJSON methodNode;
	private HashMap<NodeJSON, NodeJSON> methodReturnCache;
	private HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator;
	private ArrayList<Integer> scopeArray;
	

	public ThreadedMethodReturnFetch(NodeJSON candidateMethodNode,
			HashMap<NodeJSON, NodeJSON> methodReturnCache,
			HashMultimap<ArrayList<Integer>, NodeJSON> candidateAccumulator,
			ArrayList<Integer> scopeArray, 
			GraphServerAccess graphModel) 
	{
		this.methodNode = candidateMethodNode;
		this.methodReturnCache = methodReturnCache;
		this.candidateAccumulator = candidateAccumulator;
		this.scopeArray = scopeArray;
		this.model = graphModel;
	}

	@Override
	public void run()
	{
		NodeJSON retElement = model.getMethodReturn(methodNode, methodReturnCache);
		if(retElement!=null)
		{
			candidateAccumulator.put(scopeArray, retElement);
		}
		
	}
}
