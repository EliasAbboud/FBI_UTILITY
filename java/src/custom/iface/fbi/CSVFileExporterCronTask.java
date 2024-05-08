package custom.iface.fbi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.table.DefaultTableModel;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.tivoli.maximo.script.ScriptCache;
import com.ibm.tivoli.maximo.script.ScriptDriverFactory;
import com.ibm.tivoli.maximo.script.ScriptEngineContext;
import com.ibm.tivoli.maximo.script.ScriptInfo;

import ilog.views.util.data.IlvCSVReader;
import psdi.app.system.CrontaskParamInfo;
import psdi.mbo.Mbo;
import psdi.mbo.MboConstants;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.mbo.MboValue;
import psdi.mbo.MboValueInfo;
import psdi.server.CronTask;
import psdi.server.MXServer;
import psdi.server.SimpleCronTask;
import psdi.util.MXApplicationException;
import psdi.util.MXException;
import psdi.util.MXFormat;
import psdi.util.StringUtility;


public class CSVFileExporterCronTask extends SimpleCronTask implements CronTask {

	public static final String DEFAULT_DIR_DATE_FORMAT= "yyyyMMddHHmmssSSS";
	public static final String DEFAULT_FILE_DATE_FORMAT= "yyyyMMddHHmmssSSS";
	public static final String OUTPUT_FILE_EXTENSION = ".csv";
	public static final String CHARACTER_ENCODING = "UTF-8";
	public static final int BUFFER_SIZE = 4096;

	private Date runDate = null;
	private MboSetRemote mboSet = null;
	private String objectName="";
	private String whereClause = "";
	private String outputMainDir = "";
	private String outputSubDirPrefix = "";
	private String outputSubDirDateFormat = "";
	private boolean outputHasSubDir = false;
	private String outputSubDir = "";
	private boolean outputSubDirCompress = false;
	private String processedFlagAttribute = "";
	private String processedDateAttribute = "";
	private int ouputFilesCount = 1;
	private JSONArray ouputFilesDescriptors = null;
	private String mboNumFieldName = "";
	private String valueDateFormat;
	private String valueTimeFormat;
	private String valueDateTimeFormat;
	private boolean generateMergedZip  = false;
	

	/*
	 * Sample files descriptor for 2 output files
[
	{
		"filenamePrefix": "H_",
		"fileDateFormat": "yyyyMMddHHmmssSSS",
		"columnTitles": ["COL1","COL2","COL3","COL4","COL5"],
		"linesCount": "2",
		"linesDescriptor": [
			["ASSETNUM", "ASSET.LOCATION", "NULL", "PREFIX>ASSET.LOCATION", "ASSET.LOCATION<SUFFIX"],
			["ASSETNUM", "ASSET.LOCATION", "NULL", "PREFIX>ASSET.LOCATION", "ASSET.LOCATION<SUFFIX"]
		],
		"fileOperation": "newFile"
	},
	{
		"filenamePrefix": "H_",
		"fileDateFormat": "yyyyMMddHHmmssSSS",
		"columnTitles": [],
		"linesCount": "1",
		"linesDescriptor": [
			["ASSETNUM", "ASSET.LOCATION", "NULL", "PREFIX>ASSET.LOCATION", "ASSET.LOCATION<SUFFIX", "PREFIX>ASSET.LOCATION<SUFFIX"]
		],
		"fileOperation": "newFile"
	}
]
	 * */



	@Override 
	public void cronAction() {
		try {
			runDate = MXServer.getMXServer().getDate();
			validateParameters();
			
			fetchMboSet();			
			if (mboSet==null || mboSet.isEmpty())
				return;
			
			File outputDir = getOutputDir();

			StringBuffer[] outputSb = new StringBuffer[ouputFilesCount]; 
			ArrayList<String>[] columnTitles = new ArrayList[ouputFilesCount];
			HashMap<Integer, String[]>[] linesDescriptors = new HashMap[ouputFilesCount];
			HashSet<Long>[] fbiLoggerMap = new HashSet[ouputFilesCount];
			HashMap<Long, String> fbiLoggerMboRecKeyValues = new HashMap<Long, String>();
			HashMap<Long, String> fbiLoggerMboRecOrgId = new HashMap<Long, String>();
			//HashMap<String,String> dataMap = new HashMap<String,String>();

			for (int f = 0;f<ouputFilesCount;f++) {
				JSONObject outputFileDescriptor = (JSONObject) ouputFilesDescriptors.get(f);
				if (outputFileDescriptor==null) {
					throw new MXApplicationException("fileExporterCron", "outputFilesDescriptorsMissing");
				}

				outputSb[f] = new StringBuffer();
				fbiLoggerMap[f] = new HashSet<Long>();
				
				String[] fileColumnTitles = getColumnTitles(outputFileDescriptor);
				ArrayList<String> fileColumnTitlesAL = new ArrayList<String>();
				if (fileColumnTitles!=null && fileColumnTitles.length>0) {
					for (int a=0;a<fileColumnTitles.length;a++) {
						String fileColumnTitle = fileColumnTitles[a];
						fileColumnTitlesAL.add(fileColumnTitle);
						emitCell(fileColumnTitle, outputSb[f]);
						if (a<fileColumnTitles.length-1)
							emitComma(outputSb[f]);
					}
					endRow(outputSb[f]);
				}
				columnTitles[f] = fileColumnTitlesAL;



				HashMap<Integer, String[]> fileLinesDescriptors = new HashMap<Integer, String[]>();
				int linesCount = getLinesCount(outputFileDescriptor);
				for (int l=0;l<linesCount;l++) {
					fileLinesDescriptors.put(l, getLineAttributes(outputFileDescriptor, l));
				}

				linesDescriptors[f] = fileLinesDescriptors;

			}



			// write the data to string buffer
			MboRemote mbo = null;
			for (int i=0;(mbo = mboSet.getMbo(i))!=null;i++) {
				try {
					for (int f = 0;f<ouputFilesCount;f++) {
						HashMap<Integer, String[]> fileLinesDescriptors = linesDescriptors[f];
						for (Entry<Integer, String[]> fileLineDescriptor:fileLinesDescriptors.entrySet()) {
							int lineNum = fileLineDescriptor.getKey();
							String[] attributes = fileLineDescriptor.getValue();
							for (int a=0;a<attributes.length;a++) {
								String attribute = attributes[a];
								String strData = getValueAsString(mbo, attribute, i);
								emitCell(strData, outputSb[f]);
								
								//dataMap.put(f+","+i+","+lineNum+","+a, strData);
								
								if (a<attributes.length-1)
									emitComma(outputSb[f]);
							}
							endRow(outputSb[f]);
						}
						
						//Added to handle views or objects with no UniqueId defined
						long uniqueIndex = mbo.getUniqueIDValue();
						if (uniqueIndex==0) {
							uniqueIndex = -System.currentTimeMillis();
						}
						//End Of handle views or objects with no UniqueId defined
						
						fbiLoggerMap[f].add(uniqueIndex);
						if (!StringUtility.isEmpty(mboNumFieldName) && !mbo.isNull(mboNumFieldName))
							fbiLoggerMboRecKeyValues.put(uniqueIndex, mbo.getString(mboNumFieldName));
						
						String mboOrgId=getOrgId(mbo);
						if (!StringUtility.isEmpty(mboOrgId))
							fbiLoggerMboRecOrgId.put(uniqueIndex, mboOrgId);
					}
					markAsProcessed(mbo);
				} catch (Exception mboEx) {
					//mboEx.printStackTrace();
					//getCronTaskLogger().error(mboEx);
					throw mboEx; //not writing anything if any mbo export fails
				}
			}
			
			try {
				//save mboset if needed
				if (mboSet.toBeSaved())
					mboSet.save(MboConstants.NOACCESSCHECK);
			} catch (Exception mboSaveEx) {
				//mboSaveEx.printStackTrace();
				//getCronTaskLogger().error(mboSaveEx);
				throw mboSaveEx; //not writing anything if any mbo export fails
			}
			

			

			// write and close output files
			String[] outputFilenames = new String[ouputFilesCount];
			FileOutputStream[] outputFos = new FileOutputStream[ouputFilesCount];
			try {	
				for (int f = 0;f<ouputFilesCount;f++) {
					JSONObject outputFileDescriptor = (JSONObject) ouputFilesDescriptors.get(f);
					if (outputFileDescriptor==null) {
						throw new MXApplicationException("fileExporterCron", "outputFilesDescriptorsMissing");
					}
					String filename = getFilename(outputFileDescriptor);
					String fullFilename = outputDir.getAbsolutePath() + "/" + filename + OUTPUT_FILE_EXTENSION;

					FileOutputStream fos = null;
					File outputFile = new File(fullFilename);
					if (outputFile.exists()) {
						String fileOperation = getFileOperation(outputFileDescriptor);
						if ("A".equalsIgnoreCase(fileOperation)) {
							fos = new FileOutputStream(outputFile, true);
						} else if ("O".equalsIgnoreCase(fileOperation)) {
							fos = new FileOutputStream(outputFile);
						} else {
							int suffixInt = 0;
							do {
								suffixInt++;
								fullFilename = outputDir.getAbsolutePath() + "/" + filename + "_" + suffixInt + OUTPUT_FILE_EXTENSION;
								outputFile = new File(fullFilename);
							} while (outputFile.exists());
							fos = new FileOutputStream(outputFile);
						}
					} else {
						fos = new FileOutputStream(outputFile);
					}

					outputFilenames[f] = fullFilename;
					outputFos[f] = fos;
					byte[] byteBuffer = outputSb[f].toString().getBytes(Charset.forName(CHARACTER_ENCODING));
					fos.write(byteBuffer);
					fos.flush();
					fos.close();

				}
				
				//handle compression
				if (outputHasSubDir && outputSubDirCompress) {
					zip(outputFilenames, outputSubDir+".zip");
					
					for (int f = 0;f<ouputFilesCount;f++) {
						Thread.sleep(1000);
						Files.deleteIfExists(Paths.get(outputFilenames[f]));
					}
					Thread.sleep(1000);
					Files.deleteIfExists(Paths.get(outputSubDir));
				}
				
				dbSaveFbiLoggerFiles(outputFilenames, fbiLoggerMap, outputSb, fbiLoggerMboRecKeyValues,fbiLoggerMboRecOrgId);
				
			} catch (Exception outputFileEx) {
				//outputFileEx.printStackTrace();
				//getCronTaskLogger().error(outputFileEx);
				for (int f = 0;f<ouputFilesCount;f++) {
					if (outputFos[f]!=null) {
						outputFos[f].close();
					}
					Thread.sleep(1000);
					if (!StringUtility.isEmpty(outputFilenames[f])) {
						Files.deleteIfExists(Paths.get(outputFilenames[f]));
					}
				}
				Thread.sleep(1000);
				Files.deleteIfExists(Paths.get(outputSubDir));
				Thread.sleep(1000);
				Files.deleteIfExists(Paths.get(outputSubDir+".zip"));
				
				throw outputFileEx; //not writing anything if log fails
			}
			
			
			
			
			if (outputHasSubDir && outputSubDirCompress && generateMergedZip && ouputFilesCount>1 && !StringUtility.isEmpty((outputSb[0].toString()))) {
				try {

					StringBuffer mergedSb = generateMergedFile(outputSb);
					if (mergedSb!=null && mergedSb.length()>0) {
						String mergedFileName = outputSubDir+".merged.csv"; //change name as needed
						FileOutputStream fos = new FileOutputStream(new File(mergedFileName));//overwrite existing
						byte[] byteBuffer = mergedSb.toString().getBytes(Charset.forName(CHARACTER_ENCODING));
						fos.write(byteBuffer);
						fos.flush();
						fos.close(); 
						zip(new String[] {mergedFileName}, outputSubDir+".merged.zip");
					}

				} catch (Exception mergedOutputFileEx) {
					throw mergedOutputFileEx; //not writing anything if merge fails
				}
			}


		} catch (Exception e) {
			//e.printStackTrace();
			//getCronTaskLogger().error(e);
			dbLogError(e);
		}

	}


	private String getOrgId(MboRemote mbo) throws RemoteException, MXException {
		if (mbo==null)
			return null;
		if (mbo.getThisMboSet().getMboSetInfo().getAttribute("SITEID")!=null && !mbo.isNull("SITEID")) {
			return MXServer.getMXServer().getOrganization(mbo.getString("SITEID"));
		} else if (mbo.getThisMboSet().getMboSetInfo().getAttribute("ORGID")!=null && !mbo.isNull("ORGID")) {
			return mbo.getString("ORGID");
		} else {
			return null;
		}
	}


	private StringBuffer generateMergedFile(StringBuffer[] outputSb) throws MXApplicationException {
		StringBuffer mergedOutPutSB = new StringBuffer();
		String[] referenceFileLines = outputSb[0].toString().split("\n");
		int firstDataRow = 0;
		
		JSONObject outputFileDescriptor = (JSONObject) ouputFilesDescriptors.get(0);
		if (outputFileDescriptor==null) {
			throw new MXApplicationException("fileExporterCron", "outputFilesDescriptorsMissing");
		}
		
		String[] fileColumnTitles = getColumnTitles(outputFileDescriptor);
		if (fileColumnTitles!=null && fileColumnTitles.length>0) {
			firstDataRow=1;
		}
		
		for (int r=firstDataRow;r<referenceFileLines.length;r++) {
			String seqNum= readFirstColumn(referenceFileLines[r]);
			
			mergedOutPutSB.append(removeFirstColumn(referenceFileLines[r]));
			endRow(mergedOutPutSB);
			
			
			for (int f = 1;f<ouputFilesCount;f++) {
				String[] linesToWrite = getLinesForSeqNum(outputSb[f],seqNum);
				if (linesToWrite!=null && linesToWrite.length>0) {
					for (int l=0;l<linesToWrite.length;l++) {
						mergedOutPutSB.append(removeFirstColumn(linesToWrite[l]));
						endRow(mergedOutPutSB);
					}
				}
			}
		}
		
		return mergedOutPutSB;
	}
	
	private String[] getLinesForSeqNum(StringBuffer fileStringBuffer, String seqNum) {

		if (fileStringBuffer == null || fileStringBuffer.length()<=0)
			return null;
		if (StringUtility.isEmpty(seqNum))
			return null;
		
		String fileStrData = fileStringBuffer.toString();
		String[] linesInFile = fileStrData.split("\n");
		
		if (linesInFile == null || linesInFile.length<=0)
			return null;
		
		ArrayList<String> linesToReturn = new ArrayList<String>();
		for (int l=0;l<linesInFile.length;l++) {
			String lineSeqNum = readFirstColumn(linesInFile[l]);
			if (seqNum.equals(lineSeqNum)) {
				linesToReturn.add(removeFirstColumn(linesInFile[l]));
			}
		}
		return linesToReturn.toArray(new String[] {}); 
	}

	private String removeFirstColumn(String strCsvRow) {
		//sequence is assumed to be at Column 0
		String[] columns = strCsvRow.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
		if (columns!=null && columns.length>1) {
			StringBuffer sb = new StringBuffer();
			for (int c=1;c<columns.length;c++) {
				emitCell(columns[c], sb);
				if (c<columns.length-1)
					emitComma(sb);
			}
			return sb.toString();
		} else {
			return "";
		}

	}

	private String readFirstColumn(String strCsvRow) {
		//sequence is assumed to be at Column 0
		String[] columns = strCsvRow.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
		if (columns!=null && columns.length>0)
			return columns[0];
		else 
			return "";
	}

	private void dbLogError(Exception e) {
		try {
			if (e==null)
				return;
			
			String strToLog = e.getMessage();
			if (StringUtility.isEmpty(strToLog)) 
				throw e;
			

			
			MboSetRemote fbiLoggerFilesMboSet = MXServer.getMXServer().getMboSet("FBILOGGERFILE", getRunasUserInfo()); 
			
			MboValueInfo mbvi = fbiLoggerFilesMboSet.getMboSetInfo().getAttribute("ERRORMSG");
			if (mbvi==null) {
				fbiLoggerFilesMboSet.close();
				throw e;
			}
			
			int maxLen = mbvi.getLength();
			if (maxLen<strToLog.length())
				strToLog = strToLog.substring(0,  mbvi.getLength());
			
			MboRemote fbiLoggerFileMbo = fbiLoggerFilesMboSet.add(MboConstants.NOACCESSCHECK);
			fbiLoggerFileMbo.setValue("FILETYPE", "ERR", MboConstants.NOACCESSCHECK);
			fbiLoggerFileMbo.setValue("EXPORTDATE", runDate, MboConstants.NOACCESSCHECK);
			fbiLoggerFileMbo.setValue("CRONTASKNAME", getCrontaskInstance().getString("CRONTASKNAME"), MboConstants.NOACCESSCHECK);
			fbiLoggerFileMbo.setValue("INSTANCENAME", getCrontaskInstance().getString("INSTANCENAME"), MboConstants.NOACCESSCHECK);
			fbiLoggerFileMbo.setValue("ERRORMSG", strToLog, MboConstants.NOACCESSCHECK);
			
			if (fbiLoggerFilesMboSet.toBeSaved())
				fbiLoggerFilesMboSet.save(MboConstants.NOACCESSCHECK);
			
		} catch(Exception errLogEx) {
			errLogEx.printStackTrace();
			getCronTaskLogger().error(errLogEx);
			e.printStackTrace();
			getCronTaskLogger().error(e);
		}

		
	}

	private void dbSaveFbiLoggerFiles(String[] fileNames, HashSet<Long>[] loggerMap, StringBuffer[] stringBuffers, HashMap<Long, String> mboRecKeyValues, HashMap<Long, String> mboRecOrgId) throws RemoteException, MXException {

		
		MboSetRemote fbiLoggerFilesMboSet = MXServer.getMXServer().getMboSet("FBILOGGERFILE", getRunasUserInfo()); 
		for (int f = 0;f<ouputFilesCount;f++) {
			MboRemote fbiLoggerFileMbo = fbiLoggerFilesMboSet.add(MboConstants.NOACCESSCHECK);
			long fbiLoggerMboId = fbiLoggerFileMbo.getUniqueIDValue();
			fbiLoggerFileMbo.setValue("FILETYPE", "CSV", MboConstants.NOACCESSCHECK);
			fbiLoggerFileMbo.setValue("FILENAME", fileNames[f], MboConstants.NOACCESSCHECK);
			byte[] byteBuffer = stringBuffers[f].toString().getBytes(Charset.forName(CHARACTER_ENCODING));
			fbiLoggerFileMbo.setValue("FILEDATA", byteBuffer, MboConstants.NOACCESSCHECK);
			fbiLoggerFileMbo.setValue("EXPORTDATE", runDate, MboConstants.NOACCESSCHECK);
			fbiLoggerFileMbo.setValue("CRONTASKNAME", getCrontaskInstance().getString("CRONTASKNAME"), MboConstants.NOACCESSCHECK);
			fbiLoggerFileMbo.setValue("INSTANCENAME", getCrontaskInstance().getString("INSTANCENAME"), MboConstants.NOACCESSCHECK);
			
			MboSetRemote fbiLoggerMboSet = fbiLoggerFileMbo.getMboSet("FBILOGGER"); 
			Long[] loggerMapForFile = loggerMap[f].toArray(new Long[] {});
			

			
			if (loggerMapForFile!=null && loggerMapForFile.length>0) {
				for (int i=0;i<loggerMapForFile.length;i++) {
					MboRemote fbiLoggerMbo = fbiLoggerMboSet.add(MboConstants.NOACCESSCHECK);
					fbiLoggerMbo.setValue("FBILOGGERFILEID", fbiLoggerMboId, MboConstants.NOACCESSCHECK);
					fbiLoggerMbo.setValue("FILEEXPORTED", "Y", MboConstants.NOACCESSCHECK);
					fbiLoggerMbo.setValue("EXPORTDATE", runDate, MboConstants.NOACCESSCHECK);
					
					fbiLoggerMbo.setValue("CRONTASKNAME", getCrontaskInstance().getString("CRONTASKNAME"), MboConstants.NOACCESSCHECK);
					fbiLoggerMbo.setValue("INSTANCENAME", getCrontaskInstance().getString("INSTANCENAME"), MboConstants.NOACCESSCHECK);
					fbiLoggerMbo.setValue("OWNERTABLE", objectName, MboConstants.NOACCESSCHECK);
					fbiLoggerMbo.setValue("OWNERID", loggerMapForFile[i], MboConstants.NOACCESSCHECK);
					
					//MBONUM
					if (!StringUtility.isEmpty(mboNumFieldName)) {
						String recKeyVal = mboRecKeyValues.get(loggerMapForFile[i]);
						if (!StringUtility.isEmpty(recKeyVal))
							fbiLoggerMbo.setValue("MBONUM", recKeyVal, MboConstants.NOACCESSCHECK);
						
					}
					
					//ORGID
					String mboOrgId=mboRecOrgId.get(loggerMapForFile[i]);
					if (!StringUtility.isEmpty(mboOrgId))
						fbiLoggerMbo.setValue("ORGID", mboOrgId, MboConstants.NOACCESSCHECK);
				}
			}

			
		}
		
		if (fbiLoggerFilesMboSet.toBeSaved())
			fbiLoggerFilesMboSet.save(MboConstants.NOACCESSCHECK);
		
		
	}

	private String getValueAsString(MboRemote mbo, String attributename, int recNum) throws RemoteException, MXException {
		if (StringUtility.isEmpty(attributename) || "NULL".equalsIgnoreCase(attributename)) 
			return "";
		
		String preffix="";
		String suffix="";
		String realAttributename = attributename;
		if (realAttributename.indexOf(">")>0) {
			preffix = realAttributename.substring(0,realAttributename.indexOf(">")) ; //has preffix
			realAttributename = realAttributename.substring(realAttributename.indexOf(">")+1) ;
		}
		if (attributename.indexOf("<")>0) {
			suffix = realAttributename.substring(realAttributename.indexOf("<")+1) ;  //has suffix
			realAttributename = realAttributename.substring(0,realAttributename.indexOf("<")) ;
		}
		if (StringUtility.isEmpty(realAttributename)) {
			return preffix+suffix;
		} else {
			if (realAttributename.indexOf("$")==0) {
				//this is a script function call
				String scriptName = realAttributename.substring(realAttributename.indexOf("$") + 1, realAttributename.indexOf("("));
				String functionName = realAttributename.substring(realAttributename.indexOf("(") + 1, realAttributename.indexOf(","));
				String csvArgs = realAttributename.substring(realAttributename.indexOf("[") + 1, realAttributename.indexOf("]"));
				return preffix+invokeScriptFunction(mbo,scriptName,functionName,csvArgs,recNum)+suffix;
			} else {
				MboValue mbv = getMboValue((Mbo) mbo, realAttributename);
				return preffix+getMboValueAsString(mbv)+suffix; 
			}

		}


		
	}
	
	private String invokeScriptFunction(MboRemote mbo, String scriptName, String functionName, String csvArgs, int recNum)  {
		try {
			if (StringUtility.isEmpty(scriptName))
				return "";
			if (StringUtility.isEmpty(functionName))
				return "";
			String aScriptName = scriptName.toUpperCase();
			ScriptInfo scriptInfo = ScriptCache.getInstance().getScriptInfo(aScriptName);
			if (scriptInfo==null) //script does not exist
				return "";
			Object[] args;
			if (StringUtility.isEmpty(csvArgs)) {
				args = new Object[] {mbo};
			} else {
				String[] strArgs = csvArgs.split(",");
				args = new Object[strArgs.length+1];
				args[0] = mbo;
				for (int i = 1; i<args.length;i++) {
					args[i]=resolveArg(strArgs[i-1],recNum);
				}
			}
			

			HashMap<String, Object> context = new HashMap<String, Object>();
			context.put("invokeFunction", functionName);
			context.put("invokeArgs", args);
			ScriptEngineContext scrThreadContext = ScriptEngineContext.getCurrentContext();
			boolean contextOwner = false;
			try {
				if (scrThreadContext == null) {
					contextOwner = true;
					scrThreadContext = ScriptEngineContext.createCurrentContext();
				}
				ScriptDriverFactory.getInstance().getScriptDriver(aScriptName).runScript(aScriptName, context);
				if (context.get("invokeResponse")==null)
					return "";
				return (String) context.get("invokeResponse");
			} finally {
				if (contextOwner) {
					ScriptEngineContext.destroyCurrentContext();
				}
				context.clear();
			}
		} catch (Exception e) {
			return "";
		}

	}

	private Object resolveArg(String argVal, int recNum) throws RemoteException, MXException {
		if (StringUtility.isEmpty(argVal))
			return "";
		if (StringUtility.isEmpty(argVal.trim()))
			return "";
		if (argVal.startsWith("$")) {
			return substituteArg(argVal.substring(argVal.indexOf("$") + 1).toUpperCase(), recNum); 
		} else {
			return argVal;
		}
	}

	private Object substituteArg(String paramName, int recNum) throws RemoteException, MXException {
		switch (paramName) {
		case "RUNDATE": return runDate;
		case "OBJECTNAME": return objectName;
		case "INSTANCENAME": return getCrontaskInstance().getString("INSTANCENAME");
		case "RECNUM": return recNum;
		default: return paramName;
		}
	}

	private String getMboValueAsString(MboValue mbv) throws MXException {
		if (mbv==null)
			return "";
		String retVal ="";
		String type = mbv.getMboValueInfo().getType();
		switch (type) {
		case "ALN":
		case "LONGALN":
		case "UPPER":
		case "LOWER":
		case "GL":
			retVal = mbv.getString();
			break;
		case "DATE":
			retVal = StringUtility.isEmpty(valueDateFormat)?MXFormat.dateToString(mbv.getDate()):(new SimpleDateFormat(valueDateFormat)).format(mbv.getDate());
			break;
		case "DATETIME":
			retVal = StringUtility.isEmpty(valueDateTimeFormat)?MXFormat.dateTimeToString(mbv.getDate()):(new SimpleDateFormat(valueDateTimeFormat)).format(mbv.getDate());
			break;
		case "TIME":
			retVal = StringUtility.isEmpty(valueTimeFormat)?MXFormat.timeToString(mbv.getDate()):(new SimpleDateFormat(valueTimeFormat)).format(mbv.getDate());
			break;
		case "SMALLINT":
		case "INTEGER":
			retVal = String.valueOf(mbv.getInt());
			break;
		case "BIGINT":
			retVal = String.valueOf(mbv.getLong());
			break;
		case "FLOAT":
			retVal = String.valueOf(mbv.getFloat());
			break;
		case "DECIMAL":
		case "DURATION":
		case "AMOUNT":
			retVal = String.valueOf(mbv.getDouble());
			break;
		case "YORN":
			retVal = mbv.getBoolean()?"1":"0";
			break;
		case "CRYPTO":
		case "CRYPTOX":
		case "CLOB":
		case "BLOB":
			//retVal = mbv.getString(); // not supported
			retVal = "";
			break;
		default:
			retVal = "";
			break;
		}
		return retVal;
	}

	private MboValue getMboValue(Mbo mbo, String attributename) {
		try {
			Mbo attMbo = (Mbo) mbo.getMboForAttribute(attributename);
			if (mbo == attMbo) {
				return mbo.getMboValue(attributename);
			} else {
				int index = attributename.indexOf(46);
				String newAttribname = index != -1 ? attributename.substring(index + 1) : attributename;
				return getMboValue(attMbo,newAttribname);
			}
		} catch (Exception e) {
			return null;
		}

	}

	private void endRow(StringBuffer buffer) {
		buffer.append("\n");
	}


	private void emitComma(StringBuffer buffer) {
		buffer.append(",");
	}

	private void emitCell(String stringValue, StringBuffer buffer) {
		int commaFound = stringValue.indexOf(",");
		int doubleQuoteFound = stringValue.indexOf("\"");
		if (commaFound > -1 && doubleQuoteFound > -1) {
			stringValue = stringValue.replace("\"", "\"\"");
			buffer.append("\"" + stringValue + "\"");
		} else if (commaFound > -1) {
			buffer.append("\"" + stringValue + "\"");
		} else {
			buffer.append(stringValue);
		}
	}
	

	private String getFileOperation(JSONObject outputFileDescriptor) {
		String outputfileOperation =(String) outputFileDescriptor.get("fileOperation");
		if (StringUtility.isEmpty(outputfileOperation)) {
			return "N";
		}

		String firstChar = outputfileOperation.substring(0, 1).toUpperCase();
		switch (firstChar) {
		case "A": return "A";
		case "O": return "O";
		case "N": return "N";
		default: return "N";
		}

	}

	private File getOutputDir() throws MXApplicationException {
		try {
			File folder = new File(outputMainDir);
			if (!folder.exists()) {
				boolean createSucceeded = folder.mkdirs();
				if (!createSucceeded)
					throw new MXApplicationException("fileExporterCron", "invalidOutputDir");
			}
			if (StringUtility.isEmpty(outputSubDirPrefix)) {
				return folder;
			}
			outputHasSubDir = true;
			outputSubDir = folder.getAbsolutePath()+"/"+outputSubDirPrefix+getOutputSubDirDateStr();
			File subFolder = new File(outputSubDir);
			if (!subFolder.exists()) {
				boolean createSucceeded = subFolder.mkdirs();
				if (!createSucceeded) 
					throw new MXApplicationException("fileExporterCron", "invalidOutputSubDir");
			}
			return subFolder;
		} catch (Exception e) {
			throw new MXApplicationException("fileExporterCron", "invalidOutputDir");
		}

	}



	private String[] getLineAttributes(JSONObject outputFileDescriptor, int line) {
		JSONArray linesAttributes = (JSONArray) outputFileDescriptor.get("linesDescriptor");
		if (linesAttributes == null || linesAttributes.size()<=0)
			return new String[] {};

		JSONArray lineAttributes = (JSONArray) linesAttributes.get(line);
		if (lineAttributes == null || lineAttributes.size()<=0)
			return new String[] {};
		return (String[]) lineAttributes.toArray(new String[lineAttributes.size()]);
	}

	private int getLinesCount(JSONObject outputFileDescriptor) {
		String linesCountStr = (String) outputFileDescriptor.get("linesCount");
		if (StringUtility.isEmpty(linesCountStr)) {
			return 1;
		}
		int retVal= 1; 
		try {
			retVal = Integer.parseInt(linesCountStr);
		} catch (NumberFormatException nfe) {
			return 1;
		}

		if (retVal<1)
			return 1;

		return retVal;
	}

	private String[] getColumnTitles(JSONObject outputFileDescriptor) {
		JSONArray columnTitlesArray = (JSONArray) outputFileDescriptor.get("columnTitles");
		if (columnTitlesArray==null || columnTitlesArray.size()<=0)
			return new String[] {};
		return (String[]) columnTitlesArray.toArray(new String[columnTitlesArray.size()]);
	}

	private String getFilename(JSONObject outputFileDescriptor) {

		String outputfileDateFormat =(String) outputFileDescriptor.get("fileDateFormat");

		if (StringUtility.isEmpty(outputfileDateFormat)) {
			outputfileDateFormat = DEFAULT_FILE_DATE_FORMAT;
		}
		if (outputFileDescriptor.get("filenamePrefix")==null) {
			return (new SimpleDateFormat(outputfileDateFormat)).format(runDate);
		} else {
			if (StringUtility.isEmpty((String) outputFileDescriptor.get("fileDateFormat"))) {
				return ((String)outputFileDescriptor.get("filenamePrefix"));
			} else {
				return ((String)outputFileDescriptor.get("filenamePrefix")) + (new SimpleDateFormat(outputfileDateFormat)).format(runDate);
			}
		}
	}

	private void markAsProcessed(MboRemote mbo) throws RemoteException, MXException {
		if (!StringUtility.isEmpty(processedFlagAttribute)) {
			mbo.setValue(processedFlagAttribute, "Y", MboConstants.NOACCESSCHECK);
		}
		if (!StringUtility.isEmpty(processedDateAttribute)) {
			mbo.setValue(processedDateAttribute, runDate, MboConstants.NOACCESSCHECK);
		}

	}

	private void validateParameters() throws MXApplicationException {
		if (StringUtility.isEmpty(objectName)) {
			throw new MXApplicationException("fileExporterCron", "invalidObjectName");
		}
		if (StringUtility.isEmpty(outputMainDir)) {
			throw new MXApplicationException("fileExporterCron", "invalidOutputDir"); 
		}
		if (ouputFilesCount<=0) {
			throw new MXApplicationException("fileExporterCron", "noOutputFilesRequested");
		}
		if (ouputFilesDescriptors == null || ouputFilesDescriptors.size() <= 0) {
			throw new MXApplicationException("fileExporterCron", "outputFilesDescriptorsMissing");
		}
		if (ouputFilesCount>0 && ouputFilesDescriptors.size() != ouputFilesCount) {
			throw new MXApplicationException("fileExporterCron", "outputFilesDescriptorsMismatch");
		}
	}

	private void fetchMboSet() throws RemoteException, MXException {
		mboSet = MXServer.getMXServer().getMboSet(objectName, getRunasUserInfo());
		if (!StringUtility.isEmpty(whereClause)) {
			mboSet.setWhere(whereClause);
		}
		mboSet.reset();
	} 





	private String getOutputSubDirDateStr() {
		if (StringUtility.isEmpty(outputSubDirDateFormat)) {
			outputSubDirDateFormat = DEFAULT_DIR_DATE_FORMAT;
		}
		return (new SimpleDateFormat(outputSubDirDateFormat)).format(runDate);
	}

	private void zip(String[] files, String destZipFile) throws FileNotFoundException, IOException {
        List<File> listFiles = new ArrayList<File>();
        for (int i = 0; i < files.length; i++) {
            listFiles.add(new File(files[i]));
        }
        zip(listFiles, destZipFile);
    }
	
	private void zip(List<File> listFiles, String destZipFile) throws FileNotFoundException, IOException {
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZipFile));
		for (File file : listFiles) {
			if (file.isDirectory()) {
				zipDirectory(file, file.getName(), zos);
			} else {
				zipFile(file, zos);
			}
		}
		zos.flush();
		zos.close();
	}
	
    private void zipDirectory(File folder, String parentFolder, ZipOutputStream zos) throws FileNotFoundException, IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                zipDirectory(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }
            zos.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            long bytesRead = 0;
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = bis.read(bytesIn)) != -1) {
                zos.write(bytesIn, 0, read);
                bytesRead += read;
            }
            zos.closeEntry();
            bis.close();
        }
    }
    
    private void zipFile(File file, ZipOutputStream zos) throws FileNotFoundException, IOException {
        zos.putNextEntry(new ZipEntry(file.getName()));
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        long bytesRead = 0;
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = bis.read(bytesIn)) != -1) {
            zos.write(bytesIn, 0, read);
            bytesRead += read;
        }
        zos.closeEntry();
        bis.close();
    }

	@Override
	public void init() throws MXException {
		super.init();
		loadConfig();

	}

	@Override
	public void start() {
		super.start();
		loadConfig();
	}

	@Override
	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException {
		try {
			String[] names = {"OBJECTNAME", "WHERECLAUSE", "OUTPUT_MAIN_FOLDER", "OUPUT_SUBFOLDER_PREFIX", "OUPUT_SUBFOLDER_DATE_FORMAT", "OUTPUT_SUBFOLDER_COMPRESS", 
					"PROCESSED_FLAG_ATTRIBUTE", "PROCESSED_DATE_ATTRIBUTE", "OUTPUT_FILES_COUNT", "OUTPUT_FILES_DESCRIPTORS_JSON","VALUE_DATE_FORMAT","VALUE_TIME_FORMAT","VALUE_DATETIME_FORMAT","GENERATE_MERGED_ZIP"};
			String[] defs = {"", "", "", "", "yyyyMMddHHmmssSSS", "N", "", "", "1", "", "yyyy/MM/dd", "HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "N" };
			String msgGroup ="fileExporterCron";
			
			CrontaskParamInfo[] ret = new CrontaskParamInfo[names.length];
			for (int i = 0; i < names.length; i++) {
				ret[i] = new CrontaskParamInfo();
				ret[i].setName(names[i]);
				ret[i].setDefault(defs[i]);
				ret[i].setDescription(msgGroup, names[i]);
			}
			return ret;
		} catch (Exception e) {
			if (getCronTaskLogger().isErrorEnabled()) {
				getCronTaskLogger().error(e);
			}
		}
		return null;
	}


	private void loadConfig() {

		try {

			objectName = getParamAsString("OBJECTNAME");
			whereClause = getParamAsString("WHERECLAUSE");
			outputMainDir = getParamAsString("OUTPUT_MAIN_FOLDER");
			outputSubDirPrefix = getParamAsString("OUPUT_SUBFOLDER_PREFIX");
			outputSubDirDateFormat = getParamAsString("OUPUT_SUBFOLDER_DATE_FORMAT");
			outputSubDirCompress = StringUtility.isEmpty(getParamAsString("OUTPUT_SUBFOLDER_COMPRESS")) ?false:getParamAsBoolean("OUTPUT_SUBFOLDER_COMPRESS");//getParamAsBoolean("OUTPUT_SUBFOLDER_COMPRESS");
			processedFlagAttribute = getParamAsString("PROCESSED_FLAG_ATTRIBUTE");
			processedDateAttribute = getParamAsString("PROCESSED_DATE_ATTRIBUTE");
			ouputFilesCount  = getParamAsInt("OUTPUT_FILES_COUNT");
			ouputFilesDescriptors = JSONArray.parse(getParamAsString("OUTPUT_FILES_DESCRIPTORS_JSON"));
			valueDateFormat = getParamAsString("VALUE_DATE_FORMAT");
			valueTimeFormat = getParamAsString("VALUE_TIME_FORMAT");
			valueDateTimeFormat = getParamAsString("VALUE_DATETIME_FORMAT");
			
			generateMergedZip = StringUtility.isEmpty(getParamAsString("GENERATE_MERGED_ZIP")) ?false:getParamAsBoolean("GENERATE_MERGED_ZIP");
			
			MboSetRemote mboNumDomValSet = MXServer.getMXServer().getMboSet("ALNDOMAIN", getRunasUserInfo());
			mboNumDomValSet.setWhere("DOMAINID='APPRECORD' and value='"+objectName+"'");
			mboNumDomValSet.reset();
			
			MboRemote mboNumDomValMbo = mboNumDomValSet.getMbo(0);
			if (mboNumDomValMbo!=null) {
				mboNumFieldName = mboNumDomValMbo.getString("DESCRIPTION");
				if (!StringUtility.isEmpty(mboNumFieldName)) {
					MboValueInfo mbvi = MXServer.getMXServer().getMboSet(objectName, getRunasUserInfo()).getMboSetInfo().getAttribute(mboNumFieldName);
					if (mbvi==null) {
						mboNumFieldName = "";
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			getCronTaskLogger().error(e);
		}

	}



}
