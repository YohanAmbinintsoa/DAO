package Test;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Vector;

import Test.Personne;
import database.Connector;
import utilities.MyParser;

public class Main {
    public static void main(String[] args) {
        try {
            Personne pres=new Personne();
            Vector<Personne> allPersonne=pres.select(null);
            for (Personne personne : allPersonne) {
                System.out.println(personne.getBirth());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}