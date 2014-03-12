package RestAPI;

import java.util.HashMap;


import Node.NodeJSON;

public class ThreadedMethodContainerFetch implements Runnable 
{
	private GraphServerAccess model;
	private NodeJSON methodNode;
	private HashMap<NodeJSON, NodeJSON> methodContainerCache;
	
	public ThreadedMethodContainerFetch(NodeJSON methodNode, HashMap<NodeJSON, NodeJSON> methodContainerCache, GraphServerAccess graphModel)
	{
		this.methodNode = methodNode;
		this.methodContainerCache = methodContainerCache;
		this.model = graphModel;
	}

	@Override
	public void run()
	{
		model.getMethodContainer(methodNode, methodContainerCache);
	}
}
