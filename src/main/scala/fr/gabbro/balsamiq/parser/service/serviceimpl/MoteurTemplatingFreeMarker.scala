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
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
/**
 * @author Georges Lipka
 * <p>-----------------------------------------------------------------------------------------------------------------------</p>
 * <p>Pour chaque widget, on récupère le nom du template à appeler.</p>
 * <p>Chaque widget pouvant être un container, il y un template pour la phase début et
 * un template pour la phase fin.</p>
 * <p>Pour un widget, on peut aussi trouver un fichier js_template.debut et js_template.fin</p>
 * <p>Si le template js_xxx existe, on l'appelle après l'exécution du template widget
 * et ceci afin de générer un fichier javascript pour chaque page html. (donc chaque fichier balsamiq)
 * ainsi qu'un fichier de code (java, scala, ...)</p>
 * <p>pour chaque widget, on génère la hashMap contenant les paramètres à passer au moteur de templating freemarker</p>
 * <p> même logique pour les templates de type code </p>
 *
 */
class MoteurTemplatingFreeMarker(val templateDirectory: String, val templateDirOut: String, val templateCodeOut: String, globalContext: GlobalContext) extends TMoteurTemplatingFreeMarker {

  /**
   *  -----------------------------------------------------------------------------------------------
   * inialisation des propriétés du moteur de templating et chargement de la table des templates
   * -----------------------------------------------------------------------------------------------
   * @return true or false
   */
  def init(): Boolean = {
    try {
      logBack.info(utilitaire.getContenuMessage("mes57"))
      // cfgFreeMarker.setDirectoryForTemplateLoading(new File(templateDirectory))
      // on utilise classTemplateLoader pour chercher l'URL des templates dans les sous répertoires
      cfgFreeMarker.setTemplateLoader(classTemplateLoader)
      cfgFreeMarker.setIncompatibleImprovements(new Version(2, 3, 20));
      cfgFreeMarker.setDefaultEncoding(cstUtf_8);
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

  /**
   * on récupère le nom du template dans la table des templates
   * puis on génère le nom des fichiers templates pour la partie HTML, javascript et code
   *
   * @param widgetName : widget name
   * @param phase : begin or end
   * @return : name of template
   */
  def determinationNomDuTemplate(widgetName: String, phase: String): (Option[String], Option[String], Option[String]) = {
    val templateName = tableDesTemplates.getOrElse(widgetName, Some(cstTemplateUndefined))

    templateName match {
      case None => (None, None, None)
      case _ => {
        val templateHTML = Some(templateName.get +
          CommonObjectForMockupProcess.templatingProperties.separator_template_file +
          phase +
          cstSuffixTemplateFreeMarkerFile)
        val templateJavascript = Some(CommonObjectForMockupProcess.templatingProperties.prefix_template_javascript +
          templateName.get +
          CommonObjectForMockupProcess.templatingProperties.separator_template_file +
          phase +
          CommonObjectForMockupProcess.templatingProperties.suffix_template_javascript +
          cstSuffixTemplateFreeMarkerFile)
        val templateCode = Some(CommonObjectForMockupProcess.templatingProperties.prefix_template_code +
          templateName.get +
          CommonObjectForMockupProcess.templatingProperties.separator_template_file +
          phase +
          CommonObjectForMockupProcess.templatingProperties.suffix_template_code +
          cstSuffixTemplateFreeMarkerFile)
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
        val templateDebut = classTemplateLoader.getURL(valeur + CommonObjectForMockupProcess.templatingProperties.separator_template_file + CommonObjectForMockupProcess.templatingProperties.phase_debut + cstSuffixTemplateFreeMarkerFile)
        val templateFin = classTemplateLoader.getURL(valeur + CommonObjectForMockupProcess.templatingProperties.separator_template_file + CommonObjectForMockupProcess.templatingProperties.phase_fin + cstSuffixTemplateFreeMarkerFile)
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
   * modif le 28 avril 2015 : replace preserve
   * modif 21 juillet 2015 : conditionnement utilisation overwriteJspOrHtmlFile
   * modif le 22 juillet 2015 : merge des fichiers à générer (ancienne version et nouvelle version)
   */
  def ecritureDuFichierHTML(NomDuFichierSourceJavaOuScala: String, sourceASauvegarder: String): Boolean = {
    var sourceEcran = sourceASauvegarder
    val fileName = utilitaire.getEmplacementFichierHtml(NomDuFichierSourceJavaOuScala, CommonObjectForMockupProcess.generationProperties.srcWebFilesDir)
    // internationalisation du fichier ??
    if (CommonObjectForMockupProcess.generationProperties.processI18nInFiles == cstTrueString) {
      logBack.info(utilitaire.getContenuMessage("mes46"))
      sourceEcran = globalContext.moteurJericho.traductHtmlFile(sourceASauvegarder) // extraction des clefs de traduction
    }
    val sourceFormat = new SourceFormatter(new Source(sourceEcran)).setIndentString("\t").setCollapseWhiteSpace(true).toString;
    // ajout par gl le 22 juillet 2015. (rq:La solution de merge retenue est windiff)
    // si le fichier à générer existe et que l'on ne peut pas overrider le fichier existant:
    //       on copie le fichier existant sur un répertoire de travail ainsi que le nouveau fichier à générer
    //       si les fichiers ne sont pas identiques, on appelle le process d'exécution de commandes externes pour exécuter le merge des 2 fichiers.
    //       le résultat du merge sera stocké dans le fichier original. 
    if (!CommonObjectForMockupProcess.generationProperties.overwriteJspOrHtmlFile && utilitaire.existFile(fileName)) {
      val (ret1, fichierAvantGeneration) = utilitaire.copyOldHtmlFiletoTemporaryDir(fileName, NomDuFichierSourceJavaOuScala) // copie du fichier initial sur le repertoire temporaire
      if (ret1) {
        val (ret2, fichierApresGeneration) = utilitaire.createNewHtmlFileInTemporaryDir(NomDuFichierSourceJavaOuScala, sourceEcran, fileName) // copie du fichier après generation sur le repertoire temporaire
        if (ret2) {
          if (!utilitaire.compareContentFile(fichierAvantGeneration, fichierApresGeneration)) { // le source a été modifié ?
            val cmd = CommonObjectForMockupProcess.generationProperties.mergeFileCommand.replace("%1", fichierAvantGeneration).replace("%2", fichierApresGeneration).replace("%3", fileName)
            val executeCommand = new ExecuteCommand
            val ret3 = executeCommand.execute(cmd)

          }
        }
      }
      return true

    } else {
      return (utilitaire.ecrire_fichier(fileName, sourceFormat))
    }
  }

  /**
   * @param widget
   * @return true or false
   * test si le widget est dans la liste des containers qui génèrent leur propre fragment
   */
  def testSiWidgetDansLaListeDesContainersGenerantLeursFragments(widget: WidgetDeBase): Boolean = {
    if (widget != null) {
      // 
      // le bmml n'est pas un fragment et le widget est dans la listegenerateFragmentFromTheseContainers
      if (!CommonObjectForMockupProcess.isAfragment && CommonObjectForMockupProcess.generationProperties.generateFragmentFromTheseContainers.map(keyValue => keyValue._1).intersect(List(widget.getWidgetNameOrComponentName)).size > 0) { return true }
      else { return false }
    } else { return false }

  }
  /**
   * si le template est dans la liste des templates pour lesquels il n'y a pas de génération
   * on ne genere pas de code pour les fils (cas de DHTMLxgrid par exemple). La propriété bypassGenerationTemplateForChildren gère ce cas
   * on ne génère pas de code pour les widgets inclus dans les containers générant leur propre fragment  : proprité generateFragmentFromTheseContainers
   * si le
   * @param widget :instance of WidgetDeBase
   * @return : true or false
   */
  def templateAGenerer(widget: WidgetDeBase): Boolean = {
    if (widget != null) {
      // 
      if (CommonObjectForMockupProcess.engineProperties.bypassGenerationTemplateForChildren.exists(token => (token == widget.getWidgetNameOrComponentName))) { return false }
      else {
        // si le widget est dans la liste des containers générant leur propre fragment on ne génère pas le template du widget en cours
        return !testSiWidgetDansLaListeDesContainersGenerantLeursFragments(widget)
      }
    } else { true }
  }

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
    if (!templateAGenerer(widgetPere) || testSiWidgetDansLaListeDesContainersGenerantLeursFragments(widget)) { (false, "", "", "") } // pas de génération de fils pour les widgets dont le pere est dans la liste xx1 ou dans la liste des container generant leur propre fragment ou pour les widgets contenu dans liste des containers generant leur propre fragment
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
          val traitementPreserveNotPresentInAdditionalParameters = parametresAdditionnels.forall(param => param._1 != cstTraitementPreserveSection)
          // traitement du template html
          val mapParametreHtml = mapParametre
          val (ok1, sourceHtml) = processExecuteFreeMarkerTemplate(templateHtml.get, widgetPere, mapParametreHtml)

          // traitement du template javascript 
          val mapParametreJavascript = mapParametre
          val (ok2, sourceJavascript) = processExecuteFreeMarkerTemplate(templateJavascript.get, widgetPere, mapParametreJavascript)
          val mapParametreCode = mapParametre
          val (ok3, codeWidget) = processExecuteFreeMarkerTemplate(templateCode.get, widgetPere, mapParametreCode)
          return (true, sourceHtml, sourceJavascript, codeWidget)
        }
      }
    }
  }
  /**
   * Execution du template freemarker
   * @param templateName : name of template
   * @param widgetPere :
   * @param templateParameter
   * @return
   */
  def processExecuteFreeMarkerTemplate(templateName: String, widgetPere: WidgetDeBase, templateParameter: java.util.Map[String, Object]): (Boolean, String) = {
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

  /**
   * @param widgetName
   * @param phase
   * @param widgetPere
   * @param parametresAdditionnels  (String, Object)*
   * @return parametresAdditionnels converted to List
   */
  def generationDuTemplate(widgetName: String, phase: String, container: WidgetDeBase, parametresAdditionnels: (String, Object)*): (Boolean, String, String, String) = {
    return generationDuTemplate(widgetName, phase, null, container, null, parametresAdditionnels.toList)
  } // fin de generationDuTemplate

  /**
   * @param widgetName
   * @param phase
   * @param widgetPere
   * @param parametresAdditionnels  Array[(String, String)]
   * @return parametresAdditionnels converted to List
   */
  def generationDuTemplate(widgetName: String, phase: String, container: WidgetDeBase, parametresAdditionnels: Array[(String, String)]): (Boolean, String, String, String) = {
    return generationDuTemplate(widgetName, phase, null, container, null, parametresAdditionnels.toList)
  } // fin de generationDuTemplate

  /**
   * Génération de la classe contrôleur : le code est généré par le template controleur
   * @param ficName : nom du fichier
   * @param sourcesDeLaClasse
   */
  def generationDuControleur(ficName: String, sourcesDeLaClasse: String): Unit = {
    var fileWriter: FileWriter = null
    var ficPropertyName: String = ""
    val dtoDir = CommonObjectForMockupProcess.generationProperties.srcDtoFilesDir.replace("\\", "/").replace("/", ".")
    var pack1 = ""

    if (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement != "") {
      val (_, packageSource1, _, _) = generationDuTemplate(cstTemplatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (cstPackageName, (dtoDir + "." + CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedControllerAlias).replace("\\", "/").replace("/", ".")), ("classname", ficName))
      val (_, packageSource2, _, _) = generationDuTemplate(cstTemplatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (cstPackageName, (dtoDir + "." + CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedControllerAlias).replace("\\", "/").replace("/", ".")), ("classname", ficName))
      pack1 = packageSource1 + packageSource2
      ficPropertyName = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedControllerAlias + System.getProperty("file.separator") + ficName + "." + CommonObjectForMockupProcess.generationProperties.languageSource
    } else {
      val (_, packageSource1, _, _) = generationDuTemplate(cstTemplatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (cstPackageName, (dtoDir + "." + CommonObjectForMockupProcess.generationProperties.generatedControllerAlias).replace("\\", "/").replace("/", ".")), ("classname", ficName))
      val (_, packageSource2, _, _) = generationDuTemplate(cstTemplatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (cstPackageName, (dtoDir + "." + CommonObjectForMockupProcess.generationProperties.generatedControllerAlias).replace("\\", "/").replace("/", ".")), ("classname", ficName))
      pack1 = packageSource1 + packageSource2
      ficPropertyName = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedControllerAlias + System.getProperty("file.separator") + ficName.capitalize + "." + CommonObjectForMockupProcess.generationProperties.languageSource
    }
    utilitaire.ecrire_fichier(ficPropertyName, pack1 + sourcesDeLaClasse)

  }

  /**
   *  enrichissement des extended attributs
   * @param container : container du widget
   * @param widget : objet widgetDeBase en cours
   * @param templateName : nom du template en cours de traitement
   * @return Map des attributs
   */
  def enrichissementDesParametresPourFreeMarker(container: WidgetDeBase, widget: WidgetDeBase, templateName: String): scala.collection.mutable.Map[String, Object] = {
    val mapParametre = scala.collection.mutable.Map[String, Object]()
    mapParametre += (cstUsecaseName -> CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement)
    mapParametre += (cstProjectName -> CommonObjectForMockupProcess.generationProperties.projectName)
    mapParametre += (cstEngineProperties -> CommonObjectForMockupProcess.engineProperties)
    mapParametre += (cstGenerationProperties -> CommonObjectForMockupProcess.generationProperties)
    mapParametre += (cstTemplatingProperties -> CommonObjectForMockupProcess.templatingProperties)
    mapParametre += (cstIsAFragment -> CommonObjectForMockupProcess.isAfragment.toString())
    if (widget != null) {
      mapParametre += (cstWidget -> widget)
      if (widget.isFormulaireHTML) {
        mapParametre += (cstIsForm -> cstTrueString)
      }
    }
    mapParametre += (cstWidgetContainer -> container)
    mapParametre += (cstMockupContext -> CommonObjectForMockupProcess.mockupContext)
    mapParametre += (cstGenerateController -> CommonObjectForMockupProcess.generateController.toString)
    mapParametre += (cstGlobalContext -> IBalsamiqFreeMarker.globalContext)
    mapParametre += (cstGeneratedFileName -> CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement)
    mapParametre += (cstMainMockupName -> CommonObjectForMockupProcess.ecranContenantLeSegment)
    mapParametre += (cstUtility -> utilitaire)
    mapParametre += (cstTemplateName -> templateName)
    mapParametre += (cstCommonObject -> CommonObjectForMockupProcess)
    mapParametre += (cstConstants -> CommonObjectForMockupProcess.constants)
    return mapParametre

  }
  // ------------------------------------------------------------------------------------------------------------

}
