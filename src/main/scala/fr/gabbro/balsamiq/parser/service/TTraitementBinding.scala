package fr.gabbro.balsamiq.parser.service
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

import scala.collection.mutable.ArrayBuffer
import org.slf4j.LoggerFactory
import scala.beans.BeanProperty
import fr.gabbro.balsamiq.parser.modelimpl.Utilitaire
import fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase

trait TTraitementBinding {
  protected val logBack = LoggerFactory.getLogger(this.getClass());
  protected val utilitaire = new Utilitaire
  class StructureMap(@BeanProperty var mapName: String, @BeanProperty var key: String, @BeanProperty var value: String)

  /**
   *
   * Description de la class Field
   *    fieldNameOrClassName : nom du champ ou nom de la classe
   *    typeDuChamp : type au sens java ou scala (il n'y a pas de controle de type).
   *    children : tableau des fils
   *     controlTypeID : nom du widget ou du composant
   *     widget : widget  qui traite le bind
   * @author fra9972467
   *
   */
  class Field(val fieldNameOrClassName: String, val instanceName: String, val typeDuChamp: String, var children: ArrayBuffer[Field], var controlTypeID: String, var widget: WidgetDeBase,var isAnArray:String)
  protected var tableauDesVariables = ArrayBuffer[Field]()
  protected val point = "."

}