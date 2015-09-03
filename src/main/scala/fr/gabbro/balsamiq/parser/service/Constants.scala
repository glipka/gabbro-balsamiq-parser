package fr.gabbro.balsamiq.parser.service

//import org.mozilla.javascript.BeanProperty
//import scala.beans.BeanProperty
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

class Constants {
  final val cstAsc = "asc"
  final val cstAssets = "assets"
  final val cstAttributName = "attributName"
  final val cstBalsamiqFileSuffix = ".bmml"
  final val cstBidirect = "bidirect"
  final val cstBind = "bind"
  final val cstBootstrapColOffset = "bootstrapColOffset"
  final val cstBootstrapColWidth = "bootstrapColWidth"
  final val cstBreadCrumbs = "com.balsamiq.mockups::BreadCrumbs"
  final val cstButtonBar = "com.balsamiq.mockups::ButtonBar"
  final val cstCalendar = "calendar"
  final val cstCanvas = "com.balsamiq.mockups::Canvas"
  final val cstCenter = "C"
  final val cstCheckBox = "com.balsamiq.mockups::CheckBox"
  final val cstCheckBoxGroup = "com.balsamiq.mockups::CheckBoxGroup"
  final val cstCheckboxShort = "checkbox"
  final val cstChildrenInCurrentColumn = "childrenInCurrentColumn"
  final val cstClassName = "className"
  final val cstCode = "code"
  final val cstColNumber = "colNumber"
  final val cstColorPicker = "com.balsamiq.mockups::ColorPicker"
  final val cstColorpicker = "colorpicker"
  final val cstColumnNumber = "columnNumber"
  final val cstColumns = "columns"
  final val cstComboBox = "com.balsamiq.mockups::ComboBox"
  final val cstCombobox = "combobox"
  final val cstCommonObject = "commonObject"
  final val cstCommonSection = "commonSection"
  final val cstComponentBalsamiq = "com.balsamiq.mockups::Component"
  final val cstConstants = "constants"
  final val cstContainer = "container"
  final val cstContainerIsForm = "containerIsForm"
  final val cstContainerName = "containerName"
  final val cstControl = "control"
  final val cstControlID = "controlID"
  final val cstControlName = "controlName"
  final val cstControlProperties = "controlProperties"
  final val cstControlTypeID = "controlTypeID"
  final val cstControls = "controls"
  final val cstCptString = "Cpt"
  final val cstCurrentTag = "currentTag"
  final val cstCustomData = "customData"
  final val cstCustomID = "customID"
  final val cstCustomsteps = "auchan-steps"
  final val cstDatagrid = "com.balsamiq.mockups::DataGrid"
  final val cstDateChooser = "com.balsamiq.mockups::DateChooser"
  final val cstDesc = "desc"
  final val cstDhtmlxgrid = "dhtmlxgrid"
  final val cstDirectoryFile = "directoryFile"
  final val cstDisabled = "disabled"
  final val cstDisabledSelected = "disabledSelected"
  final val cstEngineProperties = "engineProperties"
  final val cstEstimatedHeightInChar = "estimatedHeightInChar"
  final val cstEstimatedWidthInChar = "estimatedWidthInChar"
  final val cstFalseString = "false"
  final val cstFichierJavaScriptformate = "FichierJavaScriptformate.js"
  final val cstFichierJavaScriptNonformate = "FichierJavaScriptNonformate.js"
  final val cstFieldName = "fieldName"
  final val cstFieldType = "fieldType"
  final val cstFormularAction = "formularAction"
  final val cstFragment = "fragment"
  final val cstGenerateController = "generateController"
  final val cstGeneratedFileName = "generatedFileName"
  final val cstGeneratedFragment = "generatedFragment"
  final val cstGenerationProperties = "generationProperties"
  final val cstGlobalContext = "globalContext"
  final val cstGroupChildrenDescriptors = "groupChildrenDescriptors"
  final val cstGroupConstante = "__group__"
  final val cstH = "h"
  final val cstHeaderHttp = "http://"
  final val cstHeight = "%height%"
  final val cstHierarchiePere = "hierarchiePere"
  final val cstHorizontal = "horizontal"
  final val cstHref = "href"
  final val cstHrefs = "hrefs"
  final val cstHtml = "html"
  final val cstIcons = "icons"
  final val cstIcon = "com.balsamiq.mockups::Icon"
  final val cstIconShort = "icon"
  final val cstId = "id"
  final val cstImage = "com.balsamiq.mockups::Image"
  final val cstImg = "img"
  final val cstIndex = "index"
  final val cstInternalId = "internalId"
  final val cstIsAnArray = "isAnArray"
  final val cstIsAttribute = "isAttribute"
  final val cstIsAFragment = "isAFragment"
  final val cstIsForm = "isForm"
  final val cstIsInGroup = "isInGroup"
  final val cstItems = "items"
  final val cstItemsVar = "itemsVar"
  final val cstJavascript = "javascript"
  final val cstJavascriptFileName = "javascriptFileName"
  final val cstJavascriptUseCase = "javascriptUseCase"
  final val cstKey = "key"
  final val cstLabel = "com.balsamiq.mockups::Label"
  final val cstLeft = "L"
  final val cstLink = "com.balsamiq.mockups::Link"
  final val cstLinkShort = "link"
  final val cstList = "list"
  final val cstListHTML = "com.balsamiq.mockups::List"
  final val cstLocation = "location"
  final val cstLocked = "locked"
  final val cstMainMockupName = "mainMockupName"
  final val cstMapBinding = "mapBinding"
  final val cstMarkup = "markup"
  final val cstMeasuredH = "measuredH"
  final val cstMeasuredW = "measuredW"
  final val cstMenu = "com.balsamiq.mockups::Menu"
  final val cstMockupContext = "mockupContext"
  final val cstNew = "new"
  final val cstNumeric = "numeric"
  final val cstNumericStepper = "com.balsamiq.mockups::NumericStepper"
  final val cstOld = "old"
  final val cstOverrideString = "override"
  final val cstPackageName = "packageName"
  final val cstPercentageBottomBannerWithRespectToContainerHeight = "percentageBottomBannerWithRespectToContainerHeight"
  final val cstPercentageHeightWithRespectToContainerHeight = "percentageHeightWithRespectToContainerHeight"
  final val cstPercentageHeightWithRespectToTotalHeight = "percentageHeightWithRespectToTotalHeight"
  final val cstPercentageLeftBannerWithRespectToContainerWidth = "percentageLeftBannerWithRespectToContainerWidth"
  final val cstPercentageRightBannerWithRespectToContainerWidth = "percentageRightBannerWithRespectToContainerWidth"
  final val cstPercentageTopBannerWithRespectToContainerHeight = "percentageTopBannerWithRespectToContainerHeight"
  final val cstPercentageWidhtWithRespectToContainerWidth = "percentageWidhtWithRespectToContainerWidth"
  final val cstPercentageWidthWithRespectToTotalWidth = "percentageWidthWithRespectToTotalWidth"
  final val cstPositionIn12th = "positionIn12th"
  final val cstPositionInContainer = "positionInContainer"
  final val cstPreserveSection = "preservesection"
  final val cstProjectName = "projectName"
  final val cstRadio = "radio"
  final val cstRadioButton = "com.balsamiq.mockups::RadioButton"
  final val cstRadioButtonGroup = "com.balsamiq.mockups::RadioButtonGroup"
  final val cstRadiobuttonShort = "radiobutton"
  final val cstRight = "R"
  final val cstRoundButton = "com.balsamiq.mockups::RoundButton"
  final val cstRowNumber = "rowNumber"
  final val cstSelected = "selected"
  final val cstSrc = "src"
  final val cstState = "state"
  final val cstSuffixTemplateFreeMarkerFile = ".ftl"
  final val cstSuffixDesFichiersJavaScript = ".js"
  final val cstTabbar = "com.balsamiq.mockups::TabBar"
  final val cstTabs = "tabs"
  final val cstTabulation = "tabulation"
  final val cstTagBody = "body"
  final val cstTagTitle = "title"
  final val cstTemplateBuildTraductionKey = "buildTraductionKey"
  final val cstTemplateClass = "class"
  final val cstTemplateClefDeTraduction = "clefDeTraduction"
  final val cstTemplateCol = "col"
  final val cstTemplateField = "field"
  final val cstTemplateInstance = "instance"
  final val cstTemplateJavascript = "javascriptMockup"
  final val cstTemplateName = "templateName"
  final val cstTemplatePackage = "package"
  final val cstTemplateRow = "row"
  final val cstTemplateUndefined = "undefined"
  final val cstTemplatingProperties = "templatingProperties"
  final val cstText = "text"
  final val cstTextArea = "com.balsamiq.mockups::TextArea"
  final val cstTextInput = "com.balsamiq.mockups::TextInput"
  final val cstTextareaShort = "textarea"
  final val cstTraitementPreserveSection = "traitementPreserveSection"
  final val cstTraitementPreserveSectionAlias1 = "traitementPreserveSectionAlias1"
  final val cstTraitementPreserveSectionAlias2 = "traitementPreserveSectionAlias2"
  final val cstTraitementPreserveSectionAlias3 = "traitementPreserveSectionAlias3"
  final val cstTraitementPreserveSectionOther = "traitementPreserveSectionOther"
  final val cstTrueString = "true"
  final val cstUnknown = "unknown"
  final val cstUp = "up"
  final val cstUrls = "urls"
  final val cstUsecaseName = "usecaseName"
  final val cstUtf8 = "UTF8"
  final val cstUtf_8 = "UTF-8"
  final val cstUtility = "utility"
  final val cstValidate = "validate"
  final val cstVariableBinding = "variableBinding"
  final val cstVariableBindingTail = "variableBindingTail"
  final val cstVariablesValidate = "variablesValidate"
  final val cstVertical = "vertical"
  final val cstVerticalTabbar = "com.balsamiq.mockups::VerticalTabBar"
  final val cstW = "w"
  final val cstWidget = "widget"
  final val cstWidgetContainer = "widgetContainer"
  final val cstWidgetName = "widgetName"
  final val cstWidth = "%width%"
  final val cstX = "x"
  final val cstXAbsolute = "xAbsolute"
  final val cstXRelative = "xRelative"
  final val cstY = "y"
  final val cstYAbsolute = "yAbsolute"
  final val cstYRelative = "yRelative"
  final val cstZ = "z"
  final val cstZip = ".zip"
  final val cstZOrder = "zOrder"

}