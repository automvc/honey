/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.logging;

import org.teasoft.bee.logging.Log;

/**
 * @author Kingstar
 * @since  1.4
 */
public class LoggerFactory {
	private static Log log;
	
	private LoggerFactory(){}
	
	public static Log getLog() {
		if(log==null) {
			log = new SystemLogger();
		}
		return log;
	}

	public void setLog(Log log) {
		this.log = log;
	}
	
	public static Log getLogger(){
		return getLog();
	}
	
	public static Log getLogger(String name){
		return getLog().getLogger(name);
	}
	
	public  static Log getLogger(Class<?> clazz) {
		return getLog().getLogger(clazz);
	}
}

