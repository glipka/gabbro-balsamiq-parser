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
// See the individual licence texts for more details.package fr.gencodefrombalsamiq.service.serviceimpl

import freemarker.cache.URLTemplateLoader
import freemarker.cache.TemplateLoader
import java.net.URL
import java.io._
import java.nio.file.Paths
import java.net.URI
import java.io.FileReader
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.charset.Charset
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import scala.collection.mutable.ArrayBuffer
import org.mozilla.javascript.BeanProperty
import scala.beans.BeanProperty
import java.nio.file.NoSuchFileException
import fr.gabbro.balsamiq.parser.service.TTraitementCommun

/**
 * <p>Traitement des preserves section</p>
 * <p>chaque section commence par preserve debut et est suivie par le nom du template.</p>
 * <p>chaque section se termine par preserve fin</p>
 * <p>on récupére le code dans chaque section que l'on met en table sous forme de tuple</p>
 * <p>on utilise deux hashMap pour retrouver le contenu des sections </p>
 * <p>Une première Map dont la clef est le N° de section et le nom du template </p>
 * <p>Une deuxième Map permet de retrouver le n° de section pour le template en cours. </p>
 * <p>Pour mémorise les n° de sections extraites par template (grace à la map mapDesPreserveSection)
 * @author georges Lipka
 *
 */
class TraitementPreserveSection extends TTraitementCommun {
  
  // la map des preservesections
  private var mapDesPreserveSection = Map[(Int, String), String]() // clef = n° de section et nom de template
  private var maptemplateByKeyNumber = Map[String, Int]() // clef = templateName valeur=n° de clef
  var fichierEnCoursDeTraitement = ""
  @BeanProperty var fichierPresent = false

  /**
   * <p>Récupération du code de la section, cette procédure est appelée depuis le template freemarker</p>
   * <p>maptemplateByKeyNumber sert à stocker le dernier numéro de préserve section utilisé pour ce template</p>
   * <p>mapDesPreserveSection contient le code des sections (la clef est le nom du template et le n° de section pour ce template</p>
   *
   * @param templateName
   * @param initialContent contenu initial qui sera retourné si le contenu de la section n'a pas été modifié.
   * @return content of section
   */
  def getSectionContent(templateName: String, initialContent: String): String = {
    var dernierNumeroClefPourLeTemplate = maptemplateByKeyNumber.getOrElse(templateName, -1) // dernièr n° de clef lu pour le template ?
    dernierNumeroClefPourLeTemplate += 1
    maptemplateByKeyNumber.put(templateName, dernierNumeroClefPourLeTemplate) // on met à jour le n° de la dernière section pour le template
    // Si le contenu a été modifié on retourne le contenu modifié, sinon on retourne le contenu initial
    val codeDeLaSection = mapDesPreserveSection.getOrElse((dernierNumeroClefPourLeTemplate, templateName), "")
    logBack.debug(utilitaire.getContenuMessage("mes53"), fichierEnCoursDeTraitement, templateName, codeDeLaSection)
    if (codeDeLaSection.trim != "" && codeDeLaSection != initialContent) {
      codeDeLaSection
    } else {
      initialContent
    }

  }

  /**
   * lecture du fichier pour extraire les preserve sections
   * @param filename : nom du fichier à traiter
   * @return traitementPreserveSection
   */
  def process(filename: String): TraitementPreserveSection = {
    var bufferATraiter = List[String]()
    fichierEnCoursDeTraitement = filename
    logBack.debug(utilitaire.getContenuMessage("mes50"), filename.toString)
    try {
      // on met en mémoire le fichier à traiter
      try {
        bufferATraiter = Files.readAllLines(Paths.get(filename.trim.replace("\\", "/")), Charset.defaultCharset()).toList
      } catch {

        case nsfex: NoSuchFileException => return this
        case ex: Exception =>
          logBack.error(utilitaire.getContenuMessage("mes51"), filename, ex.getMessage, "x");
          return this

        case ex: Exception =>
          val exception = ex;
          logBack.error(utilitaire.getContenuMessage("mes51"), filename, ex.getMessage, "x"); return this

      }
      fichierPresent = true
      traitementPreserve(bufferATraiter.mkString("\r\n")) // enrichissement de la table mapDesPreserveSection
      maptemplateByKeyNumber = maptemplateByKeyNumber.empty // table de travail
      this // on retourne l'instance de l'objet

    } catch {
      case ex: Exception => {
        val exception = ex
        logBack.error(utilitaire.getContenuMessage("mes38"), filename.toString)
        null
      }
    }

  }
  /**
   * <p>On récupère le contenu entre le header debut et le header fin</p>
   * <p>le nom du template qui a généré la preserve section est juste derrière le header.</p>
   * <p>que l'on met dans une hashMap.</p>
   * <p>Attention la syntaxe des preserve sections est différente selon le type de fichier (jsp ou java)</p>
   *
   * @param bufferATraiter
   */
  private def traitementPreserve(bufferATraiter: String): Unit = {
    var preserveSectionBegin = ""
    var preserveSectionEnd = ""
    // la syntaxe des preserve section est différente selon le type de fichier (jsp ou java)
    if (fichierEnCoursDeTraitement.endsWith(CommonObjectForMockupProcess.generationProperties.generatedFrontFilesSuffix)) {
      preserveSectionBegin = CommonObjectForMockupProcess.templatingProperties.preserveSectionFrontBegin
      preserveSectionEnd = CommonObjectForMockupProcess.templatingProperties.preserveSectionFrontEnd
    } else {
      preserveSectionBegin = CommonObjectForMockupProcess.templatingProperties.preserveSectionCodeBegin
      preserveSectionEnd = CommonObjectForMockupProcess.templatingProperties.preserveSectionCodeEnd
    }
    val positionPreserveDebut = bufferATraiter.indexOf(preserveSectionBegin)
    val positionPreserveFin = bufferATraiter.indexOf(preserveSectionEnd, 1)
    if (positionPreserveDebut >= 0) {
      if (positionPreserveFin > 0 && positionPreserveFin > positionPreserveDebut + preserveSectionBegin.size) {
        val content = bufferATraiter.substring(positionPreserveDebut + preserveSectionBegin.size, positionPreserveFin)
        val (templateName, positionFinTemplateName) = getTemplateName(content) // nom du template qui a généré la section
        if (positionFinTemplateName > 0) { // on ne met en talbe que si on trouve un nom de template
          val key = maptemplateByKeyNumber.getOrElse(templateName, -1)
          // la map mapKeyNumberByTemplate sert à stocker les numeros de contenu par template
          maptemplateByKeyNumber.put(templateName, key + 1)
          mapDesPreserveSection += ((key + 1, templateName) -> content.substring(positionFinTemplateName))
        }
        traitementPreserve(bufferATraiter.substring(positionPreserveFin + preserveSectionEnd.size))

      } else { return }
    } else { return }

  }
  /**
   * **** récupération nom du template dans le header ****
   * @param content
   * @return (templateName, position fin du template)
   */
  private def getTemplateName(content: String): (String, Int) = {
    var templateName = ""
    val positionTemplateNameDebut = content.indexOf(CommonObjectForMockupProcess.templatingProperties.delimiterTemplateNameBeginInPreserveSection)
    val positionTemplateNameFin = content.indexOf(CommonObjectForMockupProcess.templatingProperties.delimiterTemplateNameEndInPreserveSection, 1)
    if (positionTemplateNameDebut >= 0) {
      if (positionTemplateNameFin > 0 && positionTemplateNameFin > positionTemplateNameDebut + CommonObjectForMockupProcess.templatingProperties.delimiterTemplateNameEndInPreserveSection.size) {
        templateName = content.substring(positionTemplateNameDebut + CommonObjectForMockupProcess.templatingProperties.delimiterTemplateNameBeginInPreserveSection.size, positionTemplateNameFin).trim
        logBack.debug(utilitaire.getContenuMessage("mes52"), templateName, fichierEnCoursDeTraitement, "x")
        val position_fin = positionTemplateNameFin + CommonObjectForMockupProcess.templatingProperties.delimiterTemplateNameEndInPreserveSection.size
        return (templateName.trim, position_fin)

      } else {
        logBack.debug(utilitaire.getContenuMessage("mes52"), templateName, fichierEnCoursDeTraitement, "x")

        return ("", -1)
      }
    } else {
      logBack.debug(utilitaire.getContenuMessage("mes52"), templateName, fichierEnCoursDeTraitement, "x")

      return (templateName.trim, -1)
    }
  }

}
