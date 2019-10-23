/*
 * Copyright 2013-2019 the original author.All rights reserved.
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

	public void begin() {
//		try {

			if (finished) {
				finished = false;
//				new CacheDeleteThread("CacheDeleteThread start thread" + (num++)).start(); //不能再用new
//				this.setName("CacheDeleteThread" + (num++));
				this.start();
////				System.out.println("========in CacheDeleteThread==============" + Thread.currentThread().getName());
//				System.out.println("========in CacheDeleteThread==============" + this.getName());
			}
//		} catch (Exception e) {
//			System.err.println(e.getMessage());
//		}
	}

	@Override
	public void run() {
		CacheUtil.delCacheInBetween(delEndIndex); 
		finished = true;
	}
}
