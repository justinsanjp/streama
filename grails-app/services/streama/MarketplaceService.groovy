package streama

import grails.converters.JSON
import grails.transaction.Transactional

@Transactional
class MarketplaceService {

  def grailsApplication

  Map readManifest(){
    def manifestFile = grailsApplication.parentContext.getResource('classpath:docs/marketplace/manifest.json')?.file
    if(!manifestFile || !manifestFile.exists()){
      manifestFile = new File(grailsApplication.mainContext?.getResource('docs/marketplace/manifest.json')?.file ?: 'docs/marketplace/manifest.json')
    }
    if(!manifestFile?.exists()){
      return [plugins: [], themes: []]
    }
    JSON.parse(manifestFile.text) as Map
  }

  Map sanitizePayload(Map payload){
    def cleaned = [:]
    cleaned.name = payload.name
    cleaned.author = payload.author
    cleaned.version = payload.version
    cleaned.description = payload.description
    cleaned.downloadUrl = payload.downloadUrl ?: payload.download_url
    cleaned.type = (payload.type ?: payload.extensionType ?: 'PLUGIN').toString().toUpperCase()
    cleaned.capabilities = payload.capabilities ?: []
    cleaned.dependencies = payload.dependencies ?: []
    cleaned.screenshots = payload.screenshots ?: []
    cleaned.config = payload.config ?: [:]
    cleaned.manifestSource = payload.manifestSource ?: 'local-manifest'
    cleaned
  }

  ExtensionPackage install(Map payload){
    def cleaned = sanitizePayload(payload)
    if(!cleaned.downloadUrl?.startsWith('http')){
      throw new IllegalArgumentException('Only http/https download URLs are accepted')
    }
    def typeEnum = ExtensionType.valueOf(cleaned.type)

    ExtensionPackage extension = ExtensionPackage.findByNameAndType(cleaned.name, typeEnum) ?: new ExtensionPackage()
    extension.name = cleaned.name
    extension.author = cleaned.author
    extension.version = cleaned.version
    extension.description = cleaned.description
    extension.downloadUrl = cleaned.downloadUrl
    extension.type = typeEnum
    extension.enabled = payload.enabled instanceof Boolean ? payload.enabled : true
    extension.installed = true
    extension.manifestSource = cleaned.manifestSource
    extension.setCapabilities(cleaned.capabilities as List<String>)
    extension.setDependencies(cleaned.dependencies as List<String>)
    extension.setScreenshots(cleaned.screenshots as List<String>)
    extension.setConfig(cleaned.config as Map)
    extension.save flush: true, failOnError: true
    extension
  }

  List<ExtensionPackage> installedExtensions(){
    ExtensionPackage.list(sort: 'name')
  }

  Map listCapabilities(){
    installedExtensions().collectEntries { pkg ->
      [(pkg.name): pkg.capabilities]
    }
  }

  Map dependencyState(ExtensionPackage pkg){
    def missing = []
    pkg.dependencies?.each { dep ->
      def provider = installedExtensions().find { it.capabilities?.contains(dep) }
      if(!provider){
        missing << dep
      }
    }
    [missing: missing]
  }
}
