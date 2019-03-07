/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(automvc@163.com)
 * The license,see the LICENSE file.
 */

package org.honey.osql.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Kingstar
 * @since  1.4
 */
public class DateUtil {
	
	
	private static SimpleDateFormat defaultFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static SimpleDateFormat format =null;
	
	public static String currentDate(){
		return defaultFormat.format(new Date());
	}
	
	public static String currentDate(String formatStr){
		if(formatStr==null || "".trim().equals(formatStr)) 
			format=defaultFormat;
		else 
		   format = new SimpleDateFormat(formatStr);
		return format.format(new Date());
	}

}
