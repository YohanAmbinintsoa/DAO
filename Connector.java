package database;

import java.lang.reflect.Method;
import java.sql.*;
/* 
 * This Class is used to connect to any JDBC by specifying the name
*/
public class Connector {
    
    public Connection postgresql(String user,String password,String database) throws Exception{
        Class.forName("org.postgresql.Driver");
        Connection con=DriverManager.getConnection("jdbc:postgresql://localhost:5432/"+database,user,password);
        con.setAutoCommit(false);
        return con;
    }

    public Connection oracle(String user,String password,String database) throws Exception{
        Class.forName("oracle.jdbc.driver.OracleDriver");
        Connection con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:"+database, user, password);
        return con;
    }

    public static Connection Connect(String jdbc,String user,String password,String database) throws Exception{
        Connector connect=new Connector();
        Method met=connect.getClass().getMethod(jdbc, String.class,String.class,String.class);
        Connection con=(Connection)met.invoke(connect, user,password,database);
        return con;
    }

    public static String postgresSequence(String seqname){
        return "select nextval('"+seqname+"')";
    }

    public static String oracleSequence(String seqname){
        return seqname+".nextval";
    }

    public static String getSequenceSyntax(String jdbc,String seq_name){
        if (jdbc.equals("postgresql")) {
            return Connector.postgresSequence(seq_name);
        } else if (jdbc.equals("oracle")) {
            return Connector.oracleSequence(seq_name);
        }
        return "";
    }
}
