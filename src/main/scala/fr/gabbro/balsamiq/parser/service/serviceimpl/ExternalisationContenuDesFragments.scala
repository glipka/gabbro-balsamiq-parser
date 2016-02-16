package fr.gabbro.balsamiq.parser.service.serviceimpl
// Gabbro - scala program to manipulate balsamiq sketches files an generate code with FreeMarker
// Version 1.0
// Copyright (C) 2015 Georges Lipka
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

//import scala.reflect.io.Directory
import java.io.File
import org.apache.commons.io.FileUtils
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
import fr.gabbro.balsamiq.parser.service.TTraitementCommun
import scala.collection.JavaConversions._
import fr.gabbro.balsamiq.parser.modelimpl.CatalogAPlat
import fr.gabbro.balsamiq.parser.modelimpl.CatalogBalsamiq
import fr.gabbro.balsamiq.parser.model.TCatalogAPlat
import fr.gabbro.balsamiq.parser.modelimpl.CatalogDesComposants
import scala.collection.mutable.ArrayBuffer
import fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase
import scala.collection.mutable.Map
import fr.gabbro.balsamiq.parser.modelimpl.MockupContext
import fr.gabbro.balsamiq.parser.modelimpl.CatalogAPlat
import fr.gabbro.balsamiq.parser.modelimpl.GlobalContext

/**
 * @author FRA9972467
 * cette classe sert à générer les fragments bmml depuis le contenu de certains containers dans le mockup principal de l'écran.
 * Les containers sont référencés dans le paramètre generateFragmentFromTheseContainers du fichier properties
 */
class ExternalisationContenuDesFragments(val repertoireDesBmmlATraiter: String, val catalogDesComposantsCommuns: CatalogDesComposants, val moteurTemplatingFreeMarker: MoteurTemplatingFreeMarker, val traitementBinding: TraitementBinding,globalContext:GlobalContext) extends TCatalogAPlat {
  final val repertoireContenuDesFragmentsGeneres = repertoireDesBmmlATraiter + "/" + cstGeneratedFragment
  var nombreDeFragmentsGeneres = 0
  /**
   * Le repertoire  repertoireContenuDesFragments va contenir les fichiers bmml générés.
   */
  def process(): Unit = {
    suppressionRepertoireContenuDesFragments() // suppression du repertoire de travail.
    traitementDesFichiersBmmlNonFragments(new File(repertoireDesBmmlATraiter).listFiles().toList)
  }

  /**
   * le repertoire cstGeneratedFragment contient les fragments générés depuis le contenu de composants spécifiques dans le mockup principal
   */
  def suppressionRepertoireContenuDesFragments(): Unit = {
    try {
      FileUtils.deleteDirectory(new File(repertoireContenuDesFragmentsGeneres))
      FileUtils.forceMkdir(new File(repertoireContenuDesFragmentsGeneres))
    } catch {
      case ex: Exception =>
    }
  } 

  /**
   * scan itératif du répertoire et traitement de chaque fichier bmml qui n'est pas un fragment.
   * @param files
   */
  def traitementDesFichiersBmmlNonFragments(files: List[File]): Unit = {
    files.foreach(file => {
      // on traite de façon itérative les sous répertoires.
      if ((file.isDirectory()) && (file.getName() != cstAssets) && (file.getName() != cstGeneratedFragment)) { traitementDesFichiersBmmlNonFragments(file.listFiles.toList) }
      else {
        // on ne sélectionne que les fichiers se terminant par .bmml
        if (file.getName.endsWith(cstBalsamiqFileSuffix)) {
          val (fic, rep, useCase, fileNameComplet, isAfragment, fragmentName, generateController, ecranContenantLeFragment, typeDeFragment) = utilitaire.getFileInformation(file)
          if (!isAfragment) { // on ne traite que les fichiers principaux et non les fragments
            traitementDesFragmentsDuMockupBalsamiq(file) // generation des fragments du mockup en cours
            
          }
        }
      }

    })
    logBack.info(utilitaire.getContenuMessage("mes64"), nombreDeFragmentsGeneres) // nbre total de fragments générés.

  }
  // -------------------------------------------------------------------------------------------------
  // catalogBalsamiq : pour chaque mockup traité, on constitue le catalogue hiérarchique des widgets à partir du fichier BMML 
  // ce catalogue va servir a récupérer le 
  // -----------------------------------------------------------------------------------
  /**
   * @param fichierBalsamiq : File fichier bmml en cours de traitement
   *
   * @return true of false
   * pour chaque mockup traité, on constitue le catalogue hiérarchique des widgets à partir du fichier bmml
   */
  def traitementDesFragmentsDuMockupBalsamiq(fichierBalsamiq: File): Boolean = {
    val (fic, rep, useCase, fileNameComplet, isAfragment, fragmentName, generateController, ecranContenantLeFragment, typeDeFragment) = utilitaire.getFileInformation(fichierBalsamiq)

    val catalogAPlat = new CatalogAPlat(fichierBalsamiq, moteurTemplatingFreeMarker, traitementBinding, catalogDesComposantsCommuns)
    val catalogBalsamiq = new CatalogBalsamiq(traitementBinding) // catalogBalsamiq final
    val mapDesSourcesDesFragmentsAGenerer = Map[(String, String), String]() // tableau des fragments à générer pour le fichier bmml en cours : cle=nom du fragment et type de fragment, valeur : contenu du fragment

    val (ok, w, h) = catalogAPlat.chargementCatalog() // chargement du catalogue
    if (!ok) {
      logBack.info(utilitaire.getContenuMessage("mes9"), fichierBalsamiq.getName())
      false // fin de traitement
    } else {
      catalogBalsamiq.creation_catalog(catalogAPlat.catalog, w, h) // creation et enrichissement du catalogue balsamiq
      // la map mapDesSourcesDesFragmentsAGenerer  contient le code source des fragments à générer. 
      recuperationCodeXmlContainersDeFragment(catalogBalsamiq.catalog, mapDesSourcesDesFragmentsAGenerer,fic, useCase) // récuperation du contenu des fragements
      generationDesfichiersFragmentDuMockupEnCours(mapDesSourcesDesFragmentsAGenerer, fic, useCase, w.toString, h.toString)
    }
    true
  }

  /**
   * @param branche : ArrayBuffer[WidgetDeBase]
   * @param niveau : Map[String,String] nom du fragment et code source du fragment
   * Detection des containers pour lesquels on génère des fragments dans la branche en cours.
   * On scanne l'arbre des composants du catalog (relation 1 à n) en traitant le composant et ses enfants.
   * si le widget en cours est un container pour lequel on doit générer fragment, on récupère le contenu xml de chaque widget parent et enfants
   * le contenu récupéré sera écrit dans un fichier fragment.
   *
   */
  import scala.collection.mutable.Map
  def recuperationCodeXmlContainersDeFragment(branche: ArrayBuffer[WidgetDeBase], mapDesSourcesDesFragmentsAGenerer: Map[(String, String), String],fic:String,usecase:String) {
    var numero_En_cours = 0

    branche.foreach(controle => { // traitement de la branche en cours
      val widgetNameOrComponentName = controle.getWidgetNameOrComponentName // nom du widget ou du composant
      // le composant en cours est il dans la liste des containers pour lesquels on génère un fragment ?  
      var lewidgetGenereUnFragment = false
      var typeDuFragment = ""
      // on balaie la table des containers pouvant générer un fragement et on récupère le type de fragment s'il y a un correspondance avec le nom du widget
      // la clef de container type est un tuple : nom du composant, type du composant
      CommonObjectForMockupProcess.generationProperties.generateFragmentFromTheseContainers.foreach(containerType => {
        if (containerType._1 == widgetNameOrComponentName) {
          lewidgetGenereUnFragment = true // on génère un fragment 
          typeDuFragment = containerType._2 // on recupere le type de fragment 
        }
      })
      if (lewidgetGenereUnFragment) { // un génère un fragment pour ce container. 
        logBack.info(utilitaire.getContenuMessage("mes63"), widgetNameOrComponentName) // start generating fragment
        nombreDeFragmentsGeneres += 1
        val idDuContainer = if (controle.customId != "") { controle.customId } else { typeDuFragment + nombreDeFragmentsGeneres } // id du container pour générer le nom du fragment. Si l'ID est à blanc=> type + numero de fichier genere
        val codeSourceXmlDuFragment = recuperationCodeDuFragment(controle.tableau_des_fils, controle.sourceXmldDeLelement) // on recupere le code xml du container ainsi que le xml des enfants du container
        globalContext.tableDesContainersDesFragments += (usecase,fic,idDuContainer) -> controle  // on met en table le widget du container afin de récupérer ultérieurement ses attributs
        mapDesSourcesDesFragmentsAGenerer += (idDuContainer, typeDuFragment) -> codeSourceXmlDuFragment // on met dans un map le code source du fragement
      } else { // le widget n'est pas un container pour lequel on génère un fragment, on traite donc les fils du widget en cours.
        if (controle.tableau_des_fils.size > 0) {
          recuperationCodeXmlContainersDeFragment(controle.tableau_des_fils, mapDesSourcesDesFragmentsAGenerer,fic,usecase)
        }

      }
      numero_En_cours += 1
    })
  } // fin de  recuperationCodeXmlContainersDeFragment

 
  /**
   * @param fragment
   * @return String   xml code of fragement
   * récupération du code source del'ensemble des fils dans la branche
   */
  def recuperationCodeDuFragment(branche: ArrayBuffer[WidgetDeBase], content: String): String = {

    var contenuXMLDeLaBranche = content
    branche.foreach(controle => {
      contenuXMLDeLaBranche += controle.sourceXmldDeLelement
      if (controle.tableau_des_fils.size > 0) { contenuXMLDeLaBranche  = recuperationCodeDuFragment(controle.tableau_des_fils, contenuXMLDeLaBranche) }
    })

    contenuXMLDeLaBranche
  }
   
  /**
   * @param mapDesSourcesDesFragmentsAGenerer :Map[(String,String),String] Map contenant le nom du fragment son type, ansi que le contenu du fragment
   * la map mapDesSourcesDesFragmentsAGenerer contient le code source à générer pour chaque fragment d'un mockup principal
   * Le code source est mis en forme en ajoutant la balise xml controls qui est paramétrée cans le fichier properties
   */
  def generationDesfichiersFragmentDuMockupEnCours(mapDesSourcesDesFragmentsAGenerer: Map[(String, String), String], mainMockupName: String, useCaseName: String, w: String, h: String): Unit = {
    val x = mapDesSourcesDesFragmentsAGenerer
    mapDesSourcesDesFragmentsAGenerer.foreach(fragmentNameEtContenu => {

      if (fragmentNameEtContenu._2 != "") { // contenu du fragment vide ?
        var nomDuFichierFragmentAgenerer = ""
        val fragmentName = fragmentNameEtContenu._1._1 // part 1 of key
        val fragmentType = fragmentNameEtContenu._1._2 // part 2 of key
        // ajout d'un tag controls pour encapsuler l'ensemble des control dans le contenu xml du fragment
        val fragmentContent = CommonObjectForMockupProcess.generationProperties.headerXMLInGeneratedFragmentBegin.replace(cstWidth, w).replace(cstHeight, h) + fragmentNameEtContenu._2 + CommonObjectForMockupProcess.generationProperties.headerXMLInGeneratedFragmentEnd
        nomDuFichierFragmentAgenerer = repertoireContenuDesFragmentsGeneres + "/"
        if (useCaseName != "") {
          nomDuFichierFragmentAgenerer += useCaseName + CommonObjectForMockupProcess.engineProperties.usecaseSeparator + mainMockupName + CommonObjectForMockupProcess.engineProperties.fragmentSeparator + fragmentName
        } else {
          nomDuFichierFragmentAgenerer += mainMockupName + CommonObjectForMockupProcess.engineProperties.fragmentSeparator + fragmentName
        }
        if (fragmentType != "") {
          nomDuFichierFragmentAgenerer += CommonObjectForMockupProcess.engineProperties.fragmentTypeSeparator + fragmentType + cstBalsamiqFileSuffix
        } else {
          nomDuFichierFragmentAgenerer += cstBalsamiqFileSuffix
        }
        utilitaire.ecrire_fichier(nomDuFichierFragmentAgenerer, fragmentContent, false)
        logBack.info(utilitaire.getContenuMessage("mes65"), nomDuFichierFragmentAgenerer) // start generating fragment
      }
    })
  }

}