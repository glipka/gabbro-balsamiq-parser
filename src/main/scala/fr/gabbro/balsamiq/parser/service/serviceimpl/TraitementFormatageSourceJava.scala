package fr.gabbro.balsamiq.parser.service.serviceimpl

import java.io._
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import org.eclipse.jdt.core.ToolFactory
import org.eclipse.jdt.core.formatter.CodeFormatter
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants
import org.eclipse.jface.text.Document
import java.util.HashMap
import org.eclipse.jdt.core.JavaCore
import org.mozilla.javascript.Parser
import javax.script.ScriptEngineManager
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Context
import fr.gabbro.balsamiq.parser.service.TTraitementCommun
import net.htmlparser.jericho.SourceFormatter
import net.htmlparser.jericho.Source

/**
 * @author Georges Lipka
 *
 */
class TraitementFormatageSourceJava extends TTraitementCommun {

  /**
   * On utilise le formatter d'eclipse avec les options par défaut afin de formater le code java
   * Il faudra modifier cette méthode afin de paramétrer les valeurs des différentes options Eclipse
   * @param bufferAformater
   * @return formated buffer
   */
  def indentSourceCodeJava(bufferAformater: String): String = {
     // take default Eclipse formatting options
    val options = DefaultCodeFormatterConstants.getEclipseDefaultSettings();
    val settings = new HashMap[String, String]();
    settings.put(JavaCore.COMPILER_SOURCE, "1.7");
    settings.put(JavaCore.COMPILER_COMPLIANCE, "1.7");
    settings.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.7");
    val codeFormatter = ToolFactory.createCodeFormatter(settings);
    val doc = new Document();
    try {
      doc.set(bufferAformater)
      val edit = codeFormatter.format(CodeFormatter.K_UNKNOWN, bufferAformater, 0, bufferAformater.length(), 0, null);
      if (edit != null) {
        edit.apply(doc);
        return doc.get.toString()
      } else {
        return bufferAformater; // most likely syntax errror
      }

    } catch {
      case ex: Exception =>
        logBack.error(utilitaire.getContenuMessage("mes8"))
        return bufferAformater
    }

  }

  /**
   * @param jsCode
   * @param indentSize
   * @return formated buffer
   */
  def indentSourceCodeJavaScript(jsCode: String, indentSize: Int): String = {
    return jsCode // en attentant de corriger le bug
    val BEAUTIFY_JS = System.getProperty("user.dir") + "/" + "beautify.js"
    try {
      val cx = Context.enter();
      val scope = cx.initStandardObjects();
      val reader = new InputStreamReader(new FileInputStream(BEAUTIFY_JS))
      cx.evaluateReader(scope, reader, "__beautify.js", 1, null);
      reader.close();
      scope.put("jsCode", scope, jsCode);
      val bufferFormate = cx.evaluateString(scope, "js_beautify(jsCode, {indent_size:" + indentSize + "})", "inline", 1, null);
      return bufferFormate.toString()

    } catch {
      case ex: Exception =>
        logBack.error(utilitaire.getContenuMessage("mes34"))
        return jsCode
    }

  }
  /**
   * indent the HTML source file
   * The identation is performed by Jerichi Source Formatter class
   * @param html file location
   * @return source formated
   */
   
   def indentSourceHtml(sourceMockup:String): String = {
    val source = new Source(sourceMockup);
    // Utilisation du parser Jericho pour formater le généré HTML.
    val sourceFormat = new SourceFormatter(source).setIndentString("\t").setCollapseWhiteSpace(true);
    return  sourceFormat.toString()
   }


}