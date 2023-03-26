/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(automvc@163.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.Logger;

/**
 * @author Kingstar
 * @since  1.4
 */
public class DateUtil {
	
	private DateUtil() {}
	
	private static SimpleDateFormat getSimpleDateFormat() {
		SimpleDateFormat defaultFormat =null;  
		String dateFormatStr = HoneyConfig.getHoneyConfig().dateFormat;
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
	
	public static java.sql.Date currentSqlDate(){
		return new java.sql.Date(System.currentTimeMillis());
	}
	
	public static Timestamp toTimestamp(String dateString) {
		try {
			Date date = getSimpleDateFormat().parse(dateString);
			return new Timestamp(date.getTime());
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}

		return null;
	}
	
	public static Timestamp toTimestamp(Date date) {
		try {
			return new Timestamp(date.getTime());
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}

		return null;
	}
	
//	/**
//	 * 
//	 * @param dateString
//	 * @return
//	 * @since 2.1
//	 */
//	public static Date toDate(String dateString) {
//		try {
//			return getSimpleDateFormat().parse(dateString);
//		} catch (Exception e) {
//			Logger.error(e.getMessage(), e);
//		}
//		return null;
//	}
	
	/**
	 * 
	 * @param date
	 * @return
	 * @since 2.1
	 */
	public static String toDateStr(Date date) {
		try {
			return getSimpleDateFormat().format(date);
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}
		return null;
	}
	
//	/**
//	 * 
//	 * @return
//	 * @since 2.1
//	 */
//	public static Date currentDate2() {
//		try {
//			System.out.println(currentDate());
//			return getSimpleDateFormat().parse(currentDate());
//		} catch (Exception e) {
//			Logger.error(e.getMessage(), e);
//		}
//		return null;
//	}
	
	
	
	/**
	 * 往前或往后指定天数
	 * @param days
	 * @return new Date
	 * @since 1.11
	 */
//	public static Date jumpDays(int days) {
//		Calendar cal = Calendar.getInstance();//使用默认时区和语言环境获得一个日历。    
//		cal.add(Calendar.DAY_OF_MONTH, days);
//		return cal.getTime();
//	}
	
	/**
	 * 获取当前Timestamp
	 * @return 当前Timestamp
	 * @since 1.11
	 */
	public static Timestamp currentTimestamp(){
		return new Timestamp(System.currentTimeMillis());
	}
	
	/**
	 * 往前或往后指定天数
	 * @param days
	 * @return new Date
	 * @since 1.11
	 */
	public static Timestamp jumpDaysExact(int days) {
		Calendar cal = Calendar.getInstance();//使用默认时区和语言环境获得一个日历。    
		cal.add(Calendar.DAY_OF_MONTH, days);
		return new Timestamp(cal.getTimeInMillis()); 
	}
	
	/**
	 * 往前或往后指定天数,指定天最后秒设置为23:59:59
	 * @param days
	 * @return Timestamp对象.instance of Timestamp.
	 */
	public static Timestamp jumpDays(int days) {
		Calendar cal = Calendar.getInstance();//使用默认时区和语言环境获得一个日历。    
		cal.add(Calendar.DAY_OF_MONTH, days);
		cal.set(Calendar.HOUR, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		return new Timestamp(cal.getTimeInMillis()); 
	}
	
	
	public static Timestamp jumpDays(Timestamp base, int days) {
		Calendar cal = Calendar.getInstance();//使用默认时区和语言环境获得一个日历。   
		
		cal.setTime(base); //set to base
		
		cal.add(Calendar.DAY_OF_MONTH, days);
		
		return new Timestamp(cal.getTimeInMillis()); 
	}
	
	
	public static boolean isNowEffect(Timestamp expirationDate) {
		Calendar now = Calendar.getInstance();
		return now.getTime().before(expirationDate);
	}

}
