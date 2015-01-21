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
 
// -------------------------------------------------------------------------- 
// cette classe est instanciée dans le module principal IbalsamiqFreeMarker
//  la tables des champs tableauDesVariables stockera l'ensemble des champs référencés la méthode bind=
//  et servira à la géneration des DTO 
//
// traitement du binding entre zones 
// Le champ CustomControl ID doit commencer par bind=obj1.obj2.champ1:Int 
//     les types permis sont :
//     datetime, date, Int, long,double, string 
//     les listes se terminent par * (pas encore implémenté)
//     Exemple bind=personne/adresse:Int
//
//
// le champ CustomControl ID peut être aussi du type : bind=map1("nom","adresse") 
// pour binder un champ à une map en localstorage par exemple.Ce type de binding 
// est utilisé conjointement avec les templates javascript js_xxxxx afin de coder l'alimentationn
// de l'objet. (execmple mis en place avec le template select)
// 
// La génération des DTO se fait par 2 templates Freemarker : class et field 
// On gnénère un fichier par écran
// --------------------------------------------------------------------------
class TraitementBinding(moteurTemplatingFreeMarker: MoteurTemplatingFreeMarker, sessionBalsamiq: GlobalContext) extends TTraitementBinding {
  // cette hashMap va servir à stocker le code par classe  
  val mapCodeClasse = scala.collection.mutable.Map[Field, String]()
  // ------------------------------------------------------------------------------------------------
  // le binding peut être un MAP(key,value) 
  // dans ce cas là, il n'y aura pas de mise en table 
  // des variables, car la map est créée et alimentée depuis le serveur
  //
  // cette méthode est appelée depuis la classe widgetDeBase lors du traitement des attributs
  // -------------------------------------------------------------------------------------------------
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
  def get_variable_binding(input: String, widgetEnCours: WidgetDeBase): (Boolean, String) = {
    val value = input.trim

    val nomDesObjets = value
    // On peut renvoyer le nom du dto, ainsi que la variable qui sera bindée au widget HTML 
    // le résultat esst sous la forme: dto_famille_adresse.rue
    // !!!!!!!  prévoir de passer un objet complexe afin de pouvoir parametrer le template freeMarker.
    val tableauDesVariables = nomDesObjets.split(":").head.split("\\.")
    val bindingVariable = nomDesObjets.split(":").head
    (true, bindingVariable) //on splite pour enlever le type de variable
  }
  // ----------------------------------------------------------------------------------------------
  // methode appelée dans la classe WidgetDeBase, 
  // on va enrichir la table "tableauDesVariables" qui sera utilisée pour générer le code source
  // des classes utilisées dans le binding.
  // le traitement des objets "class1.class2.nomObjet" se fait dans la fonction traitement branche 
  // ----------------------------------------------------------------------------------------------

  def mise_en_table_classes_binding(input: String, containerPere: WidgetDeBase, widgetEnCours: WidgetDeBase): Unit = {
    val value = input.trim
    var nomDesObjets = value
    // si le containerPere est un container qui contient une valeur de bind valide  
    // on concatène a
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
  // --------------------------------------------------------------------------- 
  //  
  // l'expression "a.b.c" est splitée par "."
  // si on ne trouve pas l'element "a" dans la branche en cours
  //   si la taille du tableau des variables > 1  
  //     mise en table du 1er élément qui est une classe.
  //     traitmement itératif de la queue de la table "b.c" pour renseigner le champ children de l'element de la table en cours
  //   sinon l'element est un champ, on récupère son type (split par ":")
  //       le type de chmap 
  // sinon  on se repositionne sur l'element de la table en cours et traitement itératif de "b.c" pour renseigner le champ children de l'element en cours
  // ---------------------------------------------------------------------------
  private def traitement_branche(brancheEnCours: ArrayBuffer[Field], champs: Array[String], widgetEnCours: WidgetDeBase): ArrayBuffer[Field] = {
    //  champ non trouve dans la branche en cours 
    var typeDuChamp = ""
    var premierChamp = champs.head.trim // on prend le 1er champ
    var fieldName = ""
    //  var bindedToArray = if (List(widgetEnCours.controlTypeID).intersect(CommonObjectForMockupProcess .widgetsBindedToAnArray).size > 0 || List(widgetEnCours.componentName).intersect(CommonObjectForMockupProcess .widgetsBindedToAnArray).size > 0) { true } else { false }
    val controlTypeID = if (widgetEnCours.isAComponent) { widgetEnCours.componentName } else { widgetEnCours.controlTypeID.split(":").last }
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
      // s'il y a plusieurs champs on crée le 1er champ dans la table et on relit les enfants
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
  // ----------------------------------------------------------------------------
  // on balaie l'ensemble des classes du 1er niveau du tableau des Fields 
  // ----------------------------------------------------------------------------
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
  // -----------------------------------------------------------------------
  // Génération du code source d'une classe
  // ========================================
  // -----------------------------------------------------------------------

  private def generation_code_source_classe(classeEnCours: Field, niveau: Int, pere: Field, hierarchiePere: String): Unit = {
    var codeDeLaClasse = new StringBuilder
    val tabulation = "\t" * niveau
    val traitementPreserveSection = new TraitementPreserveSection().process(getClassLocation(classeEnCours.fieldNameOrClassName)) // utilisé pour récupérer le contenu des preserves section
    val (ret1, source1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateClass, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.className, classeEnCours.fieldNameOrClassName.capitalize), (CommonObjectForMockupProcess.constants.tabulation, tabulation), (CommonObjectForMockupProcess.constants.widgetName, classeEnCours.controlTypeID),(CommonObjectForMockupProcess.constants.traitementPreserveSection,traitementPreserveSection))
    codeDeLaClasse.append(source1)
    // traitement de chaque champ de la classe      
    classeEnCours.children.foreach(field => {
      if (field.children.size > 0) { // c'est une classe
        val hierarchie = if (hierarchiePere == "") { classeEnCours.fieldNameOrClassName }
        else { hierarchiePere + "." + classeEnCours.fieldNameOrClassName }

        // on génère la définition de la classe dans le code source
        val (ret6, source6, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateField, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.fieldName, field.instanceName), (CommonObjectForMockupProcess.constants.fieldType, field.fieldNameOrClassName.capitalize), (CommonObjectForMockupProcess.constants.tabulation, tabulation), (CommonObjectForMockupProcess.constants.widgetName, field.controlTypeID),(CommonObjectForMockupProcess.constants.traitementPreserveSection,traitementPreserveSection))
        val (ret7, source7, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateField, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.fieldName, field.instanceName), (CommonObjectForMockupProcess.constants.fieldType, field.fieldNameOrClassName.capitalize), (CommonObjectForMockupProcess.constants.tabulation, tabulation), (CommonObjectForMockupProcess.constants.widgetName, field.controlTypeID),(CommonObjectForMockupProcess.constants.traitementPreserveSection,traitementPreserveSection))
        codeDeLaClasse.append(source6 + source7)
          if (hierarchiePere == "") { generation_code_source_classe(field, niveau + 1, field, classeEnCours.fieldNameOrClassName) }
        else { generation_code_source_classe(field, niveau + 1, classeEnCours, hierarchiePere + "." + classeEnCours.fieldNameOrClassName) }
      } else { // c'est un champ 
        val (ret3, source3, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateField, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.fieldName, field.fieldNameOrClassName),
          (CommonObjectForMockupProcess.constants.fieldType, field.typeDuChamp), (CommonObjectForMockupProcess.constants.tabulation, tabulation), (CommonObjectForMockupProcess.constants.widgetName, field.controlTypeID),(CommonObjectForMockupProcess.constants.traitementPreserveSection,traitementPreserveSection))

        val (ret4, source4, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateField, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.fieldName, field.fieldNameOrClassName),
          (CommonObjectForMockupProcess.constants.fieldType, field.typeDuChamp), (CommonObjectForMockupProcess.constants.tabulation, tabulation), (CommonObjectForMockupProcess.constants.widgetName, field.controlTypeID),(CommonObjectForMockupProcess.constants.traitementPreserveSection,traitementPreserveSection))
        codeDeLaClasse.append(source3 + source4)

      }
    })
    // generation fin de classe. 
    val (ret2, source2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateClass, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.className, classeEnCours.fieldNameOrClassName), (CommonObjectForMockupProcess.constants.tabulation, tabulation), (CommonObjectForMockupProcess.constants.widgetName, classeEnCours.controlTypeID),(CommonObjectForMockupProcess.constants.traitementPreserveSection,traitementPreserveSection))
    codeDeLaClasse.append(source2)
    mapCodeClasse.put(classeEnCours, codeDeLaClasse.toString())

  }
  // --------------------------------------------------------------------------
  // récupération de l'emplacement de la classe
  // ------------------------------------------------------------------------
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
  // ------------------------------------------------------------------------------------------------------------------------------------------
  // ecriture des sources générés : 1 fichier par classe 
  // le fichier est écrit dans le répertoire le sous répertoire UC puis dans le sous répertoire form ou DTO en fonction du suffix du fichier.
  //  on rajoute le nom du package en début de classe : Le nom du package est déduit du repertoire DTO
  // ----------------------------------------------------------------------------------------------------------------------------------------- 
  def generationDuSourceDesClassesEtCreationDuFichier: Unit = {
    generation_code_source_classes(tableauDesVariables)
    mapCodeClasse.foreach(classe => {
      ecriture_fichier(classe._1.fieldNameOrClassName, classe._2)
    })
    def replaceSystemFileSeparatoirByPoint(repositoryName: String): String = {
      repositoryName.replace("\\", "/").replace("/", ".")
    }
    // ------------------------------------------------------------------------

    // récupération du code source du nom du package
    def getPackageSources(className: String): String = {
      var packageSourceDebut = ""
      var packageSourceFin = ""
      val dtoDir = replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.srcDtoFilesDir)
      if (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement != "") {
        if (className.endsWith(CommonObjectForMockupProcess.generationProperties.generatedDtoAlias.capitalize)) {
          val (_, packageSource1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, dtoDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedDtoAlias)))
          val (_, packageSource2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, dtoDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedDtoAlias)))
          packageSourceDebut = packageSource1
          packageSourceFin = packageSource2
        } else if (className.endsWith(CommonObjectForMockupProcess.generationProperties.generatedFormAlias.capitalize)) {
          val (_, packageSource1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, dtoDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedFormAlias)))
          val (_, packageSource2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, dtoDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedFormAlias)))
          packageSourceDebut = packageSource1
          packageSourceFin = packageSource2
        } else {
          val (_, packageSource1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, dtoDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedOtherAlias)))
          val (_, packageSource2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, dtoDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + "." + CommonObjectForMockupProcess.generationProperties.generatedOtherAlias)))
          packageSourceDebut = packageSource1
          packageSourceFin = packageSource2
        }
      } else { // *** pas de useCase renseigné ***
        if (className.endsWith(CommonObjectForMockupProcess.generationProperties.generatedDtoAlias.capitalize)) {
          val (_, packageSource1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, dtoDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.generatedDtoAlias)))
          val (_, packageSource2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, dtoDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.generatedDtoAlias)))
          packageSourceDebut = packageSource1
          packageSourceFin = packageSource2
        } else if (className.endsWith(CommonObjectForMockupProcess.generationProperties.generatedFormAlias.capitalize)) {
          val (_, packageSource1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, dtoDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.generatedFormAlias)))
          val (_, packageSource2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, dtoDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.generatedFormAlias)))
          packageSourceDebut = packageSource1
          packageSourceFin = packageSource2
        } else {
          val (_, packageSource1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.packageName, dtoDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.generatedOtherAlias)))
          val (_, packageSource2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templatePackage, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.packageName, dtoDir + "." + replaceSystemFileSeparatoirByPoint(CommonObjectForMockupProcess.generationProperties.generatedOtherAlias)))
          packageSourceDebut = packageSource1
          packageSourceFin = packageSource2
        }
      }
      packageSourceDebut + packageSourceFin
    }
    // *** ecriture du fichier ***
    def ecriture_fichier(className: String, sourcesDeLaClasse: String): Unit = {
      var fileWriter: FileWriter = null
      val classLocation = getClassLocation(className).replace("\\","/").trim
      val packageSources = getPackageSources(className)
      // on rajoute le nom du package en début de classe
      val traitementFormatageSourceJava = new TraitementFormatageSourceJava
      utilitaire.ecrire_fichier(classLocation, traitementFormatageSourceJava.indentSourceCodeJava(packageSources + sourcesDeLaClasse))
    }
  }

}  // fin de la classe TraitementBinding