package RestAPI;

import java.util.HashMap;
import java.util.List;


import Node.NodeJSON;

public class ThreadedMethodContainerFetch implements Runnable 
{
	private GraphServerAccess model;
	private NodeJSON methodNode;
	private HashMap<NodeJSON, NodeJSON> methodContainerCache;
	private List<NodeJSON> replacementClassNodesList;
	

	public ThreadedMethodContainerFetch(NodeJSON candidateMethodNode,
			HashMap<NodeJSON, NodeJSON> methodContainerCache2,
			List<NodeJSON> methodContainerList,
			GraphServerAccess graphModel) 
	{
		this.methodNode = candidateMethodNode;
		this.methodContainerCache = methodContainerCache2;
		this.replacementClassNodesList = methodContainerList;
		this.model = graphModel;
	}

	@Override
	/*public void run()
	{
		model.getMethodContainer(methodNode, methodContainerCache);
	}*/
	public void run()
	{
		NodeJSON fcname = model.getMethodContainer(methodNode, methodContainerCache);
		if(fcname!=null)
			replacementClassNodesList.add(fcname);
	}
}
