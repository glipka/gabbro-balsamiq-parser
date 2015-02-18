package fr.gabbro.balsamiq.parser.service.serviceimpl
// IbalsamiqFreeMarker - scala program to manipulate balsamiq sketches files an generate code with FreeMarker
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

import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import freemarker.template.Version
import java.util.Locale
import scala.collection.JavaConversions._
import scala.beans.BeanProperty
import java.io.File
import java.io.FileWriter
import java.io.StringWriter
import scala.collection.JavaConversions._
import java.util.Properties
import java.io.FileInputStream
import net.htmlparser.jericho._
import org.slf4j.LoggerFactory
import java.io.IOException
import fr.gabbro.balsamiq.parser.modelimpl.GlobalContext
import fr.gabbro.balsamiq.parser.service.TMoteurTemplatingFreeMarker
import fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase

// -----------------------------------------------------------------------------------------------------------------------
// Pour chaque widget, on récupère le nom du template à appeler.
// Chaque widget pouvant être un container, il y un template pour la phase début et
// un template pour la phase fin.
// pour un widget, on peut aussi trouver un fichier js_template.debut et js_template.fin
// Si le template js_xxx existe, on l'appelle après l'exécution du template widget
// et ceci afin de générer un fichier javascript pour chaque page html. (donc chaque fichier balsamiq)
// ainsi qu'un fichier de code (java, scala, ...)
// pour chaque widget, on génère la hashMap contenant les paramètres à passer au moteur de templating freemarker
// -----------------------------------------------------------------------------------------------------------------------
// engineProperties.freemarkerVariablePrefix

/**
 * @author Georges Lipka
 * -----------------------------------------------------------------------------------------------------------------------
 * <p>Pour chaque widget, on récupère le nom du template à appeler.</p>
 * <p>Chaque widget pouvant être un container, il y un template pour la phase début et
 * un template pour la phase fin.</p>
 * <p>Pour un widget, on peut aussi trouver un fichier js_template.debut et js_template.fin</p>
 * <p>Si le template js_xxx existe, on l'appelle après l'exécution du template widget
 * et ceci afin de générer un fichier javascript pour chaque page html. (donc chaque fichier balsamiq)
 * ainsi qu'un fichier de code (java, scala, ...)</p>
 * <p>pour chaque widget, on génère la hashMap contenant les paramètres à passer au moteur de templating freemarker</p>
 *
 *
 */
class MoteurTemplatingFreeMarker(val templateDirectory: String, val templateDirOut: String, val templateCodeOut: String, sessionBalsamiq: GlobalContext) extends TMoteurTemplatingFreeMarker {

  /**
   *  -----------------------------------------------------------------------------------------------
   * inialisation des propriétés du moteur de templating et chargement de la table des templates
   * -----------------------------------------------------------------------------------------------
   *
   * @return
   */
  def init(): Boolean = {
    try {
      logBack.info(utilitaire.getContenuMessage("mes57"))
      // cfgFreeMarker.setDirectoryForTemplateLoading(new File(templateDirectory))
      // on utilise classTemplateLoader pour chercher l'URL des templates dans les sous répertoires
      cfgFreeMarker.setTemplateLoader(classTemplateLoader)
      cfgFreeMarker.setIncompatibleImprovements(new Version(2, 3, 20));
      cfgFreeMarker.setDefaultEncoding(CommonObjectForMockupProcess.constants.utf_8);
      // cfgFreeMarker.setLocale(Locale.FRANCE);
      cfgFreeMarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
      if (CommonObjectForMockupProcess.templatingProperties.freemarkerAutoIncludeFile != null && CommonObjectForMockupProcess.templatingProperties.freemarkerAutoIncludeFile != "") {
        cfgFreeMarker.addAutoInclude(CommonObjectForMockupProcess.templatingProperties.freemarkerAutoIncludeFile)
      }
      if (CommonObjectForMockupProcess.templatingProperties.freemarkerAutoImportFile != null && CommonObjectForMockupProcess.templatingProperties.freemarkerAutoImportFile != "" && CommonObjectForMockupProcess.templatingProperties.freemarkerAutoImportNamespace != null && CommonObjectForMockupProcess.templatingProperties.freemarkerAutoImportNamespace != "") {
        cfgFreeMarker.addAutoImport(CommonObjectForMockupProcess.templatingProperties.freemarkerAutoImportNamespace, CommonObjectForMockupProcess.templatingProperties.freemarkerAutoImportFile)
      }

      val (ok, table1) = getAllPropertiesTemplatesHTML(templateDirectory)
      tableDesTemplates = table1
      if (!ok) {
        logBack.error(utilitaire.getContenuMessage("mes17"))
        return false
      } else {
        return true
      }
    } catch {
      case ex: Exception =>
        logBack.error(utilitaire.getContenuMessage("mes11") + ex.getMessage())
        false
    }
    true

  }

  /**
   * recupération nom du template
   * @param widget : object widgetDeBase
   * @param phase : begin or end
   * @return : name of template
   */
  def determinationNomDuTemplate(widget: WidgetDeBase, phase: String): (Option[String], Option[String], Option[String]) = {
    val name = widget.getWidgetNameOrComponentName()
    return determinationNomDuTemplate(widget.getWidgetNameOrComponentName(), phase)
  }
  // ------------------------------------------------------------------------------------------
  // on récupère le nom du template dans la table des templates
  // puis on génère le nom des fichiers templates pour la partie HTML, javascript et code
  // ------------------------------------------------------------------------------------------

  /**
   * on récupère le nom du template dans la table des templates
   * puis on génère le nom des fichiers templates pour la partie HTML, javascript et code
   *
   * @param widgetName : widget name
   * @param phase : begin or end
   * @return : name of template
   */
  def determinationNomDuTemplate(widgetName: String, phase: String): (Option[String], Option[String], Option[String]) = {
    val templateName = tableDesTemplates.getOrElse(widgetName, Some(CommonObjectForMockupProcess.constants.templateUndefined))

    templateName match {
      case None => (None, None, None)
      case _ => {
        val templateHTML = Some(templateName.get +
          CommonObjectForMockupProcess.templatingProperties.separator_template_file +
          phase +
          CommonObjectForMockupProcess.constants.suffixTemplateFreeMarkerFile)
        val templateJavascript = Some(CommonObjectForMockupProcess.templatingProperties.prefix_template_javascript +
          templateName.get +
          CommonObjectForMockupProcess.templatingProperties.separator_template_file +
          phase +
          CommonObjectForMockupProcess.templatingProperties.suffix_template_javascript +
          CommonObjectForMockupProcess.constants.suffixTemplateFreeMarkerFile)
        val templateCode = Some(CommonObjectForMockupProcess.templatingProperties.prefix_template_code +
          templateName.get +
          CommonObjectForMockupProcess.templatingProperties.separator_template_file +
          phase +
          CommonObjectForMockupProcess.templatingProperties.suffix_template_code +
          CommonObjectForMockupProcess.constants.suffixTemplateFreeMarkerFile)
        (templateHTML, templateJavascript, templateCode)
      }
    }

  }
 
  /**
   * lecture du fichier properties et chargement dans une map des identifications des templates ***
   * on verifie que les fichiers template existent physiquement (debut et fin)
   * @param templateDirectory : directory where templates are located
   * @return (returnCode,Map of templates[String,String])
   */
  def getAllPropertiesTemplatesHTML(templateDirectory: String): (Boolean, scala.collection.mutable.Map[String, Option[String]]) = {
    val tableDesTemplates = scala.collection.mutable.Map[String, Option[String]]()
    var templateEncours = ""
    var ficPropertyName = ""
    var ok = true
    try {
      val props = new Properties();
      ficPropertyName = CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatePropertiesFile
      props.load(new FileInputStream(ficPropertyName));

      val enuKeys = props.keys().toList
      enuKeys.foreach(clef => {
        val valeur = props.getProperty(clef.toString()).trim
        tableDesTemplates += (clef.toString().trim -> Some(valeur))
        templateEncours = valeur
        // on verifie que les fichiers template début et template fin existent.
        val templateDebut = classTemplateLoader.getURL(valeur + CommonObjectForMockupProcess.templatingProperties.separator_template_file + CommonObjectForMockupProcess.templatingProperties.phase_debut + CommonObjectForMockupProcess.constants.suffixTemplateFreeMarkerFile)
        val templateFin = classTemplateLoader.getURL(valeur + CommonObjectForMockupProcess.templatingProperties.separator_template_file + CommonObjectForMockupProcess.templatingProperties.phase_fin + CommonObjectForMockupProcess.constants.suffixTemplateFreeMarkerFile)
        if (templateDebut == null || templateFin == null) {
          ok = false
        }
      })
    } catch {
      case ex: IOException => {
        logBack.error(utilitaire.getContenuMessage("mes12"), ex.getMessage() + " " + ficPropertyName)
        ok = false;
        (false, null)
      }
      case ex: Exception => {
        logBack.error(utilitaire.getContenuMessage("mes12"), ex.getMessage() + " " + templateEncours)
        ok = false;
        (false, null)
      }
    }
    (ok, tableDesTemplates)

  }
   
  /**
 * @param NomDuFichierSourceJavaOuScala : html fileName to write
 * @param sourceEcran : buffer to write
 * @return : true or false
 */
def ecritureDuFichierHTML(NomDuFichierSourceJavaOuScala: String, sourceEcran: String): Boolean = {
    val fileName = utilitaire.getEmplacementFichierHtml(NomDuFichierSourceJavaOuScala, CommonObjectForMockupProcess.generationProperties.srcWebFilesDir)
    val source = new Source(sourceEcran);
    // Utilisation du parser Jericho pour formater le généré HTML.
    val sourceFormat = new SourceFormatter(source).setIndentString("\t").setTidyTags(true).setCollapseWhiteSpace(true);
    utilitaire.ecrire_fichier(fileName, sourceFormat.toString())
    true
  }

    
/**
 * si le template est dans la liste des templates pour lesquels il n'y a pas de génération
 * on ne genere pas de code pour les fils (cas de DHTMLxgrid par exemple)
 * @param widget :instance of WidgetDeBase
 * @return : true or false
 */
def templateAGenerer(widget: WidgetDeBase): Boolean = {
    if (widget != null) { !CommonObjectForMockupProcess.engineProperties.bypassGenerationTemplateForChildren.exists(token => (token == widget.controlTypeID || token == widget.componentName)) }
    else { true }
  }

  // --------------------------------------------------
  // *** generation du template ***
  // *** phase : debut ou fin ***
  // --------------------------------------------------
  

/**
 * generation du template ***
 * @param widget : name of widget
 * @param phase : begin or end 
 * @param widgetPere : container of widget
 * @param params : List of parameters (name, content
 * @return : (true or false, sourceHtml, sourceJavascript, codeJavaOrScalOfWidget)
 */
def generationDuTemplate(widget: WidgetDeBase, phase: String, widgetPere: WidgetDeBase, params: (String, Object)*): (Boolean, String, String, String) = {
    return generationDuTemplate(widget.getWidgetNameOrComponentName(), phase, widget, widgetPere, widget.generationTableauDeParametresPourTemplates, params.toList)
  } // fin de generationDuTemplate

  import scala.collection.mutable.Map
  
  
  /**
 * @param widgetName
 * @param phase : begin or end
 * @param widget : object widgetDeBase 
 * @param widgetPere : container of widget
 * @param parametresDuWidget :Map of parameters (name, content
 * @param parametresAdditionnels
 * @return
 */
def generationDuTemplate(widgetName: String, phase: String, widget: WidgetDeBase, widgetPere: WidgetDeBase, parametresDuWidget: Map[String, Object], parametresAdditionnels: List[(String, Object)]): (Boolean, String, String, String) = {
    if (!templateAGenerer(widgetPere)) { (false, "", "", "") } // pas de génération de fils ?
    else {
      var (templateHtml, templateJavascript, templateCode) = determinationNomDuTemplate(widgetName.trim, phase.trim)
      templateHtml match {
        case None => (false, "", "", "") // à corriger
        case _ => {
          //  try {
          var templateParameter = if (parametresDuWidget != null) parametresDuWidget else scala.collection.mutable.Map[java.lang.String, Object]()
          var mapParametre = templateParameter
          parametresAdditionnels.foreach {
            param => mapParametre += (param)
          }
          val templateName = templateHtml.get
          mapParametre = mapParametre ++ enrichissementDesParametresPourFreeMarker(widgetPere, widget, templateName)
          val traitementPreserveNotPresentInAdditionalParameters = parametresAdditionnels.forall(param => param._1 != CommonObjectForMockupProcess.constants.traitementPreserveSection)
          // traitement du template html
          val mapParametreHtml = mapParametre
          //    if (CommonObjectForMockupProcess.traitementPreserveSectionTemplateHtml != null && traitementPreserveNotPresentInAdditionalParameters) { mapParametreHtml += (CommonObjectForMockupProcess.constants.traitementPreserveSection -> CommonObjectForMockupProcess.traitementPreserveSectionTemplateHtml) }
          val (ok1, sourceHtml) = processExecuteFreeMarkerTemplate(templateHtml.get, widgetPere, mapParametreHtml)

          // traitement du template javascript 
          val mapParametreJavascript = mapParametre
          //      if (CommonObjectForMockupProcess.traitementPreserveSectionTemplateJavascript != null && traitementPreserveNotPresentInAdditionalParameters) { mapParametreJavascript += (CommonObjectForMockupProcess.constants.traitementPreserveSection -> CommonObjectForMockupProcess.traitementPreserveSectionTemplateJavascript) }
          val (ok2, sourceJavascript) = processExecuteFreeMarkerTemplate(templateJavascript.get, widgetPere, mapParametreJavascript)

          // traitement du template code
          // -------------------------------------------------------------------------------------------------------------------
          // pour le code : on passe en paramètre les preserve secions pour les sous packages
          // other, custom1, custom2, custom3 si le parametre presectionSection n'est pas déjà renseigné
          // Dans ce cas, c'est le template qui va décider quelle preserve section utiliser en fonction du sous package
          // dans lequel il veut générer les fichiers.
          // Ceci est dû au fait qu'un template peut générer plusieurs classes dans des sous packages différents.
          // --------------------------------------------------------------------------------------------------------------------
          val mapParametreCode = mapParametre
          val (ok3, codeWidget) = processExecuteFreeMarkerTemplate(templateCode.get, widgetPere, mapParametreCode)
          return (true, sourceHtml, sourceJavascript, codeWidget)
        }
      }
    }
  }
  /**
 * @param templateName : name of template
 * @param widgetPere : 
 * @param templateParameter
 * @return
 */
def processExecuteFreeMarkerTemplate(templateName: String, widgetPere: WidgetDeBase, templateParameter: java.util.Map[String, Object]): (Boolean, String) = {
    if (!templateAGenerer(widgetPere)) { (false, "") } // pas de génération de fils ?
    else {
      try {
        val template = cfgFreeMarker.getTemplate(templateName)
        if (template != null) {
          val out = new StringWriter();
          if (CommonObjectForMockupProcess.engineProperties.freemarkerVariablePrefix != "") { template.process(templateParameter.map(keyvalue => (CommonObjectForMockupProcess.engineProperties.freemarkerVariablePrefix + keyvalue._1, keyvalue._2)), out) };
          else { template.process(mutableMapAsJavaMap(templateParameter), out) };

          // template.process(templateParameter, out);
          (true, out.toString())
        } else { (false, "") }
      } catch {
        case ex: Exception => (false, "")
      }
    }

  }
  def generationDuTemplate(widgetName: String, phase: String, widgetPere: WidgetDeBase, parametresAdditionnels: (String, Object)*): (Boolean, String, String, String) = {
    return generationDuTemplate(widgetName, phase, null, widgetPere, null, parametresAdditionnels.toList)
  } // fin de generationDuTemplate

  def generationDuTemplate(widgetName: String, phase: String, widgetPere: WidgetDeBase, parametresAdditionnels: Array[(String, String)]): (Boolean, String, String, String) = {
    return generationDuTemplate(widgetName, phase, null, widgetPere, null, parametresAdditionnels.toList)
  } // fin de generationDuTemplate

  // ------------------------------------------------------------------------------
  // Générération du controleur : le code est généré par le template controleur
  // ----------------------------------------------------------------------------
  def generationDuControleur(ficName: String, sourcesDeLaClasse: String): Unit = {
    var fileWriter: FileWriter = null
    var ficPropertyName: String = ""
    val dtoDir = CommonObjectForMockupProcess.generationProperties.srcDtoFilesDir.replace("\\", "/").replace("/", ".")
    var pack1 = ""

    if (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement != "") {
      val (_, packageSource1, _, _) = generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, (dtoDir + "." + CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedControllerAlias).replace("\\", "/").replace("/", ".")), ("classname", ficName))
      val (_, packageSource2, _, _) = generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, (dtoDir + "." + CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedControllerAlias).replace("\\", "/").replace("/", ".")), ("classname", ficName))
      pack1 = packageSource1 + packageSource2
      ficPropertyName = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedControllerAlias + System.getProperty("file.separator") + ficName + "." + CommonObjectForMockupProcess.generationProperties.languageSource
    } else {
      val (_, packageSource1, _, _) = generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, (dtoDir + "." + CommonObjectForMockupProcess.generationProperties.generatedControllerAlias).replace("\\", "/").replace("/", ".")), ("classname", ficName))
      val (_, packageSource2, _, _) = generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, (dtoDir + "." + CommonObjectForMockupProcess.generationProperties.generatedControllerAlias).replace("\\", "/").replace("/", ".")), ("classname", ficName))
      pack1 = packageSource1 + packageSource2
      ficPropertyName = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedControllerAlias + System.getProperty("file.separator") + ficName.capitalize + "." + CommonObjectForMockupProcess.generationProperties.languageSource
    }
    utilitaire.ecrire_fichier(ficPropertyName, pack1 + sourcesDeLaClasse)

  }

  // enrichissement des extended attributs 
  def enrichissementDesParametresPourFreeMarker(widgetPere: WidgetDeBase, widget: WidgetDeBase, templateName: String): scala.collection.mutable.Map[String, Object] = {
    val mapParametre = scala.collection.mutable.Map[String, Object]()
    mapParametre += (CommonObjectForMockupProcess.constants.usecaseName -> CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement)
    mapParametre += (CommonObjectForMockupProcess.constants.projectName -> CommonObjectForMockupProcess.generationProperties.projectName)
    mapParametre += (CommonObjectForMockupProcess.constants.engineProperties -> CommonObjectForMockupProcess.engineProperties)
    mapParametre += (CommonObjectForMockupProcess.constants.generationProperties -> CommonObjectForMockupProcess.generationProperties)
    mapParametre += (CommonObjectForMockupProcess.constants.templatingProperties -> CommonObjectForMockupProcess.templatingProperties)
    mapParametre += (CommonObjectForMockupProcess.constants.isAFragment -> CommonObjectForMockupProcess.isAfragment.toString())
    if (widget != null) {
      mapParametre += (CommonObjectForMockupProcess.constants.widget -> widget)
      if (widget.isFormulaireHTML) {
        mapParametre += (CommonObjectForMockupProcess.constants.isForm -> CommonObjectForMockupProcess.constants.trueString)
      }

    }
    mapParametre += (CommonObjectForMockupProcess.constants.widgetContainer -> widgetPere)
    mapParametre += (CommonObjectForMockupProcess.constants.mockupContext -> CommonObjectForMockupProcess.mockupContext)
    mapParametre += (CommonObjectForMockupProcess.constants.generateController -> CommonObjectForMockupProcess.generateController.toString)
    mapParametre += (CommonObjectForMockupProcess.constants.globalContext -> IBalsamiqFreeMarker.globalContext)
    mapParametre += (CommonObjectForMockupProcess.constants.generatedFileName -> CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement)
    mapParametre += (CommonObjectForMockupProcess.constants.mainMockupName -> CommonObjectForMockupProcess.ecranContenantLeSegment)
    mapParametre += (CommonObjectForMockupProcess.constants.utility -> utilitaire)
    mapParametre += (CommonObjectForMockupProcess.constants.templateName -> templateName)
    mapParametre += (CommonObjectForMockupProcess.constants.commonObject -> CommonObjectForMockupProcess)
    mapParametre += (CommonObjectForMockupProcess.constants.constants -> CommonObjectForMockupProcess.constants)
    return mapParametre

  }
  // ------------------------------------------------------------------------------------------------------------

}
