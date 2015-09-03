package fr.gabbro.balsamiq.parser.service.serviceimpl
// Gabbro - scala program to manipulate balsamiq sketches files an generate code with FreeMarker
// Version 1.0
// Copyright (C) 2014 Georges Lipka
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of either one of the following licences:
//
// 1. The Eclipse Public License (EPL) version 1.0,
//   available at http://www.eclipse.org/legal/epl-v10.html
//
// 2. The GNU Lesser General Public License (LGPL) version 2.1 or later,
//    available at http://www.gnu.org/licenses/lgpl.txt
//
// This program is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the individual licence texts for more details.

import java.sql.DriverManager
import java.sql.Connection
import java.sql.Statement
import org.json.JSONObject
import org.json.XML;
import fr.gabbro.balsamiq.parser.service.TIBalsamiqFreeMarker
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * @author Georges Lipka
 */
class Sqlite extends TIBalsamiqFreeMarker {

  def test() {

    try {
      Class.forName("org.sqlite.JDBC");
      val dbURL = "jdbc:sqlite:c:/temp/balsamiq/balsamiq3/test1.bmpr";
      val conn = DriverManager.getConnection(dbURL);
      if (conn != null) {
        System.out.println("Connected to the database");
        val dm = conn.getMetaData();
        System.out.println("Driver name: " + dm.getDriverName());
        System.out.println("Driver version: " + dm.getDriverVersion());
        System.out.println("Product name: " + dm.getDatabaseProductName());
        System.out.println("Product version: " + dm.getDatabaseProductVersion());
        conn.close();
      }
    } catch {
      case ex: Exception => println("erreur interceptee" + ex.getMessage + " " + ex.printStackTrace())
    }

  }

  def extractProject(projectBmpr: String): Unit = {

    var c: Connection = null;
    var stmt: Statement = null;
    try {
      Class.forName("org.sqlite.JDBC");
      val dbURL = "jdbc:sqlite:c:/temp/balsamiq/balsamiq3/test2.bmpr";
      val directory="c:/temp/balsamiq/balsamiq3" 
       val xstream = new XStream(new JettisonMappedXmlDriver());
      c = DriverManager.getConnection(dbURL);
      c.setAutoCommit(false);
      System.out.println("Opened database successfully");

      stmt = c.createStatement();
      val rs = stmt.executeQuery("SELECT attributes,data FROM RESOURCES;");
      while (rs.next()) {
        
        val attributesJSON = rs.getString("attributes");
        val name = new JSONObject(attributesJSON).getString("name")
        System.out.println(name);
        xstream.toXML();
        println(rs.getString("data"))
         
        val dataXML = XML.toString(new JSONObject({ rs.getString("data") })); ;
        utilitaire.fileWrite(directory+"/"+name+".bmml",dataXML)
        println("attributes=" + name + " data=" + dataXML)

      }
      rs.close();
      stmt.close();
      c.close();
    } catch {
      case e: Exception => System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }

    System.out.println("Operation done successfully");
  }
}



