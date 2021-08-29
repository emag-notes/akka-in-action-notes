package aia.structure.scattergather

import java.util.Date

case class PhotoMessage(id: String, photo: String, creationTime: Option[Date] = None, speed: Option[Int] = None)
