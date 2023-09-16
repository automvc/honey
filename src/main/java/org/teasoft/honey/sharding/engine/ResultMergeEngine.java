/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;

import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.util.ObjectUtils;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class ResultMergeEngine {
	
	// result merge
	public static <T> List<T> merge(CompletionService<List<T>> completionService, int size) {

		List<T> rsList = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			try {
				List<T> t = completionService.take().get();
				if (ObjectUtils.isNotEmpty(t)) rsList.addAll(t);
			} catch (InterruptedException e) {
				Logger.error(e.getMessage(), e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				Logger.error(e.getMessage(), e);
			}
		}

		return rsList;
	}
	
	
	public static List<String> mergeJsonResult(CompletionService<String> completionService, int size) {
		List<String> rsList = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			try {
				String tempStr = completionService.take().get();
				if (ObjectUtils.isNotEmpty(tempStr) && !"[]".equals(tempStr))
					rsList.add(tempStr);
			} catch (InterruptedException e) {
				Logger.error(e.getMessage(), e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				Logger.error(e.getMessage(), e);
			}
		}
		return rsList;
	}
	
	public static List<String> mergeFunResult(CompletionService<String> completionService, int size) {
		List<String> rsList = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			try {
				String tempStr = completionService.take().get();
				if (ObjectUtils.isNotEmpty(tempStr))
					rsList.add(tempStr);
			} catch (InterruptedException e) {
				Logger.error(e.getMessage(), e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				Logger.error(e.getMessage(), e);
			}
		}
		return rsList;
	}
	
	public static int mergeInteger(CompletionService<Integer> completionService, int size) {
		int r = 0;
		for (int i = 0; i < size; i++) {
			try {
				Integer part = completionService.take().get();
				if (part != null) r += part;
			} catch (InterruptedException e) {
				Logger.error(e.getMessage(), e);
				Thread.currentThread().interrupt();
			} catch (Exception e) { // java.lang.NullPointerException, 在分片批量插入时,多线程下,有可能pkName设置不成功(在HoneyUtil)引起.
				Logger.error(e.getMessage(), e);
			}
		}
		return r;
	}

}
