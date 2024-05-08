package custom.iface.fbi;

import java.rmi.RemoteException;
import java.util.HashMap;

import com.ibm.tivoli.maximo.script.ScriptDriverFactory;
import com.ibm.tivoli.maximo.script.ScriptEngineContext;

import psdi.mbo.Mbo;
import psdi.mbo.MboSet;
import psdi.mbo.MboSetRemote;
import psdi.util.MXException;
import psdi.util.StringUtility;

public class FBILoggerMain extends Mbo{

	public FBILoggerMain(MboSet ms) throws RemoteException {
		super(ms);	
	}
	
	
	@Override
	public MboSetRemote smartFindByObjectName(String sourceObj, String targetAttrName, String value, boolean exact) throws MXException, RemoteException {
		if ("ownerid".equalsIgnoreCase(targetAttrName)) {
			String targetRecordId = invokeScriptFunction("FBILOGGER", "getTargetRecordId");
			String targetAttributeName = invokeScriptFunction("FBILOGGER", "getTargetAttributeName");
			
			if (!StringUtility.isEmpty(targetRecordId) && !StringUtility.isEmpty(targetAttributeName)) {
				return getMboSet("$FBI_"+sourceObj+"_"+getString("OWNERTABLE")+"_"+String.valueOf(getLong("OWNERID")),sourceObj,targetAttributeName+"="+targetRecordId);
			}			
		}
		return super.smartFindByObjectName(sourceObj, targetAttrName, value, exact);
	}
	
	public String invokeScriptFunction(String scriptName, String functionName) {
		try {
			Object[] args = new Object[] {this};
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
				ScriptDriverFactory.getInstance().getScriptDriver(scriptName).runScript(scriptName, context);
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
}
