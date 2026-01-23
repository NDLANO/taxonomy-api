package millbuild

import mill._
import mill.javalib.JavaModule

object TypescriptPluginHelper {

  def fetchOpenApi(
    apiUrl: String,
    timeoutMs: Long,
    backoffMs: Long
  ): Option[String] = {
    val url = new java.net.URI(apiUrl).toURL()
    val startTime = System.currentTimeMillis()
    var result: Option[String] = None

    while (result.isEmpty && (System.currentTimeMillis() - startTime) < timeoutMs) {
      try {
        val conn = url.openConnection().asInstanceOf[java.net.HttpURLConnection]
        conn.setRequestMethod("GET")
        conn.setConnectTimeout(3000)
        conn.setReadTimeout(10000)
        val code = conn.getResponseCode

        if (code == 200) {
          val is = conn.getInputStream
          val bytes = is.readAllBytes()
          is.close()
          result = Some(new String(bytes, java.nio.charset.StandardCharsets.UTF_8))
          println(s"Successfully fetched API docs (${bytes.length} bytes)")
        } else {
          println(s"Got HTTP $code, retrying...")
        }
      } catch {
        case _: Throwable =>
          if (result.isEmpty) Thread.sleep(backoffMs)
      }
    }

    result
  }

  def startApp(
    javaCmd: String,
    classpath: String,
    mainClass: String,
    profile: String
  ): os.SubProcess = {
    os.proc(
      javaCmd,
      "-cp",
      classpath,
      mainClass,
      s"--spring.profiles.active=$profile"
    ).spawn(stdout = os.Inherit, stderr = os.Inherit)
  }

  def stopApp(proc: os.SubProcess): Unit = {
    try {
      proc.destroy()
      Thread.sleep(1000)
    } catch {
      case _: Throwable => ()
    }
  }
}

trait TypescriptPlugin extends JavaModule {

  def typescriptProfile: String = "typescript"
  def typescriptApiUrl: String = "http://localhost:5000/api-docs"
  def typescriptTimeoutMs: Long = 120000L
  def typescriptBackoffMs: Long = 500L

  def generateTypescript() = Task.Command {
    val cp = runClasspath().map(_.path)
    val classpath = cp.map(_.toString).mkString(java.io.File.pathSeparator)

    val javaCmd = sys.props.get("java.home") match {
      case Some(h) =>
        val javaPath = os.Path(h) / "bin" / "java"
        if (os.exists(javaPath)) javaPath.toString else "java"
      case None => "java"
    }

    val mainCls = mainClass().getOrElse {
      throw new RuntimeException("mainClass must be defined")
    }

    println(s"Starting Spring Boot app with profile: $typescriptProfile")

    val proc = TypescriptPluginHelper.startApp(javaCmd, classpath, mainCls, typescriptProfile)

    println(s"Polling $typescriptApiUrl until available...")

    val apiContent = TypescriptPluginHelper.fetchOpenApi(
      typescriptApiUrl,
      typescriptTimeoutMs,
      typescriptBackoffMs
    )

    try {
      apiContent match {
        case Some(body) =>
          var projectRoot = os.pwd
          while (!os.exists(projectRoot / "build.mill") && projectRoot != os.root) {
            projectRoot = projectRoot / os.up
          }
          val outFile = projectRoot / "typescript" / "openapi.json"
          os.makeDir.all(outFile / os.up)
          os.write.over(outFile, body)
          println(s"✓ Wrote OpenAPI spec to $outFile")

        case None =>
          throw new RuntimeException(
            s"Failed to fetch $typescriptApiUrl within ${typescriptTimeoutMs}ms timeout"
          )
      }
    } finally {
      println("Shutting down Spring Boot app...")
      TypescriptPluginHelper.stopApp(proc)
    }
  }
}
