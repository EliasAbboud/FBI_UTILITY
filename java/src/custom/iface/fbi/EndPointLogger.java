package custom.iface.fbi;

import java.util.Date;

import com.ibm.json.java.JSONObject;

import psdi.iface.router.ScriptHTTPResp;
import psdi.mbo.MboConstants;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.server.MXServer;
import psdi.util.MXApplicationException;

/**
 * This class for logging all the needed information related to the calling of
 * any endpoint
 * 
 * @author rimon.koroni@maximo.ae
 * @since 09/11/2023
 *
 */
public class EndPointLogger {

	/**
	 * Logging all the needed informations related to the calling of any endpoint
	 * 
	 * @param resp           the response of the endpoint
	 * @param reqData        the request body
	 * @param crontaskName   the group name of the logging
	 * @param instanceName   the subgroup name of the logging
	 * @param mbonumRefName  the mbonum reference name in the message tracking data
	 *                       of the the logging
	 * @param ownerIdRefName the owner id reference name in the message tracking
	 *                       data of the logging
	 * @param ownerTable     the owner table of the logging
	 * 
	 * @author rimon.koroni@maximo.ae
	 * @since 09/11/2023
	 */
	public void log(ScriptHTTPResp resp, byte[] reqData, String crontaskName, String instanceName, String mbonumRefName,
			String ownerIdRefName, String ownerTable, String meaMessageId) {
		try {
			MXServer mxserver = MXServer.getMXServer();
			Date runDate = MXServer.getMXServer().getDate();
			MboSetRemote fbiLoggerFilesMboSet = mxserver.getMboSet("FBILOGGERFILE", mxserver.getSystemUserInfo());
			MboRemote fbiLoggerFileMbo = fbiLoggerFilesMboSet.add(MboConstants.NOACCESSCHECK);
			long fbiLoggerMboId = fbiLoggerFileMbo.getUniqueIDValue();
			fbiLoggerFileMbo.setValue("FILEDATA", reqData, MboConstants.NOACCESSCHECK);
			fbiLoggerFileMbo.setValue("EXPORTDATE", runDate, MboConstants.NOACCESSCHECK);
			fbiLoggerFileMbo.setValue("CRONTASKNAME", crontaskName, MboConstants.NOACCESSCHECK);
			fbiLoggerFileMbo.setValue("INSTANCENAME", instanceName, MboConstants.NOACCESSCHECK);
			fbiLoggerFileMbo.setValue("RESPONSEDATA", resp.getData(), MboConstants.NOACCESSCHECK);

			MboSetRemote fbiLoggerMboSet = fbiLoggerFileMbo.getMboSet("FBILOGGER");
			MboRemote fbiLoggerMbo = fbiLoggerMboSet.add(MboConstants.NOACCESSCHECK);
			fbiLoggerMbo.setValue("FBILOGGERFILEID", fbiLoggerMboId, MboConstants.NOACCESSCHECK);
			int code = resp.getResponseCode();
			fbiLoggerMbo.setValue("FILEEXPORTED", (code >= 200 && code <= 299) ? "Y" : "E", MboConstants.NOACCESSCHECK);
			fbiLoggerMbo.setValue("EXPORTDATE", runDate, MboConstants.NOACCESSCHECK);
			fbiLoggerMbo.setValue("CRONTASKNAME", crontaskName, MboConstants.NOACCESSCHECK);
			fbiLoggerMbo.setValue("INSTANCENAME", instanceName, MboConstants.NOACCESSCHECK);
			fbiLoggerMbo.setValue("OWNERTABLE", ownerTable, MboConstants.NOACCESSCHECK);
			fbiLoggerMbo.setValue("MEAMSGID", meaMessageId, MboConstants.NOACCESSCHECK);
			fbiLoggerMbo.setValue("RESPONSECODE", resp.getResponseCode(), MboConstants.NOACCESSCHECK);
			fbiLoggerMbo.setValue("RESPONSEMSG", resp.geResponseMsg(), MboConstants.NOACCESSCHECK);
			
			String jsonString = new String(reqData);
			
			JSONObject jsonObject = JSONObject.parse(jsonString);
			fbiLoggerMbo.setValue("OWNERID", (String)jsonObject.get(ownerIdRefName), MboConstants.NOACCESSCHECK);
			fbiLoggerMbo.setValue("MBONUM",  (String)jsonObject.get(mbonumRefName), MboConstants.NOACCESSCHECK);

			fbiLoggerFilesMboSet.save(MboConstants.NOACCESSCHECK);

		} catch (Exception errLogEx) {
			errLogEx.printStackTrace();
		}
	}

}
