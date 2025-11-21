package streama

class SslConfiguration {

  String domainName
  String email
  Boolean autoRenew = true
  Boolean staging = false
  String certificatePath
  String statusMessage
  Date lastAttempt
  Date lastSuccess

  static constraints = {
    domainName nullable: false, blank: false
    email nullable: false, blank: false
    certificatePath nullable: true, blank: true
    statusMessage nullable: true, blank: true
    lastAttempt nullable: true
    lastSuccess nullable: true
  }
}
