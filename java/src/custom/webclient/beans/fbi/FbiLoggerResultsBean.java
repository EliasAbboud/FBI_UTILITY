package custom.webclient.beans.fbi;

import java.rmi.RemoteException;

import psdi.util.MXException;
import psdi.webclient.system.beans.DataBean;
import psdi.webclient.system.beans.ResultsBean;

public class FbiLoggerResultsBean extends ResultsBean {


	public int REEXPORT() throws RemoteException, MXException {
		DataBean fbiLoggerAppBean = app.getAppBean();
		if (fbiLoggerAppBean!=null && fbiLoggerAppBean instanceof FbiLoggerAppBean) {
			((FbiLoggerAppBean)fbiLoggerAppBean).REEXPORT();
		}
		return EVENT_HANDLED;
	}
	

}
