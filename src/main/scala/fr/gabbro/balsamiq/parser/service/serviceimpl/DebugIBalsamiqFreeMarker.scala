package fr.gabbro.balsamiq.parser.service.serviceimpl
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

// ==============================================================================================================================  
// *** Principe general IbalsamiqFreeMarker ***
// ==============================================================================================================================
// Scan du repertoire .asset pour mettre en table les composants Balsamiq partagés entre les différents projets
// Scan du repertoire contenant les maquettes de l'application
// Pour chaque fichier bmml trouvé :
//    Mise en table des widgets de l'écran en cours (format xml vers format plat)
//    A partir du format plat, pour chaque widget, détermination des fils (widgets inclus dans un container)
//    Puis constitution du catalogue final en respectant la hiérarchie container, fils.
//    le catalogue est un modele 1 n, chaque entrée d'un container pouvant être lui même un container
//    Enrichissement de chaque entrée du catalogue : calcul de position dans le container :
//    on détermine à la fois le n° de ligne et le numéro de cellule en 12eme par rapport au container.
// Exécution d'un premier traitement de validité des informations dans le catalogue
//    Test d'inclusion de l'ensemble des widgets dans un gabarit principal
//    Test de chevauchement de contenu dans un container
//    Test de duplication de contenu dans le catalogue (2 objets ayant la même position et la même taille
// Genération de code à partir du catalogue enrichi
//    Pour chaque container, extraction des composants triés par ligne et par colonne.
//    A chaque composant, on associe un template et on fait appel au moteur de templating Fréémarker pour exécuter le emaplte.
//    Le code source étant généré, ecriture des fichiers HTML et code (java, scala).
//    On génere 2 fichiers HTML, le 2eme contenant les clefs des libelles à traduire.
//    Remarque pour chaque template on peut associer un template javascript et un template code
//    POur un widget donné, les templates sont exécutés automatiquement à chaque instanciation d'un composant.
//    Ce mécanisme permet de générer pour un écran à la fois la partie HTML, la partie code java et la partie javascript
// ==============================================================================================================================
object DebugIBalsamiqFreeMarker extends App with TIBalsamiqFreeMarker {
 

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
 
  if (!init()) { System.exit(99) }
  else {
    process() // process du batch
    destroy // destroy des ressources du batch
    System.exit(0)
  }

  /**
   *  initialiation des properties du programme
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
      traitementMenu = new TraitementMenu(globalContext) // va servir à mettre en session les menuItem

      if (moteurTemplateFreeMarker.init()) { // init du moteur et chargement des templates
        moteurJericho = new MoteurAnalyseJericho(moteurTemplateFreeMarker, utilitaire) // les trad
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
   * En fin de traitement de l'ensemble des fichiers, on sauvegarde les clefs de traduction dans un fichier commun
   * --------------------------------------------------------------------------------------------------------------------
   *
   */
  private def process(): Boolean = {
    logBack.info(utilitaire.getContenuMessage("mes40"))
    if (catalogDesComposantsCommuns.chargementDesCatalogues(CommonObjectForMockupProcess.generationProperties.balsamiqAssetDir)) { // chargement du catalog BootStrap  
      logBack.info(utilitaire.getContenuMessage("mes41"))
      traitementDesFichiersDuRepertoireBalsamiq(CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir) // traitement ensemble des fichiers
      logBack.info(utilitaire.getContenuMessage("mes59"))
      moteurJericho.sauvegardeDesClefsDeTraduction // ecriture dans fichier properties des clefs de traduction
      moteurJericho.traitementDeltaDesFichiersDeTraductionDesDifferentsPays; // mise à jours des fichiers properties internationalisés
      return   true 
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
   * <p>---------------------------------------------------------------------------------------------------------------------
   *
   * @param url of directory containing mockup to process
   * @return number of files processed
   */
  private def traitementDesFichiersDuRepertoireBalsamiq(directory1: String): Int = {
    var compteur_fichiers_traites = 0
    logBack.info(utilitaire.getContenuMessage("mes60"), directory1)
    val fichiersBalsamiqAtraiter = new File(directory1).listFiles.toList
    fichiersBalsamiqAtraiter.foreach(file => {
      if ((file.isDirectory()) && (file.getName() != CommonObjectForMockupProcess.constants.assets)) { compteur_fichiers_traites += traitementDesFichiersDuRepertoireBalsamiq(file.getPath()) }
      else {
        if (file.getName.endsWith(CommonObjectForMockupProcess.constants.balsamiqFileSuffix)) {
          compteur_fichiers_traites += 1;
          traitementFichierBalsamiq(file)
        }
      }
    })
    logBack.info(utilitaire.getContenuMessage("mes42"))
    // *** templates à exécuter après le traitement de tous les écrans ***
    traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.globalExecutionTemplate1, CommonObjectForMockupProcess.generationProperties.globalExecutionFilePath1, traitementFormatageSourceJava)
    traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.globalExecutionTemplate2, CommonObjectForMockupProcess.generationProperties.globalExecutionFilePath2, traitementFormatageSourceJava)
    traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.globalExecutionTemplate3, CommonObjectForMockupProcess.generationProperties.globalExecutionFilePath3, traitementFormatageSourceJava)
    /**
     * ------------------------------------------------------------
     * on génère les fichiers javascript
     * les fichiers javascript sont stockés dans une hashMap
     * ------------------------------------------------------------
     *
     */
    logBack.info(utilitaire.getContenuMessage("mes44"))
    globalContext.generation_fichiers_javascript
    // A modifier dès qu'une solution pou rle menu est trouvée
    val menu = globalContext.generation_code_source_menu
    logBack.info(utilitaire.getContenuMessage("mes14"), directory1, compteur_fichiers_traites)
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
    new DetectDependencies(CommonObjectForMockupProcess.mockupContext).process() // mise en table des dépendances
    if (!isAfragment) { // si ce n'est pas un fragment => mise en table menu et recherche des fragments
      CommonObjectForMockupProcess.mockupContext.fragments ++= new DetectFragments(utilitaire).processEtMiseEntable() // mise en table des fragments
      // si ce n'est pas un fragment, on génère le traitement preserve section pour le fichier javascript qui sera généré au niveau de l'écran principal
      //     CommonObjectForMockupProcess.traitementPreserveSectionTemplateJavascript = new TraitementPreserveSection().process(globalContext.getNomduFichierJavascript(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement)) // on met en table le contenu du fichier javascript pour traitment des preserve section
      globalContext.mapDesTraitementsPreserveSection += ((CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.templatingProperties.preserveCodeScript, "") -> new TraitementPreserveSection().process(globalContext.getNomduFichierJavascript(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement)))

      // on ne met en table les menus que pour les écrans. 
      // on ne met en table les menus que pour les écrans. 
      // les noms des écrans démarrent par "ec"
      // la dépendance est sous la forme : uc1.ecr1.ecr2
      if (CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement.toLowerCase().startsWith(CommonObjectForMockupProcess.generationProperties.generateControllerForPrefix.toLowerCase())) {
        traitementMenu.mise_en_table_classes_menu_item(fileNameComplet, useCase, "url")
      }
    } // fin de isAFragment
    /**
     * <p> ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------</p>
     * <p> *** une preserve section pour le fichier code java par sous package***</p>
     * <p> il faudra remodifier cette liste à chaque ajout de sous package de code java ou scala</p>
     * <p> principe de la preserve section: on met en table le code lors de la lecture de la preserve section</p>
     * <p> remarque : le code java est généré depuis freemarker par appel de la methode  MockupCOntext.setCodeClasse(className: String, classCode: String, subPackageName: String)</p>
     * <p>
     * <p> ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------</p>
     */
    val listeDesSubPackageCode = List(CommonObjectForMockupProcess.generationProperties.generatedOtherAlias,
      CommonObjectForMockupProcess.generationProperties.generatedSubPackage1,
      CommonObjectForMockupProcess.generationProperties.generatedSubPackage2,
      CommonObjectForMockupProcess.generationProperties.generatedSubPackage3)
    setPreserveSection(CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.templatingProperties.preserveCodeJavaOrScala, listeDesSubPackageCode)
    // création d'une preserve section pour le fichier html
    globalContext.mapDesTraitementsPreserveSection += ((CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.templatingProperties.preserveCodeIhm, "") -> new TraitementPreserveSection().process(utilitaire.getEmplacementFichierHtml(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.generationProperties.srcWebFilesDir))) // on met en table le contenu du fichier javascript pour traitment des preserve section
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
      catalogBalsamiq.creation_catalog(catalogAPlat.catalog) // creation et enrichissemebt du catalogue balamiq
      val controleValiditeProcess = new ControleValidite(catalogBalsamiq.catalog, traitementBinding, globalContext).process
      val generationDeCode = new ModuleGenerationCode(moteurTemplateFreeMarker)
      // traitement de l'ensemble des composants
      val (source2, sourceJavascript2, code2) = generationDeCode.traitement_widget_par_ligne_colonne(catalogBalsamiq.catalog(0).tableau_des_fils, 0, 0, null, catalogBalsamiq.catalog(0), false)
      // catalogBalsamiq.catalog(0) contient le widget gabarit de la pagel
      // qui doit être généré après le contenu de la page afin de traiter le javascript et le code
      // de l'ensemble des widgets de la maquette.
      val (_, source1, sourceJavascript1, code1) = moteurTemplateFreeMarker.generationDuTemplate(catalogBalsamiq.catalog(0), CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.javascript, sourceJavascript))
      sourceEcran = sourceEcran.append(source1); // header
      sourceEcran = sourceEcran.append(source2); // contenu
      sourceJavascript = sourceJavascript.append(sourceJavascript1)
      sourceJavascript = sourceJavascript.append(sourceJavascript2)
      codeDesComposants = codeDesComposants.append(code1) // code java ou scala début du gabarit
      codeDesComposants = codeDesComposants.append(code2) // code java ou scala début du gabarit
      val (_, source3, sourceJavascript3, code3) = moteurTemplateFreeMarker.generationDuTemplate(catalogBalsamiq.catalog(0), CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.javascript, sourceJavascript))
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

      // internationalisation du fichier ??
      if (CommonObjectForMockupProcess.generationProperties.processI18nInFiles == "true") {
        logBack.info(utilitaire.getContenuMessage("mes46"))
        moteurJericho.traductHtmlFile(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuRepertoirerEnCoursDeTraitement, CommonObjectForMockupProcess.generationProperties.srcWebFilesDir) // extraction des clefs de traduction
      }
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
   * <p>mise à jour table des preserves section pour le fichier de type code</p>
   * <p>les fichiers codes peuvent être localisés dans les sous-packages suivant:</p>
   *      <p>GenerationProperties.generatedOtherAlias = "" // alias other</p>
   *       <p>GenerationProperties.generatedsubPackage1 = "" // sous package1</p>
   *       <p>GenerationProperties.generatedsubPackage2 = "" // sous package2</p>
   *      <p>GenerationProperties.generatedsubPackage3 = "" // sous package3</p>
   *
   * @param typeDePreserve : type of preserve : code, javascript, html
   * @param subPackageList : List of subPackages
   */
  private def setPreserveSection(useCase: String, fileName: String, typeDePreserve: String, subPackageList: List[String]): Unit = {
    subPackageList.foreach(subPackage => {
      //FIXME passer TraitementPreserveSection en static ?
      globalContext.mapDesTraitementsPreserveSection += ((useCase, fileName, typeDePreserve, subPackage) -> new TraitementPreserveSection().process(utilitaire.getNomDuFichierCodeJavaOuScala(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, subPackage)))
    })

  }

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
      val traitementPreserveSection = new TraitementPreserveSection().process(filename) // utilisé pour récupérer le contenu des preserves section
      val (ret1, source1, _, _) = moteurTemplateFreeMarker.generationDuTemplate(localExecutionTemplate, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.traitementPreserveSection, traitementPreserveSection))
      val (ret2, source2, _, _) = moteurTemplateFreeMarker.generationDuTemplate(localExecutionTemplate, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.traitementPreserveSection, traitementPreserveSection))
      if ((source1 + source2).trim.size > 0) {
        var codeFormatte = ""
        if (filename.endsWith(CommonObjectForMockupProcess.generationProperties.languageSource)) {
          codeFormatte = traitementFormatageSourceJava.indentSourceCodeJava(source1 + source2)
        } else if (filename.endsWith(CommonObjectForMockupProcess.constants.suffixDesFichiersJavaScript)) {
          codeFormatte = traitementFormatageSourceJava.indentSourceCodeJavaScript(source1 + source2, 5)
        } else { codeFormatte = source1 + source2 }
        utilitaire.ecrire_fichier(filename, codeFormatte)
      }
    }
  }

  /**
   * <p>----------------------------------------------------------------------------------------------------</p>
   * <p>lecture du fichier properties et des propriétés passées en system properties et mise en table</p>
   * <p>Les propriétés sont classées de la façon suivante :</p>
   * <p> engineProperties     : propriétés d'exécution du moteur Gabbro</p>
   * <p> generationProperties : propriétés liées à la génération</p>
   *  <p>templatingProperties : propriétés du moteur de templating freeMarker</p>
   * <p>----------------------------------------------------------------------------------------------------</p>
   *
   * @return true or false
   */
  




    // lecture du fichier properties et des propriétés passées en system properties et mise en table
   def initProperties(): Boolean = {
    var ok = true
    //     CommonObjectForMockupProcess .generationProperties.projectName = System.getProperty("gencodefrombalsamiq.projectName").trim
    CommonObjectForMockupProcess.generationProperties.projectName = "projetBalsamiq2"
    CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatePropertiesFile = "c:/Temp/balsamiq/templates/freeMarkerTemplatesHTML.properties"
    CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir = "c:/temp/balsamiq/templates"
    CommonObjectForMockupProcess.engineProperties.messagesFile = "c:/temp/balsamiq/sourcesBalsamiq/messages.properties"
    CommonObjectForMockupProcess.generationProperties.generatedProjectDir = "C:/georges/projets/Zk/%project%".trim.replace("%project%", CommonObjectForMockupProcess.generationProperties.projectName)
    CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir = "c:/temp/balsamiq/sourcesBalsamiq"
    CommonObjectForMockupProcess.generationProperties.balsamiqAssetDir = "c:/temp/balsamiq/assets"
    CommonObjectForMockupProcess.generationProperties.configProperties = "C:/Temp/balsamiq/sourcesBalsamiq/config.properties"
    CommonObjectForMockupProcess.engineProperties.messagesFile = "C:/Temp/balsamiq/sourcesBalsamiq/messages.properties"
    if (CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir == null ||
      CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir == null ||
      CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir == null ||
      CommonObjectForMockupProcess.engineProperties.messagesFile == null ||
      CommonObjectForMockupProcess.generationProperties.configProperties == null) {
      logBack.error(utilitaire.getContenuMessage("mes31"))
      ok = false
    } else {
      val props = new Properties();
      val ficPropertyName = CommonObjectForMockupProcess.generationProperties.configProperties
      props.load(new InputStreamReader(new FileInputStream(ficPropertyName), CommonObjectForMockupProcess.constants.utf8));
 
      val propsMap = props.toMap[String, String]
      CommonObjectForMockupProcess.engineProperties.loadProperties(propsMap)
      CommonObjectForMockupProcess.generationProperties.loadProperties(propsMap)
      CommonObjectForMockupProcess.templatingProperties.loadProperties(propsMap)
      val fic1 = new File(CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir)
      val fic3 = new File(CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir)
      if (!fic1.exists()) { println(utilitaire.getContenuMessage("mes10").format(CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir)); ok = false }
      if (!fic3.exists()) { println(utilitaire.getContenuMessage("mes10").format(CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir)); ok = false }
      //   if (!fic4.exists()) { println("GetpropertiesBalsamiq : fichier %s non trouve".format(CommonObjectForMockupProcess .sourceDesComposantsBalsamiqEtendus)); ok = false }
    }
    ok
  }

} // fin de la classe FileConvert

