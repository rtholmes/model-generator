import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Splitter
{
	public static void main(String args[]) throws IOException
	{
		File ip=new File("/home/s23subra/Desktop/maven_data/parts.unique");
		
		File classList = new File("/home/s23subra/Desktop/maven_data/parts.unique_class");
		File interfaceList = new File("/home/s23subra/Desktop/maven_data/parts.unique_interface");
		File fieldList = new File("/home/s23subra/Desktop/maven_data/parts.unique_field");
		File methodList = new File("/home/s23subra/Desktop/maven_data/parts.unique_method");
		
		BufferedWriter classWriter=new BufferedWriter(new FileWriter(classList));
		BufferedWriter interfaceWriter=new BufferedWriter(new FileWriter(interfaceList));
		BufferedWriter fieldWriter=new BufferedWriter(new FileWriter(fieldList));
		BufferedWriter methodWriter=new BufferedWriter(new FileWriter(methodList));
		
		
		BufferedReader br=new BufferedReader(new FileReader(ip));
		String line=null;
		while((line=br.readLine())!=null)
		{
			if(line.startsWith("class;"))
			{
				String[] semicolonbreak = line.split(";");
				
			}
		}
	}
}