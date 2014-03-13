package RestAPI;

import java.util.ArrayList;
import java.util.HashMap;


import Node.NodeJSON;

public class ThreadedMethodContainerFetch implements Runnable 
{
	private GraphServerAccess model;
	private NodeJSON methodNode;
	private HashMap<NodeJSON, NodeJSON> methodContainerCache;
	private ArrayList<NodeJSON> replacementClassNodesList;
	
	public ThreadedMethodContainerFetch(NodeJSON candidateMethodNode,
			HashMap<NodeJSON, NodeJSON> methodContainerCache2,
			ArrayList<NodeJSON> replacementClassNodesList,
			GraphServerAccess graphModel) 
	{
		this.methodNode = candidateMethodNode;
		this.methodContainerCache = methodContainerCache2;
		this.replacementClassNodesList = replacementClassNodesList;
		this.model = graphModel;
	}

	@Override
	/*public void run()
	{
		model.getMethodContainer(methodNode, methodContainerCache);
	}*/
	public void run()
	{}
}
