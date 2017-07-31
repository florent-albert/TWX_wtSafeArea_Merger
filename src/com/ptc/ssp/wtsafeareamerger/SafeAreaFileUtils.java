package com.ptc.ssp.wtsafeareamerger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

import com.thingworx.things.repository.FileRepositoryThing;

public class SafeAreaFileUtils
{
	public static ArrayList<String> getSourceFiles(String sourceZipFilePath, FileRepositoryThing repo, boolean includeFolders) throws Exception
	{
		ArrayList<String> files = new ArrayList<String>();
		ZipInputStream zis = new ZipInputStream(repo.openFileForRead(sourceZipFilePath));

		//test safeArea name in list siteMod, ptcMod
		//check content if it contains a root folder (siteMod, ptcMod) or directly the content

		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) 
		{
			if( includeFolders || !entry.isDirectory())
			{
				String fullName;
				if(entry.getName().startsWith("siteMod") || entry.getName().startsWith("ptcMod")){
					fullName = entry.getName().substring(entry.getName().indexOf('/')+1);
				}
				else{
					fullName = entry.getName();
				}
				files.add(fullName);
			}
		}
		zis.close();	
		return files;
	}
	
	public static List<String> getFileContent(String sourceZipFilePath, FileRepositoryThing repo, String fileName) throws Exception
	{
		ZipInputStream zis = new ZipInputStream(repo.openFileForRead(sourceZipFilePath));
		ArrayList<String> result = new ArrayList<>();
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) 
		{
			if(!entry.isDirectory())
			{
				String fullName;
				if(entry.getName().startsWith("siteMod") || entry.getName().startsWith("ptcMod")){
					fullName = entry.getName().substring(entry.getName().indexOf('/')+1);
				}
				else{
					fullName = entry.getName();
				}
				if(fullName.equalsIgnoreCase(fileName))
				{
					Scanner sc = new Scanner(zis);
				    while(sc.hasNextLine()) {
				        result.add(sc.nextLine());
				    }
				}
			}
		}
		zis.close();	
		return result;
	}
	
	public static List<String> fileToLines(File file) throws IOException 
	{
        final List<String> lines = new ArrayList<String>();
        String line;
        final BufferedReader in = new BufferedReader(new FileReader(file));
        while ((line = in.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }
	
	public static List<String> fileToLines(FileInputStream fis) throws IOException 
	{
        final List<String> lines = new ArrayList<String>();
        Scanner sc = new Scanner(fis);
	    while(sc.hasNextLine()) {
           lines.add(sc.nextLine());
        }
        return lines;
    }
}
