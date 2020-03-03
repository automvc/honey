/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.logging;

import org.teasoft.bee.logging.Log;
import org.teasoft.bee.logging.Path;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.util.DateUtil;
import org.teasoft.honey.osql.util.FileUtil;

/**
 * @author Kingstar
 * @since  1.4
 */
public class FileLogger implements Log{
	
	private static String TRACE="TRACE";
	private static String DEBUG="DEBUG";
	private static String INFO="INFO";
	private static String WARN="WARN";
	private static String ERROR="ERROR";
	private static String APACE=" ";
	private static String LEFT="[";
	private static String RIGHT="]";
	
	private static boolean donotPrintCurrentDate=HoneyConfig.getHoneyConfig().isShowSQL_donotPrint_currentDate();
	private static boolean donotPrintLevel=HoneyConfig.getHoneyConfig().isLog_donotPrint_level();
	
	public FileLogger(){
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
		if(this.className!=null) 
			print(DEBUG,msg,className);
		else
			print(DEBUG,msg);
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public void info(String msg) {
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
		if(this.className!=null) 
			print(WARN,msg,className);
		else
			print(WARN,msg);
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
		
	}
	
	@Override
	public boolean isOff() {
		return false;
	}


	private void print(String level,String msg){
		StringBuffer b=new StringBuffer();
		
		if (donotPrintCurrentDate) {
			//nothing
		} else {
			b.append(DateUtil.currentDate());
			b.append(APACE);
		}
		
		if(donotPrintLevel){
			//nothing
		}else{
			b.append(level)
			 .append(APACE);
		}
		
		b.append(msg);
		
//		if(ERROR.equals(level) || WARN.equals(level))
//			System.err.println(b.toString());
//		else
//		   System.out.println(b.toString());
		
		appendFile(b.toString());
		
	}
	
	private void print(String level,String msg,String className){
		StringBuffer b=new StringBuffer();
		
		if (donotPrintCurrentDate) {
			//nothing
		} else {
			b.append(DateUtil.currentDate());
			b.append(APACE);
		}
		
		if(donotPrintLevel){
			//nothing
		}else{
			b.append(level)
			 .append(APACE);
		}
		
		b
		 .append(LEFT)
		 .append(className)
		 .append(RIGHT)
		 .append(APACE)
		
		 .append(msg);
		
//		if(ERROR.equals(level) || WARN.equals(level))
//			System.err.println(b.toString());
//		else
//		   System.out.println(b.toString());
		
		appendFile(b.toString());
	}
	
	private void appendFile(String content){
		FileUtil.genAppendFile(Path.getFullPath(), content);
	}

	@Override
	public Log getLogger() {
		this.className=null;
		return this;
	}

	@Override
	public Log getLogger(String name) {
		FileLogger sLog=new FileLogger();
		sLog.className=name;
		return sLog;
	}

	@Override
	public Log getLogger(Class<?> clazz) {
		return getLogger(clazz.getName());
	}
	
	private String className=null;
	
}