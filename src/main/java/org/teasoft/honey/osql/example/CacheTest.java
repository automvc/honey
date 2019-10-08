/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.example;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.Suid;
import org.teasoft.honey.osql.core.BeeFactory;
import org.teasoft.honey.osql.example.entity.Orders;

/**
 * @author Kingstar
 * @since  1.1
 */
public class CacheTest {
	public static void main(String[] args) {
		
		 try {
				
			Suid suid=BeeFactory.getHoneyFactory().getSuid();
			
			Orders orders0=new Orders();
			orders0.setUserid("bee0");
			
			Orders orders1=new Orders();
			orders1.setId(100001L);
			orders1.setName("Bee--ORM Framework");
			
			Orders orders2=new Orders();
			orders2.setUserid("bee2");
			orders2.setName("Bee--ORM Framework");
			orders2.setRemark("");  //empty String test
			
			Orders orders3=new Orders();
			orders3.setUserid("bee3");
			
			Orders orders4=new Orders();
			orders4.setUserid("bee4");
			
			Orders orders5=new Orders();
			orders5.setUserid("bee5");
			
			Orders orders6=new Orders();
			orders6.setUserid("bee6");
			
			Orders orders7=new Orders();
			orders7.setUserid("bee7");
			
			Orders orders8=new Orders();
			orders8.setUserid("bee8");
			
			Orders orders9=new Orders();
			orders9.setUserid("bee9");
			
			Orders orders10=new Orders();
			orders10.setUserid("bee10");
			
			Orders orders11=new Orders();
			orders11.setUserid("bee11");
			
			Orders orders12=new Orders();
			orders12.setUserid("bee12");
			
			
			
			suid.select(orders0);
			suid.select(orders1);
			
			orders1.setRemark("other");
			suid.update(orders1);
			suid.select(orders1);
			
			suid.select(orders2);
			suid.select(orders3);
			suid.select(orders4);
			suid.select(orders5);
			suid.select(orders6);
			suid.select(orders6); //select again
			
			try {
				Thread.sleep(12000);
			} catch (Exception e) {
				// TODO: handle exception
			}
			
			suid.select(orders3);  //delete 0,3
			
			suid.select(orders7);
			suid.select(orders8);
			suid.select(orders9);
			suid.select(orders10);
			
			try {
				Thread.sleep(12000);
			} catch (Exception e) {
				// TODO: handle exception
			}
			
			suid.select(orders3);
			suid.select(orders8);
			suid.select(orders11);
			
			
			try {
				Thread.sleep(12000);
			} catch (Exception e) {
				// TODO: handle exception
			}
			
//			suid.select(orders8);  //delete one
			suid.select(orders11);//delte some
			suid.select(orders8);  
			
		  } catch (BeeException e) {
			 e.printStackTrace();
		  }
	}
}
