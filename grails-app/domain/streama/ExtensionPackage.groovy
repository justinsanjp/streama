package streama

enum ExtensionType {
  PLUGIN,
  THEME
}

class ExtensionPackage {

  String name
  String author
  String version
  String description
  String downloadUrl
  ExtensionType type
  Boolean enabled = true
  Boolean installed = true
  Date installedAt = new Date()
  String manifestSource
  String screenshotsJson
  String capabilitiesJson
  String dependenciesJson
  String configJson

  static hasMany = [webhooks: PluginWebhook]

  static constraints = {
    name nullable: false
    author nullable: false
    version nullable: false
    type nullable: false
    downloadUrl nullable: false, url: true
    description nullable: true
    manifestSource nullable: true
    screenshotsJson nullable: true, blank: true
    capabilitiesJson nullable: true, blank: true
    dependenciesJson nullable: true, blank: true
    configJson nullable: true, blank: true
  }

  static mapping = {
    description sqlType: 'text'
    screenshotsJson sqlType: 'text'
    capabilitiesJson sqlType: 'text'
    dependenciesJson sqlType: 'text'
    configJson sqlType: 'text'
  }

  List<String> getScreenshots(){
    if(!screenshotsJson){
      return []
    }
    grails.converters.JSON.parse(screenshotsJson) as List<String>
  }

  void setScreenshots(List<String> shots){
    screenshotsJson = (shots ?: []) as grails.converters.JSON
  }

  List<String> getCapabilities(){
    if(!capabilitiesJson){
      return []
    }
    grails.converters.JSON.parse(capabilitiesJson) as List<String>
  }

  void setCapabilities(List<String> caps){
    capabilitiesJson = (caps ?: []) as grails.converters.JSON
  }

  List<String> getDependencies(){
    if(!dependenciesJson){
      return []
    }
    grails.converters.JSON.parse(dependenciesJson) as List<String>
  }

  void setDependencies(List<String> deps){
    dependenciesJson = (deps ?: []) as grails.converters.JSON
  }

  Map getConfig(){
    if(!configJson){
      return [:]
    }
    grails.converters.JSON.parse(configJson) as Map
  }

  void setConfig(Map config){
    configJson = (config ?: [:]) as grails.converters.JSON
  }
}
