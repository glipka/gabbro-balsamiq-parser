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

import scala.beans.BeanProperty
import scala.collection.mutable.ArrayBuffer
import fr.gabbro.balsamiq.parser.service.EngineProperties
import fr.gabbro.balsamiq.parser.service.TemplatingProperties
import fr.gabbro.balsamiq.parser.modelimpl.MockupContext
import fr.gabbro.balsamiq.parser.service.GenerationProperties
import fr.gabbro.balsamiq.parser.service.Constants


/**
 * @author fra9972467
 *
 */
object CommonObjectForMockupProcess {
  @BeanProperty val engineProperties = new EngineProperties
  @BeanProperty val generationProperties = new GenerationProperties
  @BeanProperty val templatingProperties = new TemplatingProperties
  @BeanProperty val constants = new Constants
  var ecranContenantLeSegment = ""
  var nomDuFichierEnCoursDeTraitement = ""
  var nomDuRepertoirerEnCoursDeTraitement = ""
  var nomDuUseCaseEnCoursDeTraitement: String = ""
  var typeDuFragmentEnCoursDeTraitement = ""
  @BeanProperty var mockupContext: MockupContext = null; // contexte de l'écran en cours
  var generateController = false
  var isAfragment = false
  var subDirectoryName = List[String]()
  var listeNomdesFormulaires = new ArrayBuffer[String] // utilisé pour stocker le nom des formulaires 
 
}