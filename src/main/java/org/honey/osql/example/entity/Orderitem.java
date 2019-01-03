package org.honey.osql.example.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
*@author Honey
*Create on 2018-08-05 14:12:48
*/
public class Orderitem implements Serializable {

	private static final long serialVersionUID = 15987743456878723L;

	private Long id;
	private String userid;
	private Long orderid;
	private String category;
	private BigDecimal price;
	private String status;
	private String remark;
	private Timestamp createtimne;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public Long getOrderid() {
		return orderid;
	}

	public void setOrderid(Long orderid) {
		this.orderid = orderid;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Timestamp getCreatetimne() {
		return createtimne;
	}

	public void setCreatetimne(Timestamp createtimne) {
		this.createtimne = createtimne;
	}

	 public String toString(){	
		 StringBuffer str=new StringBuffer();	
		 str.append("Orderitem[");			
		 str.append("id=").append(id);		 
		 str.append(",userid=").append(userid);		 
		 str.append(",orderid=").append(orderid);		 
		 str.append(",category=").append(category);		 
		 str.append(",price=").append(price);		 
		 str.append(",status=").append(status);		 
		 str.append(",remark=").append(remark);		 
		 str.append(",createtimne=").append(createtimne);		 
		 str.append("]");			 
		 return str.toString();			 
	 }		 
}