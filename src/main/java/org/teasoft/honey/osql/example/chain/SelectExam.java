package org.teasoft.honey.osql.example.chain;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.chain.Select;
import org.teasoft.bee.osql.chain.UnionSelect;
import org.teasoft.honey.osql.chain.SelectImpl;
import org.teasoft.honey.osql.chain.UnionSelectImpl;

/**
 * @author Kingstar
 * @since  1.3
 */
public class SelectExam {
public static void main(String[] args) {
	try{	
	  SelectImpl joinSelect =new SelectImpl();
		joinSelect.select("*")
		.from("Orders,Orderitem")
		.where("Orders.id=Orderitem.orderid")
		.start(2)
		.size(10)
		;
		System.out.println(joinSelect.toSQL());
		
		UnionSelect unionSelect =new UnionSelectImpl();
		Select select1 =new SelectImpl();
		Select select2 =new SelectImpl();
		select1.select()
		.from("orders")
		.where()
		.op("userid", Op.eq, "client01")
		;
		select2.select()
		.from("orders")
//		.where()
//		.op("userid", Op.eq, "bee")
		;
		unionSelect.union(select1, select2);
//		unionSelect.unionAll(select1, select2);
		System.out.println(unionSelect.toSQL());
	  } catch (BeeException e) {
		e.printStackTrace();
	  }
	}
}
