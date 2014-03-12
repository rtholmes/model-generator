package RestAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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
	private int NThreads = 20;
	ExecutorService getMethodContainerExecutor = Executors.newFixedThreadPool(NThreads);
	
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
		ArrayList<NodeJSON> methods = model.getCandidateMethodNodes(methodExactName, candidateMethodNodesCache);
		for(NodeJSON method : methods)
		{
			model.getMethodParams(method, methodParameterCache);
			
			//ThreadedMethodContainerFetch tmcf = new ThreadedMethodContainerFetch(method, methodContainerCache, model);
			//getMethodContainerExecutor.execute(tmcf);
			
			
		}
		
		/*getMethodContainerExecutor.shutdown();
		while(!getMethodContainerExecutor.isTerminated())
		{
			
		}*/
	}
}
