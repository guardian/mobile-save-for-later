package com.gu.sfl.persistance

case class PersistenceConfig(app: String, stage: String) {
  val tableName = s"$app-$stage-articles"
}
