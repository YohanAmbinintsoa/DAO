package utilities;

import java.text.SimpleDateFormat;

public class MyParser {
    public  static Object parse(Object toParse,Class parser) throws Exception{
        if (parser.equals(Integer.class)) {
            return Integer.valueOf(toParse.toString());
        } else if (parser.equals(Float.class)) {
            return Float.valueOf(toParse.toString());
        } else if (parser.equals(java.util.Date.class)) {
            SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd");
            return formatter.parse(toParse.toString());
        } else if (parser.equals(java.sql.Date.class)){
            SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd");
            return new java.sql.Date(formatter.parse(toParse.toString()).getTime());
        } else if (parser.equals(java.sql.Timestamp.class)) {
            SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            return new java.sql.Timestamp(formatter.parse(toParse.toString().replace("T", " ")).getTime());
        } else if(parser.equals(String.class)){
            return toParse.toString();
        }
        return null;
    }
}
