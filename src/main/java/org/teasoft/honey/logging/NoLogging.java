/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.logging;

import org.teasoft.bee.logging.Log;

/**
 * @author Kingstar
 * @since  1.8
 */
public class NoLogging implements Log {

    private String loggerName;

    public NoLogging(){}
    
    public NoLogging(String loggerName){
        this.loggerName = loggerName;
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void error(String msg, Throwable e) {
        error(msg);
        if (e != null) {
            e.printStackTrace();
        }
    }

    @Override
    public void error(String msg) {
        if (msg != null) {
            System.err.println(loggerName + " : " + msg);
        }
    }

    @Override
    public void debug(String msg) {
    }

    @Override
    public void debug(String msg, Throwable e) {
    }

    @Override
    public void warn(String msg) {
        if (msg != null) {
            System.err.println(loggerName + " : " + msg);
        }
    }

    @Override
    public void warn(String msg, Throwable e) {
    	warn(msg);
        if (e != null) {
            e.printStackTrace();
        }
    }

	@Override
	public boolean isTraceEnabled() {
		return false;
	}

	@Override
	public void trace(String msg) {
	}

	@Override
	public boolean isInfoEnabled() {
		return false;
	}

	@Override
	public void info(String msg) {
	}

	@Override
	public boolean isWarnEnabled() {
		return false;
	}

	@Override
	public boolean isErrorEnabled() {
		return false;
	}
}
