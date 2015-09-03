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

import java.io.BufferedReader
import java.io.InputStreamReader
import scala.collection.JavaConversions._
import fr.gabbro.balsamiq.parser.service.TTraitementCommun

/**
 * @author FRA9972467
 */
class ExecuteCommand extends TTraitementCommun {

  // val path=System.getenv("PATH").split(";") 

  /**
   * execution d'une commande via  RunTime
   * @param command : String
   * @return (result of command:string)
   */

  def execute(command: String): String = {
    var output = "";
    try {
      var p = Runtime.getRuntime().exec(command);
      p.waitFor();
      var reader =
        new BufferedReader(new InputStreamReader(p.getInputStream()));
      var line = "";
      do {
        line = reader.readLine();
        output += line
        output += "\n"

      } while (line != "" && line != null)

    } catch {
      case ex: Exception => logBack.error(utilitaire.getContenuMessage("mes62"),ex.getMessage)
    }

    return output.toString();

  }
}