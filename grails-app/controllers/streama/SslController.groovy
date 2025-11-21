package streama

import grails.converters.JSON
import grails.transaction.Transactional

@Transactional(readOnly = true)
class SslController {

  static responseFormats = ['json']
  static allowedMethods = [requestCertificate: 'POST', renew: 'POST']

  SslCertificateService sslCertificateService

  def config(){
    respond sslCertificateService.currentConfig()
  }

  @Transactional
  def requestCertificate(){
    def body = request.JSON as Map
    try{
      def cfg = sslCertificateService.configure(body)
      respond cfg
    }catch(Exception ex){
      log.error('SSL request failed', ex)
      response.status = 400
      render([error: ex.message] as JSON)
    }
  }

  @Transactional
  def renew(){
    try{
      def cfg = sslCertificateService.renew()
      respond cfg
    }catch(Exception ex){
      response.status = 400
      render([error: ex.message] as JSON)
    }
  }
}
