/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.teasoft.bee.logging.Log;

/**
 * @author Kingstar
 * @since  1.8
 */
public class Jdk14LoggingImpl implements Log {

	private Logger log;

	private String loggerName;

	public Jdk14LoggingImpl(String loggerName) {
		this.loggerName = loggerName;
		log = Logger.getLogger(loggerName);
	}

	@Override
	public boolean isDebugEnabled() {
		return log.isLoggable(Level.FINE);
	}

	@Override
	public void error(String msg, Throwable e) {
		log.logp(Level.SEVERE, loggerName, Thread.currentThread().getStackTrace()[1].getMethodName(), msg, e);
	}

	@Override
	public void error(String msg) {
		log.logp(Level.SEVERE, loggerName, Thread.currentThread().getStackTrace()[1].getMethodName(), msg);
	}

	@Override
	public void debug(String msg) {
		log.logp(Level.FINE, loggerName, Thread.currentThread().getStackTrace()[1].getMethodName(), msg);
	}

	@Override
	public void debug(String msg, Throwable e) {
		log.logp(Level.FINE, loggerName, Thread.currentThread().getStackTrace()[1].getMethodName(), msg, e);
	}

	@Override
	public void warn(String msg) {
		log.logp(Level.WARNING, loggerName, Thread.currentThread().getStackTrace()[1].getMethodName(), msg);
	}

	@Override
	public void warn(String msg, Throwable e) {
		log.logp(Level.WARNING, loggerName, Thread.currentThread().getStackTrace()[1].getMethodName(), msg, e);
	}

	@Override
	public void info(String msg) {
		log.logp(Level.INFO, loggerName, Thread.currentThread().getStackTrace()[1].getMethodName(), msg);
	}

	@Override
	public boolean isInfoEnabled() {
		return log.isLoggable(Level.INFO);
	}

	@Override
	public boolean isWarnEnabled() {
		return log.isLoggable(Level.WARNING);
	}

	@Override
	public boolean isTraceEnabled() {
		return log.isLoggable(Level.FINER);
	}

	@Override
	public void trace(String msg) {
		//		log.log(Level.FINER, msg);
		log.logp(Level.FINER, loggerName, Thread.currentThread().getStackTrace()[1].getMethodName(), msg);
	}

	@Override
	public boolean isErrorEnabled() {
		return log.isLoggable(Level.SEVERE); //TODO
	}

}
