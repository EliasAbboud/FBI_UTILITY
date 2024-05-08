package custom.iface.fbi;

import java.nio.charset.Charset;
import java.rmi.RemoteException;

import psdi.mbo.MboValue;
import psdi.mbo.MboValueAdapter;
import psdi.util.MXException;

public class FldFBILoggerFileNpFileData extends MboValueAdapter{
	
	 public FldFBILoggerFileNpFileData(MboValue mbv) {
	        super(mbv);
	 }
	 
	 @Override
	public void initValue() throws MXException, RemoteException {
		super.initValue();
		byte[] fileData = mboValue.getMbo().getBytes("FILEDATA");
		if (fileData!=null && fileData.length>0) {
		    String strVal = new String(fileData,Charset.forName("UTF-8"));
		    mboValue.setValue(strVal,11);
		}
	}
	 

}
