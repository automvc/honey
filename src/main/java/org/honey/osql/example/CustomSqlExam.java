package org.honey.osql.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bee.osql.PreparedSQL;
import org.honey.osql.core.BeeFactory;
import org.honey.osql.core.CustomSqlLib;
import org.honey.osql.example.entity.Orders;

public class CustomSqlExam {
	public static void main(String[] args) {

		PreparedSQL preparedSql = BeeFactory.getHoneyFactory().getPreparedSQL();

		String sql = CustomSqlLib.getCustomSql("osql.example.entity.selectOrders");
		System.out.println("getCustomSql:  " + sql); //只能在一行的.

		List<Orders> list1 = preparedSql.selectSomeField(sql, new Orders(), new Object[] { "bee" });
		for (int i = 0; i < list1.size(); i++) {
			System.out.println(list1.get(i));
		}

		String sql2 = CustomSqlLib.getCustomSql("osql.example.entity.selectOrdersViaMap");
		System.out.println("getCustomSql:  " + sql2); 

		Map<String, Object> map = new HashMap<>();
		map.put("userid", "bee");
		List<Orders> list2 = preparedSql.select(sql2, new Orders(), map);//map
		for (int i = 0; i < list2.size(); i++) {
			System.out.println(list2.get(i));
		}
		
		
		String sql3 = CustomSqlLib.getCustomSql("osql.example.entity.selectOrdersLikeNameViaMap");
		System.out.println("getCustomSql:  " + sql3); 

		Map<String, Object> map2 = new HashMap<>();
		map2.put("name", "Bee");
		List<Orders> list3 = preparedSql.select(sql3, new Orders(), map2);//map
		for (int i = 0; i < list3.size(); i++) {
			System.out.println(list3.get(i));
		}

	}

}
