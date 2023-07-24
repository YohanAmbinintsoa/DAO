package DAO;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
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
    /*
     * Insert data to Table
     */
    public Object insert(Connection con) throws Exception{
        TableInfo info=null;
        if (!this.getClass().isAnnotationPresent(TableInfo.class)) {
            throw new Exception("Please, define the database connectivity credentials!");
        }
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

        PreparedStatement state=this.getInsertionQuery(con,tableName,info);
        state.execute();
        //Closing the connection if this method opened one
        if (opened==true) {
            con.commit();
            con.close();
        }
        return new Object();
    }

    String fieldToList(Field[] fields) throws Exception{
        TableInfo tableInfo=this.getClass().getAnnotation(TableInfo.class);
        String list="(";
        for (Field field : fields) {
            field.setAccessible(true);
            Column colInfo=null;
            if (field.isAnnotationPresent(Column.class)) {
                colInfo=field.getAnnotation(Column.class);
                if (colInfo.isPrimary()==false) {
                    list+=colInfo.name()+",";
                }
                if (colInfo.isPrimary()==true&&tableInfo.idtype()!=0) {
                    list+=colInfo.name()+",";
                }
            }
        }
        list=list.substring(0,list.lastIndexOf(","));
        list+=")";
        return list;
    }

    PreparedStatement getInsertionQuery(Connection con,String tablename,TableInfo tab) throws Exception{
        Field[] fields=this.getClass().getDeclaredFields();
        String query="INSERT INTO "+tablename+" "+this.fieldToList(fields)+" values(";
        ArrayList<Field> settedField=new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Column colInfo=null;
            if (field.isAnnotationPresent(Column.class)) {
                colInfo=field.getAnnotation(Column.class);
                String value="NULL,";
                if (field.get(this)!=null&&colInfo.isPrimary()==false) {
                        value="?,";
                        settedField.add(field);
                } else if(field.get(this)==null&&colInfo.isPrimary()==true&&tab.idtype()!=0) {
                    value="'"+this.primaryKey(con,tab)+"',";
                    System.out.println("Misy PK="+value);
                } else if(field.get(this)==null&&colInfo.isPrimary()==true&&tab.idtype()==0){
                    value="";
                }
                query+=value;
            }
        }
        query=query.substring(0, query.lastIndexOf(","));
        query+=")";
        System.out.println(query);
        PreparedStatement prep=null;
        if(settedField.size()>0){
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

    void CreateSequence(Connection con,String sequenceName) throws Exception{
        Statement state=con.createStatement();
        state.execute("create sequence "+sequenceName+" start with 1 increment by 1");
        con.commit();
    }

    String getSequenceValue(Connection con,TableInfo tab) throws Exception{
        String sequenceName=tab.sequence();
        if (sequenceName.equalsIgnoreCase("")) {
            sequenceName=this.getClass().getSimpleName()+"_seq";
        }
        String syntax=Connector.getSequenceSyntax(tab.jdbc(), sequenceName);
        DatabaseMetaData metaData=con.getMetaData();
        ResultSet resultSet = metaData.getTables(null, "public", sequenceName, new String[]{"SEQUENCE"});
        if (!resultSet.next()) {
            resultSet.close();
            this.CreateSequence(con, sequenceName);
        }
        Statement state=con.createStatement();
        ResultSet seq=state.executeQuery(syntax);
        String value="";
        if (seq.next()) {
            value=String.valueOf(seq.getInt(1));
        }
        return value;
    }

    String primaryKey(Connection con,TableInfo tab) throws Exception{
        String idName=this.getClass().getSimpleName().toUpperCase();
        String seqValue=this.getSequenceValue(con, tab);
        for (int i = 0; i < tab.dimId()-seqValue.length(); i++) {
            idName+="0";
        }
        idName+=seqValue;
        return idName;
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

    public Object update(Connection con) {
        return new Object();
    }

    public Object delete(Connection con){
        return new Object();
    }
    
}