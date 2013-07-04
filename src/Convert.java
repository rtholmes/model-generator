import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

class Convert
{
	static int count_notset=0;
	public Convert() throws IOException
	{
		File ip=new File("/home/s23subra/Desktop/parts.unique");
		BufferedReader br=new BufferedReader(new FileReader(ip));

		Document root=DocumentHelper.createDocument();
		Element main_root=root.addElement("dependencyGraph");
		Element declarations=main_root.addElement("declarations");
		Element classList=declarations.addElement("classList");
		Element init=classList.addElement("ce");
		init.addAttribute("id", "::UnknownType::");
		init.addAttribute("isExt", "true");
		init.addAttribute("vis", "notset");
		Element classDetails=declarations.addElement("classDetails");
		Element relationships=main_root.addElement("relationships");
		Element inherits=relationships.addElement("inherits");
		Element stacks=main_root.addElement("stacks");
		Element callstacks=stacks.addElement("callStacks");
		callstacks.addAttribute("count", "0");
		callstacks.addAttribute("popCount", "0");
		callstacks.addAttribute("pushCount", "0");
		String line=null;

		while((line=br.readLine())!=null)
		{
			count_notset++;
			if(count_notset%100==0)
				System.out.println(count_notset);
			if(line.startsWith("method;"))
			{
				getmethod(line,classDetails);
			}
			else if(line.startsWith("class;"))
			{
				getclass(line,classList,inherits);
				
			}
			else if(line.startsWith("field;"))
			{
				getfield(line, classDetails);
			}
			else if(line.startsWith("interface;"))
			{
				getinterface(line,classList, inherits);
			}
			else
				System.out.println(line);
		}
		OutputFormat format = OutputFormat.createPrettyPrint();
		XMLWriter output = new XMLWriter(new FileWriter(new File("/home/s23subra/Desktop/parts.unique_xml.xml")),format);
		output.write( main_root);
		output.close();

		//output = new XMLWriter( System.out, format );
		//output.write( document );
		System.out.println("Completed");
		System.out.println(count_notset);
	}

	public static void getclass(String line, Element classList, Element inherits)
	{
		int flag=0;
		String[] temp=line.split(";");
		String id=temp[1];
		String isAbs="false";
		String isInt="false";
		String isExt="false";
		String vis="notset";
		if(temp[2].contains("abstract ")==true)
		{	
			isAbs="true";
		}
		String[] temp_next=temp[2].split(" ");
		vis=getVisibility(temp_next[0]);
		List<Element>blah=classList.elements("ce");
		ListIterator<Element>iter=blah.listIterator(blah.size());
		while(iter.hasPrevious())
		{
			Element ele=iter.previous();
			if(ele.attributeValue("id").equals(id))
				flag=1;
		}
		if(flag==0)
		{
			Element ce=classList.addElement("ce");
			ce.addAttribute("id", id);
			ce.addAttribute("vis", vis);
			ce.addAttribute("isAbs", isAbs);
			ce.addAttribute("isInt", isInt);
			ce.addAttribute("isExt", isExt);
		}
		String superclass=null;
		for(int i=0;i<temp_next.length;i++)
		{
			String word=temp_next[i];
			if(word.trim().equals("extends"))
			{
				superclass=temp_next[i+1];
				break;
			}
		}
		if(superclass!=null)
		{
			List<Element>blah2=inherits.elements("inh");
			ListIterator<Element>iter2=blah2.listIterator(blah2.size());
			int flag2=0;
			/*
			while(iter.hasPrevious())
			{
				Element inh_temp=iter.previous();
				if(inh_temp.attributeValue("p").equals(superclass) && inh_temp.attributeValue("c").equals(id))
				{
					flag2=1;
					System.out.println("flag*** "+ line);
					break;
				}
			}
			*/
			if(flag2==0)
			{
				Element inh=inherits.addElement("inh");
				inh.addAttribute("p", superclass);
				inh.addAttribute("c", id);
			}
		}
		
		String [] implemented_classes=null;
		int implements_flag=0;
		for(int i=0;i<temp_next.length;i++)
		{
			String word=temp_next[i];
			if(word.trim().equals("implements"))
			{
				implemented_classes=temp_next[i+1].split(",");
				//System.out.println("---"+temp_next[i+1]);
				implements_flag=1;
				break;
			}
		}
		if(implements_flag==1)
		{
			for(String tempstring:implemented_classes)
			{
				Element inh=inherits.addElement("inh");
				inh.addAttribute("p", tempstring);
				inh.addAttribute("c", id);
			}
		}
	}

	public static void getmethod(String line, Element classDetails)
	{
		String[] temp=line.split(";");
		String return_type="void";
		String shortname=temp[1];
		String cname=temp[2];
		if(cname.charAt(temp[2].length()-1)=='{')
		{
			//System.out.println(cname);
			cname=cname.substring(0, cname.length()-1);
			//System.out.println(cname);
		}
		String id=cname+'.';
		String[] params=null;
		String vis="notset";
		int constructor_flag=0;
		if(shortname.startsWith("access$"))
		{
			return;
		}
		if(Character.isUpperCase(shortname.charAt(0)) && temp[2].endsWith("."+shortname))
		{
			id=id+"<init>";
			return_type="void";
			constructor_flag=1;
		}
		else
		{
			id=id+shortname;
		}
		String[] temp_next=temp[3].split(" ");
		vis=getVisibility(temp_next[0]);
		String temp1 = null;
		if(constructor_flag==0)
		{
		for(String ret: temp_next)
		{
			if(ret.contains("(")==true)
			{
				if(temp1!=null)
					return_type=temp1;
				break;
			}
			temp1=ret;
		}
		}
		int i1=line.indexOf("(");
		int i2=line.lastIndexOf(")");
		id=id+line.substring(i1, i2+1);
		params=line.substring(i1+1, i2).split(",");
		Element current=null;
		List<Element> blah = classDetails.elements("ce");
		ListIterator<Element> iter=blah.listIterator(blah.size());
		while(iter.hasPrevious())
		{
			Element ele=iter.previous();
			if(ele.attributeValue("id").equals(cname))
			{
				current=ele;
				break;
			}
		}

		if(current==null)
		{
			current=classDetails. addElement("ce");
			current.addAttribute("id", cname);
		}

		List<Element> blah2=current.elements("me");
		ListIterator<Element> iter2=blah2.listIterator(blah2.size());
		int flag=0;
		/*
		while(iter2.hasPrevious())
		{
			Element ele=iter2.previous();
			if(ele.attributeValue("id").equals(id) && ele.attributeValue("vis").equals(vis))
			{
				
				if(ele.element("return").attributeValue("id").equals(return_type))
				{
					System.out.println(ele.asXML());
					System.out.println("flag+++ "+ line);
					System.out.println("%%%"+id);
					flag=1;
				}
			}
		}
		*/
		if(flag==0)
		{
			Element me=current.addElement("me");
			me.addAttribute("id", id);
			me.addAttribute("vis", vis);
			Element return_node=me.addElement("return");
			return_node.addAttribute("id", return_type);
			if(i1+1!=i2)
			{
				Element params_node=me.addElement("params");
				int i=0;
				for(String param:params)
				{
					Element param_node=params_node.addElement("param");
					param_node.addAttribute("index", String.valueOf(i));
					param_node.addAttribute("type", param.trim());
					i++;
				}
			}
		}

	}

	public static void getfield(String line,Element classDetails)
	{
		String[] temp=line.split(";");
		String shortname=temp[1];
		String cname=temp[2];
		if(cname.charAt(temp[2].length()-1)=='{')
			cname=cname.substring(0, cname.length()-1);
		String id=cname+'.'+shortname;
		String type="::UnknownType::";
		String isExt="false";
		String vis="notset";
		
		if(shortname.contains("this$") || shortname.equals("{}"))
		{
			return;
		}
		String[] temp_next=temp[3].split(" ");
		vis=getVisibility(temp_next[0]);
		
		String temp1=null;
		for(String ret: temp_next)
		{
			if(ret.trim().equals(shortname)==true)
			{
				if(temp1!=null)
					type=temp1;
				break;
			}
			temp1=ret;
		}
		
		Element current=null;
		List<Element> blah = classDetails.elements("ce");
		ListIterator<Element> iter=blah.listIterator(blah.size());
		while(iter.hasPrevious())
		{
			Element ele=iter.previous();
			if(ele.attributeValue("id").equals(cname))
			{
				current=ele;
				break;
			}
		}

		if(current==null)
		{
			current=classDetails. addElement("ce");
			current.addAttribute("id", cname);
		}

		List<Element> blah2=current.elements("fe");
		ListIterator<Element> iter2=blah2.listIterator(blah2.size());
		int flag=0;
		/*
		while(iter2.hasPrevious())
		{
			Element ele=iter2.previous();
			if(ele.attributeValue("id").equals(id))
			{
				System.out.println("flag--- "+ line);
				flag=1;
			}
		}
		*/
		if(flag==0)
		{
			Element fe=current.addElement("fe");
			fe.addAttribute("id", id);
			fe.addAttribute("vis", vis);
			fe.addAttribute("isExt", isExt);
			fe.addAttribute("type", type);
		}
	}
	public static void getinterface(String line, Element classList, Element inherits)
	{
		int flag=0;
		//add to classlist
		String[] temp=line.split(";");
		String id=null;
		String isAbs="false";
		String isInt="true";
		String isExt="false";
		String vis="notset";
		if(temp[2].contains("abstract ")==true)
		{	
			isAbs="true";
		}
		String[] temp_next=temp[2].split(" ");

		vis=getVisibility(temp_next[0]);

		List<Element>blah=classList.elements("ce");
		ListIterator<Element>iter=blah.listIterator(blah.size());
		
		for(int i=0;i<temp_next.length;i++)
		{
			if(temp_next[i].trim().equals("interface"))
			{
				id=temp_next[i+1];
				break;
			}
		}
		
		while(iter.hasPrevious())
		{
			Element ele=iter.previous();
			if(ele.attribute("id").equals(id))
				flag=1;
		}
		if(flag==0)
		{
			Element ce=classList.addElement("ce");
			ce.addAttribute("id", id);
			ce.addAttribute("vis", vis);
			ce.addAttribute("isAbs", isAbs);
			ce.addAttribute("isInt", isInt);
			ce.addAttribute("isExt", isExt);
		}
		String superclass=null;
		for(int i=0;i<temp_next.length;i++)
		{
			if(temp_next[i].trim().equals("extends"))
			{
				superclass=temp_next[i+1];
				break;
			}
		}
		if(superclass!=null)
		{
			List<Element>blah2=inherits.elements("inh");
			ListIterator<Element>iter2=blah2.listIterator(blah2.size());
			int flag2=0;
			/*
			while(iter.hasPrevious())
			{
				Element inh_temp=iter.previous();
				if(inh_temp.attributeValue("p").equals(superclass) && inh_temp.attributeValue("c").equals(id))
				{
					flag2=1;
					break;
				}
			}
			*/
			if(flag2==0)
			{
				Element inh=inherits.addElement("inh");
				inh.addAttribute("p", superclass);
				inh.addAttribute("c", id);
			}
		}
		String [] implemented_classes=null;
		int implements_flag=0;
		for(int i=0;i<temp_next.length;i++)
		{
			String word=temp_next[i];
			if(word.trim().equals("implements"))
			{
				implemented_classes=temp_next[i+1].split(",");
				//System.out.println("---"+temp_next[i+1]);
				implements_flag=1;
				break;
			}
		}
		if(implements_flag==1)
		{
			for(String tempstring:implemented_classes)
			{
				Element inh=inherits.addElement("inh");
				inh.addAttribute("p", tempstring);
				inh.addAttribute("c", id);
			}
		}
	}

	private static String getVisibility(String string) 
	{
		string=string.trim();
		if(string.equals("public")==true || string.equals("private")==true || string.equals("protected")==true)
		{
			return string;
		}
		else
			return "notset";

	}
}