/**
 * Copyright 2023 Frank Zeyda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import scala.sys.process._

ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "org.zeyda"

val chiselVersion = "5.0.0"

lazy val root = (project in file("."))
  .settings(
    name := "j1.chisel",
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % chiselVersion,
      "org.scalatest" %% "scalatest" % "3.2.16"
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      //"-P:chiselplugin:genBundleElements",
    ),
    addCompilerPlugin("org.chipsalliance" % "chisel-plugin" %
                      chiselVersion cross CrossVersion.full),
  )

lazy val deploy = inputKey[Unit]("Deploys generated [System]Verilog files in the 'generated' folder to the ChiselTests project on the falcon machine.")

deploy := {
  val one = (Compile / run).evaluated
  println("Calling external deploy script ...")
  "./deploy-rtl.sh" !
}
