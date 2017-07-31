package com.ptc.ssp.wtsafeareamerger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public class ReportGenerator
{
	private static String CSS;
	private File original;
	private File revised;
	private List<String> originalFileLines;
	private List<String> revisedFileLines;
	
	static{
		CSS = "/* Default heading font (outside of tables) */\r\n" + 
				"body { font-family: sans-serif; font-size: 11pt; }\r\n" + 
				"\r\n" + 
				"td { vertical-align: top; padding-left: 4px; padding-right: 4px; }\r\n" + 
				"\r\n" + 
				"/* File Difference Report styles - Color */\r\n" + 
				"table.fc { border-top: 1px solid Black; border-left: 1px solid Black; width: 100%; font-family: monospace; font-size: 10pt; }\r\n" + 
				"\r\n" + 
				"tr.secBegin td { border-left: none; border-top: none; border-right: 1px solid Black; }\r\n" + 
				"tr.secMiddle td { border-left: none; border-top: none; border-right: 1px solid Black; }\r\n" + 
				"tr.secEnd td { border-left: none; border-top: none; border-bottom: 1px solid Black; border-right: 1px solid Black; }\r\n" + 
				"tr.secAll td { border-left: none; border-top: none; border-bottom: 1px solid Black; border-right: 1px solid Black; }\r\n" + 
				"tr.secSubEnd td { border-left: none; border-top: none; border-bottom: 1px solid Gray; border-right: 1px solid Black; }\r\n" + 
				"tr.secSubAll td { border-left: none; border-top: none; border-bottom: 1px solid Gray; border-right: 1px solid Black; }\r\n" + 
				"tr.secGap td { font-size: 4px; border-left: none; border-top: none; border-bottom: 1px solid Black; border-right: 1px solid Black; }\r\n" + 
				"\r\n" + 
				"td.LineNum { text-align: right; }\r\n" + 
				"td.LineRange { font-family: sans-serif; }\r\n" + 
				"td.SubLineRange { font-family: sans-serif; border-bottom: none; }\r\n" + 
				"\r\n" + 
				"td.AlignLeft { text-align: left; }\r\n" + 
				"td.AlignRight { text-align: right; }\r\n" + 
				"td.AlignCenter { text-align: center; }\r\n" + 
				"td.Caption { text-align: left; background-color: #E7E7E7; padding-top: 8px; }\r\n" + 
				"\r\n" + 
				"td.Normal { }\r\n" + 
				"td.HasSimilar { background-color: #F0F0FF; }\r\n" + 
				"td.HasMismatch { background-color: #FFF0F0; }\r\n" + 
				"td.HasOrphan { background-color: #FFF0F0; }\r\n" + 
				"td.Added { background-color: #FFF0F0; }\r\n" + 
				"td.Deleted { background-color: #FFF0F0; text-decoration: line-through; }\r\n" + 
				"td.UAdded { background-color: #F0F0FF; }\r\n" + 
				"td.UDeleted { background-color: #F0F0FF; text-decoration: line-through; }\r\n" + 
				"\r\n" + 
				".ttSigDiff { color: #FF0000; }\r\n" + 
				".ttInsigDiff { color: #0000FF; }\r\n" + 
				".ttAdded { color: #FF0000; }\r\n" + 
				".ttDeleted { text-decoration: line-through; }\r\n" + 
				"\r\n" + 
				"/* Directory Comparison Report styles - Color */\r\n" + 
				"table.dc { border-top: 1px solid Black; border-left: 1px solid Black; width: 100%; font-family: sans-serif; font-size: 10pt; }\r\n" + 
				"\r\n" + 
				"table.dc tr.secBegin td { border-bottom: 1px solid Silver; }\r\n" + 
				"table.dc tr.secMiddle td { border-bottom: 1px solid Silver; }\r\n" + 
				"\r\n" + 
				".ttNewer { color: #FF0000; }\r\n" + 
				".ttOlder { color: #808080; }\r\n" + 
				".ttOrphan { color: #0000FF; }\r\n" + 
				".ttGhosted { color: #008080; }";
	}
	
	public ReportGenerator(File original, File revised)
	{
		this.original=original;
		this.revised=revised;
	}
	
	public ReportGenerator(List<String> originalFileLines, List<String> revisedFileLines)
	{
		this.originalFileLines=originalFileLines;
		this.revisedFileLines=revisedFileLines;
	}
	
	public String getCodeDiffReport() throws IOException
	{
		StringBuffer htmlReport = new StringBuffer();
		htmlReport.append("<html>\n");
		htmlReport.append("<head>\n");
		htmlReport.append("<META http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">\n");
		htmlReport.append("<style>\n");
		htmlReport.append(CSS);
		htmlReport.append("</style>\n");
		htmlReport.append("</head>\n");
		htmlReport.append("<body>\n");
		//htmlReport.append(original.getName() + " Diff report\n");
		htmlReport.append("<h1>Diff report<h1>\n");
		htmlReport.append("<br> Left is source file, Right is target file\n");
		htmlReport.append("<br> &nbsp; &nbsp;\n");
		//htmlReport.append("<br> File: "+revised.getAbsolutePath()+"\n");
		htmlReport.append("<table class=\"fc\" cellspacing=\"0\" cellpadding=\"0\">\n");;

        Patch patch = DiffUtils.diff(originalFileLines, revisedFileLines);
        List<Delta> deltas = patch.getDeltas();
        for (Delta delta : deltas) 
        {
        	Chunk source = delta.getOriginal();
        	Chunk target = delta.getRevised();
        	int sourceLineNumber = source.getPosition() + 1;
    		int targetLineNumber = target.getPosition() + 1;
        	if(source.getLines().size()>1 || target.getLines().size() >1)
        	{	       		
        		htmlReport.append("<tr class=\"secBegin\">\n");
        		htmlReport.append(printDiff(
        				getLine(source.getLines(), 0), 
        				sourceLineNumber, 
        				getLine(target.getLines(), 0), 
        				targetLineNumber, 
        				delta.getType()));
        		htmlReport.append("</tr>\n");
        		if((source.getLines().size()>2))
        		{
	        		for(int i=1; i<source.getLines().size()-1; i++)
	        		{
	        			sourceLineNumber++;
	        			targetLineNumber++;
	        			htmlReport.append("<tr class=\"secMiddle\">\n");
	            		htmlReport.append(printDiff(
	            				getLine(source.getLines(), i), 
	            				sourceLineNumber, 
	            				getLine(target.getLines(), i), 
	            				targetLineNumber, 
	            				delta.getType()));
	            		htmlReport.append("</tr>\n");
	        		}
        		}
        		sourceLineNumber++;
    			targetLineNumber++;
        		htmlReport.append("<tr class=\"secEnd\">\n");
        		htmlReport.append(printDiff(
        				getLine(source.getLines(), source.getLines().size()-1), 
        				sourceLineNumber, 
        				getLine(target.getLines(), target.getLines().size()-1), 
        				targetLineNumber, 
        				delta.getType()));
        		htmlReport.append("</tr>\n");
        	}
        	else{
        		htmlReport.append("<tr class=\"secAll\">\n");
        		htmlReport.append(printDiff(
        				getLine(source.getLines(), 0), 
        				sourceLineNumber, 
        				getLine(target.getLines(), 0), 
        				targetLineNumber, 
        				delta.getType()));
        		htmlReport.append("</tr>\n");
        	}
        }
        htmlReport.append("</table>\n");
        htmlReport.append("</body>\n");
        htmlReport.append("</html>");
        return htmlReport.toString();
	}
	
	private String printDiff(String source, int sourceLineNumber, String target, int targetLineNumber, Delta.TYPE deltaType)
	{
		StringBuffer res = new StringBuffer();
		res.append("<td class=\"LineNum\">"+sourceLineNumber+"</td>");
		switch(deltaType){
    	case INSERT: //insert = revised has more lines
    		res.append("<td class=\"HasOrphan\">&nbsp;</td>");
    		res.append("<td class=\"AlignCenter\">&lt;&gt;</td>");
    		res.append("<td class=\"LineNum\">"+targetLineNumber+"</td>");
    		res.append("<td class=\"HasOrphan\">");
    		res.append("<span class=\"ttSigDiff\">"+txtToHtml(target)+"</span></td>");
    		break;
    	case CHANGE: //change = lines differs
    		String diffSourceTarget = org.apache.commons.lang.StringUtils.difference(source, target);
    		String diffTargetSource = org.apache.commons.lang.StringUtils.difference(target, source);
    		String sourceCommonStart = source.substring(0, source.indexOf(diffTargetSource));
    		String targetCommonStart = target.substring(0, target.indexOf(diffSourceTarget));
    		res.append("<td class=\"HasMismatch\">"+txtToHtml(sourceCommonStart));
    		res.append("<span class=\"ttSigDiff\">"+txtToHtml(diffTargetSource)+"</span></td>");
    		res.append("<td class=\"AlignCenter\">&lt;&gt;</td>");
    		res.append("<td class=\"LineNum\">"+targetLineNumber+"</td>");
    		res.append("<td class=\"HasMismatch\">"+txtToHtml(targetCommonStart));
    		res.append("<span class=\"ttSigDiff\">"+txtToHtml(diffSourceTarget)+"</span></td>");
    		break;
    	case DELETE: //delete = original has more lines
    		res.append("<td class=\"HasOrphan\">");
    		res.append("<span class=\"ttSigDiff\">"+txtToHtml(target)+"</span></td>");
    		res.append("<td class=\"AlignCenter\">&lt;&gt;</td>");
    		res.append("<td class=\"LineNum\">"+targetLineNumber+"</td>");
    		res.append("<td class=\"HasOrphan\">&nbsp;</td>");
    		break;
    	}
		return res.toString();
	}
	
	
	public static String getDiffTOC(ArrayList<String> listFiles) throws IOException
	{
		StringBuffer htmlReport = new StringBuffer();
		htmlReport.append("<html>\n");
		htmlReport.append("<head>\n");
		htmlReport.append("<META http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">\n");
		htmlReport.append("<style>\n");
		htmlReport.append(CSS);
		htmlReport.append("</style>\n");
		htmlReport.append("</head>\n");
		htmlReport.append("<body>\n");
		htmlReport.append("Diff report table of content<br><br>");
		htmlReport.append("<table class=\"fc\" cellspacing=\"0\" cellpadding=\"0\">\n");	
		
		for(String fileName:listFiles)
		{
			String sourcePath = fileName.replaceAll(Matcher.quoteReplacement(File.separator), "/");
			htmlReport.append("<tr><td>");
			int level = StringUtils.countMatches(sourcePath, "/");
			if(level == 0 ){//special case for root folders
				htmlReport.append(sourcePath);
			}
			else{
				htmlReport.append("");
				for(int i = 0; i < level; i++){
					htmlReport.append("&nbsp;&nbsp;&nbsp;&nbsp;");
				}
				if(fileName.contains(".")){
					htmlReport.append("|- <a href=\""+sourcePath+"_report.html"+"\">"+sourcePath.substring(sourcePath.lastIndexOf("/"))+"</a>");
				}
				else{
					String[] pathElements = sourcePath.split("/");
					if(sourcePath.endsWith("/")){
						htmlReport.append("|-"+pathElements[pathElements.length-1]);
					}
					else{
						htmlReport.append("|-"+sourcePath.substring(sourcePath.lastIndexOf("/")));
					}
				}
			}
			htmlReport.append("</td></tr>");
		}
		htmlReport.append("</table>");
		htmlReport.append("</body>\n");
        htmlReport.append("</html>");
		return htmlReport.toString();
	}
	
	
	/**
	 * Method necessary for INSERT and DELETE cases: The diff list is empty
	 * @param lines
	 * @param index
	 * @return
	 */
	private String getLine(List lines, int index)
	{
		if(index >= lines.size() || index < 0){
			return "";
		}
		else{
			return lines.get(index).toString();
		}
	}
	
	private String txtToHtml(String s) {
        StringBuilder builder = new StringBuilder();
        boolean previousWasASpace = false;
        for (char c : s.toCharArray()) {
            if (c == ' ') {
                if (previousWasASpace) {
                    builder.append("&nbsp;");
                    previousWasASpace = false;
                    continue;
                }
                previousWasASpace = true;
            } else {
                previousWasASpace = false;
            }
            switch (c) {
                case '<':
                    builder.append("&lt;");
                    break;
                case '>':
                    builder.append("&gt;");
                    break;
                case '&':
                    builder.append("&amp;");
                    break;
                case '"':
                    builder.append("&quot;");
                    break;
                case '\n':
                    builder.append("<br>");
                    break;
                // We need Tab support here, because we print StackTraces as HTML
                case '\t':
                    builder.append("&nbsp; &nbsp; &nbsp;");
                    break;
                default:
                    builder.append(c);

            }
        }
        String converted = builder.toString();
        
        return converted;
    }
}
