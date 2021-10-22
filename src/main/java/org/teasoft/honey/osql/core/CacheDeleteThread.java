/*
 * Copyright 2013-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

/**
 * @author Kingstar
 * @since  1.4
 */
public class CacheDeleteThread extends Thread {

	private static boolean finished = true;
//	private static int num = 0;

	private int delEndIndex;

	public CacheDeleteThread() {}

	public CacheDeleteThread(int delEndIndex) {
		this.delEndIndex = delEndIndex;
	}

	public CacheDeleteThread(String name) {
		super(name);
	}

	public static synchronized boolean getFlag() {
		boolean f=finished;
		if(f) finished = false;
		return f;
	}
	public void begin() {
			if (getFlag()) {
				
//				new CacheDeleteThread("CacheDeleteThread start thread" + (num++)).start(); //不能再用new
//				this.setName("CacheDeleteThread" + (num++));
				
				this.start();
			}
	}

	@Override
	public void run() {
		CacheUtil.delCacheInBetween(delEndIndex); 
		finished = true;
	}
}
