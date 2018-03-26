import ch.qos.logback.classic.Level

def logLevel = System.getProperty("loglevel", "INFO")

appender("STDOUT", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = "%level %logger - %msg%n"
  }
}

root(OFF, ["STDOUT"])

logger("fr.spironet", Level.valueOf(logLevel), ["STDOUT"], false)