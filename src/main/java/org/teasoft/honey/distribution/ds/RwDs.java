/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution.ds;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.teasoft.bee.distribution.ds.Route;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.honey.osql.core.HoneyContext;

/**
 * 一个写数据库,多个读数据库.One Write DB,more read DB.
 * @author Kingstar
 * @since  1.7.3
 */
public class RwDs implements Route{

	private String writeDs;
	private List<String> readDsList;

	private static int count = 0;
	private static int max_cout = Integer.MAX_VALUE - 1000000;
	private static Random r = new Random();
	private byte lock[] = new byte[0];
	
	private static int r_routeWay;
	
	{  //非static,每次new都会执行
		init();
	}
	
	private void init(){
		
		//要判断从配置文件拿来的信息不能为空。
		
		String wDs="ds1";
		setWriteDs(wDs);  //TODO
		List<String> rList=new ArrayList();
		rList.add("ds2");
		rList.add("ds3");
		setReadDsList(rList);
		
		getReadDsList().remove(wDs); //写库不参放在只读库列表
		r_routeWay=1; //TODO
		
	}
	
	@Override
	public String getDsName() {
		RouteStruct routeStruct = HoneyContext.getCurrentRoute();
		
		//test
		if(routeStruct==null) System.err.println("=============================== routeStruct is null");

		if (routeStruct!=null && SuidType.SELECT == routeStruct.getSuidType()) {
			return getReadDs(r_routeWay);
		} else {
			return getWriteDs();
		}
	}

	public String getWriteDs() {
		System.err.println("--------------------------getWriteDs---------------"+writeDs);
		return writeDs;
	}

	public void setWriteDs(String writeDs) {  //TODO if master change, need update
		this.writeDs = writeDs;
	}
	
	public String getReadDs() {
		
		return getReadDs(0); //rand
	}

	public String getReadDs(int type) {
		
		

		int index = 0;
		if (type == 1)
			index = poll();
		else
			index = rand();
		
		System.err.println("==============================getReadDs==============="+getReadDsList().get(index));
		return getReadDsList().get(index);
	}

	private int poll() {

		int size = readDsList.size();
		if (count > max_cout) {
			synchronized (lock) {
				if (count > max_cout) count = 0;
			}
		}
		int index = count % size;
		count++;

		return index;
	}

	private int rand() {
		int size = readDsList.size();
		int index = r.nextInt(size);

		return index;
	}

	public List<String> getReadDsList() {
		return readDsList;
	}

	public void setReadDsList(List<String> readDsList) {
		this.readDsList = readDsList;
	}

}
