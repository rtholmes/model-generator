import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;


public class PackageLister {
	
	public static void main(String[] args) throws IOException 
	{
		// TODO Auto-generated method stub
		
		String file = "/home/s23subra/maven_data/all.sorted.unique";
		String op = "/home/s23subra/maven_data/packages.txt";
		
		HashMap<String, String> set = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(file));
		BufferedWriter bw = new BufferedWriter(new FileWriter(op));
		
		String s = "";
		while((s = br.readLine()) != null)
		{
			if(s.startsWith("class;"))
			{
				int index1 = 6;
				int index2 = s.indexOf(';', index1);
				
				String className = s.substring(index1, index2);
				String packageName = getPackage(className);
				if(packageName != "")
					set.put(packageName, className);
				
			}
		}
		
		for(String packageName : set.values())
		{
			bw.write(packageName + '\n');
		}
			
		System.out.println(set.keySet().size());	
		System.out.println(set.values().size());	
		
		br.close();
		bw.close();
	}
	
	public static String getPackage(String className) 
	{
		int i;
		for(i = 0; i < className.length(); i++)
		{
			if(Character.isUpperCase(className.charAt(i)))
			{
				if(i > 1)
					return className.substring(0, i-1);
				else
					return "";
			}
		}
		return "";
	}

}
