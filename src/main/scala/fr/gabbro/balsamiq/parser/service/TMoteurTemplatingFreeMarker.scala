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

 
import org.slf4j.LoggerFactory
import freemarker.template.Configuration
import freemarker.cache.TemplateLoader
import fr.gabbro.balsamiq.parser.modelimpl.Utilitaire
import fr.gabbro.balsamiq.parser.service.serviceimpl.ClassTemplateLoader
 

trait TMoteurTemplatingFreeMarker {
  protected val logBack = LoggerFactory.getLogger(this.getClass());
  protected val utilitaire = new Utilitaire
  protected val cfgFreeMarker = new Configuration();
  protected var tableDesTemplates: scala.collection.mutable.Map[String, Option[String]] = null
  protected var classTemplateLoader= new ClassTemplateLoader
}