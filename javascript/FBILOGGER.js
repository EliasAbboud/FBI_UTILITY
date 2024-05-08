function getAppLinks(fbiLoggerMainMbo) {
    targetObject = fbiLoggerMainMbo.getString("OWNERTABLE");
    switch(targetObject) {
        case "ADRLINESUM": return '["ADR"]';
        case "ASSET": return '["ASSET"]';
        case "ASSETTRANS": return '["ASSET"]';
        case "INVTRANS": return '["INVENTOR"]';
        case "MATRECTRANS": 
			matRecTransMbo = getMboRecordByOwnerId(fbiLoggerMainMbo);
			if (matRecTransMbo!==null && !matRecTransMbo.isNull("PONUM") && !matRecTransMbo.isNull("SITEID")) {
				return '["TRRECEIPTS"]';
			}
			if (matRecTransMbo!==null && matRecTransMbo.isNull("PONUM") && !matRecTransMbo.isNull("SITEID") && !matRecTransMbo.isNull("tostoreloc") && !matRecTransMbo.isNull("itemsetid") && !matRecTransMbo.isNull("itemnum") ) {
				return '["INVENTOR"]';
			}
			return null; 
        case "MATUSETRANS":
			matUseTransMbo = getMboRecordByOwnerId(fbiLoggerMainMbo);
			if (matUseTransMbo!==null && !matUseTransMbo.isNull("refwo") && !matUseTransMbo.isNull("SITEID")) {
				return '["PLUSDWOTRK"]';
			}
			if (matUseTransMbo!==null && !matUseTransMbo.isNull("mrnum") && !matUseTransMbo.isNull("SITEID")) {
				return '["MR"]';
			}
			return null; 
        case "VEHICLEHIRING": return '["ASSET"]';
        case "WORKORDER": return '["PLUSDWOTRK"]';
        default: return null;
    }
}

function getTargetAttributeName(fbiLoggerMainMbo) {
    targetObjectName=getTargetObjectName(fbiLoggerMainMbo);
    if (targetObjectName!==null) {
        uidName=Packages.psdi.server.MXServer.getMXServer().getMboSet(targetObjectName, fbiLoggerMainMbo.getUserInfo()).getMboSetInfo().getUniqueIDName();
        if (uidName!==null) {
            return uidName;
        }
    }
    
    return null;
}

function getTargetRecordId(fbiLoggerMainMbo) {
    targetObject = fbiLoggerMainMbo.getString("OWNERTABLE");
    switch(targetObject) {
        case "ADRLINESUM": 
            adrLineSumMbo=getMboRecordByMboNum(fbiLoggerMainMbo);
            if (adrLineSumMbo!==null && !adrLineSumMbo.isNull("ADRNUM")) {
                adrMbo=getMboRecordByWhereClause(fbiLoggerMainMbo,"ADR","ADRNUM='"+adrLineSumMbo.getString("ADRNUM")+"'");
                if (adrMbo!==null) {
                    return ""+ adrMbo.getUniqueIDValue();
                }
            }
            return null;
        case "ASSET": 
            return ""+fbiLoggerMainMbo.getLong("OWNERID");
        case "ASSETTRANS": 
            assetTransMbo=getMboRecordByOwnerId(fbiLoggerMainMbo);
            if (assetTransMbo!==null && !assetTransMbo.isNull("ASSETNUM") && !assetTransMbo.isNull("SITEID")) {
                assetMbo=getMboRecordByWhereClause(fbiLoggerMainMbo,"ASSET","ASSETNUM='"+assetTransMbo.getString("ASSETNUM")+"' and SITEID='"+assetTransMbo.getString("SITEID")+"'");
                if (assetMbo!==null) {
                    return ""+ assetMbo.getUniqueIDValue();
                }
            }
            return null;
        case "INVTRANS": 			
			invTransMbo=getMboRecordByOwnerId(fbiLoggerMainMbo);
            if (invTransMbo!==null && !invTransMbo.isNull("storeloc") && !invTransMbo.isNull("itemnum") && !invTransMbo.isNull("SITEID") && !invTransMbo.isNull("itemsetid")) {
                invMbo=getMboRecordByWhereClause(fbiLoggerMainMbo,"INVENTORY","itemnum = '"+invTransMbo.getString("itemnum")+"' and location = '"+invTransMbo.getString("storeloc")+"' and itemsetid = '"+invTransMbo.getString("itemsetid")+"' and siteid='"+invTransMbo.getString("siteid")+"'");
                if (invMbo!==null) {
                    return ""+ invMbo.getUniqueIDValue();
                }
            }
            return null;
        case "MATRECTRANS":
			matRecTransMbo = getMboRecordByOwnerId(fbiLoggerMainMbo);
			if (matRecTransMbo!==null && !matRecTransMbo.isNull("PONUM") && !matRecTransMbo.isNull("SITEID")) {
				poMbo = getMboRecordByWhereClause(fbiLoggerMainMbo,"PO","ponum = '"+matRecTransMbo.getString("ponum")+"' and SITEID = '"+matRecTransMbo.getString("SITEID")+"'");
				if (poMbo!==null) {
                    return ""+ poMbo.getUniqueIDValue();
                }
			}
			if (matRecTransMbo!==null && matRecTransMbo.isNull("PONUM") && !matRecTransMbo.isNull("SITEID") && !matRecTransMbo.isNull("tostoreloc") && !matRecTransMbo.isNull("itemsetid") && !matRecTransMbo.isNull("itemnum") ) {
				invMbo=getMboRecordByWhereClause(fbiLoggerMainMbo,"INVENTORY","itemnum = '"+matRecTransMbo.getString("itemnum")+"' and location = '"+matRecTransMbo.getString("tostoreloc")+"' and itemsetid = '"+matRecTransMbo.getString("itemsetid")+"' and siteid='"+matRecTransMbo.getString("siteid")+"'");
				if (invMbo!==null) {
                    return ""+ invMbo.getUniqueIDValue();
                }
			}
			return null; 
        case "MATUSETRANS": 
			matUseTransMbo = getMboRecordByOwnerId(fbiLoggerMainMbo);
			if (matUseTransMbo!==null && !matUseTransMbo.isNull("refwo") && !matUseTransMbo.isNull("SITEID")) {
				woMbo = getMboRecordByWhereClause(fbiLoggerMainMbo,"WORKORDER","wonum = '"+matUseTransMbo.getString("refwo")+"' and SITEID = '"+matUseTransMbo.getString("SITEID")+"'");
				if (woMbo!==null) {
                    return ""+ woMbo.getUniqueIDValue();
                }
			}
			if (matUseTransMbo!==null && !matUseTransMbo.isNull("mrnum") && !matUseTransMbo.isNull("SITEID")) {
				mrMbo = getMboRecordByWhereClause(fbiLoggerMainMbo,"MR","mrnum = '"+matUseTransMbo.getString("mrnum")+"' and SITEID = '"+matUseTransMbo.getString("SITEID")+"'");
				if (mrMbo!==null) {
                    return ""+ mrMbo.getUniqueIDValue();
                }
			}
			return null; 
        case "VEHICLEHIRING": 
            vehicleHiringMbo=getMboRecordByOwnerId(fbiLoggerMainMbo);
            if (vehicleHiringMbo!==null && !vehicleHiringMbo.isNull("ASSETNUM") && !vehicleHiringMbo.isNull("SITEID")) {
                assetMbo=getMboRecordByWhereClause(fbiLoggerMainMbo,"ASSET","ASSETNUM='"+vehicleHiringMbo.getString("ASSETNUM")+"' and SITEID='"+vehicleHiringMbo.getString("SITEID")+"'");
                if (assetMbo!==null) {
                    return ""+ assetMbo.getUniqueIDValue();
                }
            }
            return null;
        case "WORKORDER": return ""+fbiLoggerMainMbo.getLong("OWNERID");
        default: return null;
    }
}

function getTargetObjectName(fbiLoggerMainMbo) {
    targetObject = fbiLoggerMainMbo.getString("OWNERTABLE");
    switch(targetObject) {
        case "ADRLINESUM": return "ADR";
        case "ASSET": return "ASSET";
        case "ASSETTRANS": return "ASSET";
        case "INVTRANS": return "INVENTORY"; 
        case "MATRECTRANS":
			matRecTransMbo = getMboRecordByOwnerId(fbiLoggerMainMbo);
			if (matRecTransMbo!==null && !matRecTransMbo.isNull("PONUM") && !matRecTransMbo.isNull("SITEID")) {
				return "PO";
			}
			if (matRecTransMbo!==null && matRecTransMbo.isNull("PONUM") && !matRecTransMbo.isNull("SITEID") && !matRecTransMbo.isNull("tostoreloc") && !matRecTransMbo.isNull("itemsetid") && !matRecTransMbo.isNull("itemnum") ) {
				return "INVENTORY";
			}
			return null; 
        case "MATUSETRANS":
			matUseTransMbo = getMboRecordByOwnerId(fbiLoggerMainMbo);
			if (matUseTransMbo!==null && !matUseTransMbo.isNull("refwo") && !matUseTransMbo.isNull("SITEID")) {
				return "WORKORDER";
			}
			if (matUseTransMbo!==null && !matUseTransMbo.isNull("mrnum") && !matUseTransMbo.isNull("SITEID")) {
				return "MR";
			}
			return null; 
        case "VEHICLEHIRING": return "ASSET";
        case "WORKORDER": return "WORKORDER";
        default: return null;
    }
}

function getMboRecordByOwnerId(fbiLoggerMainMbo) {
    targetObject = fbiLoggerMainMbo.getString("OWNERTABLE");
    recordId = fbiLoggerMainMbo.getLong("OWNERID");
    if (targetObject!==null && targetObject!=="" && recordId!==null && recordId>0) {
        return Packages.psdi.server.MXServer.getMXServer().getMboSet(targetObject, fbiLoggerMainMbo.getUserInfo()).getMboForUniqueId(recordId);
    }
    return null;
}

function getMboRecordByMboNum(fbiLoggerMainMbo) {
    targetObject = fbiLoggerMainMbo.getString("OWNERTABLE");
    mboNum = fbiLoggerMainMbo.getString("MBONUM");
    if (targetObject!==null && targetObject!=="" && mboNum!==null && mboNum!=="") {
        alnDomainSet=Packages.psdi.server.MXServer.getMXServer().getMboSet("ALNDOMAIN", fbiLoggerMainMbo.getUserInfo());
        alnDomainSet.setWhere("DOMAINID='APPRECORD' and value='"+targetObject+"'");
        alnDomainSet.reset();
        alnDomainMbo=alnDomainSet.getMbo(0);
        if (alnDomainMbo!==null) {
            mboKeyName=alnDomainMbo.getString("DESCRIPTION");
            if (mboKeyName!==null && mboKeyName!=="") {
                mboRecordSet=Packages.psdi.server.MXServer.getMXServer().getMboSet(targetObject, fbiLoggerMainMbo.getUserInfo());
                mboRecordSet.setWhere(mboKeyName+"='"+mboNum+"'");
                mboRecordSet.reset();
                return mboRecordSet.getMbo(0);
            }
        }
    }
    return null;
}

function getMboRecordByWhereClause(fbiLoggerMainMbo,targetObject,whereClause) {
    if (targetObject!==null && targetObject!=="" && whereClause!==null && whereClause!=="") {
        mboRecordSet=Packages.psdi.server.MXServer.getMXServer().getMboSet(targetObject, fbiLoggerMainMbo.getUserInfo());
        mboRecordSet.setWhere(whereClause);
        mboRecordSet.reset();
        return mboRecordSet.getMbo(0);
    }
    return null;
}