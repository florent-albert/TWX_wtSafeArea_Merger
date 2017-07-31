package com.ptc.ssp.wtsafeareamerger;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.utils.ThingUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.ThingworxBaseTemplateDefinition;
import com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinition;
import com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinitions;
import com.thingworx.metadata.annotations.ThingworxDataShapeDefinition;
import com.thingworx.metadata.annotations.ThingworxFieldDefinition;
import com.thingworx.metadata.annotations.ThingworxImplementedShapeDefinition;
import com.thingworx.metadata.annotations.ThingworxImplementedShapeDefinitions;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.things.Thing;
import com.thingworx.things.repository.FileRepositoryThing;
import com.thingworx.types.ConfigurationTable;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.IPrimitiveType;
import com.thingworx.types.primitives.StringPrimitive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;

@ThingworxBaseTemplateDefinition(name = "GenericThing")
@ThingworxImplementedShapeDefinitions(shapes = {
		@ThingworxImplementedShapeDefinition(name = "SourceCodeLocationsHelper") })
@ThingworxConfigurationTableDefinitions(tables = {
		@ThingworxConfigurationTableDefinition(name = "baseURLs", description = "", isMultiRow = false, ordinal = 0, dataShape = @ThingworxDataShapeDefinition(fields = {
				@ThingworxFieldDefinition(name = "baseURL_1", description = "", baseType = "STRING", ordinal = 0, aspects = {
						"isRequired:true", "defaultValue:http://ah-opengrok.ptcnet.ptc.com/" }),
				@ThingworxFieldDefinition(name = "baseURL_2", description = "", baseType = "STRING", ordinal = 1, aspects = {
						"defaultValue:http://bla-grok-01/source/" }),
				@ThingworxFieldDefinition(name = "baseURL_3", description = "", baseType = "STRING", ordinal = 1, aspects = {
						"defaultValue:http://bla-opengrok.ptcnet.ptc.com/source/" })})) })
public class GrokDownloaderTemplate extends Thing
{
	private static final long serialVersionUID = -7712933653056170444L;
	private HashMap<String, ArrayList<String>> major_CPS = null;
	private HashMap<String, String> product_codeRepo = new HashMap<>();
	private static Logger _logger = LogUtilities.getInstance().getApplicationLogger(GrokDownloaderTemplate.class);

	public GrokDownloaderTemplate()
	{
		// TODO Auto-generated constructor stub
	}

	@ThingworxServiceDefinition(name = "getMajorVersions", description = "Return all major versions (such as X26)", category = "", isAllowOverride = false, aspects = {
			"isAsync:false" })
	@ThingworxServiceResult(name = "Result", description = "Result", baseType = "INFOTABLE", aspects = {
			"dataShape:StringListDataShape" })
	public InfoTable getMajorVersions() throws Exception
	{
		getAllProducts();
		_logger.debug("Entering Service: getMajorVersions");
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("StringListDataShape");
		for (String product : major_CPS.keySet())
		{
			ValueCollection row = new ValueCollection();
			row.put("Field1", new StringPrimitive(product));
			result.addRow(row);
		}
		_logger.debug("Exiting Service: getMajorVersions");
		return result;
	}

	@SuppressWarnings("all")
	public void getAllProducts() throws Exception
	{
		_logger.debug("Entering INTERNAL Service: getAllProducts");
		major_CPS = new HashMap<>();
		ConfigurationTable baseURLs = getConfigurationTable("baseURLs");
		ValueCollection url = baseURLs.getRow(0);
		Collection c = url.values();
		for (IPrimitiveType urlValue : url.values())
		{
			String grokURL = urlValue.toString();
			_logger.debug("Getting products for URL : " + grokURL);
			Document page = Jsoup.connect(grokURL).get();
			Element projects = page.select("select#project").first();
			Iterator<Element> projectRows = projects.select("option").iterator();
			while (projectRows.hasNext())
			{
				Element option = projectRows.next();
				String project = option.text();
				if (project != null)
				{
					_logger.debug("Checking if project " + project + " is supported");
					if (!project.equals("x-24_CPS"))
					// need to find a way to treat duplicates for x-24
					// projects (both in Grok and Bla-Grok)
					{
						Pattern versionPattern = Pattern.compile("(Windchill )?[xX][0-9]?[-[FM]&&[0-9]+]?.*");
						Matcher versionMatcher = versionPattern.matcher(project);
						//if (versionMatcher.matches())
						if(project.startsWith("wnc-wnc.") || versionMatcher.matches())
						{
							_logger.debug("Project " + project + " supported !");
							String product = "";
							String cps = "";
							if (project.contains("CPS")){
								product = project.substring(0, project.lastIndexOf("CPS")-1);
								cps = project.substring(project.indexOf("CPS"));
							} 
							else{
								product = project;
							}
							ArrayList<String> cpsList = major_CPS.get(product);
							if (cpsList == null){
								cpsList = new ArrayList<String>();
							}
							cpsList.add(cps);
							major_CPS.put(product, cpsList);
							product_codeRepo.put(project, grokURL);
						}
					}
				}
			}
		}
		_logger.debug("Exiting INTERNAL Service: getAllProducts");
	}

	@ThingworxServiceDefinition(name = "getCPSforProduct", description = "", category = "", isAllowOverride = false, aspects = {
			"isAsync:false" })
	@ThingworxServiceResult(name = "Result", description = "", baseType = "INFOTABLE", aspects = {
			"isEntityDataShape:true", "dataShape:StringListDataShape" })
	public InfoTable getCPSforProduct(
			@ThingworxServiceParameter(name = "product", description = "", baseType = "STRING", aspects = {
					"isRequired:true" }) String product)
			throws Exception
	{
		_logger.debug("Entering Service: getCPSforProduct");
		InfoTable result = InfoTableInstanceFactory.createInfoTableFromDataShape("StringListDataShape");
		ArrayList<String> cpsList = major_CPS.get(product);
		if(cpsList != null)
		{
			for (String cps : cpsList)
			{
				ValueCollection row = new ValueCollection();
				row.put("Field1", new StringPrimitive(cps));
				result.addRow(row);
			}
		}
		_logger.debug("Exiting Service: getCPSforProduct");
		return result;
	}

	@ThingworxServiceDefinition(name = "getTargetSourceCode", description = "", category = "", isAllowOverride = false, aspects = {
			"isAsync:false" })
	@ThingworxServiceResult(name = "Result", description = "downloadable zip containing target source code", baseType = "HYPERLINK", aspects = {})
	public String getTargetSourceCode(
			@ThingworxServiceParameter(name = "targetVersion", description = "", baseType = "STRING", aspects = {
					"isRequired:true" }) String targetVersion,
			@ThingworxServiceParameter(name = "sourceZipFilePath", description = "", baseType = "STRING", aspects = {
					"isRequired:true" }) String sourceZipFilePath,
			@ThingworxServiceParameter(name = "repo", description = "", baseType = "THINGNAME") String repo,
			@ThingworxServiceParameter(name = "generateReport", description = "", baseType = "BOOLEAN") Boolean generateReport)
			throws Exception
	{
		_logger.trace("Entering Service: getTargetSourceCode");
		String result = null;
		try
		{
			Thing repoThing = ThingUtilities.findThing(repo);
			if (repoThing instanceof FileRepositoryThing)
			{
				FileRepositoryThing fileRepoThing = (FileRepositoryThing) repoThing;
				String targetName = "target" + System.currentTimeMillis();
				fileRepoThing.CreateFolder(targetName);
				StringBuffer listOfAllTargetFiles = new StringBuffer();
				ArrayList<String> allSourceFiles = SafeAreaFileUtils.getSourceFiles(sourceZipFilePath, fileRepoThing, false);
				ArrayList<String> allSourceEntries = SafeAreaFileUtils.getSourceFiles(sourceZipFilePath, fileRepoThing, true);
				for (String sourcePath : allSourceFiles)
				{
					if (listOfAllTargetFiles != null && listOfAllTargetFiles.length() > 0){
						listOfAllTargetFiles.append(",");
					}
					String normalizedSourcePath = sourcePath.replaceAll(Matcher.quoteReplacement(File.separator), "/");
					String downloadableFile = findTargetResource(normalizedSourcePath, targetVersion);
					if (downloadableFile != null)
					{
						File file = new File(targetName + "/" + normalizedSourcePath + "_tmp");
						FileUtils.copyURLToFile(new URL(downloadableFile), file);
						fileRepoThing.CreateTextFile(targetName + "/" + normalizedSourcePath,
								FileUtils.readFileToString(file), true);
					} 
					else
					{
						_logger.warn(
								"Could not find resource " + normalizedSourcePath + " in project " + targetVersion);
						fileRepoThing.CreateTextFile(targetName + "/" + normalizedSourcePath,
								"Resource not found in project " + targetVersion, true);
					}
					listOfAllTargetFiles.append(targetName + "/" + normalizedSourcePath);
					if(generateReport.booleanValue())
					{
						ReportGenerator report = new ReportGenerator(
								SafeAreaFileUtils.getFileContent(sourceZipFilePath, fileRepoThing, normalizedSourcePath), 
								SafeAreaFileUtils.fileToLines(fileRepoThing.openFileForRead(targetName + "/" + normalizedSourcePath)));
						String reportString = report.getCodeDiffReport();
						fileRepoThing.CreateTextFile(targetName + "/DIFF_REPORT/" + normalizedSourcePath+"_report.html",
								reportString, true);
						listOfAllTargetFiles.append(","+targetName + "/DIFF_REPORT/" + normalizedSourcePath+"_report.html");
					}
				}
				if(generateReport.booleanValue())
				{
					String reportString = ReportGenerator.getDiffTOC(allSourceEntries);
					fileRepoThing.CreateTextFile(targetName + "/DIFF_REPORT/ToC_report.html",
							reportString, true);
					listOfAllTargetFiles.append(","+targetName + "/DIFF_REPORT/ToC_report.html");
				}
				fileRepoThing.CreateZipArchive(targetName + ".zip", "/", listOfAllTargetFiles.toString());
				InfoTable zipFilesForDownload = fileRepoThing.GetFileListingWithLinks("/", targetName + ".zip");
				for (int i = 0; i < zipFilesForDownload.getLength(); i++)
				{
					ValueCollection vc = zipFilesForDownload.getRow(i);
					String name = vc.getValue("name").toString();
					if (name.equals(targetName + ".zip")){
						result = vc.getValue("downloadLink").toString();
					}
				}
			}
		} catch (FileNotFoundException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		_logger.trace("Exiting Service: getTargetSourceCode");
		return result;
	}

	private String findTargetResource(String sourceFilePath, String targetProduct) throws IOException
	{
		String downloadableFile = null;
		String resourceName = sourceFilePath.substring(sourceFilePath.lastIndexOf("/")+1);
		String comparableSourcePath;
		if (sourceFilePath.startsWith("src/")){
			comparableSourcePath = sourceFilePath.substring(3);
		} 
		else if (sourceFilePath.startsWith("codebase/")){
			comparableSourcePath = sourceFilePath.substring(8);
		}
		else{
			comparableSourcePath = "/" + sourceFilePath;
		}
		System.out.println("Looking for " + comparableSourcePath);
		String openGrokURL = product_codeRepo.get(targetProduct);
		String grokUrl = openGrokURL + "search?q=&project=" + targetProduct + "&defs=&refs=&path=" + resourceName
				+ "&hist=";
		Document page = Jsoup.connect(grokUrl).get();
		Element results = page.select("div#results").first();
		Element table = results.select("table").first();
		if (table == null){
			return null;
		}
		Iterator<Element> resultRows = table.select("tr").iterator();
		while (resultRows.hasNext())
		{
			Element tr = resultRows.next();
			Element a = tr.select("a").first();
			if (a != null)
			{
				String href = a.attr("href");
				String targetResourceName = href.substring(href.indexOf("/src"));
				if (targetResourceName.startsWith("/src_web/"))
				{
					if (targetResourceName.substring(8).equals(comparableSourcePath)){
						downloadableFile = openGrokURL + "raw/" + href.substring(href.indexOf(targetProduct.replaceAll(" ", "%20")));
						return downloadableFile;
					}
				} 
				else if (targetResourceName.startsWith("/src/"))
				{
					if (targetResourceName.substring(4).equals(comparableSourcePath)){
						downloadableFile = openGrokURL + "raw/" + href.substring(href.indexOf(targetProduct.replaceAll(" ", "%20")));
						return downloadableFile;
					}
				}
			}
		}
		return downloadableFile;
	}
}
