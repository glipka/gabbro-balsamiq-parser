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
import org.slf4j.LoggerFactory
import fr.gabbro.balsamiq.parser.modelimpl.MockupContext
import fr.gabbro.balsamiq.parser.modelimpl.Utilitaire
import fr.gabbro.balsamiq.parser.model.composantsetendus.DirectoryFile
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
// --------------------------------------------------------------------------------------------------------
// Détermination des dépendances.  
// on scanne les sous repertoires afin de trouver les fichiers commençant par le nom de l'ecran en cours
// on met ensuite à jour le tableau des liens de la classe EcranBalsamiq 
// --------------------------------------------------------------------------------------------------------
class DetectDependencies(mockupContext: MockupContext) {
  protected val logBack = LoggerFactory.getLogger(this.getClass());
  val repertoireDesSources = CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir
  val subRepertoire = CommonObjectForMockupProcess.subDirectoryName
  val mockup = CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement
  val utilitaire = new Utilitaire

  // -------------------------------
  // *** traitement du fichier ***  
  // -------------------------------  
  def process(): Unit = {
    logBack.info(utilitaire.getContenuMessage("mes54"),mockup)
    subRepertoire.foreach(subRepertoire => {
      val repertoireName = repertoireDesSources + "/" + subRepertoire
      traitementDesFichiersDuRepertoireBalsamiq(repertoireName, mockup, mockupContext)

    })
  }

  private def traitementDesFichiersDuRepertoireBalsamiq(directory1: String, nomEcran: String, ecranBalsamiq: MockupContext): Unit = {
    var compteur_fichiers_traites = 0
    val fichiersBalsamiqAtraiter = new File(directory1).listFiles
    if (fichiersBalsamiqAtraiter != null) {
      fichiersBalsamiqAtraiter.foreach(file => {
        if (file.isFile && file.getName().startsWith(nomEcran) && file.getName().endsWith(cstBalsamiqFileSuffix)) {
          val fileName = file.getName().split("\\.").head
          val filePath = file.getPath().split("\\.").init.mkString(".") + "/" + fileName + "." + CommonObjectForMockupProcess.generationProperties.generatedFrontFilesSuffix
          if (fileName != nomEcran) { // on ne traite pas le fichier en cours 
            val directoryFile = new DirectoryFile(fileName, filePath)
            // ce lien sera accessible depuis tous les écrans
            ecranBalsamiq.links.add(directoryFile)
          }
        }

      })
    }

  }
}