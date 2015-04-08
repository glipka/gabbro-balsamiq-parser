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

import java.io.File
import java.io.FileWriter
import scala.collection.mutable.ArrayBuffer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.beans.BeanProperty
import fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase
import fr.gabbro.balsamiq.parser.modelimpl.FormulaireCode
import fr.gabbro.balsamiq.parser.modelimpl.GlobalContext
import fr.gabbro.balsamiq.parser.service.TTraitementBinding

/**
 *  cette classe est instanciée dans le module principal IbalsamiqFreeMarker
 *  la tables des champs tableauDesVariables stockera l'ensemble des champs référencés la méthode bind=
 *  et servira à la géneration des DTO
 *
 * traitement du binding entre zones
 * Le champ CustomControl ID doit commencer par bind=obj1.obj2.champ1:Int
 *     les types permis sont :
 *   datetime, date, Int, long,double, string
 *     les listes se terminent par * (pas encore implémenté)
 *     Exemple bind=personne/adresse:Int
 *
 *
 * le champ CustomControl ID peut être aussi du type : bind=map1("nom","adresse")
 * pour binder un champ à une map en localstorage par exemple.Ce type de binding
 * est utilisé conjointement avec les templates javascript js_xxxxx afin de coder l'alimentationn
 * de l'objet. (execmple mis en place avec le template select)
 *
 * La génération des DTO se fait par 2 templates Freemarker : class et field
 * On génère un fichier par écran
 * @author fra9972467
 *
 */
class TraitementBinding(moteurTemplatingFreeMarker: MoteurTemplatingFreeMarker, sessionBalsamiq: GlobalContext) extends TTraitementBinding {
  // cette hashMap va servir à stocker le code par classe  
  val mapCodeClasse = scala.collection.mutable.Map[Field, String]()
  /**
   *  // le binding peut être un MAP(key,value)
   * dans ce cas là, il n'y aura pas de mise en table
   * des variables, car la map est créée et alimentée depuis le serveur
   *
   * cette méthode est appelée depuis la classe widgetDeBase lors du traitement des attributs
   * @param bind : content  (bind=mapName(key1,value1)
   * @return StructureMap(mapName, key1, value1))
   */
  def traitementMap(bind: String): (Boolean, StructureMap) = {
    val input = bind.substring(5)
    val regExp1 = "(.)*\\((.)*,(.)*\\)".r
    val regExp10 = "\\((.)*,".r
    val regExp10b = "([^,])*".r // qd le bug sera corrigé : "([^\\(,])*"
    val regExp11 = ",(.)*\\)".r

    val regExp11b = "\\w+".r // qd le bug sera corrigé : "([^\\),])*"
    val regExp12 = "(.)*\\(".r
    val expressionComplete = regExp1.findFirstIn(input)
    if (expressionComplete.getOrElse("") == "") {
      return (false, null)
    } else {
      val key1 = regExp10.findFirstIn(expressionComplete.getOrElse(""))
      val key = regExp10b.findFirstIn(key1.getOrElse("")).getOrElse("").replace("(", "")
      val value1 = regExp11.findFirstIn(expressionComplete.getOrElse(""))
      val value = regExp11b.findFirstIn(value1.getOrElse("")).getOrElse("")
      val mapName = regExp12.findFirstIn(expressionComplete.getOrElse("")).getOrElse("").replace("(", "")
      return { (true, new StructureMap(mapName, key, value)) }
    }
  }
  // *** récupération nom de la variable ***

  /**
   * Cette méthode renvoie renvoie la variable bindée sans le type
   * @param input : content for example object1.object2.attribute:String
   * @param widgetEnCours // no use
   * @return object1.object2.attribute
   */
  def get_variable_binding(input: String, widgetEnCours: WidgetDeBase): (Boolean, String) = {
    val nomDesObjets = input.trim
    val bindingVariable = nomDesObjets.split(":").head
    (true, bindingVariable) //on splite pour enlever le type de variable
  }

  /**
   *  methode appelée dans la classe WidgetDeBase,
   * on va enrichir la table "tableauDesVariables" qui sera utilisée pour générer le code source
   * des classes utilisées dans le binding.
   * le traitement des objets "class1.class2.nomObjet" se fait dans la fonction traitement branche
   *
   * @param input contenu du champ object1.object2.attribute1:type1
   * @param containerPere
   * @param widgetEnCours
   */
  def mise_en_table_classes_binding(input: String, containerPere: WidgetDeBase, widgetEnCours: WidgetDeBase): Unit = {
    var nomDesObjets = input.trim
    // si le containerPere est un container qui contient une valeur de bind valide  
    // on concatène le nom de classe bindée dans le containerPere avec le champ bindé du composant en cours.
    if (CommonObjectForMockupProcess.generationProperties.concatenateContainerIdToWidgetId && containerPere != null && containerPere.bind != "") {
      nomDesObjets = containerPere.bind.trim + "." + nomDesObjets
    }
    if (nomDesObjets.contains(".")) {
      val tableauObjets = nomDesObjets.split("\\.")
      if (tableauObjets.size > 1) {
        // tableauObjets.init va contenir la hierarchie des objets
        tableauDesVariables = traitement_branche(tableauDesVariables, tableauObjets, widgetEnCours)
      } else { (logBack.error(utilitaire.getContenuMessage("mes8"), nomDesObjets)) }
    } else { // un suel objet =>  c'est une classe
      tableauDesVariables = traitement_branche(tableauDesVariables, Array(nomDesObjets), widgetEnCours)
    }

  } // fin traitment_custom_controlId 

  /**
   * <p>l'expression "a.b.c" est splitée par "."</p>
   * <p>si on ne trouve pas l'element "a" dans la branche en cours</p>
   * <p>  si la taille du tableau des variables > 1  </p>
   * <p>    mise en table du 1er élément qui est une classe.</p>
   * <p>    traitmement itératif de la queue de la table "b.c" pour renseigner le champ children de l'element de la table en cours</p>
   * <p>  sinon l'element est un champ, on récupère son type (split par ":")</p>
   * <p>      le type de chmap </p>
   * <p>sinon  on se repositionne sur l'element de la table en cours et traitement itératif de "b.c" pour renseigner le champ children de l'element en cours</p>
   * @param brancheEnCours : ArrayBuffer[Field]
   * @param champs : tableau des variables
   * @param widgetEnCours
   * @return
   */
  private def traitement_branche(brancheEnCours: ArrayBuffer[Field], champs: Array[String], widgetEnCours: WidgetDeBase): ArrayBuffer[Field] = {
    var typeDuChamp = ""
    var premierChamp = champs.head.trim // on prend le 1er champ
    var fieldName = ""
    val controlTypeID = if (widgetEnCours.isAComponent) { widgetEnCours.componentName } else { widgetEnCours.controlTypeID.split(":").last }

    // ------------------------------------------------------------
    // traitement du 1er champ qui peut aussi contenir un type 
    // ------------------------------------------------------------
    if (premierChamp.contains(":")) {
      val tableau1 = premierChamp.split(":")
      typeDuChamp = tableau1.last
      fieldName = tableau1.head
    } else {
      typeDuChamp = "????"
      fieldName = premierChamp
    }

    // le champ en cours n'existe pas dans la branche
    if (!brancheEnCours.exists(field => { (field.instanceName == fieldName) })) {
      // s'il y a plusieurs champs on crée le 1er champ dans la table et on traite les enfants
      if (champs.size > 1) {
        val newField = new Field(fieldName.capitalize, fieldName, typeDuChamp, new ArrayBuffer[Field](), controlTypeID, widgetEnCours)
        newField.children = traitement_branche(newField.children, champs.tail, widgetEnCours)
        brancheEnCours += newField
        brancheEnCours
      } // un seul champ on le cree
      else {
        val newField = new Field(fieldName, fieldName, typeDuChamp, new ArrayBuffer[Field](), controlTypeID, widgetEnCours)
        brancheEnCours += newField
        brancheEnCours
      }
    } else { // le champ existe dans la branche
      // on se repositionne sur l'objet déjà défini afin d'enrichir les fils
      // on vérifie qye l'objet déjà défini est du même type

      val brancheEnrichie = brancheEnCours.map(field => {
        if (field.instanceName == fieldName) {
          if (champs.size > 1) { field.children = traitement_branche(field.children, champs.tail, widgetEnCours) }
        }

        field
      }) // reste à calculer le type de champ
      brancheEnrichie

    }

  }

  /**
   * On balaie le 1er niveau des classes du tableau des Fields
   * Pour chaque classe :
   *        on appelle le moteur de templating pour générer le code d'instanciation de la classe
   *        on met en table (locale et gobale) les informations liées à la classe.
   *        Si la classe est un formulaire, on met à jour la table des formulaires
   *        puis on génère le contenu de la classe (generation_code_source_class)
   * @param classes : tableau des classes
   */
  private def generation_code_source_classes(classes: ArrayBuffer[Field]): Unit = {
    classes.foreach(classe => {
      // on vérifie si le widget doit être bindé à un tableau
      val (ret8, instanceCodeBegin, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateInstance, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.tabulation, ""), (CommonObjectForMockupProcess.constants.templateInstance, classe.instanceName), (CommonObjectForMockupProcess.constants.hierarchiePere, ""), (CommonObjectForMockupProcess.constants.className, classe.fieldNameOrClassName.capitalize), (CommonObjectForMockupProcess.constants.widgetName, classe.controlTypeID))
      val (ret9, instanceCodeEnd, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateInstance, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.tabulation, ""), (CommonObjectForMockupProcess.constants.templateInstance, classe.instanceName), (CommonObjectForMockupProcess.constants.hierarchiePere, ""), ("classnNme", classe.fieldNameOrClassName.capitalize), (CommonObjectForMockupProcess.constants.widgetName, classe.controlTypeID))
      val shortPath = if (classe.instanceName.endsWith(CommonObjectForMockupProcess.generationProperties.generatedFormAlias.capitalize)) {
        classe.instanceName.substring(0, classe.instanceName.size - CommonObjectForMockupProcess.generationProperties.generatedFormAlias.size).toUpperCase() + "_" + CommonObjectForMockupProcess.generationProperties.generatedFormAlias.toUpperCase()
      } else {
        classe.instanceName.toUpperCase
      }
      // mise à jour table des formulaires ou table des objets. 
      // on met à jour les tables dans ecranBalsammiq pour générer l'instance de controleur
      // et les tables dans sessions balsamiq pour génerer l'interface commune à l'ensemble des écrans.
      val fc = new FormulaireCode(classe.instanceName.capitalize, classe.widget, instanceCodeBegin + instanceCodeEnd, classe.widget.isFormulaireHTML, shortPath);
      if (classe.widget.isFormulaireHTML) {
        CommonObjectForMockupProcess.mockupContext.bindedForms.add(fc);
        sessionBalsamiq.bindedForms.add(fc);
      } else { // la class n'est pas bindée par un formulaire HTML
        sessionBalsamiq.firstLevelObject.add(fc);
        CommonObjectForMockupProcess.mockupContext.firstLevelObject.add(fc);
      }
      generation_code_source_classe(classe, 0, classe, "")

    })
  }

  /**
   * <p>Génération du code source d'une classe</p>
   * <p>génération de la préserve section pour sauvegarder le contenu des preserve section </p>
   * <p>Le code de la classe sera stocké dans une hashMap (une entree par classe sans tenir compte de la hiérarchie des classes).</p>
   * <p>Pour chaque attribut de la classe, génération du code d'instanciation de l'attribut et si l'attribut est une classe traitement </p>
   * <p>itératif de l'attribut en tant que classe </p>
   * <p> La finalité de cette méthode est de mettre à jour  la hashMap mapCodeClasse contenant le code de chaque classe </p>
   * @param classeEnCours
   * @param niveau
   * @param pere
   * @param hierarchiePere
   */
  private def generation_code_source_classe(classeEnCours: Field, niveau: Int, pere: Field, hierarchiePere: String): Unit = {
    var codeDeLaClasse = new StringBuilder
    val tabulation = "\t" * niveau
    val traitementPreserveSection = new TraitementPreserveSection().process(getClassLocation(classeEnCours.fieldNameOrClassName)) // utilisé pour récupérer le contenu des preserves section
    val (ret1, source1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateClass, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.className, classeEnCours.fieldNameOrClassName.capitalize), (CommonObjectForMockupProcess.constants.tabulation, tabulation), (CommonObjectForMockupProcess.constants.widgetName, classeEnCours.controlTypeID), (CommonObjectForMockupProcess.constants.traitementPreserveSection, traitementPreserveSection))
    codeDeLaClasse.append(source1)
    // traitement de chaque champ de la classe      
    classeEnCours.children.foreach(field => {
      // --------------------------------------------------------------------------------------------------
      // *** traitement du contenu de la classe *** 
      // le code généré va contenir l'instanciation de l'ensemble des objets contenus dans la classe 
      // le code des  classes instanciées sera générés lors du traitement de la classe instanciée
      // --------------------------------------------------------------------------------------------------
      if (field.children.size > 0) {
        val hierarchie = if (hierarchiePere == "") { classeEnCours.fieldNameOrClassName }
        else { hierarchiePere + "." + classeEnCours.fieldNameOrClassName }
        // on génère l'instanciation de la classe dans le code source. Le contenu de la classe sera généré lors de l'appel generation_code_source_classe(field)
        val (ret6, source6, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateField, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.fieldName, field.instanceName), (CommonObjectForMockupProcess.constants.fieldType, field.fieldNameOrClassName.capitalize), (CommonObjectForMockupProcess.constants.tabulation, tabulation), (CommonObjectForMockupProcess.constants.widgetName, field.controlTypeID), (CommonObjectForMockupProcess.constants.traitementPreserveSection, traitementPreserveSection))
        val (ret7, source7, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateField, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.fieldName, field.instanceName), (CommonObjectForMockupProcess.constants.fieldType, field.fieldNameOrClassName.capitalize), (CommonObjectForMockupProcess.constants.tabulation, tabulation), (CommonObjectForMockupProcess.constants.widgetName, field.controlTypeID), (CommonObjectForMockupProcess.constants.traitementPreserveSection, traitementPreserveSection))
        codeDeLaClasse.append(source6 + source7)
        if (hierarchiePere == "") { generation_code_source_classe(field, niveau + 1, field, classeEnCours.fieldNameOrClassName) }
        else { generation_code_source_classe(field, niveau + 1, classeEnCours, hierarchiePere + "." + classeEnCours.fieldNameOrClassName) }
      } else {
        // **** c'est un champ ****  
        val (ret3, source3, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateField, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.fieldName, field.fieldNameOrClassName),
          (CommonObjectForMockupProcess.constants.fieldType, field.typeDuChamp), (CommonObjectForMockupProcess.constants.tabulation, tabulation), (CommonObjectForMockupProcess.constants.widgetName, field.controlTypeID), (CommonObjectForMockupProcess.constants.traitementPreserveSection, traitementPreserveSection))

        val (ret4, source4, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateField, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.fieldName, field.fieldNameOrClassName),
          (CommonObjectForMockupProcess.constants.fieldType, field.typeDuChamp), (CommonObjectForMockupProcess.constants.tabulation, tabulation), (CommonObjectForMockupProcess.constants.widgetName, field.controlTypeID), (CommonObjectForMockupProcess.constants.traitementPreserveSection, traitementPreserveSection))
        codeDeLaClasse.append(source3 + source4)

      }
    })
    // generation fin de classe et mise en cache du code de la classe 
    val (ret2, source2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateClass, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.className, classeEnCours.fieldNameOrClassName), (CommonObjectForMockupProcess.constants.tabulation, tabulation), (CommonObjectForMockupProcess.constants.widgetName, classeEnCours.controlTypeID), (CommonObjectForMockupProcess.constants.traitementPreserveSection, traitementPreserveSection))
    codeDeLaClasse.append(source2)
    mapCodeClasse.put(classeEnCours, codeDeLaClasse.toString())

  }

  /**
   * <p>On sert des règles de nommage dans le nom de la classe pour en déduire le répertoire dans lequel sera stocké le fichier</p>
   * <p>3 cas possibles : DTO, Formulaire, Other</p>
   * @param className
   * @return location of file
   */
  def getClassLocation(className: String): String = {
    var location: String = ""
    if (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement != "") {
      if (className.endsWith(CommonObjectForMockupProcess.generationProperties.generatedDtoAlias.capitalize)) {
        location = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedDtoAlias + System.getProperty("file.separator") + className.capitalize + "." + CommonObjectForMockupProcess.generationProperties.languageSource
      } else if (className.endsWith(CommonObjectForMockupProcess.generationProperties.generatedFormAlias.capitalize)) {
        location = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedFormAlias + System.getProperty("file.separator") + className.capitalize + "." + CommonObjectForMockupProcess.generationProperties.languageSource
      } else {
        location = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedOtherAlias + System.getProperty("file.separator") + className.capitalize + "." + CommonObjectForMockupProcess.generationProperties.languageSource
      }
    } else { // *** pas de useCase renseigné ***
      if (className.endsWith(CommonObjectForMockupProcess.generationProperties.generatedDtoAlias.capitalize)) {
        location = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedDtoAlias + System.getProperty("file.separator") + className.capitalize + "." + CommonObjectForMockupProcess.generationProperties.languageSource
      } else if (className.endsWith(CommonObjectForMockupProcess.generationProperties.generatedFormAlias.capitalize)) {
        location = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedFormAlias + System.getProperty("file.separator") + className.capitalize + "." + CommonObjectForMockupProcess.generationProperties.languageSource
      } else {
        location = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedOtherAlias + System.getProperty("file.separator") + className.capitalize + "." + CommonObjectForMockupProcess.generationProperties.languageSource
      }
    }
    location
  }

  /**
   * <p>ecriture des sources générés : 1 fichier par classe</p>
   * <p>le fichier est écrit dans le répertoire le sous répertoire UC puis dans le sous répertoire form ou DTO en fonction du suffix du fichier.</p>
   * <p>on rajoute le nom du package en début de classe : Le nom du package est déduit du repertoire DTO</p>
   */
  def generationDuSourceDesClassesEtCreationDuFichier: Unit = {
    generation_code_source_classes(tableauDesVariables)
    mapCodeClasse.foreach(classe => {
      ecriture_fichier(classe._1.fieldNameOrClassName, classe._2)
    })

    /**
     * @param repositoryName : name of directory
     * @return name modified (separator replaced by /)
     */
    def replaceSystemFileSeparatoirByPoint(repositoryName: String): String = {
      repositoryName.replace("\\", "/").replace("/", ".")
    }

    /**
     * récupération du code source du nom du package
     * On traite les cas : Dto, formualaire, Other
     * @param className: nom de la classe
     * @return sources of package
     */
    def getPackageSources(className: String): String = {
      var packageSourceDebut = ""
      var packageSourceFin = ""
      // content of srcDtoFilesDir com/company/%project%/web
      val generatedFileDir = replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.srcDtoFilesDir)
      if (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement != "") {
        if (className.endsWith(CommonObjectForMockupProcess.generationProperties.generatedDtoAlias.capitalize)) {
          val (_, packageSource1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, generatedFileDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedDtoAlias)))
          val (_, packageSource2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, generatedFileDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedDtoAlias)))
          packageSourceDebut = packageSource1
          packageSourceFin = packageSource2
        } else if (className.endsWith(CommonObjectForMockupProcess.generationProperties.generatedFormAlias.capitalize)) {
          val (_, packageSource1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, generatedFileDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedFormAlias)))
          val (_, packageSource2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, generatedFileDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedFormAlias)))
          packageSourceDebut = packageSource1
          packageSourceFin = packageSource2
        } else {
          val (_, packageSource1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, generatedFileDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedOtherAlias)))
          val (_, packageSource2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, generatedFileDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedOtherAlias)))
          packageSourceDebut = packageSource1
          packageSourceFin = packageSource2
        }
      } else { // *** pas de useCase renseigné ***
        if (className.endsWith(CommonObjectForMockupProcess.generationProperties.generatedDtoAlias.capitalize)) {
          val (_, packageSource1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, generatedFileDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.generatedDtoAlias)))
          val (_, packageSource2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, generatedFileDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.generatedDtoAlias)))
          packageSourceDebut = packageSource1
          packageSourceFin = packageSource2
        } else if (className.endsWith(CommonObjectForMockupProcess.generationProperties.generatedFormAlias.capitalize)) {
          val (_, packageSource1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, generatedFileDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.generatedFormAlias)))
          val (_, packageSource2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, generatedFileDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.generatedFormAlias)))
          packageSourceDebut = packageSource1
          packageSourceFin = packageSource2
        } else {
          val (_, packageSource1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, generatedFileDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.generatedOtherAlias)))
          val (_, packageSource2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, generatedFileDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.generatedOtherAlias)))
          packageSourceDebut = packageSource1
          packageSourceFin = packageSource2
        }
      }
      packageSourceDebut + packageSourceFin
    }
    /**
     * On rajoute le nom du package en début du code de la classe, on détermine l'url du fichier, puis on écrit le code
     * @param className : name of java or scala class
     * @param sourcesDeLaClasse : java or scala source
     */
    def ecriture_fichier(className: String, sourcesDeLaClasse: String): Unit = {
      var fileWriter: FileWriter = null
      val classLocation = getClassLocation(className).replace("\\", "/").trim
      val packageSources = getPackageSources(className)
      // on rajoute le nom du package en début de la classe
      val traitementFormatageSourceJava = new TraitementFormatageSourceJava
      utilitaire.ecrire_fichier(classLocation, traitementFormatageSourceJava.indentSourceCodeJava(packageSources + sourcesDeLaClasse))
    }
  }

  /**
   *  Pour un formulaire on force la fin du champ bindé se termine par "Form"
   * Pour un DTO, on force la fin du champ bindé se termine par "DTO"
   * @param widget : WidgetDeBase
   * @param container : WidgetDeBase
   * @return : bind content
   */
  def getBindContent(widget: WidgetDeBase, container: WidgetDeBase): String = {
    var bind = ""
    if (widget.isFormulaireHTML) { // pour un formulaire le champ doit se terminer par Form
      //init bind name for Object generation
       bind = widget.bind
      if (CommonObjectForMockupProcess.generationProperties.generatedFormAlias != "" && !widget.bind.endsWith(CommonObjectForMockupProcess.generationProperties.generatedFormAlias.capitalize)) {
        bind += CommonObjectForMockupProcess.generationProperties.generatedFormAlias.capitalize
      }
    } else { // ce n'est pas un formulaire
      // les variables du formulaire sont suffixés par DTO sauf la dernière qui est un champ
      // le container pere est-il un formulaire ?
      if (container.isFormulaireHTML) {
        var tableauVariable1 = widget.bind.split("\\.").toList
        if (tableauVariable1.size > 1) {
          // on prend toutes les champs sauf le dernièr (qui est une variable)
          // et on rajoute le suffix DTO sur l'ensemble des champs.
          val tableauDesDtos = tableauVariable1.init.map(variable => {
            var variableModifiee = variable
            if (CommonObjectForMockupProcess.generationProperties.generatedDtoAlias != "" && !variable.endsWith(CommonObjectForMockupProcess.generationProperties.generatedDtoAlias.capitalize)) {
              variableModifiee = variable + CommonObjectForMockupProcess.generationProperties.generatedDtoAlias.capitalize
            }
            variableModifiee
          })
          val tableauVariable3 = tableauDesDtos ++ List(tableauVariable1.last)
          bind = tableauVariable3.mkString(".")
        }
      } else bind = widget.bind // le container pare n'est pas un formulaire. ON retourne le bunding sans modification
    }
    return bind

  }

  /**
   *  Pour un formulaire on force la fin du champ bindé se termine par "Form"
   *  Pour un DTO, on force la fin du champ bindé se termine par "DTO"
   * @param widget : WidgetDeBase
   * @param container : WidgetDeBase
   * @return (true of false, bind,variableBinding,variableBindingTail)
   */
  def process(widget: WidgetDeBase, container: WidgetDeBase): (Boolean,String,String,String) = {
    var bind=""
    var variableBindingTail=""
     if (widget.bind.trim.size > 0) {
      // vérification de la syntaxe des noms des formulaires et de noms des DTOs.
       bind = getBindContent(widget, container)
      // récupération nom de la variable bindée
      val (retCode, variableBinding) = get_variable_binding(widget.bind, widget)
//        widget.variableBinding = variableBinding
      if (variableBinding.contains(".")) { variableBindingTail = variableBinding.split("\\.").tail.mkString(".") }
      else { variableBindingTail = variableBinding }
      // traitement du binding afin de permettre la génération des classes
      mise_en_table_classes_binding(bind, container, widget) 
      return(true,bind,variableBinding,variableBindingTail)
    } else {return (false,"","","")}
  }

}  // fin de la classe TraitementBinding