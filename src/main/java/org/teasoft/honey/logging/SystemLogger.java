/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.logging;

import org.teasoft.bee.logging.Log;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.util.DateUtil;

/**
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
	
	private static boolean donotPrintCurrentDate=HoneyConfig.getHoneyConfig().showSQL_donotPrintCurrentDate;
	private static boolean donotPrintLevel=HoneyConfig.getHoneyConfig().logDonotPrintLevel;
	
	private String className=null;
	
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
		if(this.className!=null) 
			print(DEBUG,msg,className);
		else
			print(DEBUG,msg);
	}
	
	@Override
	public void debug(String msg, Throwable t) {
		debug(msg);
//        if (t != null) {
//            t.printStackTrace();
//        }
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
	public void warn(String msg, Throwable t) {
		warn(msg);
//        if (t != null) {
//            t.printStackTrace();
//        }
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
        if (t != null) {
            t.printStackTrace();
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
		
		if(ERROR.equals(level) || WARN.equals(level))
			System.err.println(b.toString());
		else
		   System.out.println(b.toString());
		
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
		
		if(ERROR.equals(level) || WARN.equals(level))
			System.err.println(b.toString());
		else
		   System.out.println(b.toString());
	}
}