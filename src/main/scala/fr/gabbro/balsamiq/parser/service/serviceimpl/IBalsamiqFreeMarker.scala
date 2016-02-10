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

import java.io.File
import java.io.FileInputStream
import java.util.Properties
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.propertiesAsScalaMap
import scala.collection.mutable.StringBuilder
import java.io.InputStreamReader
import fr.gabbro.balsamiq.parser.service.TIBalsamiqFreeMarker
import fr.gabbro.balsamiq.parser.modelimpl.CatalogAPlat
import fr.gabbro.balsamiq.parser.modelimpl.MockupContext
import fr.gabbro.balsamiq.parser.modelimpl.CatalogBalsamiq
import fr.gabbro.balsamiq.parser.modelimpl.CatalogDesComposants
import fr.gabbro.balsamiq.parser.modelimpl.GlobalContext
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._

/**
 * <p>==============================================================================================================================   </p>
 *  <p>*** Principe général IbalsamiqFreeMarker *** </p>
 *  <p>============================================================================================================================== </p>
 *  <p>Scan du repertoire .asset pour mettre en table les composants Balsamiq partagés entre les différents projets </p>
 *  <p>Scan du repertoire contenant les maquettes de l'application </p>
 *  <p>Pour chaque fichier bmml trouvé : </p>
 *  <p>   Mise en table des widgets de l'écran en cours (format xml vers format plat) </p>
 *  <p>   A partir du format plat, pour chaque widget, détermination des fils (widgets inclus dans un container) </p>
 *   <p>  Puis constitution du catalogue final en respectant la hiérarchie container, fils. </p>
 *   <p>  le catalogue est un modele 1 n, chaque entrée d'un container pouvant être lui même un container </p>
 *   <p>  Enrichissement de chaque entrée du catalogue : calcul de position dans le container :  </p>
 *   <p>  on détermine à la fois le n° de ligne et le numéro de cellule en 12eme par rapport au container.</p>
 *   <p>Exécution d'un premier traitement de validité des informations dans le catalogue</p>
 *   <p>  Test d'inclusion de l'ensemble des widgets dans un gabarit principal</p>
 *   <p>  Test de chevauchement de contenu dans un container</p>
 *   <p>  Test de duplication de contenu dans le catalogue (2 objets ayant la même position et la même taille</p>
 *   <p> Genération de code à partir du catalogue enrichi</p>
 *   <p>  Pour chaque container, extraction des composants triés par ligne et par colonne.</p>
 *   <p>  Le code source étant généré, ecriture des fichiers HTML et code (java, scala).</p>
 *   <p> On génere 2 fichiers HTML, le 2eme contenant les clefs des libelles à traduire.</p>
 *   <p>  Remarque pour chaque template on peut associer un template javascript et un template code</p>
 *   <p> Pour un widget donné, les templates sont exécutés automatiquement à chaque instanciation d'un composant.</p>
 *  <p>   Ce mécanisme permet de générer pour un écran à la fois la partie HTML, la partie code java et la partie javascript</p>
 * ==============================================================================================================================</p>
 */
object IBalsamiqFreeMarker extends App with TIBalsamiqFreeMarker {

  if (!init()) { System.exit(99) }
  else {
    process() // process du batch
    destroy // destroy des ressources du batch
    System.exit(0)
  }

  /**
   *  initialiation des properties du programmexx
   *  globalContext un objet scala accessible pendant le traitement de l'ensemble des mockups
   *  MoteurTemplateFreeMarker est instancié une seule fois en début de traitement
   *  MoteurAnalyseJericho est instancié une seule fois en début de traitement.
   *  CatalogDesComposants est instancié une seule fois en début de traitement et contient l'ensemble des widgets composants utilisables dans les mockups
   *
   */
  private def init(): Boolean = {
    if (!initProperties()) {
      logBack.info(utilitaire.getContenuMessage("mes7"));
      false
    } else {
      moteurTemplateFreeMarker = new MoteurTemplatingFreeMarker(CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir, CommonObjectForMockupProcess.generationProperties.srcWebFilesDir, CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath, globalContext)
      globalContext.moteurTemplatingFreeMarker = moteurTemplateFreeMarker
   
      if (moteurTemplateFreeMarker.init()) { // init du moteur et chargement des templates
        moteurJericho = new MoteurAnalyseJericho(moteurTemplateFreeMarker, utilitaire) // les trad
        globalContext.moteurJericho=moteurJericho
        catalogDesComposantsCommuns = new CatalogDesComposants // catalogue commun à l'ensemble des écrans
        true
      } else { false }

    }
  }
  /**
   *
   */
  private def destroy: Unit = {

  }
  /**
   * -------------------------------------------------------------------------------------------------------------------
   * process de l'ensemble des fichiers du repertoire balsamiq
   * on extrait d'abord les fragments des fichiers mockups principaux
   * Execution des traitement globaux puis génération des fichiers javascript 
   * En fin de traitement de l'ensemble des fichiers, on sauvegarde les clefs de traduction dans un fichier commun
   * --------------------------------------------------------------------------------------------------------------------
   *
   */
  private def process(): Boolean = {
    logBack.info(utilitaire.getContenuMessage("mes40"))
    // on met en table les composants du catalogue
    if (catalogDesComposantsCommuns.chargementDesCatalogues(CommonObjectForMockupProcess.generationProperties.balsamiqAssetDir)) { // chargement du catalog BootStrap  
      logBack.info(utilitaire.getContenuMessage("mes41"))
      // generation des fragments depuis les mockups principaux. l'instance de GlobalContext et traitementBinding est temporaire.
      if (CommonObjectForMockupProcess.generationProperties.processExtractFragments) { // on extrait les fragments du bmml principal ?
        new ExternalisationContenuDesFragments(CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir, catalogDesComposantsCommuns, moteurTemplateFreeMarker, new TraitementBinding(moteurTemplateFreeMarker, new GlobalContext)).process()   // extraction des fragments
      }
      traitementDesFichiersDuRepertoireBalsamiq(CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir) // traitement ensemble des fichiers

      logBack.info(utilitaire.getContenuMessage("mes42"))
      // *** templates à exécuter après le traitement de tous les écrans ***
      traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.globalExecutionTemplate1, CommonObjectForMockupProcess.generationProperties.globalExecutionFilePath1, traitementFormatageSourceJava)
      traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.globalExecutionTemplate2, CommonObjectForMockupProcess.generationProperties.globalExecutionFilePath2, traitementFormatageSourceJava)
      traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.globalExecutionTemplate3, CommonObjectForMockupProcess.generationProperties.globalExecutionFilePath3, traitementFormatageSourceJava)
      logBack.info(utilitaire.getContenuMessage("mes44"))
      globalContext.generation_fichiers_javascript // generation de l'ensemble des fichiers javascript
      //globalContext.printBindedForms()
      logBack.info(utilitaire.getContenuMessage("mes59"))
      moteurJericho.sauvegardeDesClefsDeTraduction // ecriture dans fichier properties des clefs de traduction
      moteurJericho.traitementDeltaDesFichiersDeTraductionDesDifferentsPays; // mise à jours des fichiers properties internationalisés
      return true
    } else { false }

  }

  /**
   *
   * <p>--------------------------------------------------------------------------------------------------------------------</p>
   * <p>scan du répertoire contenant les sources de balsamiq</p>
   * <p> traitement du repertoire en cours  et des sous repertoires</p>
   * <p>le répertoire "assets" contient les fichiers balsamiq communs et ne doit pas être traité</p>
   * <p>En fin de traitement de tous les mockups, exécution des traitements globaux pour générer par exemple les menu</p>
   * <p>En fin de traitement on génère l'ensemble des fichiers javascript (1 par mokcup principal.</p>
   * <p>Pour rappel, le contenu javascript du mockup principal et de ses fragments sont stockés dans le même fichier.</p>
   * <p>Modif le 22/4/15 par gl : le traitement des fichiers se fait en 2 passes , d'abord les fragments puis les écrans principaux   *
   * <p>---------------------------------------------------------------------------------------------------------------------
   *
   * @param url of directory containing mockup to process
   * @return number of files processed
   */
  private def traitementDesFichiersDuRepertoireBalsamiq(directory1: String): Int = {
    var compteur_fichiers_traites = 0
    logBack.info(utilitaire.getContenuMessage("mes60"), directory1)
    // on traite d'abord les fragments pour mettre en globalSection toutes les références
    val fichiersBalsamiqATraiter = new File(directory1).listFiles.toList
    val utilZip = new UtilZip() // utilitaire winzip pour extraction des fichiers bmml despuis l'archive générée par l'export balsamiq3
    utilZip.scanRepositoryToExtractBmmlFile(fichiersBalsamiqATraiter)

    traitementDesFichiers(fichiersBalsamiqATraiter, true) // on traite d'abord les fragments pour mettre à jour les dépendances en global pour les écrans principaux.
    traitementDesFichiers(fichiersBalsamiqATraiter, false) // puis on traite les autres fichiers (ecrans principaux).

    logBack.info(utilitaire.getContenuMessage("mes14"), directory1, compteur_fichiers_traites)

    def traitementDesFichiers(files: List[File], onlyFragment: Boolean): Unit = {
      files.foreach(file => {
        // on traite de façon itérative les sous répertoires.
        if ((file.isDirectory()) && (file.getName() != cstAssets)) { traitementDesFichiers(file.listFiles.toList, onlyFragment) }
        else {
          // on ne sélectionne que les fichiers se terminant par .bmml
          if (file.getName.endsWith(cstBalsamiqFileSuffix)) {
            val (fic, rep, useCase, fileNameComplet, isAfragment, fragmentName, generateController, ecranContenantLeFragment, typeDeFragment) = utilitaire.getFileInformation(file)
            if (onlyFragment) { // on ne traite que les fragments 
              if (isAfragment) {
                compteur_fichiers_traites += 1;
                traitementFichierBalsamiq(file)
              }
            } else { // on ne traite que les écrans principaux
              if (!isAfragment) {
                compteur_fichiers_traites += 1;
                traitementFichierBalsamiq(file)

              }
            }
          }
        }

      })
    }
    compteur_fichiers_traites
  }

  /**
   * <p>--------------------------------------------------------------------------------------------------------------------------------------------</p>
   * <p><b>processing a mockup</b></p>
   * <p>---------------------------------------------------------------------------------------------------------------------------------------------</p>
   * <p>le nom des Mockups doit respecter les règles suivantes</p>
   * <p>pour un Ecran Principal : nom du usecase - nom de l'ecran commençant par ec exemple:useCase2-ectestgl01.bmml</p>
   * <p>pour un fragment : nom du usecase - nom de l'ecran principal $ nom du fragment § type du fragment  exemple:useCase2-ectestgl01$fragment1§Panel.bmml.old</p>
   * <p>MockupContext est instanciée en début de traitement du mockup et accessible depuis CommonObjectForMockupProcess</p>
   * <p>TraitementBinding est instanciée en début de traitement</p>
   * <p>MockupContext contient l'ensemble des fragments liés au traitement du mockup en cours. Cette classe sert à stocker des</p>
   * <p>informations spécifiques au mockup en cours et accessibles depuis le moteur de template freeMarker.</p>
   * <p>depuis le nom du mockup en cours de traitement on déduit les informations suivates:</p>
   *  <p>   le nom du usecase, le nom du mockup ou fragment</p>
   * <p>On déduit aussi si le mockup est moockup est un fragment, s'il faut générer un contôleur, ...</p>
   * <p>si le mockup en cours n'est pas un fragment(donc un mockup principal), on met alors en table l'ensemble des fragments</p>
   * <p>On met en table les preserve Sections à la fois pour :</p>
   * <p>       Le fichier généré html, le fichier généré javascript (si le mockup n'est pas un fragment et l'ensemble des fichiers java générés par sous package.</p>
   * <p>       Les preserves sections sont stockées dans une map de GlobalContext, car les fichiers javascript sont enrichis par les fragments, elles ne peuvent donc être locales </p>
   * <p>       au traitement du mockup. Le contenu des preserve section est récupéré depuis les templates freeMarker<p>
   * <p>       Attention : les preserve sections des classes générées par le binding sont gérées directement par la classe TraitementBindinp>
   * <p>--------------------------------------------------------------------------------------------------------------------------------------------------------</p>
   * @param fichierBalsamiq : mockup to process
   * @return : true or false
   */
  private def traitementFichierBalsamiq(fichierBalsamiq: File): Boolean = {
    logBack.info(utilitaire.getContenuMessage("mes8"), fichierBalsamiq.getCanonicalPath)
    var sourceEcran = new StringBuilder() // contiendra le code genere depuis les templates
    CommonObjectForMockupProcess.mockupContext = new MockupContext // zone commune de communication pour l'ensemble des widgets de la page
    CommonObjectForMockupProcess.globalContext = globalContext
    var traitementBinding = new TraitementBinding(moteurTemplateFreeMarker, globalContext) // init de la classe binding
    // fileNameComplet contient 
    val (fic, rep, useCase, fileNameComplet, isAfragment, fragmentName, generateController, ecranContenantLeFragment, typeDeFragment) = utilitaire.getFileInformation(fichierBalsamiq)
    CommonObjectForMockupProcess.isAfragment = isAfragment
    CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement = if (isAfragment) { fragmentName } else { fic }
    CommonObjectForMockupProcess.nomDuRepertoirerEnCoursDeTraitement = rep
    CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement = useCase
    CommonObjectForMockupProcess.generateController = generateController
    CommonObjectForMockupProcess.ecranContenantLeSegment = if (ecranContenantLeFragment != "") ecranContenantLeFragment else CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement
    CommonObjectForMockupProcess.typeDuFragmentEnCoursDeTraitement = typeDeFragment
    // la détection des dépendances ne sert plus plus
  //  new DetectDependencies(CommonObjectForMockupProcess.mockupContext).process() // mise en table des dépendances (donc des fragments)
    if (!isAfragment) { // si ce n'est pas un fragment => mise en table menu et recherche des fragments
      CommonObjectForMockupProcess.mockupContext.fragments ++= new DetectFragments(utilitaire).processEtMiseEntable() // mise en table des fragments

    } // fin de isAFragment
    else {

    }
    /**
     * ------------------------------------------------------------------------------------------
     * <p>*** on indique la location dans la zone de l'écran pour le moteur de template ***</p>
     * ------------------------------------------------------------------------------------------
     */
    val location = globalContext.retrieveLocation(fichierBalsamiq.getName)
    CommonObjectForMockupProcess.mockupContext.location = location
    globalContext.paths.add(location)
    logBack.info(utilitaire.getContenuMessage("mes55"), fichierBalsamiq.getName())

    val catalogAPlat = new CatalogAPlat(fichierBalsamiq, moteurTemplateFreeMarker, traitementBinding, catalogDesComposantsCommuns)
    var sourceJavascript = new StringBuilder() // contiendra le code généré depuis les templates
    var codeDesComposants = new StringBuilder() // contiendra le code java généré pour la page
    val catalogBalsamiq = new CatalogBalsamiq(traitementBinding) // catalogBalsamiq final
    val (ok, w, h) = catalogAPlat.chargementCatalog() // chargement du catalogue
    if (!ok) {
      logBack.info(utilitaire.getContenuMessage("mes9"), fichierBalsamiq.getName())
      false // fin de traitement
    } else {
      logBack.info(utilitaire.getContenuMessage("mes56"), fichierBalsamiq.getName())
      CommonObjectForMockupProcess.mockupContext.global_max_width = w
      CommonObjectForMockupProcess.mockupContext.global_max_height = h
      catalogBalsamiq.creation_catalog(catalogAPlat.catalog, w, h) // creation et enrichissemebt du catalogue balamiq
      val controleValiditeProcess = new ControleValidite(catalogBalsamiq.catalog, traitementBinding, globalContext).process
      val generationDeCode = new ModuleGenerationCode(moteurTemplateFreeMarker) // module qui genère le coode
      // traitement de l'ensemble des composants
      val (source2, sourceJavascript2, code2) = generationDeCode.traitement_widget_par_ligne_colonne(catalogBalsamiq.catalog(0).tableau_des_fils, 0, 0, null, catalogBalsamiq.catalog(0), false)
      // catalogBalsamiq.catalog(0) contient le widget gabarit de la pagel
      // qui doit être généré après le contenu de la page afin de traiter le javascript et le code
      // de l'ensemble des widgets de la maquette.
      val (_, source1, sourceJavascript1, code1) = moteurTemplateFreeMarker.generationDuTemplate(catalogBalsamiq.catalog(0), CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (cstJavascript, sourceJavascript))
      sourceEcran = sourceEcran.append(source1); // header
      sourceEcran = sourceEcran.append(source2); // contenu
      sourceJavascript = sourceJavascript.append(sourceJavascript1)
      sourceJavascript = sourceJavascript.append(sourceJavascript2)
      codeDesComposants = codeDesComposants.append(code1) // code java ou scala début du gabarit
      codeDesComposants = codeDesComposants.append(code2) // code java ou scala début du gabarit
      val (_, source3, sourceJavascript3, code3) = moteurTemplateFreeMarker.generationDuTemplate(catalogBalsamiq.catalog(0), CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (cstJavascript, sourceJavascript))
      sourceEcran = sourceEcran.append(source3); // footer
      sourceJavascript = sourceJavascript.append(sourceJavascript3)
      codeDesComposants = codeDesComposants.append(code3) // code java ou scala fin du gabarit
      /**
       * <p>-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------</p>
       * <p>genération des classes java ou scala</p>
       * <p>Le source des classes est généré par freemarker méthod MockupContext.setCodeClasse</p>
       * <p>En fin de traitement du mockup, on génère les des classes java du mockup (sauf les dto et les classes générées par le binding qui sont gérées par la classe traitementBinding</p>
       * <p>-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------</p>
       */
      logBack.info(utilitaire.getContenuMessage("mes45"))

      CommonObjectForMockupProcess.mockupContext.ecritureDuCodeJaveOuScala // ecriture des fichiers contenant les classes java ou scala
      moteurTemplateFreeMarker.ecritureDuFichierHTML(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, sourceEcran.toString) // generation du fichier HTML et extraction des libelles à traduire

      // génération des classes java ou scala qui sont définies dans le binding
      logBack.info(utilitaire.getContenuMessage("mes47"))
      traitementBinding.generationDuSourceDesClassesEtCreationDuFichier
      /**
       * ---------------------------------------------------------------------
       * *** template à exécuter après le traitement de chaque écran ***
       * attention obligatoirement après le traitement des widgets
       * ---------------------------------------------------------------------
       */
      logBack.info(utilitaire.getContenuMessage("mes43"))
      traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.localExecutionTemplate1, CommonObjectForMockupProcess.generationProperties.localExecutionFilePath1, traitementFormatageSourceJava)
      traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.localExecutionTemplate2, CommonObjectForMockupProcess.generationProperties.localExecutionFilePath2, traitementFormatageSourceJava)
      traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.localExecutionTemplate3, CommonObjectForMockupProcess.generationProperties.localExecutionFilePath3, traitementFormatageSourceJava)
      logBack.info(utilitaire.getContenuMessage("mes9"), fichierBalsamiq.getName())
      true
    }
  } // fin de la fonction processFilea

  /**
   * -----------------------------------------------------------------------------------------------------------
   * *** appel d'un template local ou global ***
   * En fin de traitement, le contenu du fichier est indenté en fonction de son type (java ou javascript)
   * -----------------------------------------------------------------------------------------------------------
   *
   * @param localExecutionTemplate : name of template
   * @param localExecutionPath : path of template
   * @param traitementFormatageSourceJava :instance of class TraitementFormatageSourceJava
   */
  private def traitementLocalOuGlobalTemplate(localExecutionTemplate: String, localExecutionPath: String, traitementFormatageSourceJava: TraitementFormatageSourceJava): Unit = {
    if (localExecutionTemplate != "") {
      val filename = CommonObjectForMockupProcess.generationProperties.generatedProjectDir + System.getProperty("file.separator") + utilitaire.substituteKeywords(localExecutionPath).replace(System.getProperty("file.separator"), "/").replace("//", "/")
      val (ret1, source1, _, _) = moteurTemplateFreeMarker.generationDuTemplate(localExecutionTemplate, CommonObjectForMockupProcess.templatingProperties.phase_debut, null)
      val (ret2, source2, _, _) = moteurTemplateFreeMarker.generationDuTemplate(localExecutionTemplate, CommonObjectForMockupProcess.templatingProperties.phase_fin, null)
      if ((source1 + source2).trim.size > 0) {
        utilitaire.ecrire_fichier(filename, source1 + source2)
      }
    }
  }

  /**
   * <p>----------------------------------------------------------------------------------------------------</p>
   * <p>lecture du fichier properties et des propriétés passées en system properties et mise en table</p>
   * <p>Les propriétés sont classées de la façon suivante :</p>
   * <p> engineProperties     : propriétés d'exécution du moteur Gabbro</p>
   * <p> generationProperties : propriétés liées à la génération</p>
   * <p>templatingProperties : propriétés du moteur de templating freeMarker</p>
   * <p>----------------------------------------------------------------------------------------------------</p>
   *
   * @return true or false
   */
  private def initProperties(): Boolean = {
    var ok = true
    logBack.info(utilitaire.getContenuMessage("mes58"))

    CommonObjectForMockupProcess.generationProperties.projectName = System.getProperty("gencodefrombalsamiq.projectName")
    CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatePropertiesFile = System.getProperty("gencodefrombalsamiq.freemarkerTemplatesPropertiesFile")
    CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir = System.getProperty("gencodefrombalsamiq.freemarkerTemplatesDir")
    CommonObjectForMockupProcess.engineProperties.messagesFile = System.getProperty("gencodefrombalsamiq.messagesFile")
    if (CommonObjectForMockupProcess.generationProperties.projectName != null) { CommonObjectForMockupProcess.generationProperties.generatedProjectDir = System.getProperty("gencodefrombalsamiq.generatedProjectDir").trim.replace("%project%", CommonObjectForMockupProcess.generationProperties.projectName) }
    CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir = System.getProperty("gencodefrombalsamiq.freemarkerTemplatesDir")
    CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir = System.getProperty("gencodefrombalsamiq.balsamiqBmmlDir")
    CommonObjectForMockupProcess.generationProperties.balsamiqAssetDir = System.getProperty("gencodefrombalsamiq.freemarkerCatalogComposantsDir")
    CommonObjectForMockupProcess.generationProperties.configProperties = System.getProperty("gencodefrombalsamiq.propertiesFile")

    if (CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir == null ||
      CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir == null ||
      CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir == null ||
      CommonObjectForMockupProcess.engineProperties.messagesFile == null ||
      CommonObjectForMockupProcess.generationProperties.projectName == null ||
      CommonObjectForMockupProcess.generationProperties.configProperties == null) {
      logBack.error(utilitaire.getContenuMessage("mes31"))
      ok = false

    } else {
      CommonObjectForMockupProcess.generationProperties.projectName = CommonObjectForMockupProcess.generationProperties.projectName.trim
      val props = new Properties();
      val ficPropertyName = CommonObjectForMockupProcess.generationProperties.configProperties
      props.load(new InputStreamReader(new FileInputStream(ficPropertyName), cstUtf8));
      //    props.load(new FileInputStream(ficPropertyName))
      val propsMap = props.toMap[String, String]
      CommonObjectForMockupProcess.engineProperties.loadProperties(propsMap)
      CommonObjectForMockupProcess.generationProperties.loadProperties(propsMap)
      CommonObjectForMockupProcess.templatingProperties.loadProperties(propsMap)
      val fic1 = new File(CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir)
      val fic3 = new File(CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir)
      if (!fic1.exists()) { logBack.error(utilitaire.getContenuMessage("mes10"), Array(CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir)); ok = false }
      if (!fic3.exists()) { logBack.error(utilitaire.getContenuMessage("mes10"), Array(CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir)); ok = false }
    }
    ok
  }

} // fin de la classe FileConvert
