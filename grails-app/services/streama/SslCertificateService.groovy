package streama

import grails.transaction.Transactional

@Transactional
class SslCertificateService {

  def grailsApplication

  SslConfiguration currentConfig(){
    SslConfiguration.first()
  }

  SslConfiguration configure(Map payload){
    if(!payload.domainName || !payload.email){
      throw new IllegalArgumentException('Domain and email are required to request certificates')
    }
    SslConfiguration config = currentConfig() ?: new SslConfiguration()
    config.domainName = payload.domainName
    config.email = payload.email
    config.autoRenew = payload.autoRenew instanceof Boolean ? payload.autoRenew : true
    config.staging = payload.staging instanceof Boolean ? payload.staging : false
    config.lastAttempt = new Date()
    config.statusMessage = 'Certificate request initialized'

    def certDir = new File('config/certs')
    if(!certDir.exists()){
      certDir.mkdirs()
    }
    def certFile = new File(certDir, "${config.domainName}.pem")
    certFile.text = "---BEGIN CERTIFICATE---\nplaceholder for ${config.domainName}\n---END CERTIFICATE---"
    config.certificatePath = certFile.absolutePath
    config.lastSuccess = new Date()
    config.statusMessage = 'Certificate placeholder generated via certbot flow (simulated)'
    config.save flush: true, failOnError: true
    config
  }

  SslConfiguration renew(){
    SslConfiguration config = currentConfig()
    if(!config){
      throw new IllegalStateException('SSL configuration missing')
    }
    config.lastAttempt = new Date()
    config.statusMessage = 'Renewal scheduled'
    config.save flush: true, failOnError: true
    config
  }
}
