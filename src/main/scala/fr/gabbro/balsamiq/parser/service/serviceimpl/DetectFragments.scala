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
import org.slf4j.LoggerFactory
import java.util.ArrayList
import fr.gabbro.balsamiq.parser.modelimpl.Utilitaire
import fr.gabbro.balsamiq.parser.modelimpl.Fragment
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
/**
 *  Determination des fragments de l'écran en cours
 * on scanne le repertoire en cours afin de trouver les fichiers commençant par le nom de l'ecran en cours
 * on met ensuite à jour le tableau des liens de la classe globalContext
 * @author fra9972467
 *
 */

class DetectFragments(utilitaire: Utilitaire) {
  protected val logBack = LoggerFactory.getLogger(this.getClass());
  val repertoireDesSources = CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir
  val subRepertoire = CommonObjectForMockupProcess.subDirectoryName
  val nomEcran = CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement
  val isAfragment = CommonObjectForMockupProcess.isAfragment
  /**
   * appel de la procédure raitementDesFragmentsDuRepertoireBalsamiq et retour liste des fragments.
 * @return ArrayList[Fragment]
 */
def processEtMiseEntable(): ArrayList[Fragment] = {
    val repertoireName = repertoireDesSources
    return traitementDesFragmentsDuRepertoireBalsamiq(repertoireName, nomEcran)

  }
  /**
   * traitement des fragments pour un mockup principal
   * Si le fichier en cours de traitement est un fragment et qu'il y a correspondance entre le usecase et le mockup principla
   * alors on met en table le fragement 
   * remarque : le nom des fragments est sous la forme: useCase-mockupPrincipal$fragmentName
   * 
 * @param directory1 : name of repository to scan
 * @param nomEcran : name of mainMockup
 * @return ArrayList[Fragment]
 */
private def traitementDesFragmentsDuRepertoireBalsamiq(directory1: String, nomEcran: String): ArrayList[Fragment] = {
    var compteur_fichiers_traites = 0
    val listeDesFragments = new ArrayList[Fragment]();
    val fichiersBalsamiqAtraiter = new File(directory1).listFiles
    if (fichiersBalsamiqAtraiter != null) {
      fichiersBalsamiqAtraiter.foreach(file => {
        if (file.getName().endsWith(cstBalsamiqFileSuffix)) { // on ne traite que les fichiers bmml
          val (ficname, rep, usecaseDuSegment, fileNameComplet, isAfragment, fragmentName, generateContoller, ecranContenantLeFragment, typeDeFragment) = utilitaire.getFileInformation(file)
          if (isAfragment && ficname.toLowerCase().startsWith(nomEcran.toLowerCase()) && (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement == usecaseDuSegment)) {
            listeDesFragments.add(IBalsamiqFreeMarker.globalContext.createFragment(file.getName.split("\\.").head))
          }
        }
      })
    }
    return listeDesFragments
  }
}