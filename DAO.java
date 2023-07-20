package DAO;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;

import TableMapping.Column;
import TableMapping.TableInfo;
import database.Connector;
import utilities.MyParser;

public class DAO {
    
    public Object insert(Connection con){
        return new Object();
    }

    public Object update(Connection con) {
        return new Object();
    }

    public Object delete(Connection con){
        return new Object();
    }

    /*
     * As it says, this method is used to retrieve data from database
     */
    public Vector select(Connection con) throws Exception{
        TableInfo info=null;
        if (!this.getClass().isAnnotationPresent(TableInfo.class)) {
            throw new Exception("Please, define the database connectivity credentials!");
        }
        Vector<Object> data=new Vector<>();
        //Getting the TableInfo Annotation
        info=this.getClass().getAnnotation(TableInfo.class);
        
        //Define whether this method opened a connection
        boolean opened=false;
        if (con==null) {
            con=Connector.Connect(info.jdbc(), info.user(), info.pass(), info.database());
            opened=true;
        }
        //Getting the table name
        String tableName=info.name();
        if (tableName.equalsIgnoreCase("")) {
            //Setting the tableName as the class name if the name isn't defined in the annotation
            tableName=this.getClass().getSimpleName();
        }
        
        PreparedStatement state=this.getFieldPredicates(con, tableName);
        ResultSet res=state.executeQuery();
        while (res.next()) {
            Constructor construct=this.getClass().getConstructor();
            Object instance=construct.newInstance();
            Field[] fields=instance.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Column colinfo=null;
                if (field.isAnnotationPresent(Column.class)) {
                    colinfo=field.getAnnotation(Column.class);
                    Object databaseObject=MyParser.parse(res.getObject(colinfo.name()), field.getType());
                    field.set(instance, field.getType().cast(databaseObject));
                }
            }
            data.add(instance);
        }
        //Closing the connection if this method opened one
        if (opened==true) {
            con.close();
        }
        return data;
    }

    /*
     * This method is used to construct the Statement of the SQL query
     */
    PreparedStatement getFieldPredicates(Connection con,String tableName) throws Exception{
        String query="select * from "+tableName+" where ";
        Field[] fields=this.getClass().getDeclaredFields();
        ArrayList<Field> settedField=new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Column colInfo=null;
            if (field.isAnnotationPresent(Column.class)&&field.get(this)!=null) {
                colInfo=field.getAnnotation(Column.class);
                query+=colInfo.name()+"= ? and ";
                settedField.add(field);
            }
        }
        PreparedStatement prep=null;
        if (settedField.size()<1) {
            query+="1=1";
            return con.prepareStatement(query);
        } else{
            query=query.substring(0,query.lastIndexOf("and"));
            prep=con.prepareStatement(query);
            int index=1;
            for (Field field : settedField) {
                Method getter=this.getClass().getDeclaredMethod("get"+this.Capitalize(field.getName()));
                prep.setObject(index, getter.invoke(this));
                index++;
            }
        }
        return prep;
    }

    /*
     * Used to capitalize the first letter of a string
     */
    public String Capitalize(String input){
        String up=input.substring(0, 1).toUpperCase().concat(input.substring(1));
        return up;
    }
    
}