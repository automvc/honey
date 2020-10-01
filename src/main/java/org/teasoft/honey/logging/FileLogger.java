/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.logging;

import java.io.File;
import java.text.SimpleDateFormat;

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
	
	private String className=null;
	
	private String LINE_SEPARATOR = System.getProperty("line.separator"); // 换行符
	
	private static String TRACE="TRACE";
	private static String DEBUG="DEBUG";
	private static String INFO="INFO";
	private static String WARN="WARN";
	private static String ERROR="ERROR";
	private static String SPACE=" ";
	private static String LEFT="[";
	private static String RIGHT="]";
	
	private static boolean donotPrintCurrentDate=HoneyConfig.getHoneyConfig().isShowSQL_donotPrint_currentDate();
	private static boolean donotPrintLevel=HoneyConfig.getHoneyConfig().isLog_donotPrint_level();
	
	public FileLogger(){
	}
	
	public FileLogger(String className){
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
		if(this.className!=null) 
			print(DEBUG,msg+LINE_SEPARATOR+t.getMessage(),className);
		else
			print(DEBUG,msg+LINE_SEPARATOR+t.getMessage());	
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
		if(this.className!=null) 
			print(WARN,msg+LINE_SEPARATOR+t.getMessage(),className);
		else
			print(WARN,msg+LINE_SEPARATOR+t.getMessage());	
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
		if(this.className!=null) 
			print(ERROR,msg+LINE_SEPARATOR+t.getMessage(),className);
		else
			print(ERROR,msg+LINE_SEPARATOR+t.getMessage());	
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
			b.append(LEFT)
			 .append(level)
			 .append(RIGHT)
			 .append(SPACE);
		}
		
		b.append(msg);
		
		appendFile(b.toString());
		
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
		
		appendFile(b.toString());
	}
	
	private void appendFile(String content) {

		if (Path.getFullPath() == null || "".equals(Path.getFullPath())) { //v1.8.15
			
			SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd HH.mm.ss.SS");
			String datetime=df.format(System.currentTimeMillis());
			
			String fileSeparator=File.separator;
			String path=System.getProperty("user.dir") + fileSeparator + "src" + fileSeparator + "main" + fileSeparator
					+ "resources" + fileSeparator + "log" + fileSeparator + "{datatime}.txt".replace("{datatime}", datetime);
			System.err.println("[Bee] [WARN] Set the path for FileLogger automatically:  " + path);
			
			//set the path and file name of log file
			Path.setFullPath(path);
		}

		FileUtil.genAppendFile(Path.getFullPath(), content);
	}
	
}