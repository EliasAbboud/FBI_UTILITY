package custom.iface.fbi;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import psdi.mbo.Mbo;
import psdi.mbo.MboServerInterface;
import psdi.mbo.MboSet;
import psdi.mbo.MboSetInfo;
import psdi.mbo.custapp.CustomMboSet;
import psdi.util.MXException;
import psdi.util.StringUtility;

public class FBILoggerMainSet extends CustomMboSet {

	public FBILoggerMainSet(MboServerInterface ms) throws RemoteException {
		super(ms);
	}

	@Override
	protected Mbo getMboInstance(MboSet ms) throws MXException, RemoteException {
		return new FBILoggerMain(ms);
		
	}
	
	@Override
	public MboSetInfo getMboSetInfo() {
		MboSetInfo msi = super.getMboSetInfo();
		setUniqueIDName("VIEWKEY", msi);
		return msi;
		
	}
	
	
	private void setUniqueIDName(String name, MboSetInfo msi) {
		if (!StringUtility.isEmpty(msi.getUniqueIDName()))
			return;
		
		try {
			Method setUniqueIDNameMethod = msi.getClass().getDeclaredMethod("setUniqueIDName", String.class);
			setUniqueIDNameMethod.setAccessible(true);
			setUniqueIDNameMethod.invoke(msi, name);
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}
	

}
