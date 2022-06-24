/*
 *  CongruenceKontakt.scala
 *  (carpa7)
 *
 *  Copyright (c) 2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.carpa7

import circledetection.Hough_Transform
import com.jhlabs.composite.DifferenceComposite
import de.sciss.file._
import ij.ImagePlus
import ij.io.FileInfo
import ij.process.ImageProcessor
import ij_transforms.Transform_Perspective
import mpicbg.ij.InverseTransformMapping

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.plugins.jpeg.JPEGImageWriteParam
import javax.imageio.stream.FileImageOutputStream
import javax.imageio.{IIOImage, ImageIO, ImageTypeSpecifier, ImageWriteParam}
import scala.annotation.tailrec
import scala.math.abs

object CongruenceKontakt {
  def main(args: Array[String]): Unit = {
    val baseDir = userHome / "Documents" / "projects" / "Kontakt"
    val dirIn   = baseDir / "materials" / "photos"
    val fileIn  = dirIn / "snap-220223_180118.jpg"
    val fileCmp = dirIn / "snap-220223_120118-crop.jpg"
    implicit val config: Config = Config()
    val imgIn   = readCropImage(fileIn)
    val imgCmp  = ImageIO.read(fileCmp)
    // println(s"imgCmp.type = ${imgCmp.getType}")

    val wD      = imgCmp.getWidth
    val hD      = imgCmp.getHeight
    val imgD    = new BufferedImage(wD, hD, BufferedImage.TYPE_INT_ARGB)
    val gD      = imgD.createGraphics()
    val cmpNorm = gD.getComposite
    val cmpDiff = new DifferenceComposite(1f)
    val arrD    = new Array[Int](wD * hD)

    val dpSeqBase = for (dx <- Vector(-8, 0, +8); dy <- Vector(-8, 0, +8)) yield Point2D(dx, dy)
    @tailrec
    def buildDc(rem: Int, res: Vector[Vector[Point2D]]): Vector[Vector[Point2D]] =
      if (rem == 0) res
      else {
        val comb = dpSeqBase.flatMap { dp =>
          res.map { tail => dp +: tail }
        }
        buildDc(rem = rem - 1, res = comb)
      }
    val dcSeq = buildDc(4, Vector(Vector.empty))
    val numDC = dcSeq.size
    // println(s"dcSeq.size ${dcSeq.size}")
    // assert (dcSeq.forall(_.size == 4))

    val t1 = System.currentTimeMillis()

    def mkDiff(imgTop: BufferedImage): Unit = {
      gD.setComposite(cmpNorm)
      gD.drawImage(imgCmp, 0, 0, null)
      gD.setComposite(cmpDiff)
      gD.drawImage(ensureARGB(imgTop), 0, 0, null)
    }

    def calcDiff(): Double = {
      imgD.getRGB(0, 0, wD, hD, arrD, 0, wD)
      var i = 0
      var sum = 0.0
      while (i < arrD.length) {
        val argb = arrD(i)
        val gray = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 765.0
        sum += gray
        i += 1
      }
      sum
    }

    val (circleCenters0, imgTrns0) = runOne(fileIn, imgIn, interp = ImageProcessor.NEAREST_NEIGHBOR)
    //    val (df0, imgDiff0) = diff(imgCmp, imgTrns0)
    mkDiff(imgTrns0)
    val df0 = calcDiff()
    // var bestImg     = copyImage(imgD, BufferedImage.TYPE_INT_ARGB) // imgDiff0 // imgTrns0
    var bestCenters = circleCenters0
    var bestDP      = Vector.fill(4)(Point2D(0,0))
    var bestDF      = df0
    println(f"Init df is $bestDF%1.1f, centers $bestCenters")

    println("_" * 100)
    var lastProg = 0

    dcSeq.zipWithIndex.foreach { case (dpSeq, dpi) =>
      val circleCentersU = circleCenters0.zip(dpSeq).map { case (p0, dp) => p0 + dp }
      val (_, imgTrnsU) = runOne(fileIn, imgIn, circleCentersU, interp = ImageProcessor.NEAREST_NEIGHBOR)

      mkDiff(imgTrnsU)

//      val (dfU, imgDiffU) = diff(imgCmp, imgTrnsU)
      val dfU = calcDiff()
      if (dfU < bestDF) {
        bestDF      = dfU
        // bestImg     = copyImage(imgD, BufferedImage.TYPE_INT_ARGB) // imgDiffU
        bestCenters = circleCentersU
        bestDP      = dpSeq
        // println(f"Improved df is $bestDF%1.1f with $dpSeq")
      }

      val prog = ((dpi + 1) * 100) / numDC
      while (lastProg < prog) {
        print('#')
        lastProg  += 1
      }
    }

    val t2 = System.currentTimeMillis()
    println()
    println(f"Best df is $bestDF%1.1f dp $bestDP, centers $bestCenters")
    println(f"Diff took ${(t2 - t1)/1000.0}%1.1f s.")

    val (_, imgTrnsBest) = runOne(fileIn, imgIn, bestCenters, ImageProcessor.BICUBIC)
    mkDiff(imgTrnsBest)

    val imgOut   = imgTrnsBest
    val imgOutD  = imgD
    val fileOut  = baseDir / "materials" / "_killme.jpg"
    val fileOutD = baseDir / "materials" / "_killmeD.jpg"
    writeImage(imgOut , fileOut )
    writeImage(imgOutD, fileOutD)
  }

  case class Config(
                     verbose        : Boolean = false,
                     normExtent     : Int     =  850,
                     cropExtent     : Int     =  720,
                     houghScale     : Double  =    0.5,
                     houghMinRadius : Int     =   30,
                     houghMaxRadius : Int     =   50,
                     preCropLeft    : Int     =  500,
                     preCropRight   : Int     =  500, // 1000,
                     preCropTop     : Int     =    0,
                     preCropBottom  : Int     =    0,
                     quality        : Int     =   90,
                   ) {

    require (normExtent     >= 2)
    require (cropExtent     >= 2)
    require (houghScale     <= 1.0)
    require (houghMinRadius >= 0)
    require (houghMaxRadius >= houghMinRadius)
    require (preCropLeft    >= 0)
    require (preCropRight   >= 0)
    require (preCropTop     >= 0)
    require (preCropBottom  >= 0)

    def hasPreCrop: Boolean = preCropLeft > 0 || preCropRight > 0 || preCropTop > 0 || preCropBottom > 0
  }

  case class IntPoint2D(x: Int, y: Int) {
    def distanceSq(that: IntPoint2D): Long = {
      val dx = abs(this.x - that.x)
      val dy = abs(this.y - that.y)
      dx * dx + dy * dy
    }
  }

  case class Point2D(x: Double, y: Double) {
    def distanceSq(that: Point2D): Double = {
      val dx = abs(this.x - that.x)
      val dy = abs(this.y - that.y)
      dx * dx + dy * dy
    }

    def + (that: Point2D): Point2D = copy(x = x + that.x, y = y + that.y)
  }

  def readCropImage(fileIn: File)(implicit config: Config): BufferedImage = {
    import config._
    val imgIn0  = ImageIO.read(fileIn)
    if (verbose) {
      println(s"Raw input size ${imgIn0.getWidth}, ${imgIn0.getHeight}")
    }
    val imgInC  = if (!hasPreCrop) imgIn0 else {
      val _wC   = imgIn0.getWidth  - (preCropLeft + preCropRight  )
      val _hC   = imgIn0.getHeight - (preCropTop  + preCropBottom )
      val res   = new BufferedImage(_wC, _hC, BufferedImage.TYPE_INT_RGB)
      val g     = res.createGraphics()
      g.drawImage(imgIn0, -preCropLeft, -preCropTop, null)
      g.dispose()
      res
    }

    if (verbose) {
      println(s"Cropped input size ${imgInC.getWidth}, ${imgInC.getHeight}")
    }

    imgInC
  }

  def ensureType(in: BufferedImage, tpe: Int): BufferedImage =
    if (in.getType == tpe) in else copyImage(in, tpe)

  def copyImage(in: BufferedImage, tpe: Int): BufferedImage = {
    val b = new BufferedImage(in.getWidth, in.getHeight, tpe)
    val g = b.createGraphics()
    g.drawImage(in, 0, 0, null)
    g.dispose()
    b
  }

  def ensureARGB(in: BufferedImage): BufferedImage = ensureType(in, BufferedImage.TYPE_INT_ARGB)
  def ensureRGB (in: BufferedImage): BufferedImage = ensureType(in, BufferedImage.TYPE_INT_RGB)

  def runOne(fileIn: File, imgInC: BufferedImage, circleCenters0: Seq[Point2D] = Nil, interp: Int)
            (implicit config: Config): (Seq[Point2D], BufferedImage) = {
    import config._
    val wC   = imgInC.getWidth
    val hC   = imgInC.getHeight

    def toImageJ(in: BufferedImage): ImagePlus = {
      val res = new ImagePlus(fileIn.name, in)
      val fi  = new FileInfo
      fi.fileFormat = FileInfo.IMAGEIO
      fi.fileName   = fileIn.name
      fileIn.parentOption.foreach { p => fi.directory = p.path + File.separator }
      res.setFileInfo(fi)
      res
    }

    val needsHough = circleCenters0.isEmpty

    val circleCenters: Seq[Point2D] = if (!needsHough) circleCenters0 else {
      val imgInS  = if (houghScale == 1.0) imgInC else {
        val wS    = (imgInC.getWidth  * houghScale).toInt
        val hS    = (imgInC.getHeight * houghScale).toInt
        val res   = new BufferedImage(wS, hS, BufferedImage.TYPE_INT_RGB)
        val g     = res.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING    , RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(imgInC, 0, 0, wS, hS, null)
        g.dispose()
        res
      }
      if (verbose) {
        println(s"Hough-scaled input size ${imgInS.getWidth}, ${imgInS.getHeight}")
      }

      val imgInSP: ImagePlus = toImageJ(imgInS)

      val wS      = imgInSP.getWidth
      val hS      = imgInSP.getHeight
      if (verbose) {
        println(s"Hough input size $wS, $hS")
      }
      val procInS = imgInSP.getProcessor
      val ht      = new Hough_Transform()
      ht.setParameters(houghMinRadius, houghMaxRadius, 4)
      val htRes = ht.runHeadless(procInS)
      val _circleCenters: Seq[Point2D] = htRes.iterator.map { case Array(x, y, _) =>
        Point2D(x / houghScale, y / houghScale)
      } .toList

      /*

        expected output:

        - first image -

        x 1682, y 582, radius 46
        x 157, y 615, radius 46
        x 108, y 198, radius 47
        x 1693, y 192, radius 46

        - second image -

        x 202, y 123, radius 49
        x 1069, y 956, radius 46
        x 1080, y 115, radius 48
        x 235, y 976, radius 45

       */

      if (verbose) {
        println("Circle centers:")
        for (Array(x, y, r) <- htRes) {
          println(s"x $x, y $y, radius $r")
        }
      }

      _circleCenters
    }

    val numCircles  = circleCenters.size
    require (numCircles == 4, numCircles)
    val topLeftIn   = circleCenters.minBy(_.distanceSq(Point2D(0d, 0d)))
    val topRightIn  = circleCenters.minBy(_.distanceSq(Point2D(wC, 0d)))
    val botLeftIn   = circleCenters.minBy(_.distanceSq(Point2D(0d, hC)))
    val botRightIn  = circleCenters.minBy(_.distanceSq(Point2D(wC, hC)))
    val ptIn        = List(topLeftIn, topRightIn, botLeftIn, botRightIn)
    require (ptIn.distinct.size == 4, ptIn)

    if (verbose && needsHough) {
      println(s"top    left  in : $topLeftIn")
      println(s"top    right in : $topRightIn")
      println(s"bottom left  in : $botLeftIn")
      println(s"bottom right in : $botRightIn")
    }

    val inWidthT  = topRightIn.x - topLeftIn .x
    val inWidthB  = botRightIn.x - botLeftIn .x
    val inHeightL = botLeftIn .y - topLeftIn .y
    val inHeightR = botRightIn.y - topRightIn.y
    val inWidthM  = (inWidthT  + inWidthB ) * 0.5
    val inHeightM = (inHeightL + inHeightR) * 0.5

    if (verbose && needsHough) {
      println(f"input width  (mean): $inWidthM%1.1f")   // e.g. 856
      println(f"input height (mean): $inHeightM%1.1f")  // e.g. 847
    }

    //      val normSideLength  = 850
    //      val normExtent      = normSideLength/2
    //      val normSideLength  = normExtent << 1

    val cx    = (topLeftIn.x + topRightIn.x + botLeftIn.x + botRightIn.x) / 4
    val cy    = (topLeftIn.y + topRightIn.y + botLeftIn.y + botRightIn.y) / 4
    val dxTL  = (cx - normExtent) - topLeftIn .x
    val dyTL  = (cy - normExtent) - topLeftIn .y
    val dxTR  = (cx + normExtent) - topRightIn.x
    val dyTR  = (cy - normExtent) - topRightIn.y
    val dxBL  = (cx - normExtent) - botLeftIn. x
    val dyBL  = (cy + normExtent) - botLeftIn .y
    val dxBR  = (cx + normExtent) - botRightIn.x
    val dyBR  = (cy + normExtent) - botRightIn.y

    val topLeftOut = Point2D(
      topLeftIn.x + dxTL,
      topLeftIn.y + dyTL,
    )
    val topRightOut = Point2D(
      topRightIn.x + dxTR,
      topRightIn.y + dyTR,
    )
    val botLeftOut = Point2D(
      botLeftIn.x + dxBL,
      botLeftIn.y + dyBL,
    )
    val botRightOut = Point2D(
      botRightIn.x + dxBR,
      botRightIn.y + dyBR,
    )

    if (verbose && needsHough) {
      println(s"top    left  out: $topLeftOut")
      println(s"top    right out: $topRightOut")
      println(s"bottom left  out: $botLeftOut")
      println(s"bottom right out: $botRightOut")
    }

    def round(xs: Array[Double]): Array[Int] =
      xs.map(x => (x + 0.5).toInt)

    val tp = new Transform_Perspective
    tp.setPointMatches(
      round(Array(topLeftIn .x, topRightIn .x, botRightIn .x, botLeftIn .x)),
      round(Array(topLeftIn .y, topRightIn .y, botRightIn .y, botLeftIn .y)),
      round(Array(topLeftOut.x, topRightOut.x, botRightOut.x, botLeftOut.x)),
      round(Array(topLeftOut.y, topRightOut.y, botRightOut.y, botLeftOut.y)),
    )

    val imgInCP   = toImageJ(imgInC)
    val procTrn   = imgInCP.getProcessor

    {
      val source  = procTrn.duplicate()
      val target  = source.createProcessor(wC, hC)
      source.setInterpolationMethod(interp) // ImageProcessor.BICUBIC)
      val mapping = new InverseTransformMapping(tp.getModel)
      mapping.mapInterpolated(source, target)
      procTrn.setPixels(target.getPixels)
    }

    val imgTrn  = procTrn.createImage()
    val sideOut = cropExtent << 1
    val imgOut  = {
      val res = new BufferedImage(sideOut, sideOut, BufferedImage.TYPE_INT_ARGB)
      val g   = res.createGraphics()
      val x   = (cropExtent - cx).toInt
      val y   = (cropExtent - cy).toInt
      g.drawImage(imgTrn, x, y, null)
      g.dispose()
      res
    }

    (circleCenters, imgOut)
  }

  def writeImage(imgOut: BufferedImage, fileOut: File)(implicit config: Config): Unit = {
    val (fmtOut, imgParam) = fileOut.extL match {
      case ext @ "png" => (ext, null)
      case _ =>
        val p = new JPEGImageWriteParam(null)
        p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
        p.setCompressionQuality(config.quality * 0.01f)
        ("jpg", p)
    }

    val imgOutT = ensureRGB(imgOut)
    val it = ImageIO.getImageWriters(ImageTypeSpecifier.createFromRenderedImage(imgOutT), fmtOut)
    if (!it.hasNext) throw new IllegalArgumentException(s"No image writer for $fmtOut")
    val imgWriter = it.next()
    fileOut.delete()
    val fos = new FileImageOutputStream(fileOut)
    try {
      imgWriter.setOutput(fos)
      imgWriter.write(null /* meta */ ,
        new IIOImage(imgOutT, null /* thumb */ , null /* meta */), imgParam)
      imgWriter.dispose()
    } finally {
      fos.close()
    }
  }
}
