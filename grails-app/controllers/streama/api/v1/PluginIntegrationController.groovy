package streama.api.v1

import grails.converters.JSON
import grails.transaction.Transactional
import streama.ExtensionPackage
import streama.MarketplaceService
import streama.PluginWebhook
import streama.ExtensionType

@Transactional(readOnly = true)
class PluginIntegrationController {

  static responseFormats = ['json']
  static allowedMethods = [register: 'POST', webhooks: 'POST']

  MarketplaceService marketplaceService

  def capabilities(){
    respond marketplaceService.listCapabilities()
  }

  @Transactional
  def register(){
    def body = request.JSON as Map
    if(!body?.name){
      response.status = 400
      render([error: 'Plugin name required'] as JSON)
      return
    }
    body.type = ExtensionType.PLUGIN.name()
    ExtensionPackage pkg
    try{
      pkg = marketplaceService.install(body)
    }catch(Exception ex){
      response.status = 400
      render([error: ex.message] as JSON)
      return
    }
    pkg.webhooks?.clear()
    (body.webhooks ?: []).each { hook ->
      if(hook.url && hook.eventType){
        pkg.addToWebhooks(new PluginWebhook(eventType: hook.eventType, targetUrl: hook.url))
      }
    }
    pkg.save flush: true, failOnError: true
    def dependencyState = marketplaceService.dependencyState(pkg)
    respond([plugin: pkg, dependencyState: dependencyState])
  }

  @Transactional
  def webhooks(){
    def body = request.JSON as Map
    if(!body?.pluginId){
      response.status = 400
      render([error: 'pluginId required'] as JSON)
      return
    }
    ExtensionPackage pkg = ExtensionPackage.get(body.pluginId as Long)
    if(!pkg){
      response.status = 404
      render([error: 'Plugin not found'] as JSON)
      return
    }
    pkg.webhooks?.clear()
    (body.webhooks ?: []).each { hook ->
      if(hook.url && hook.eventType){
        pkg.addToWebhooks(new PluginWebhook(eventType: hook.eventType, targetUrl: hook.url))
      }
    }
    pkg.save flush: true, failOnError: true
    respond([plugin: pkg, webhooks: pkg.webhooks])
  }
}
