package streama

import grails.converters.JSON
import grails.transaction.Transactional
import groovy.json.JsonSlurper

@Transactional
class MarketplaceService {

  def grailsApplication

  Map readManifest(){
    def bundledStream = this.class.classLoader.getResourceAsStream('marketplace/default-manifest.json')
    if(bundledStream){
      return new JsonSlurper().parse(bundledStream) as Map
    }

    def manifestFile = grailsApplication.mainContext?.getResource('docs/marketplace/manifest.json')?.file ?: 'docs/marketplace/manifest.json'
    def fileObj = manifestFile instanceof File ? manifestFile : new File(manifestFile.toString())
    if(fileObj?.exists()){
      return JSON.parse(fileObj.text) as Map
    }
    [plugins: [], themes: []]
  }

  Map sanitizePayload(Map payload){
    def cleaned = [:]
    cleaned.name = payload.name?.toString()?.trim()
    cleaned.author = payload.author?.toString()?.trim()
    cleaned.version = payload.version?.toString()?.trim()
    cleaned.description = payload.description?.toString()?.trim()
    cleaned.downloadUrl = (payload.downloadUrl ?: payload.download_url)?.toString()?.trim()
    cleaned.type = (payload.type ?: payload.extensionType ?: 'PLUGIN').toString().toUpperCase()
    cleaned.capabilities = payload.capabilities ?: []
    cleaned.dependencies = payload.dependencies ?: []
    cleaned.screenshots = payload.screenshots ?: []
    cleaned.config = payload.config ?: [:]
    cleaned.manifestSource = payload.manifestSource ?: 'default-manifest'
    cleaned
  }

  ExtensionPackage install(Map payload){
    def cleaned = sanitizePayload(payload)
    if(!cleaned.name){
      throw new IllegalArgumentException('Extension name is required')
    }
    if(!cleaned.author){
      throw new IllegalArgumentException('Author is required')
    }
    if(!cleaned.version){
      throw new IllegalArgumentException('Version is required')
    }
    if(!cleaned.downloadUrl){
      throw new IllegalArgumentException('Download URL is required')
    }

    URI uri
    try{
      uri = new URI(cleaned.downloadUrl)
    }catch(Exception ignored){
      throw new IllegalArgumentException('Only http/https download URLs are accepted')
    }
    if(!(uri.scheme in ['http', 'https'])){
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
