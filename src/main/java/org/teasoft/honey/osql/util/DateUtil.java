/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(automvc@163.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.Logger;

/**
 * @author Kingstar
 * @since  1.4
 */
public class DateUtil {
	
	
//	private static SimpleDateFormat defaultFormat =null;  // new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//	private static SimpleDateFormat format =null;
	
	private static SimpleDateFormat getSimpleDateFormat() {
		SimpleDateFormat defaultFormat =null;  
		String dateFormatStr = HoneyConfig.getHoneyConfig().getDateFormat();
		if (dateFormatStr != null && !"".equals(dateFormatStr.trim())) {
			try {
				defaultFormat = new SimpleDateFormat(dateFormatStr);
			} catch (Exception e) {
				Logger.warn("In DateUtil: it is error date format String :"+dateFormatStr);
				defaultFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			}
		}else {
			defaultFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
		
		
		return defaultFormat;
	}
	
	public static String currentDate(){
		return getSimpleDateFormat().format(new Date());
	}
	
	public static String currentDate(String formatStr){
		SimpleDateFormat format =null;  
		if(formatStr==null || "".trim().equals(formatStr)) 
			format=getSimpleDateFormat();
		else 
		   format = new SimpleDateFormat(formatStr);
		return format.format(new Date());
	}

}
