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
import org.teasoft.bee.osql.exception.NoConfigException;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.HoneyContext;

/**
 * 一个写数据库,多个读数据库.One Write DB,more read DB.
 * @author Kingstar
 * @since  1.8
 */
public class RwDs implements Route{

	private String writeDd;
	private List<String> readDsList;

	private static int count = 0;
	private static int max_cout = Integer.MAX_VALUE - 1000000;
	private static Random r = new Random();
	private byte lock[] = new byte[0];
	
	private static int r_routeWay;
	
	{  //非static,每次new都会执行.若writer有更新,这样可以刷新.
		init();
	}
	
	private void init(){
		String wDB=HoneyConfig.getHoneyConfig().multiDs_wDB;
		String rDB_str=HoneyConfig.getHoneyConfig().multiDs_rDB;
		//要判断从配置文件拿来的信息不能为空。
		if( (wDB==null || "".equals(wDB.trim()))  ||  (wDB==null || "".equals(wDB.trim()))){
			throw new NoConfigException("Error: bee.dosql.multi-DS.wDB and bee.dosql.multi-DS.rDB can not null or empty when bee.dosql.multi-DS.type=1! ");
		}
		
		setWriteDs(wDB);  
		setReadDsList(parseRDb(rDB_str));
		getReadDsList().remove(wDB); //写库不能放在只读库列表
		r_routeWay=HoneyConfig.getHoneyConfig().rDbRouteWay; 
	}
	
	private List<String> parseRDb(String rDB_str){
		String s[]=rDB_str.split(",");
		List<String> rList=new ArrayList<>();
		for (int i = 0; i < s.length; i++) {
			rList.add(s[i]);
		}
		return rList;
	}
	
	@Override
	public String getDsName() {
		RouteStruct routeStruct = HoneyContext.getCurrentRoute();
		
		//test
//		if(routeStruct==null) System.err.println("=============================== routeStruct is null");

		if (routeStruct!=null && SuidType.SELECT == routeStruct.getSuidType()) {
			return getReadDs(r_routeWay);
		} else {
			return getWriteDs();
		}
	}

	public String getWriteDs() {
//		System.err.println("--------------------------getWriteDs---------------"+writeDd);
		return writeDd;
	}

	public void setWriteDs(String writeDs) {  //TODO if master change, need update
		this.writeDd = writeDs;
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
		
//		System.err.println("==============================getReadDs==============="+getReadDsList().get(index));
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
