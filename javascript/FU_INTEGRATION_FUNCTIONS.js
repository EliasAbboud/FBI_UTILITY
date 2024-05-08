load("nashorn:mozilla_compat.js");

importPackage(Packages.psdi.mbo);
importPackage(Packages.java.lang);
importPackage(Packages.java.io);
importPackage(Packages.java.text);
importPackage(Packages.psdi.util);
importPackage(Packages.java.util);
importPackage(Packages.psdi.server);


function getFusionSiteId(mbo) {
	if (mbo===null) {
		 return "";
	}
    var siteId=mbo.getString("SITEID");
    if (siteId) {
        if ("TRANS" === siteId) {
            return "TRANSCO";
        } else {
            return siteId;
        }
    } else {
        return "";
    }
}

function getLegalEntityNameForSite(mbo) {
	if (mbo===null) {
		 return "";
	}
    var siteId=mbo.getString("SITEID");
    if (siteId) {
        return MXServer.getMXServer().getProperty("mxe.Fusion.LE_NAME."+siteId,mbo.getUserInfo());
    } else {
        return "";
    }
}

function getBUNameForSite(mbo) {
	if (mbo===null) {
		 return "";
	}
    var siteId=mbo.getString("SITEID");
    if (siteId) {
        return MXServer.getMXServer().getProperty("mxe.Fusion.BU_NAME."+siteId,mbo.getUserInfo());
    } else {
        return "";
    }
}

function getLegalEntityNameForSiteOld(mbo) {
	if (mbo===null) {
		 return "";
	}
    var siteId=mbo.getString("SITEID");
    if (siteId) {
        if ("TRANS" === siteId) {
            return "Abu Dhabi Transmission & Despatch Company";
        } else if ("ADDC" === siteId) {
            return "Abu Dhabi Distribution Company";
        } else if ("AADC" === siteId) {
            return "Al Ain Distribution Company";
        } else {
            return "";
        }
    } else {
        return "";
    }
}

function subString(mbo, attributeName, startAt, length) {
	if (mbo===null) {
		 return "";
	}
    if (!attributeName) {
        return "";
    }
    var strValue = mbo.getString(attributeName);
    if (strValue) {
        if (startAt) {
            if (length) {
                strValue = strValue.substr(startAt, length);
            } else {
                strValue = strValue.substr(startAt);
            }
        }
        return strValue;
    } else {
        return "";
    }
}

function getGlSegment(mbo, attributeName, segmentNum) {
	if (mbo===null) {
		 return "";
	}
    if (!attributeName) {
        return "";
    }
    var strValue = mbo.getString(attributeName);
    if (strValue) {
        if (segmentNum) {
            var retVal = strValue.split("-")[segmentNum];
			return retVal;
		}
	}
	return "";
}

function getAttribSegment(mbo, attributeName, segmentNum,sep) {
	if (mbo===null) {
		 return "";
	}
    if (!attributeName) {
        return "";
    }
    var strValue = mbo.getString(attributeName);
    if (strValue) {
        if (segmentNum) {
			segNum=Number(segmentNum);
			segs = strValue.split(sep);
			if (segs.length>segNum) {
				return segs[segmentNum];
			}
		}
	}
	return "";
}

function getAssetFARGlSegment6(mbo, attributeName) {
	if (mbo===null) {
		 return "";
	}
    if (!attributeName) {
        return "";
    }
    var strValue = mbo.getString(attributeName);
    if (strValue) {
        var retVal = strValue.split("-")[6];
        if (mbo.isBasedOn("ASSET") && !mbo.isNull("FU_PROJECTNUM") && !mbo.isNull("FU_TASKNUM")) {
            return mbo.getString("FU_PROJECTNUM");
        }
		return retVal;
	}
	return "";
}

function getFARofAssetCatGlSegment2(mbo, attributeName) {
	if (mbo===null) {
		 return "";
	}
    if (!attributeName) {
        return "";
    }
    var strValue = mbo.getString(attributeName);
    if (strValue) {
		var retVal = strValue.split("-")[1];
		if (mbo.isBasedOn("ASSET") && (mbo.getString("SITEID")==="TRANS" || mbo.getString("SITEID")==="AADC") && !mbo.isNull("FU_COSTCENTER")) {
			return mbo.getString("FU_COSTCENTER");
		}
		return retVal;
	}
	return "";
}

function formatDate(mbo, attributeName, format) {
	if (mbo===null) {
		 return "";
	}
    if (!attributeName) {
        return "";
    }
	var dateValue = mbo.getDate(attributeName);
	if (dateValue) {
		if (format) {
			return (new SimpleDateFormat(format)).format(dateValue);
		} else {
			return (new SimpleDateFormat()).format(dateValue);
		}
	} else {
        return "";
    }
	
}

function getCurrentDateTime(mbo,format) {
	dateCal = Calendar.getInstance();
	var currDateTime=dateCal.getTime();
	return (new SimpleDateFormat(format)).format(currDateTime).toString();
}

function appendCurrentDateTime(mbo,attributeName,format,sep) {
	if (mbo===null) {
		 return "";
	}
	if (!sep) {
		sep="-";//defaulting to dash if separator ot provided
	}
	dateCal = Calendar.getInstance();
	var currDateTime=dateCal.getTime();

	retDateStr = ""+sep+(new SimpleDateFormat(format)).format(currDateTime).toString();
	if (!attributeName || mbo.isNull(attributeName)) {
        return retDateStr;
    }
	return mbo.getString(attributeName)+retDateStr;
}

function getCurrentFinancialPeriod(mbo,year) {
	dateCal = Calendar.getInstance();
	if(year){
		dateCal.set(Calendar.YEAR, year);
	}
	var currDateTime=dateCal.getTime();
	return (new SimpleDateFormat("MMM-yy")).format(currDateTime).toString();
}

function getSystemProperty(mbo, propertyName) {
    propVal= MXServer.getMXServer().getProperty(propertyName,mbo.getUserInfo());
    if (!propVal)
        return "";
    return propVal;
}

function getFusionLedgerId(mbo) {
    return getSystemProperty(mbo, "Fusion.FU_Ledger_ID");
}


function formatDateValue(mbo, dateValue, format) {
	if (mbo===null) {
		 return "";
	}
	if (dateValue) {
		if (format) {
			return (new SimpleDateFormat(format)).format(dateValue);
		} else {
			return (new SimpleDateFormat()).format(dateValue);
		}
	} else {
        return "";
    }
	
}

function getWorkorderTotalCost(mbo) {
    if (mbo===null) {
		 return "0";
	}
	
	if (!mbo.isBasedOn("WORKORDER")) {
	    return "0";
	}
	
	if (mbo.getMboSet("ASSET").getMbo(0)==null)
	{
	    return "0";
	}
	
	if (mbo.getMboSet("ASSET").getMbo(0).getMboSet("FAR").getMbo(0)==null)
	{
	    return "0";
	}
	
	return ''+(mbo.getDouble("ACTLABCOST")+mbo.getDouble("ACTMATCOST")+mbo.getDouble("ACTSERVCOST")+mbo.getDouble("ACTTOOLCOST") + mbo.getMboSet("ASSET").getMbo(0).getMboSet("FAR").getMbo(0).getDouble("COST")) ;
}

function getAssetLifeInMonth(mbo, fromDateAttribute) {
    if (mbo===null) {
        return "";
    }
    if (!fromDateAttribute) {
        return "";
    }
	if (mbo.isNull(fromDateAttribute)) {
		return "";
	}
	return ''+monthDiff(mbo.getDate(fromDateAttribute),MXServer.getMXServer().getDate());
}

function monthDiff(d1, d2) {
    var months;
    months = (d2.getFullYear() - d1.getFullYear()) * 12;
    months -= d1.getMonth();
    months += d2.getMonth();
    return months <= 0 ? 0 : months;
}

function convertYearsToMonths(mbo, attributeName) {
    
    if (mbo===null) {
		 return "";
	}
	
    if (!attributeName) {
        return "";
    }
	
    var monthsVal = mbo.getInt(attributeName)*12;
    
    if (typeof monthsVal === 'number') 
        return "" + monthsVal;

    return "";
}

function getRecordNum(mbo,recNum) {
    return ''+recNum;
}

function getRecordNumWithMultiplier(mbo, recNum, multiplier) {
    return ''+(recNum*multiplier);
}

function getRecordNumWithMultiplierAndOffset(mbo, recNum, multiplier, offset) {
    return ''+((recNum*multiplier)+ offset);
}

function getCategorySegment(mbo, attributeName, segmentNum) {
	if (mbo===null) {
		 return "";
	}
    if (!attributeName) {
        return "";
    }
    var strValue = mbo.getString(attributeName);
    if (strValue) {
        if (segmentNum) {
            var retVal = strValue.split(".")[segmentNum];
			if (retVal) {
                return retVal;
            }
        }
    } 
	return "";
}


function getCombineTwoAttribute(mbo, attributeName1, attributeName2, attrLength) {
	if (mbo===null) {
		return "";
	}
	var attrValue1="";
	var attrValue2="";
	
	if(attributeName1){
		attrValue1 = mbo.getString(attributeName1);
	}
	if(attributeName2){
		attrValue2 = mbo.getString(attributeName2);
	}
	
	if(attrValue1!="" && attrValue2!=""){
		var combineStr = attrValue1 + "-" + attrValue2;
		if (attrLength) {
			combineStr = combineStr.substr(0 , attrLength);
		} 
		return combineStr;
	}
	else{
		if(attrValue1!=null){
			return attrValue1;
		}
		if(attrValue2!=null){
			return attrValue2;
		}
	}
	return "";
}

function getFARRetProvAcc(mbo) {
	if (mbo===null) {
		 return "";
	}
    var siteId=mbo.getString("SITEID");
    if (siteId) {
        if ("TRANS" === siteId) {
            return getSystemProperty(mbo, "mxe.Fusion.FARRetProvAcc.TRANSORG");
        }
		if ("AADC" === siteId) {
            return getSystemProperty(mbo, "mxe.Fusion.FARRetProvAcc.AADCORG");
        }
		if ("ADDC" === siteId) {
            return getSystemProperty(mbo, "mxe.Fusion.FARRetProvAcc.ADDCORG");
        }
    } 
    return "";
}

function getGlDebitFromProp(mbo, segmentNum) {
	if (mbo===null) {
		 return "";
	}
    
    var strValue = getFARRetProvAcc(mbo);
    if (strValue) {
        if (segmentNum) {
            var retVal = strValue.split("-")[segmentNum];
			if (retVal) {
                return retVal;
            } 
        }
    } 
    return "";
}

function getFARRetProvCrAcc(mbo) {
	if (mbo===null) {
		 return "";
	}
    var siteId=mbo.getString("SITEID");
    if (siteId) {
        if ("TRANS" === siteId) {
            return getSystemProperty(mbo, "mxe.Fusion.FARRetProvCrAcc.TRANSORG");
        }
		if ("AADC" === siteId) {
            return getSystemProperty(mbo, "mxe.Fusion.FARRetProvCrAcc.AADCORG");
        }
		if ("ADDC" === siteId) {
            return getSystemProperty(mbo, "mxe.Fusion.FARRetProvCrAcc.ADDCORG");
        }
    } 
    return "";
}

function getGlCreditFromProp(mbo, segmentNum) {
	if (mbo===null) {
		 return "";
	}
    
    var strValue = getFARRetProvCrAcc(mbo);
    if (strValue) {
        if (segmentNum) {
            var retVal = strValue.split("-")[segmentNum];
			if (retVal) {
                return retVal;
            } 
        }
    } 
    return "";
}

function getReceiverIC(mbo, attributeName) {
	if (mbo===null) {
		 return "";
	}
    if (!attributeName) {
        return "";
    }
    var strValue = mbo.getString(attributeName);
    if (strValue) {
		var retVal = strValue.split("-")[4];
		if(retVal==="6100") {
			return "TRANSCO_IC";
        }
		if(retVal==="6110") {
			return "ADDC_IC";
        }
		if(retVal==="6120") {
			return "AADC_IC";
        }
    } 
	return "";
}


function combineAttributes(mbo, attribNames, sep, maxLen) {
    if (mbo===null) {
		return "";
	}
    if (attribNames) {
        if (sep) {
			arrAttribNames=attribNames.split(sep);
			strValue ="";
			for (i=0;i<arrAttribNames.length;i++) {
				attrStrValue = mbo.getString(arrAttribNames[i]);
				if (attrStrValue===null) {
					attrStrValue = "";
				}
				strValue=strValue+attrStrValue;
				if (i<arrAttribNames.length-1) {
					strValue=strValue+sep;
				}
			}
			if (maxLen) {
				if (strValue.length>maxLen) {
					strValue = strValue.substr(0, maxLen);
				}				
			} 
			return strValue;
		} else {
			return "";
		}
    } else {
		return "";
	}
}

function subtractDoubleValues(mbo, dblAttrib1, dblAttrib2) {
    if (mbo===null) {
		return "";
	}
	if (!dblAttrib1) {
        return "";
    }
	if (!dblAttrib2) {
        return "";
    }
    return "" + Packages.psdi.util.MXMath.subtract(mbo.getDouble(dblAttrib1),mbo.getDouble(dblAttrib2));
}

function getModifiedCantorPairingLong(mbo, long1AttribName, long2AttribName) {
    if (mbo===null) {
		return "";
	}
	l1 = mbo.getLong(long1AttribName);
	l2 = mbo.getLong(long2AttribName);
	if (l1==l2) {
		return ""+l1;
	} else {
		return ""+((0.5*(l1+l2)*(l1+l2+1))+l2);
	}
}

function isLinear(siteId,locStr) {
	if (["ADDC","AADC"].indexOf(siteId)>=0) {
	    return false;
	}
	if (locStr===null || locStr==="") {
	    return false;
	}
	if ("TRANS"=== siteId) {
	    locStr2ndSeg=locStr.split("-")[1];

	    if (isNaN(Number(locStr2ndSeg))) {
	        return false;
	    } else {
	        return true;
	    }
	    /*
	    if(typeof locStr2ndSeg === 'number'){
	        return true;
	    } else {
	        return false;
	    }
	    */
	}
	return false;
}

function getLocationSegment(mbo,segmentOrderStr,locationRel) {
    retVal = getLocationSegment0(mbo,segmentOrderStr,locationRel);
    if (retVal!==null && retVal!=="") {
        return retVal.replaceAll("-","/");
    }
}

function getLocationSegment0(mbo,segmentOrderStr,locationRel) {
    segmentOrder=Number(segmentOrderStr);
	if (segmentOrder<1 || segmentOrder>7) {
	    return "";
	}
	
	switch(segmentOrder) {
	    case 1:
            return "MAXIMO";
	        break;
	    case 2:
            return "UAE";
	        break;
	    case 3:
			if (mbo===null || mbo.isNull("SITEID")) {
				return "";
			}
			siteId = mbo.getString("SITEID");
			locMbo = null;
			if (locationRel===null || locationRel==="") {
				locMbo = mbo;
			} else {
				locMbo = mbo.getMboSet(locationRel).getMbo(0);
			}
			
			if (locMbo===null) {
				return "";
			}
            if (siteId==="AADC") {
                return locMbo.isNull("LO5")?"UNSP":locMbo.getString("LO5");
            } else if (siteId==="ADDC") {
                return locMbo.isNull("REGION")?"UNSP":locMbo.getString("REGION");
            } else if (siteId==="TRANS") {
				return locMbo.isNull("FLO1")?"UNSP":locMbo.getString("FLO1");
            } else {
                return "";
            }
	        break;
	    case 4:
            return "UNSP";
	        break;
	    case 5:
			if (mbo===null || mbo.isNull("SITEID")) {
				return "";
			}
			siteId = mbo.getString("SITEID");
            if (siteId==="AADC") {
                return "UNSP";
            } else if (siteId==="ADDC") {
				locMbo = null;
				if (locationRel===null || locationRel==="") {
					locMbo = mbo;
				} else {
					locMbo = mbo.getMboSet(locationRel).getMbo(0);
				}
				if (locMbo===null) {
					return "";
				}
				return locMbo.isNull("GISAREA")?"UNSP":locMbo.getString("GISAREA");
            } else if (siteId==="TRANS") {
                return "UNSP";
            } else {
                return "";
            }
	        break;
	    case 6:
			if (mbo===null || mbo.isNull("SITEID")) {
				return "";
			}
			siteId = mbo.getString("SITEID");
			locMbo = null;
			if (locationRel===null || locationRel==="") {
				locMbo = mbo;
			} else {
				locMbo = mbo.getMboSet(locationRel).getMbo(0);
			}
			if (locMbo===null) {
				return "";
			}
			locStr=locMbo.getString("LOCATION");
			if (siteId==="AADC") {
                return locMbo.isNull("LO6")?"UNSP":locMbo.getString("LO6");
            } else if (siteId==="ADDC") {
                return locMbo.isNull("GISSECTOR")?"UNSP":locMbo.getString("GISSECTOR");
            } else if (siteId==="TRANS") {
                if (isLinear(siteId,locStr)) {
                    return "UNSP";
                } else {
                    locSegs=locStr.split("-");
                    return locSegs[0]+"-"+locSegs[1]+"-"+locSegs[2];
                }
                
            } else {
                return "";
            }
	        break;
	    case 7:
			if (mbo===null || mbo.isNull("SITEID")) {
				return "";
			}
			siteId = mbo.getString("SITEID");
            if (siteId==="AADC") {
                return "UNSP";
            } else if (siteId==="ADDC") {
				locMbo = null;
				if (locationRel===null || locationRel==="") {
					locMbo = mbo;
				} else {
					locMbo = mbo.getMboSet(locationRel).getMbo(0);
				}
				if (locMbo===null) {
					return "";
				}
				return locMbo.isNull("FLO1")?"UNSP":locMbo.getString("FLO1");
            } else if (siteId==="TRANS") {
                return "UNSP";
            } else {
                return "";
            }
	        break;
	    default:
	        return "";
    }
}


function getCombinedLocationSegments(mbo,startSegmentOrderStr,endSegmentOrderStr,locationRel,separator) {
    startSegmentOrder = Number(startSegmentOrderStr);
    endSegmentOrder = Number(endSegmentOrderStr);
    retStr="";
    for (i=startSegmentOrder;i<=endSegmentOrder;i++) {
        retStr+=getLocationSegment(mbo,i,locationRel)+separator;
    }
    return retStr.substr(0, retStr.lastIndexOf(separator));
}

function validateLocationSegment(mbo,segmentOrderStr,locationRel) {
    segmentOrder=Number(segmentOrderStr);
    if (segmentOrder<1 || segmentOrder>7) {
	    
	    throwMxApplicationException("common","error",["Only location segments 1 to 7 are currently supported"]);
	}
	if (mbo===null || mbo.isNull("SITEID")) {
	    
	    throwMxApplicationException("common","error",["cannot validate location segments on null object"]);
	}
	siteId = mbo.getString("SITEID");
	locMbo = null;
	if (locationRel===null || locationRel==="") {
		locMbo = mbo;
	} else {
		locMbo = mbo.getMboSet(locationRel).getMbo(0);
	}
	
	if (locMbo===null) {
		
		throwMxApplicationException("common","error",["cannot validate location segments on null location object"]);
	}
	switch(segmentOrder) {
	    case 3:
            if (siteId==="AADC") {
                if (locMbo.isNull("LO5")) {
                    
                    throwMxApplicationException("common","error",["Please provide value for Area or type UNSP if no value can be provided"]);
                }
            } else if (siteId==="ADDC") {
                if (locMbo.isNull("REGION")) {
                    
                    throwMxApplicationException("common","error",["Please provide value for Region"]);
                }
            } else if (siteId==="TRANS") {
                if (locMbo.isNull("FLO1")) {
                    
                    throwMxApplicationException("common","error",["Please provide value for Area "]);
                }
            } 
	        break;
	    case 5:
            if (siteId==="ADDC") {
                if (locMbo.isNull("GISAREA")) {
                    
                    throwMxApplicationException("common","error",["Please provide value for GIS Area or type UNSP if no value can be provided"]);
                }
				
            }
	        break;
	    case 6:
			locStr=locMbo.getString("LOCATION");
			if (siteId==="AADC") {
                if (locMbo.isNull("LO6")) {
                    
                    throwMxApplicationException("common","error",["Please provide value for Divison or type UNSP if no value can be provided"]);
                }
            } else if (siteId==="ADDC") {
                if (locMbo.isNull("GISSECTOR")) {
                    
                    throwMxApplicationException("common","error",["Please provide value for GIS Sector or type UNSP if no value can be provided"]);
                }
            } else if (siteId==="TRANS") {
                if (!isLinear(siteId,locStr)) {
                	if (locStr===null || locStr==="") {
                	    
                	    throwMxApplicationException("common","error",["cannot validate location segments on null location"]);
                	}
                    locSegs=locStr.split("-");
                	if (locSegs===null || locStr.length<3) {
                	    
                	    throwMxApplicationException("common","error",["Location segments 1,2 or 3 for Non Linear asset not found"]);
                	}
                	if (locSegs[0]===null || locSegs[0]==="") {
                	    
                	    throwMxApplicationException("common","error",["Location segments 1 for Non Linear asset not found"]);
                	}
                	if (locSegs[1]===null || locSegs[1]==="") {
                	    
                	    throwMxApplicationException("common","error",["Location segments 2 for Non Linear asset not found"]);
                	}
                	if (locSegs[2]===null || locSegs[2]==="") {
                	    
                	    throwMxApplicationException("common","error",["Location segments 3 for Non Linear asset not found"]);
                	}
                }
            }
	        break;
	    case 7:
	        if (siteId==="ADDC") {
                if (locMbo.isNull("FLO1")) {
                    
                    throwMxApplicationException("common","error",["Please provide value for Area or type UNSP if no value can be provided"]);
                }
            }
	        break;
	    default:
	        return "";
    }
}

function validateCombinedLocationSegments(mbo,startSegmentOrderStr,endSegmentOrderStr,locationRel) {
    startSegmentOrder = Number(startSegmentOrderStr);
    endSegmentOrder = Number(endSegmentOrderStr);
    for (i=startSegmentOrder;i<=endSegmentOrder;i++) {
        validateLocationSegment(mbo,i,locationRel);
    }
}

function throwMxApplicationException(errGrp,errKey,params) {
    throw new Packages.psdi.util.MXApplicationException(errGrp,errKey,params);
}


function replaceValue(mbo, attributeName, val, newVal) {
	if (mbo === null || mbo.isNull(attributeName)) {
		if (val) {
			return "";
		} else {
			return newVal;
		}
	}
	maxType = mbo.getThisMboSet().getMboSetInfo().getAttribute(attributeName).getType();
	switch (maxType) {
		case "ALN":
		case "LONGALN":
		case "UPPER":
		case "LOWER":
		case "GL":
		case "DATE":
		case "DATETIME":
		case "TIME":
			if (mbo.getString(attributeName).equalsIgnoreCase(val)) {
				return newVal;
			}
			break;
		case "SMALLINT":
		case "INTEGER":
			if (mbo.getInt(attributeName) == val) {
				return newVal;
			}
			break;
		case "BIGINT":
			if (mbo.getLong(attributeName) == val) {
				return newVal;
			}
			break;
		case "FLOAT":
		case "DECIMAL":
		case "DURATION":
		case "AMOUNT":
			if (mbo.getDouble(attributeName) == val) {
				return newVal;
			}
			break;
		case "YORN":
			if (mbo.getBoolean(attributeName) == val) {
				return newVal;
			}
			break;
		case "CRYPTO":
		case "CRYPTOX":
		case "CLOB":
		case "BLOB":
			if (mbo.getBytes(attributeName) == val) {
				return newVal;
			}
			break;
		default:
			if (mbo.getString(attributeName).equalsIgnoreCase(val)) {
				return newVal;
			}
			break;
	}
	return val;
}

function absValue(mbo, attribute) {
    if (mbo===null) {
		return "";
	}
	if (!attribute) {
        return "";
    }
    return "" + Packages.psdi.util.MXMath.abs(mbo.getDouble(attribute));
}

function removeComma(mbo,attribute) {
    if (mbo===null) {
		return "";
	}
	if (!attribute) {
        return "";
    }
    retVal=mbo.getString(attribute);
    if (retVal!==null && retVal!=="") {
        return retVal.replaceAll(","," ");
    }
}