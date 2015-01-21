package fr.gabbro.balsamiq.parser.service
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

import org.slf4j.LoggerFactory

trait TMoteurAnalyseJericho {
  protected val tableDesClefsValeursDeTraduction = scala.collection.mutable.LinkedHashMap[String, String]()
  protected val tableDesValeursClefsDeTraduction = scala.collection.mutable.LinkedHashMap[(String, String, String), String]()
  protected val tableDesPaysATraiter = Map[String, String]() // pays, clef de traduction
}