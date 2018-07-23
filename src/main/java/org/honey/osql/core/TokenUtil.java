package org.honey.osql.core;

/**
 * @author KingStar
 * @since  1.2
 */
public class TokenUtil {
	 public static SqlValueWrap process(String str,String startToken,String endToken,String replaceStr){
		  
	      if (str == null || str.isEmpty()) {
		        return null;
	      }
	      int start= str.indexOf(startToken);
	      if(start<0) return null;
	      
	      SqlValueWrap wrap=new SqlValueWrap();
	      
	      StringBuffer sbf=new StringBuffer(str);
	      StringBuffer value=new StringBuffer();
	      int end;
	      int len1=startToken.length();
	      int len2=endToken.length();
	      int len3=replaceStr==null?0:replaceStr.length();
	      while(start>-1){
		  if (start > 0 && sbf.charAt(start - 1) == '\\') {
		      start= sbf.indexOf(startToken, start+len1); continue;
		  }else{
		      end=sbf.indexOf(endToken,start);
		      if(end>0){
			  value.append(",");
			  value.append(sbf.substring(start+len1, end));
			  if(replaceStr!=null) sbf.replace(start, end+1,replaceStr);
		      }
		  }
		  if(replaceStr!=null){
		      start= sbf.indexOf(startToken, start+len3); 
		  }else{
		      start= sbf.indexOf(startToken, end+len2);
		  }
	      }
	      
	      if(value.length()>0) value.deleteCharAt(0);
	      
	      wrap.setSql(sbf.toString());
	      wrap.setValueBuffer(value); //just for map's key
	
	return wrap;
   }
}
