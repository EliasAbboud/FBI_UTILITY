package custom.iface.fbi;

import java.io.IOException;
import java.rmi.RemoteException;

import com.ibm.json.java.JSONArray;

import psdi.mbo.MboValue;
import psdi.mbo.MboValueAdapter;
import psdi.util.MXException;
import psdi.util.StringUtility;

public class FldFBILoggerMainOwnerId extends MboValueAdapter{
	
	 public FldFBILoggerMainOwnerId(MboValue mbv) {
	        super(mbv);
	 }
	 


	 @Override
	public String[] getAppLink() throws MXException, RemoteException {
		if (mboValue.getMbo() instanceof FBILoggerMain) {
			String arrStrAppLinks = ((FBILoggerMain)mboValue.getMbo()).invokeScriptFunction("FBILOGGER", "getAppLinks");
			if (!StringUtility.isEmpty(arrStrAppLinks)) {
				try {
					return (String[]) JSONArray.parse(arrStrAppLinks).toArray(new String[] {});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return super.getAppLink();
	}
	 

}
