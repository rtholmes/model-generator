package Node;

import org.json.JSONObject;


public class NodeJSON
{

	private JSONObject data;
	
	public JSONObject getJSONObject()
	{
		return this.data;
	}
	
	public NodeJSON(JSONObject obj) 
	{
		this.data = obj;
	}
	
	public static void main(String[] args) 
	{
		
	}

	public String getProperty(String prop) 
	{
		JSONObject obj = data.getJSONObject("data");
		String property = obj.getString(prop);
		return property;
	}

}
