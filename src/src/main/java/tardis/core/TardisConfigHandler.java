package tardis.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import tardis.TardisMod;

public class TardisConfigHandler
{
	private File tardisConfigDir;
	private File tardisSchemaDir;
	
	private HashMap<String,TardisConfigFile> cachedConfigs = new HashMap<String,TardisConfigFile>();
	
	public TardisConfigHandler(File baseConfigDir) throws IOException
	{
		tardisConfigDir = new File(baseConfigDir.toString() + "/tardis/");
		if((!tardisConfigDir.exists()) || (!tardisConfigDir.isDirectory()))
		{
			if(!tardisConfigDir.mkdirs())
				throw new IOException("Couldn't create " + tardisConfigDir.toString());
		}
		tardisSchemaDir = new File(tardisConfigDir.toString() + "/schema/");
		if((!tardisSchemaDir.exists()) || (!tardisSchemaDir.isDirectory()))
		{
			if(!tardisSchemaDir.mkdirs())
				throw new IOException("Couldn't create " + tardisSchemaDir.toString());
		}
	}
	
	public File getSchemaFile(String name)
	{
		File schema = new File(tardisSchemaDir.toString() + "/" + name + ".schema");
		if(!schema.exists())
		{
			try
			{
				InputStream fis = TardisMod.class.getResourceAsStream("/assets/tardismod/schema/"+name+".schema");
				FileOutputStream os = new FileOutputStream(schema);
				
				byte[] buffer = new byte[1024];
				int len;
				while ((len = fis.read(buffer)) != -1)
				{
				    os.write(buffer, 0, len);
				}
				fis.close();
				os.close();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		return schema;
	}
	
	public String[] getSchemas()
	{
		String[] fA = new String[0];
		ArrayList<String> found = new ArrayList<String>();
		try
		{
			URI assetSchemaURI = TardisMod.class.getResource("/assets/tardismod/schema/").toURI();
			File assetSchemaFile = new File(assetSchemaURI);
			TardisOutput.print("TCH", "Scanning" + assetSchemaFile.toString(),TardisOutput.Priority.DEBUG);
			String[] files = assetSchemaFile.list();
			for(String s:files)
			{
				if(s.endsWith(".schema") && !s.startsWith("tardis"))
					found.add(s.replace(".schema", ""));
			}
			files = tardisSchemaDir.list();
			for(String s:files)
			{
				if(s.endsWith(".schema") && !s.startsWith("tardis"))
					found.add(s.replace(".schema", ""));
			}		
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return found.toArray(fA);
	}
	
	public TardisConfigFile getConfigFile(String name)
	{
		if(cachedConfigs.containsKey(name))
			return cachedConfigs.get(name);
		
		TardisConfigFile tmp = new TardisConfigFile(new File(tardisConfigDir.toString() + "//Tardis" + name + ".cfg"));
		cachedConfigs.put(name, tmp);
		return tmp;
	}
}
