package custom.webclient.beans.fbi;

import java.io.File;
import java.rmi.RemoteException;
import java.util.HashSet;

import psdi.mbo.MboConstants;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.server.MXServer;
import psdi.util.MXApplicationException;
import psdi.util.MXException;
import psdi.util.StringUtility;
import psdi.webclient.system.beans.AppBean;
import psdi.webclient.system.beans.DataBean;

public class FbiLoggerAppBean extends AppBean {

	public void initializeApp() throws MXException, RemoteException {
		super.initializeApp();
		DataBean resultsBean = app.getResultsBean();
		resultsBean.setQbe("orgid", getUserInsertOrg());
		resultsBean.reset();
	}

	private String getUserInsertOrg() throws RemoteException {
		return MXServer.getMXServer().getOrganization(clientSession.getUserInfo().getInsertSite());
	}

	public int REEXPORT() throws RemoteException, MXException {
		MboRemote fbiLoggerMainMbo = getMbo();
		
		if (fbiLoggerMainMbo!=null) {
			if (fbiLoggerMainMbo.isNull("REEXPORTEDJUST")) {
				throw new MXApplicationException("fbilogger", "noJustification");
			}
			MboSetRemote fbiLoggerSet = fbiLoggerMainMbo.getMboSet("FBILOGGER");
			MboRemote fbiLoggerMbo = null;
			HashSet<String> filesToDelete = new HashSet<String>(); 
			for (int i=0;(fbiLoggerMbo=fbiLoggerSet.getMbo(i))!=null;i++) {
				String reExportFlag= "R";
				String fileToDelete = checkFileExists(fbiLoggerMbo);
				if (!StringUtility.isEmpty(fileToDelete)) {
					reExportFlag = "D";
					filesToDelete.add(fileToDelete);
				}
				fbiLoggerMbo.setValue("FILEEXPORTED", reExportFlag, MboConstants.NOACCESSCHECK);
				fbiLoggerMbo.setValue("REEXPORTED", "Y", MboConstants.NOACCESSCHECK);
				fbiLoggerMbo.setValue("REEXPORTEDBY", fbiLoggerMainMbo.getUserInfo().getPersonId(), MboConstants.NOACCESSCHECK);
				fbiLoggerMbo.setValue("REEXPORTEDDATE", MXServer.getMXServer().getDate(), MboConstants.NOACCESSCHECK);
				fbiLoggerMbo.setValue("REEXPORTEDJUST", fbiLoggerMainMbo.getString("REEXPORTEDJUST"), MboConstants.NOACCESSCHECK);
			}
			
			for (String fileToDelete:filesToDelete) {
				new File(fileToDelete).delete();
			}
			
			if (fbiLoggerSet.toBeSaved()) {
				fbiLoggerMainMbo.setValueNull("REEXPORTEDJUST",MboConstants.NOACCESSCHECK);
				SAVE();
			}
				
		}
		return EVENT_HANDLED;
	}

	private String checkFileExists(MboRemote fbiLoggerMbo) {
		try {
			String fileName = fbiLoggerMbo.getString("FBILOGGERFILE.FILENAME");
			if (StringUtility.isEmpty(fileName))
				return null;
			File file = new File(fileName); //check if the unzipped file exists
			if (file.exists()) {
				return file.getAbsolutePath();
			}
			
			File zipFile = new File(file.getParentFile().getAbsolutePath()+".zip");
			if (zipFile.exists()) {
				return zipFile.getAbsolutePath();
			}
			return null;
		} catch (Exception e) {
			return null;
		}
		
	}
	

}
