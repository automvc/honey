/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.logging;

import java.util.HashMap;
import java.util.Map;

import org.teasoft.bee.logging.Log;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.util.DateUtil;

/**
 * System Logger,print the error message to console.
 * eg:System.out, System.err
 * @author Kingstar
 * @since  1.4
 */
public class SystemLogger implements Log{
	
	private static final String TRACE="TRACE";
	private static final String DEBUG="DEBUG";
	private static final String INFO="INFO";
	private static final String WARN="WARN";
	private static final String ERROR="ERROR";
	private static final String SPACE=" ";
	private static final String LEFT="[";
	private static final String RIGHT="]";
	
	private static boolean donotPrintCurrentDate=HoneyConfig.getHoneyConfig().showSql_donotPrintCurrentDate;
	private static boolean donotPrintLevel=HoneyConfig.getHoneyConfig().logDonotPrintLevel;
	
	private String className=null;
	private static Map<String,Integer> levelMap;
	
	private static String systemLoggerLevel=HoneyConfig.getHoneyConfig().systemLoggerLevel;
	private static int level;
	private static final int DEBUG_NUM=1;
	private static final int INFO_NUM=2;
	private static final int WARN_NUM=3;
//	private static final int ERROR_NUM=4;
	
	static {
		levelMap = new HashMap<>();
		levelMap.put(DEBUG, 1);
		levelMap.put(INFO, 2);
		levelMap.put(WARN, 3);
		levelMap.put(ERROR, 4);
		
		level=levelMap.get(systemLoggerLevel.toUpperCase());
	}
	
	public SystemLogger(){
	}
	
	public SystemLogger(String className){
		this.className=className;
	}

	@Override
	public boolean isTraceEnabled() {
		return true;
	}

	@Override
	public void trace(String msg) {
		if(this.className!=null) 
			print(TRACE,msg,className);
		else
			print(TRACE,msg);
	}

	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public void debug(String msg) {
		if (level > DEBUG_NUM) return;
		if(this.className!=null) 
			print(DEBUG,msg,className);
		else
			print(DEBUG,msg);
	}
	
	@Override
	public void debug(String msg, Throwable t) {
		if (level > DEBUG_NUM) return;
		debug(msg);
		_printStackTrace(t);
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public void info(String msg) {
		if (level > INFO_NUM) return;
		if(this.className!=null) 
			print(INFO,msg,className);
		else
			print(INFO,msg);
		
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public void warn(String msg) {
		if (level > WARN_NUM) return;
		if(this.className!=null) 
			print(WARN,msg,className);
		else
			print(WARN,msg);
	}
	
	@Override
	public void warn(String msg, Throwable t) {
		if (level > WARN_NUM) return;
		warn(msg);
		_printStackTrace(t);
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public void error(String msg) {
		if(this.className!=null) 
			print(ERROR,msg,className);
		else
			print(ERROR,msg);		
	}

	@Override
	public void error(String msg, Throwable t) {
		error(msg);
		//开发时可打开调试
		_printStackTrace(t);
	}
	
	private void _printStackTrace(Throwable t) {
		if (t != null) {
			t.printStackTrace(); // SystemLogger print the error message to console.
		}
	}
	
	private void print(String level,String msg){
		StringBuffer b=new StringBuffer();
		
		if (donotPrintCurrentDate) {
			//nothing
		} else {
			b.append(DateUtil.currentDate());
			b.append(SPACE);
		}
		
		if(donotPrintLevel){
			//nothing
		}else{
//			b.append(level)
//			 .append(SPACE);
			
			b.append(LEFT)
			 .append(level)
			 .append(RIGHT)
			 .append(SPACE);
		}
		
		b.append(msg);
		
		if(ERROR.equals(level) || WARN.equals(level) || DEBUG.equals(level)) //为了便于识别,DEBUG在SystemLogger使用err,让其显示为红色.
			printerr(b.toString());
		else
			printout(b.toString());
		
	}
	
    private void printerr(String msg) {
    	System.err.println(msg);
    }
    
    private void printout(String msg) {
    	System.out.println(msg);
    }
	
	private void print(String level,String msg,String className){
		StringBuffer b=new StringBuffer();
		
		if (donotPrintCurrentDate) {
			//nothing
		} else {
			b.append(DateUtil.currentDate());
			b.append(SPACE);
		}
		
		if(donotPrintLevel){
			//nothing
		}else{
//			b.append(level)
//			 .append(SPACE);
			
			b.append(LEFT)
			 .append(level)
			 .append(RIGHT)
			 .append(SPACE);
		}
		
		b
		 .append(LEFT)
		 .append(className)
		 .append(RIGHT)
		 .append(SPACE)
		
		 .append(msg);
		
		if(ERROR.equals(level) || WARN.equals(level) || DEBUG.equals(level))
			printerr(b.toString());
		else
			printout(b.toString());
	}
}