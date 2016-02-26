package fr.gabbro.balsamiq.parser.service.serviceimpl

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.builder.DynamicReports._
import net.sf.dynamicreports.report.builder.DynamicReports.{ `type` => type1 }
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.jasperreports.engine.JRDataSource
import net.sf.dynamicreports.report.constant._
import java.awt.Color
import java.io.FileOutputStream
import fr.gabbro.balsamiq.parser.modelimpl.GlobalContext
import fr.gabbro.balsamiq.parser.modelimpl.Utilitaire
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._

//import net.sf.dynamicreports.report.builder.DynamicReports.{ `type` => type1 }

class DynamicReport(globalContext: GlobalContext, utilitaire: Utilitaire) {
  val rep1 = createReport()
  val directory = CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir + "/" + cstReporting
  utilitaire.createRepostoriesIfNecessary(directory)
  val fileName = directory + "/f1.pdf"
  val f1 = new FileOutputStream(fileName)
  val pdf1 = rep1.toPdf(f1)
  // création d'un rapport 
  def createReport(): JasperReportBuilder = {
    try {

      val boldStyle = stl.style().bold();
      val boldCenteredStyle = stl.style(boldStyle).setHorizontalAlignment(HorizontalAlignment.CENTER);
      val columnTitleStyle = stl.style(boldCenteredStyle).setBorder(stl.pen1Point()).setBackgroundColor(Color.LIGHT_GRAY).setFontSize(8)
      val centeredStyle = stl.style().setHorizontalAlignment(HorizontalAlignment.CENTER).setFontSize(7)
      //("bmml", "templateID", "componant","message","description","gravity")
      val bmml = col.column("bmml", "bmml", type1.stringType()).setStyle(centeredStyle).setWidth(20)
      val templateID = col.column("templateID", "templateID", type1.stringType()).setStyle(centeredStyle).setWidth(5)
      val componant = col.column("componant", "componant", type1.stringType()).setStyle(centeredStyle).setWidth(5)
      val message = col.column("message", "message", type1.stringType()).setStyle(centeredStyle).setWidth(40)
      val description = col.column("description", "description", type1.stringType()).setStyle(centeredStyle).setWidth(20)
      val gravity = col.column("gravity", "gravity", type1.stringType()).setStyle(centeredStyle).setWidth(10)

      val condition1 = stl.conditionalStyle(cnd.equal(gravity, cstError)).setBackgroundColor(Color.red);
      val condition2 = stl.conditionalStyle(cnd.equal(gravity, cstWarning)).setBackgroundColor(Color.orange);
      val plainStyle = stl.style().setFontName("FreeUniversal").setFontSize(6)

    

        
      val rep1 =
        report()
          .setColumnTitleStyle(columnTitleStyle)
          .highlightDetailEvenRows()
          .columns(bmml, message, gravity, templateID, componant, description)
          .title(cmp.text("Execution Report").setStyle(boldCenteredStyle))
          .pageFooter(cmp.pageXofY().setStyle(boldCenteredStyle))
          .detailRowHighlighters(condition1, condition2)
          // .groupBy(itemColumn)
          .setDataSource(createDataSource()) //set datasource
          .setPageFormat(1000, 400, PageOrientation.PORTRAIT)
          

      // .show();//create and show report

      return rep1
    } catch {

      case e: Exception =>
        e.printStackTrace();
        return null
    }

  } // fin de createReport 

  def createDataSource(): JRDataSource = {

    val dataSource = new DRDataSource("bmml", "templateID", "componant", "message", "description", "gravity");
    globalContext.gblTableTrace.foreach {
      case (bmml, templateID, componant, mes1, description, gravity) => dataSource.add(bmml, templateID, componant, mes1, description, gravity);
    }

    return dataSource;

  }

}



 

