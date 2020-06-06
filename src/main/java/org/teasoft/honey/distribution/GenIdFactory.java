/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.teasoft.bee.distribution.GenId;

/**
 * @author Kingstar
 * @since  1.7.3
 */
public class GenIdFactory {
	private static GenId genId;
	private static Map<String,GenId> map=new ConcurrentHashMap<>();
	
	public static long get(String type) {
		genId=getGenId(type);
		return genId.get();
	}

	public static long[] getRangeId(String type,int sizeOfIds) {
		genId=getGenId(type);
		return genId.getRangeId(sizeOfIds);
	}
	
	private static GenId getGenId(String type){
		genId=map.get(type);
		if(genId==null) {
			genId=new PearFlowerId();  //TODO 要选择不同类型
			map.put(type, genId);
		}
		return genId;
	}
}
