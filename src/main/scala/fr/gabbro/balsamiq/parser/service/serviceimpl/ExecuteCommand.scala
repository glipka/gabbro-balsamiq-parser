package fr.gabbro.balsamiq.parser.service.serviceimpl

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * @author FRA9972467
 */
class ExecuteCommand {

  
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
      case ex: Exception => println(ex.getMessage)
    }

    return output.toString();

  }
}