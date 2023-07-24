package Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Vector;

import Test.Personne;
import database.Connector;
import utilities.MyParser;

public class Main {
    public static void main(String[] args) {
        try {
            Personne pres=new Personne();
            Vector<Personne> all=pres.select(null);
            for (Personne personne : all) {
                System.out.println(personne.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}