/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(automvc@163.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.teasoft.honey.osql.core.ExceptionHelper;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.4
 */
public class DateUtil {

	private DateUtil() {}

	private static SimpleDateFormat getSimpleDateFormat() {
		SimpleDateFormat defaultFormat = null;
		String dateFormatStr = HoneyConfig.getHoneyConfig().dateFormat;
		if (dateFormatStr != null && !"".equals(dateFormatStr.trim())) {
			try {
				defaultFormat = new SimpleDateFormat(dateFormatStr);
			} catch (Exception e) {
				Logger.warn("In DateUtil: it is error date format String :" + dateFormatStr);
				defaultFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			}
		} else {
			defaultFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}

		return defaultFormat;
	}

	public static String currentDate() {
		return getSimpleDateFormat().format(new Date());
	}

	public static String currentDate(String formatStr) {
		SimpleDateFormat format = null;
		if (formatStr == null || "".trim().equals(formatStr)) format = getSimpleDateFormat();
		else format = new SimpleDateFormat(formatStr);
		return format.format(new Date());
	}

	public static java.sql.Date currentSqlDate() {
		return new java.sql.Date(System.currentTimeMillis());
	}

	/**
	 * java.util.Date transfer to  java.sql.Date.
	 * java.sql.Date 会丢失时分秒.
	 * @param date
	 * @return
	 */
	public static java.sql.Date toSqlDate(Date date) {
		return new java.sql.Date(date.getTime()); // 会丢失时分秒
	}

	public static Timestamp toTimestamp(String dateString) {
		try {
			Date date = getSimpleDateFormat().parse(dateString);
			return new Timestamp(date.getTime());
		} catch (Exception e) {
			Logger.warn(e.getMessage(), e);
		}

		return null;
	}

	public static Timestamp toTimestamp(Date date) {
		try {
			return new Timestamp(date.getTime());
		} catch (Exception e) {
			Logger.warn(e.getMessage(), e);
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
			Logger.warn(e.getMessage(), e);
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
	public static Timestamp currentTimestamp() {
		return new Timestamp(System.currentTimeMillis());
	}

	/**
	 * 往前或往后指定天数
	 * @param days
	 * @return new Date
	 * @since 1.11
	 */
	public static Timestamp jumpDaysExact(int days) {
		Calendar cal = Calendar.getInstance();// 使用默认时区和语言环境获得一个日历。
		cal.add(Calendar.DAY_OF_MONTH, days);
		return new Timestamp(cal.getTimeInMillis());
	}

	/**
	 * 往前或往后指定天数,指定天最后秒设置为23:59:59
	 * @param days
	 * @return Timestamp对象.instance of Timestamp.
	 */
	public static Timestamp jumpDays(int days) {
		Calendar cal = Calendar.getInstance();// 使用默认时区和语言环境获得一个日历。
		cal.add(Calendar.DAY_OF_MONTH, days);
		cal.set(Calendar.HOUR, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		return new Timestamp(cal.getTimeInMillis());
	}

	public static Timestamp jumpDays(Timestamp base, int days) {
		Calendar cal = Calendar.getInstance();// 使用默认时区和语言环境获得一个日历。

		cal.setTime(base); // set to base

		cal.add(Calendar.DAY_OF_MONTH, days);

		return new Timestamp(cal.getTimeInMillis());
	}

	public static boolean isNowEffect(Timestamp expirationDate) {
		Calendar now = Calendar.getInstance();
		return now.getTime().before(expirationDate);
	}

	/**
	 * return the age
	 * @param birthDate String with format yyyy-MM-dd
	 * @return the int of age
	 * @since 2.1.8
	 */
	public static int countAge(String birthDateString) {
		// 创建日期格式化对象
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date birthDate = null;
		int age = 0;
		try {
			// 解析日期字符串为Date对象
			birthDate = dateFormat.parse(birthDateString);
//	            birthDate = getSimpleDateFormat().parse(birthDateString); //change
		} catch (ParseException e) {
			throw ExceptionHelper.convert(e);
		}
		// 获取当前日期
		Calendar currentDate = Calendar.getInstance();
		// 创建Calendar对象，并设置为出生日期
		Calendar birthCalendar = Calendar.getInstance();
		birthCalendar.setTime(birthDate);
		// 计算年龄
		age = currentDate.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR);

		// 如果当前月份小于出生月份，或者当前月份等于出生月份但是当前日期小于出生日期，则年龄减1
		if (currentDate.get(Calendar.MONTH) < birthCalendar.get(Calendar.MONTH)
				|| (currentDate.get(Calendar.MONTH) == birthCalendar.get(Calendar.MONTH)
						&& currentDate.get(Calendar.DAY_OF_MONTH) < birthCalendar.get(Calendar.DAY_OF_MONTH))) {
			age--;
		}

		return age;
	}

//	private static String regexComm = "^([1-2]\\d{3}-)((0[1-9]-)|([1][0-2]-))(0[1-9]|[12][0-9]|30|31)$";   //ok
//	private static String regexComm = "^([1-2]\\d{3})-((0[1-9])|([1][0-2]))-(0[1-9]|[12][0-9]|30|31)$";  //ok  否使用^和$是没有区别
//	private static String regexComm = "([1-2]\\d{3})-((0[1-9])|([1][0-2]))-(0[1-9]|[12][0-9]|30|31)";  //ok

//	matcher.find()用于在字符串中查找任何匹配的子序列，可以进行多次调用以查找所有匹配项。
//	matcher.matches()用于判断整个字符串是否完全匹配正则表达式。
//	它们的区别在于，matcher.find()在字符串中查找匹配的子序列，而matcher.matches()对整个字符串进行匹配判断。
//	是的，当使用matcher.matches()方法时，正则表达式是否使用^和$是没有区别的。
//	matcher.matches()方法要求整个字符串与正则表达式完全匹配，即整个字符串从开头到结尾都要满足正则表达式的匹配规则。因此，如果正则表达式中包含^和$，它们会起到锚定的作用，确保整个字符串的开头和结尾与正则表达式的匹配规则一致。

//	但使用matcher.find()，是否使用^和$，则是有区别的?

	private static String yyyy = "([1-2]\\d{3})";
	private static String MM = "((0[1-9])|([1][0-2]))";
	private static String dd = "(0[1-9]|[12][0-9]|30|31)";

	/**
	 * 
	 * @param dataStr date string
	 * @param dateFormatStr  date format string
	 * @return boolean value whether is right format string
	 * @since 2.1.8
	 */
	public static boolean checkDate(String dataStr, String dateFormatStr) {
		if (dataStr == null || StringUtils.isBlank(dateFormatStr)) return false;

		String regex = dateFormatStr.replace("yyyy", yyyy).replace("MM", MM).replace("dd", dd);

		boolean flag = dataStr.matches(regex);

		if (!flag) {
			return flag;
		} else { // true
//			02-30,02-31,   04-31,06-31,  09-31,11-31
			int m1 = dateFormatStr.indexOf('M');
			String tempMM = dataStr.substring(m1, m1 + 2);
			if ("02".equals(tempMM)) {
				int d1 = dateFormatStr.indexOf('d');
				String tempdd = dataStr.substring(d1, d1 + 2);
				if ("30".equals(tempdd) || "31".equals(tempdd)) return false;
				else if ("29".equals(tempdd)) {
					//// 02-29 不返回，在后面处理
				} else {
					return true;
				}
			} else if ("04".equals(tempMM) || "06".equals(tempMM) || "09".equals(tempMM) || "11".equals(tempMM)) {
				int d1 = dateFormatStr.indexOf('d');
				String tempdd = dataStr.substring(d1, d1 + 2);
				if ("31".equals(tempdd))
					return false;
				else
					return true;
			}

			try { // 02-29
				SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatStr);
				dateFormat.setLenient(false);
				dateFormat.parse(dataStr);
				return true;
			} catch (Exception e) {
				return false;
			}
		}

	}

//	private static final String regex = "\\d{4}-\\d{2}-\\d{2}";
//	private static String regex = "^([1-2]\\d{3}-)(([0]{1}[1-9]-)|([1][0-2]-))(([0-3]{1}[0-9]))$";

//	private static String regex = "^([1-2]\\d{3}-)(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[467]|1[1])-(0[1-9]|[12][0-9]|30)))$";

	private static String regex1 = "^([1-2]\\d{3}-)(((0[1-9]|1[0-2])-(0[1-9]|[12][0-9]))|((0[13578]|1[02])-3[01])|((0[469]|11)-30))$";

	private static String regex2 = "^([1-2]\\d{3})(((0[1-9]|1[0-2])(0[1-9]|[12][0-9]))|((0[13578]|1[02])3[01])|((0[469]|11)30))$";

	/**
	 * check the date string whether is 1000-01-01 ~ 2999-12-31 (yyyy-MM-dd)
	 * @param dataStr date string
	 * @return boolean value whether is right format string
	 * @since 2.1.8
	 */
	public static boolean yyyy_MM_dd(String dataStr) {
		return _checkDate(dataStr, regex1, "yyyy-MM-dd");

//		return checkDate(dataStr,"yyyy-MM-dd");

//		return _checkDate(dataStr, null, "yyyy-MM-dd");
//		return _checkDate(dataStr, "", "yyyy-MM-dd");

//		boolean flag = dataStr.matches(regexComm);
//		return flag;
	}

	/**
	 * check the date string whether is 10000101 ~ 29991231 (yyyyMMdd)
	 * @param dataStr date string
	 * @return boolean value whether is right format string
	 * @since 2.1.8
	 */
	public static boolean yyyyMMdd(String dataStr) {
		return _checkDate(dataStr, regex2, "yyyyMMdd");
	}

	private static boolean _checkDate(String dataStr, String regex, String format) {
		if (dataStr == null || regex == null) return false;

		boolean flag = dataStr.matches(regex);
//			return flag;

		if (!flag) {
			return flag;
		} else if (!(dataStr.endsWith("02-29") || dataStr.endsWith("0229"))) {
			return flag;
		} else { // yyyy-02-29
			try {
//				 LocalDate.parse(dataStr);  //这个要改格式 todo
				SimpleDateFormat dateFormat = new SimpleDateFormat(format);
				dateFormat.setLenient(false);
				dateFormat.parse(dataStr);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}

}
