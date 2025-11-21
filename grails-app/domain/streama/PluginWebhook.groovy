package streama

class PluginWebhook {

  String eventType
  String targetUrl

  static belongsTo = [plugin: ExtensionPackage]

  static constraints = {
    eventType nullable: false
    targetUrl nullable: false, url: true
  }
}
