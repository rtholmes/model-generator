package Node;

import org.json.JSONObject;


public class NodeJSON
{

	private JSONObject data;
	
	public JSONObject getJSONObject()
	{
		return data;
	}
	
	public NodeJSON(JSONObject obj) 
	{
		data = obj;
	}
	
	public static void main(String[] args) 
	{
		
	}
	
	public Integer getNodeNumber()
	{
		if(data.has("indexed"))
		{
			String[] temp = ((String) data.get("indexed")).split("/");
			String nodeNumber = temp[temp.length-1];
			return Integer.valueOf(nodeNumber);
		}
		else
		{
			String[] temp = ((String) data.get("self")).split("/");
			String nodeNumber = temp[temp.length-1];
			return Integer.valueOf(nodeNumber);
		}
	}

	public String getProperty(String prop) 
	{
		JSONObject obj = data.getJSONObject("data");
		String property = obj.getString(prop);
		return property;
	}

}
