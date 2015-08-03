import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeSet;

public class Splitter {
	private static TreeSet<String> names = new TreeSet<String>();
	private static int split_length = 2;
	private static String baseDir = "/Users/siddharth/maven_data/";
	private static String APIList = baseDir + "all.sorted.unique";
	private static String splitFilesDirectory = baseDir + "split_files/";

	public static File getFileForClass(String line) {

		String[] semiColonBrokenArray = line.split(";");
		String[] nameTokens = semiColonBrokenArray[1].split("[\\.\\$]");
		File new_file = null;
		if (nameTokens.length > split_length) {
			String name = splitFilesDirectory;
			int i = 0;
			for (String s : nameTokens) {
				i++;
				if (i == nameTokens.length - 1)
					break;
				if (i <= split_length)
					name = name + s + ".";
				else
					break;

			}
			String file_name = name.substring(0, name.length() - 1) + ".txt";
			if (file_name.equals(splitFilesDirectory + ".txt"))
				file_name = splitFilesDirectory + "general.txt";
			new_file = new File(file_name);
		} else
			new_file = new File(splitFilesDirectory + "general.txt");

		if (semiColonBrokenArray[1].contains("$") == true) {
			String[] temp = semiColonBrokenArray[1].split("\\$");
			String last = temp[temp.length - 1];
			if (isInteger(last) == true) {
				new_file = null;
			}
		}
		return new_file;
	}

	public static File getFileForInterface(String line) {
		String[] semicolonbreak = line.split(";");
		String[] nametokens = semicolonbreak[1].split("[\\.\\$]");
		File new_file = null;
		if (nametokens.length > split_length) {
			String name = splitFilesDirectory;
			int i = 0;
			for (String s : nametokens) {

				i++;
				if (i == nametokens.length - 1)
					break;
				if (i <= split_length)
					name = name + s + ".";
				else
					break;
			}
			String file_name = name.substring(0, name.length() - 1) + ".txt";
			new_file = new File(file_name);
		} else
			new_file = new File(splitFilesDirectory + "general.txt");

		if (semicolonbreak[1].contains("$") == true) {
			String[] temp = semicolonbreak[1].split("\\$");
			String last = temp[temp.length - 1];
			if (isInteger(last) == true) {
				new_file = null;
			}
		}
		return new_file;
	}

	public static File getFileForMethod(String line) {
		String[] semicolonbreak = line.split(";");
		String[] nametokens = semicolonbreak[2].split("[\\.\\$]");
		File new_file = null;

		if (nametokens.length > split_length) {
			String name = splitFilesDirectory;
			int i = 0;
			for (String s : nametokens) {

				i++;
				if (i == nametokens.length - 1)
					break;
				if (i <= split_length)
					name = name + s + ".";
				else
					break;
			}
			String file_name = name.substring(0, name.length() - 1) + ".txt";
			new_file = new File(file_name);
		} else
			new_file = new File(splitFilesDirectory + "general.txt");

		if (semicolonbreak[1].contains("access$")) {
			String[] temp = semicolonbreak[1].split("access\\$");
			String last = temp[temp.length - 1];
			if (isInteger(last) == true) {
				new_file = null;
			}
		}
		if (semicolonbreak[2].contains("$") == true) {
			String[] temp = semicolonbreak[2].split("\\$");
			String last = temp[temp.length - 1];
			if (isInteger(last) == true) {
				new_file = null;
			}
		}
		return new_file;
	}

	public static File getFileForField(String line) {
		String[] semicolonbreak = line.split(";");
		String[] nametokens = semicolonbreak[2].split("[\\.\\$]");
		File new_file = null;

		if (nametokens.length > split_length) {
			String name = splitFilesDirectory;
			int i = 0;
			for (String s : nametokens) {

				i++;
				if (i == nametokens.length - 1)
					break;
				if (i <= split_length)
					name = name + s + ".";
				else
					break;
			}
			String file_name = name.substring(0, name.length() - 1) + ".txt";
			new_file = new File(file_name);
		} else
			new_file = new File(splitFilesDirectory + "general.txt");

		if (semicolonbreak[1].contains("this$")) {
			String[] temp = semicolonbreak[1].split("this\\$");
			String last = temp[temp.length - 1];
			if (isInteger(last) == true) {
				new_file = null;
			}
		}
		if (semicolonbreak[2].contains("$") == true) {
			String[] temp = semicolonbreak[2].split("\\$");
			String last = temp[temp.length - 1];
			if (isInteger(last) == true) {
				new_file = null;
			}
		}
		return new_file;
	}

	public static void main(String args[]) throws IOException {

		File ip = new File(APIList);
		boolean success = (new File(splitFilesDirectory)).mkdirs();
		if (!success) {
			System.out.println("Directory creation failed!");
		}
		BufferedReader br = new BufferedReader(new FileReader(ip));
		String line = null;
		int count = 0;
		File new_file = null;
		BufferedWriter bw = null;
		try {
			while ((line = br.readLine()) != null) {
				count++;
				if (count % 100 == 0)
					System.out.println(count);
				if (line.startsWith("class;")) {
					// new_file=getFileForClass(line);
					new_file = new File(baseDir + "class_file");
				} else if (line.startsWith("method;")) {
					new_file = getFileForMethod(line);
				} else if (line.startsWith("field;")) {
					new_file = getFileForField(line);
				} else if (line.startsWith("interface;")) {
					// new_file=getFileForInterface(line);
					new_file = new File(baseDir + "class_file");
				}
				if (new_file != null)
					names.add(new_file.getAbsolutePath());
				if (new_file != null) {
					bw = new BufferedWriter(new FileWriter(new_file, true));
					bw.write(line + "\n");
					bw.close();
				}
			}
		}

		catch (ArrayIndexOutOfBoundsException e) {
			System.out.println(line);
		}

		for (String temp : names) {
			System.out.println(temp);
		}
		System.out.println(names.size());
		br.close();
	}

	// Checks if a given string is the string representation of an integer.
	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
}
