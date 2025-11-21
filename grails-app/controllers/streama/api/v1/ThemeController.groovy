package streama.api.v1

import grails.converters.JSON
import grails.transaction.Transactional
import streama.MarketplaceService
import streama.Settings

@Transactional(readOnly = true)
class ThemeController {

  static responseFormats = ['json']
  static allowedMethods = [activate: 'POST']

  MarketplaceService marketplaceService

  def active(){
    def themeSetting = Settings.findByName('active_theme')
    def custom = Settings.findByName('theme_customization')
    def customizationValue = custom?.value
    def parsedCustomization = [:]
    if(customizationValue){
      try{
        parsedCustomization = grails.converters.JSON.parse(customizationValue) as Map
      }catch(Exception ignore){
        parsedCustomization = [:]
      }
    }
    respond([name: themeSetting?.value, customization: parsedCustomization])
  }

  def manifest(){
    def manifest = marketplaceService.readManifest()
    respond(manifest.themes ?: [])
  }

  @Transactional
  def activate(){
    def body = request.JSON as Map
    if(!body?.name){
      response.status = 400
      render([error: 'Theme name is required'] as JSON)
      return
    }
    Settings themeSetting = Settings.findByName('active_theme') ?: new Settings(name: 'active_theme', settingsKey: 'Active Theme', settingsType: 'string', required: false)
    themeSetting.value = body.name
    themeSetting.validationRequired = false
    themeSetting.save flush: true, failOnError: true

    Settings customization = Settings.findByName('theme_customization') ?: new Settings(name: 'theme_customization', settingsKey: 'Theme Customization', settingsType: 'string', required: false)
    customization.value = (body.findAll { k,v -> k != 'name' } ?: [:]) as JSON
    customization.validationRequired = false
    customization.save flush: true, failOnError: true
    respond([active: themeSetting.value, customization: grails.converters.JSON.parse(customization.value)])
  }
}
