package streama

import grails.converters.JSON
import grails.transaction.Transactional

@Transactional(readOnly = true)
class MarketplaceController {

  static responseFormats = ['json']
  static allowedMethods = [install: 'POST', updateState: 'PUT']

  MarketplaceService marketplaceService

  def manifest(){
    respond marketplaceService.readManifest()
  }

  def installed(){
    respond marketplaceService.installedExtensions()
  }

  @Transactional
  def install(){
    def payload = request.JSON as Map
    try{
      ExtensionPackage pkg = marketplaceService.install(payload)
      respond([extension: pkg, dependencyState: marketplaceService.dependencyState(pkg)])
    }catch(Exception ex){
      log.error('Failed to install extension', ex)
      response.status = 400
      render([error: ex.message] as JSON)
    }
  }

  @Transactional
  def updateState(Long id){
    ExtensionPackage pkg = ExtensionPackage.get(id)
    if(!pkg){
      response.status = 404
      render([error: 'Extension not found'] as JSON)
      return
    }
    def body = request.JSON ?: [:]
    if(body.enabled instanceof Boolean){
      pkg.enabled = body.enabled
    }
    pkg.save flush: true, failOnError: true
    respond([extension: pkg])
  }
}
